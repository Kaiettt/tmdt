package com.utephonehub.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Admin Page Controller
 * Handle routing cho các trang admin view
 */
@WebServlet(urlPatterns = {
    "/admin",
    "/admin/",
    "/admin/dashboard", 
    "/admin/products", 
    "/admin/orders", 
    "/admin/users", 
    "/admin/categories", 
    "/admin/brands", 
    "/admin/vouchers"
})
public class AdminPageController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String servletPath = request.getServletPath();
        
        // Admin pages routing
        switch (servletPath) {
            case "/admin":
            case "/admin/":
            case "/admin/dashboard":
                request.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(request, response);
                break;
                
            case "/admin/products":
                request.getRequestDispatcher("/WEB-INF/views/admin/products.jsp").forward(request, response);
                break;
                
            case "/admin/orders":
                request.getRequestDispatcher("/WEB-INF/views/admin/orders.jsp").forward(request, response);
                break;
                
            case "/admin/users":
                request.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(request, response);
                break;
                
            case "/admin/categories":
                request.getRequestDispatcher("/WEB-INF/views/admin/categories.jsp").forward(request, response);
                break;
                
            case "/admin/brands":
                request.getRequestDispatcher("/WEB-INF/views/admin/brands.jsp").forward(request, response);
                break;
                
            case "/admin/vouchers":
                request.getRequestDispatcher("/WEB-INF/views/admin/vouchers.jsp").forward(request, response);
                break;
                
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}

