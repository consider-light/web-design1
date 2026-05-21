package com.campus.servlet;

import com.campus.util.DBUtil;
import com.campus.util.Result;
import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {
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

        if (username.isEmpty() || password.isEmpty()) {
            resp.getWriter().print(gson.toJson(Result.error(400, "用户名和密码不能为空")));
            return;
        }

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashed);
            ps.executeUpdate();
            resp.getWriter().print(gson.toJson(Result.success(null)));
        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getErrorCode() == 1062) {
                resp.getWriter().print(gson.toJson(Result.error(400, "用户名已存在")));
            } else {
                resp.getWriter().print(gson.toJson(Result.error(500, "数据库错误")));
            }
        }
    }
}
