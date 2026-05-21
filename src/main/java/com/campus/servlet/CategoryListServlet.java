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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/resources/categories")
public class CategoryListServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, name FROM categories ORDER BY sort_order";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> cat = new HashMap<>();
                cat.put("id", rs.getInt("id"));
                cat.put("name", rs.getString("name"));
                list.add(cat);
            }
            resp.getWriter().print(gson.toJson(Result.success(list)));
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "数据库错误")));
        }
    }
}