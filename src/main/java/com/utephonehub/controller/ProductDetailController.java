package com.utephonehub.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Product Detail Page Controller
 * Handle routing cho trang chi tiết sản phẩm
 * URL pattern: /products/{id}
 */
@WebServlet(urlPatterns = {"/products/*"})
public class ProductDetailController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        // If pathInfo is null or "/", forward to product list page
        if (pathInfo == null || pathInfo.equals("/")) {
            request.getRequestDispatcher("/WEB-INF/views/product/list.jsp").forward(request, response);
            return;
        }
        
        // Extract product ID from path: /products/123
        String productIdStr = pathInfo.substring(1); // Remove leading "/"
        
        try {
            // Validate product ID is a number
            Long.parseLong(productIdStr);
            
            // Forward to product detail page
            request.getRequestDispatcher("/WEB-INF/views/product/detail.jsp").forward(request, response);
            
        } catch (NumberFormatException e) {
            // Invalid product ID, return 404
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Sản phẩm không tồn tại");
        }
    }
}

