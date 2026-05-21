package com.campus.servlet;

import com.campus.util.DBUtil;
import com.campus.util.Result;
import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/articles/*")
public class ArticleServlet extends HttpServlet {
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
            } else {
                resp.getWriter().print(gson.toJson(Result.error(404, "接口不存在")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "服务器错误")));
        }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String categoryIdStr = req.getParameter("categoryId");
            StringBuilder sql = new StringBuilder(
                "SELECT a.id, a.title, a.content, a.category_id, a.author_id, a.view_count, a.created_at, " +
                "c.name AS category_name, u.username AS author_name " +
                "FROM articles a " +
                "LEFT JOIN categories c ON a.category_id = c.id " +
                "LEFT JOIN users u ON a.author_id = u.id WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
                sql.append(" AND a.category_id = ?");
                params.add(Integer.parseInt(categoryIdStr));
            }
            sql.append(" ORDER BY a.created_at DESC");

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
                    item.put("content", rs.getString("content"));
                    item.put("summary", summary(rs.getString("content")));
                    item.put("categoryId", rs.getInt("category_id"));
                    item.put("categoryName", rs.getString("category_name"));
                    item.put("authorId", rs.getInt("author_id"));
                    item.put("authorName", rs.getString("author_name"));
                    item.put("viewCount", rs.getInt("view_count"));
                    item.put("createdAt", rs.getTimestamp("created_at"));
                    list.add(item);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("records", list);
            resp.getWriter().print(gson.toJson(Result.success(result)));
        } catch (SQLException e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "数据库错误")));
        }
    }

    private void handleDetail(HttpServletRequest req, HttpServletResponse resp, int id) throws IOException {
        // Increment view count
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE articles SET view_count = view_count + 1 WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {}

        String sql = "SELECT a.id, a.title, a.content, a.category_id, a.author_id, a.view_count, a.created_at, " +
                     "c.name AS category_name, u.username AS author_name " +
                     "FROM articles a LEFT JOIN categories c ON a.category_id = c.id LEFT JOIN users u ON a.author_id = u.id WHERE a.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", rs.getInt("id"));
                item.put("title", rs.getString("title"));
                item.put("content", rs.getString("content"));
                item.put("categoryId", rs.getInt("category_id"));
                item.put("categoryName", rs.getString("category_name"));
                item.put("authorId", rs.getInt("author_id"));
                item.put("authorName", rs.getString("author_name"));
                item.put("viewCount", rs.getInt("view_count"));
                item.put("createdAt", rs.getTimestamp("created_at"));
                resp.getWriter().print(gson.toJson(Result.success(item)));
            } else {
                resp.getWriter().print(gson.toJson(Result.error(404, "文章不存在")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "数据库错误")));
        }
    }

    private String summary(String content) {
        if (content == null) return "";
        return content.replaceAll("[#*`>\\-\\[\\]()!|{}\\\\]", "").substring(0, Math.min(120, content.length()));
    }
}
