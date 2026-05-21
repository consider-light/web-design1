package com.campus.servlet;

import com.campus.util.DBUtil;
import com.campus.util.Result;
import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/api/admin/users/*")
public class AdminUserServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        try {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT id, username, role, created_at FROM users ORDER BY id";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("id", rs.getInt("id"));
                    u.put("username", rs.getString("username"));
                    u.put("role", rs.getString("role"));
                    u.put("createdAt", rs.getTimestamp("created_at"));
                    list.add(u);
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

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            resp.getWriter().print(gson.toJson(Result.error(400, "参数错误")));
            return;
        }
        int userId = Integer.parseInt(pathInfo.substring(1));

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ? AND role != 'admin'")) {
            ps.setInt(1, userId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                resp.getWriter().print(gson.toJson(Result.success(null)));
            } else {
                resp.getWriter().print(gson.toJson(Result.error(403, "不能删除管理员账户")));
            }
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
