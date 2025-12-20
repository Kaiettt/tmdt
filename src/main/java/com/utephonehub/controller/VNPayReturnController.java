package com.utephonehub.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/payment/return")
public class VNPayReturnController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // VNPay sends orderId as vnp_TxnRef
        String vnpTxnRef = req.getParameter("vnp_TxnRef");

        if (vnpTxnRef != null && !vnpTxnRef.isEmpty()) {
            // ✅ Redirect directly to success page
            resp.sendRedirect(
                req.getContextPath() + "/orders/" + vnpTxnRef + "?success=true"
            );
        } else {
            // Fallback if missing orderId
            resp.sendRedirect(req.getContextPath() + "/orders?success=true");
        }
    }
}
