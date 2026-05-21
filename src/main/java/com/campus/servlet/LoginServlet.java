package com.campus.servlet;

import com.campus.util.DBUtil;
import com.campus.util.Result;
import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        String json = sb.toString();

        com.google.gson.JsonObject obj = gson.fromJson(json, com.google.gson.JsonObject.class);
        if (obj == null || !obj.has("username") || !obj.has("password")) {
            resp.getWriter().print(gson.toJson(Result.error(400, "缺少用户名或密码")));
            return;
        }
        String username = obj.get("username").getAsString().trim();
        String password = obj.get("password").getAsString();

        String sql = "SELECT id, username, password, role FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hashed = rs.getString("password");
                if (BCrypt.checkpw(password, hashed)) {
                    HttpSession session = req.getSession();
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", rs.getInt("id"));
                    userInfo.put("username", rs.getString("username"));
                    userInfo.put("role", rs.getString("role"));
                    session.setAttribute("user", userInfo);

                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("username", rs.getString("username"));
                    data.put("role", rs.getString("role"));
                    resp.getWriter().print(gson.toJson(Result.success(data)));
                } else {
                    resp.getWriter().print(gson.toJson(Result.error(401, "密码错误")));
                }
            } else {
                resp.getWriter().print(gson.toJson(Result.error(401, "用户不存在")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.getWriter().print(gson.toJson(Result.error(500, "服务器错误")));
        }
    }
}