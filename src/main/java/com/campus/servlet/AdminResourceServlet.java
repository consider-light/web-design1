package com.campus.servlet;

import com.campus.util.DBUtil;
import com.campus.util.Result;
import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/api/admin/resources/*")
public class AdminResourceServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            resp.getWriter().print(gson.toJson(Result.error(400, "参数错误")));
            return;
        }
        int resourceId = Integer.parseInt(pathInfo.substring(1));

        try (Connection conn = DBUtil.getConnection()) {
            // Get file path before deleting
            String filePath = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT file_path FROM resources WHERE id = ?")) {
                ps.setInt(1, resourceId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) filePath = rs.getString("file_path");
            }

            // Delete from DB
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM resources WHERE id = ?")) {
                ps.setInt(1, resourceId);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    resp.getWriter().print(gson.toJson(Result.error(404, "资源不存在")));
                    return;
                }
            }

            // Delete file from disk
            if (filePath != null && !filePath.isEmpty()) {
                File file = new File(getServletContext().getRealPath("/"), filePath);
                if (file.exists()) file.delete();
            }

            resp.getWriter().print(gson.toJson(Result.success(null)));
        } catch (SQLException e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "数据库错误")));
        }
    }

    private boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.getWriter().print(gson.toJson(Result.error(401, "未登录")));
            return false;
        }
        Map<String, Object> user = (Map<String, Object>) session.getAttribute("user");
        if (user == null || !"admin".equals(user.get("role"))) {
            resp.getWriter().print(gson.toJson(Result.error(403, "权限不足")));
            return false;
        }
        return true;
    }
}
