package com.campus.servlet;

import com.campus.util.Result;
import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/user/current")
public class CurrentUserServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.getWriter().print(gson.toJson(Result.error(401, "未登录")));
            return;
        }
        Map<String, Object> user = (Map<String, Object>) session.getAttribute("user");
        if (user == null) {
            resp.getWriter().print(gson.toJson(Result.error(401, "未登录")));
        } else {
            resp.getWriter().print(gson.toJson(Result.success(user)));
        }
    }
}