package com.campus.servlet;

import com.campus.util.DBUtil;
import com.campus.util.Result;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

@WebServlet("/api/resources/*")
@MultipartConfig(maxFileSize = 52428800, maxRequestSize = 52428800)
public class ResourceServlet extends HttpServlet {

    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(
        ".zip", ".rar", ".7z", ".pdf", ".png", ".jpg", ".jpeg", ".gif", ".txt", ".md", ".exe", ".msi", ".apk"
    ));
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleList(req, resp);
            } else if (pathInfo.matches("/\\d+")) {
                handleDetail(req, resp, Integer.parseInt(pathInfo.substring(1)));
            } else if (pathInfo.matches("/\\d+/download")) {
                handleDownload(req, resp, Integer.parseInt(pathInfo.substring(1, pathInfo.indexOf('/', 1))));
            } else {
                resp.getWriter().print(gson.toJson(Result.error(404, "接口不存在")));
            }
        } catch (NumberFormatException e) {
            resp.getWriter().print(gson.toJson(Result.error(400, "参数错误")));
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "服务器错误")));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.getWriter().print(gson.toJson(Result.error(401, "请先登录")));
            return;
        }
        Map<String, Object> user = (Map<String, Object>) session.getAttribute("user");

        try {
            String title = req.getParameter("title");
            String description = req.getParameter("description");
            String categoryIdStr = req.getParameter("categoryId");
            Part filePart = req.getPart("file");

            if (title == null || title.trim().isEmpty() || categoryIdStr == null || filePart == null || filePart.getSize() == 0) {
                resp.getWriter().print(gson.toJson(Result.error(400, "缺少必填字段")));
                return;
            }

            String originalName = filePart.getSubmittedFileName();
            String ext = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0) ext = originalName.substring(dot).toLowerCase();
            if (!ALLOWED_EXT.contains(ext)) {
                resp.getWriter().print(gson.toJson(Result.error(400, "不支持的文件类型: " + ext)));
                return;
            }

            String uuidName = UUID.randomUUID().toString() + ext;
            String uploadDir = getServletContext().getRealPath("/uploads/");
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, uuidName);
            filePart.write(dest.getAbsolutePath());

            long fileSize = dest.length();
            int categoryId = Integer.parseInt(categoryIdStr);
            int uploaderId = (Integer) user.get("id");

            String sql = "INSERT INTO resources (title, description, category_id, file_path, file_size, original_name, uploader_id) VALUES (?,?,?,?,?,?,?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, title.trim());
                ps.setString(2, description != null ? description.trim() : "");
                ps.setInt(3, categoryId);
                ps.setString(4, "uploads/" + uuidName);
                ps.setLong(5, fileSize);
                ps.setString(6, originalName);
                ps.setInt(7, uploaderId);
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                Map<String, Object> data = new HashMap<>();
                if (rs.next()) {
                    data.put("id", rs.getInt(1));
                }
                data.put("title", title.trim());
                resp.getWriter().print(gson.toJson(Result.success(data)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "数据库错误")));
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "服务器错误")));
        }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int page = parseInt(req.getParameter("page"), 1);
            int size = parseInt(req.getParameter("size"), 10);
            String keyword = req.getParameter("keyword");
            String categoryIdStr = req.getParameter("categoryId");
            String sort = req.getParameter("sort"); // "latest" or "downloads"

            StringBuilder sql = new StringBuilder(
                "SELECT r.id, r.title, r.description, r.category_id, r.file_size, r.original_name, r.download_count, r.uploader_id, r.created_at, " +
                "c.name AS category_name, u.username AS uploader_name " +
                "FROM resources r " +
                "LEFT JOIN categories c ON r.category_id = c.id " +
                "LEFT JOIN users u ON r.uploader_id = u.id WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
                sql.append(" AND r.category_id = ?");
                params.add(Integer.parseInt(categoryIdStr));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append(" AND r.title LIKE ?");
                params.add("%" + keyword.trim() + "%");
            }

            String orderBy = "r.created_at DESC";
            if ("downloads".equals(sort)) orderBy = "r.download_count DESC";
            sql.append(" ORDER BY ").append(orderBy);
            sql.append(" LIMIT ? OFFSET ?");
            params.add(size);
            params.add((page - 1) * size);

            List<Map<String, Object>> list = new ArrayList<>();
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("title", rs.getString("title"));
                    item.put("description", rs.getString("description"));
                    item.put("categoryId", rs.getInt("category_id"));
                    item.put("categoryName", rs.getString("category_name"));
                    item.put("fileSize", rs.getLong("file_size"));
                    item.put("originalName", rs.getString("original_name"));
                    item.put("downloadCount", rs.getInt("download_count"));
                    item.put("uploaderId", rs.getInt("uploader_id"));
                    item.put("uploaderName", rs.getString("uploader_name"));
                    item.put("createdAt", rs.getTimestamp("created_at"));
                    list.add(item);
                }
            }

            // Count total
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM resources WHERE 1=1");
            List<Object> countParams = new ArrayList<>();
            if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
                countSql.append(" AND category_id = ?");
                countParams.add(Integer.parseInt(categoryIdStr));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                countSql.append(" AND title LIKE ?");
                countParams.add("%" + keyword.trim() + "%");
            }

            int total = 0;
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(countSql.toString())) {
                for (int i = 0; i < countParams.size(); i++) {
                    ps.setObject(i + 1, countParams.get(i));
                }
                ResultSet rs = ps.executeQuery();
                if (rs.next()) total = rs.getInt(1);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("records", list);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (int) Math.ceil((double) total / size));
            resp.getWriter().print(gson.toJson(Result.success(result)));
        } catch (SQLException e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "数据库错误")));
        }
    }

    private void handleDetail(HttpServletRequest req, HttpServletResponse resp, int id) throws IOException {
        String sql = "SELECT r.id, r.title, r.description, r.category_id, r.file_size, r.original_name, r.download_count, r.uploader_id, r.created_at, " +
                     "c.name AS category_name, u.username AS uploader_name " +
                     "FROM resources r LEFT JOIN categories c ON r.category_id = c.id LEFT JOIN users u ON r.uploader_id = u.id WHERE r.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", rs.getInt("id"));
                item.put("title", rs.getString("title"));
                item.put("description", rs.getString("description"));
                item.put("categoryId", rs.getInt("category_id"));
                item.put("categoryName", rs.getString("category_name"));
                item.put("fileSize", rs.getLong("file_size"));
                item.put("originalName", rs.getString("original_name"));
                item.put("downloadCount", rs.getInt("download_count"));
                item.put("uploaderId", rs.getInt("uploader_id"));
                item.put("uploaderName", rs.getString("uploader_name"));
                item.put("createdAt", rs.getTimestamp("created_at"));
                resp.getWriter().print(gson.toJson(Result.success(item)));
            } else {
                resp.getWriter().print(gson.toJson(Result.error(404, "资源不存在")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "数据库错误")));
        }
    }

    private void handleDownload(HttpServletRequest req, HttpServletResponse resp, int id) throws IOException {
        String sql = "SELECT file_path, original_name, download_count FROM resources WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String filePath = rs.getString("file_path");
                String originalName = rs.getString("original_name");

                // Security: reject path traversal
                if (filePath == null || filePath.contains("..") || filePath.contains("/") && filePath.indexOf("uploads/") != 0) {
                    resp.getWriter().print(gson.toJson(Result.error(404, "文件不存在")));
                    return;
                }

                File file = new File(getServletContext().getRealPath("/"), filePath);
                if (!file.exists() || !file.isFile()) {
                    resp.getWriter().print(gson.toJson(Result.error(404, "文件不存在")));
                    return;
                }

                // Increment download count
                try (Connection c2 = DBUtil.getConnection();
                     PreparedStatement ps2 = c2.prepareStatement("UPDATE resources SET download_count = download_count + 1 WHERE id = ?")) {
                    ps2.setInt(1, id);
                    ps2.executeUpdate();
                }

                resp.setContentType("application/octet-stream");
                resp.setHeader("Content-Disposition", "attachment; filename=\"" +
                    new String(originalName.getBytes("UTF-8"), "ISO-8859-1") + "\"");
                resp.setContentLengthLong(file.length());

                try (FileInputStream fis = new FileInputStream(file);
                     OutputStream os = resp.getOutputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = fis.read(buf)) != -1) os.write(buf, 0, n);
                }
            } else {
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(gson.toJson(Result.error(404, "资源不存在")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().print(gson.toJson(Result.error(500, "数据库错误")));
        }
    }

    private int parseInt(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}
