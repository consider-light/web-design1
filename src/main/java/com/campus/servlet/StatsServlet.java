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
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/stats")
public class StatsServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            Map<String, Object> stats = new HashMap<>();
            try (Connection conn = DBUtil.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM resources");
                     ResultSet rs = ps.executeQuery()) {
                    stats.put("resourceCount", rs.next() ? rs.getInt(1) : 0);
                }
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM articles");
                     ResultSet rs = ps.executeQuery()) {
                    stats.put("articleCount", rs.next() ? rs.getInt(1) : 0);
                }
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
                     ResultSet rs = ps.executeQuery()) {
                    stats.put("userCount", rs.next() ? rs.getInt(1) : 0);
                }
            }
            resp.getWriter().print(gson.toJson(Result.success(stats)));
        } catch (SQLException e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "数据库错误")));
        }
    }
}
