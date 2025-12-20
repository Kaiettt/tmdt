<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%> <%@ taglib prefix="c"
uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>404 - Không tìm thấy trang | UTE Phone Hub</title>

    <!-- Favicon -->
    <link
      rel="icon"
      type="image/png"
      href="${pageContext.request.contextPath}/static/favicon.png"
    />
    <link
      rel="shortcut icon"
      type="image/png"
      href="${pageContext.request.contextPath}/static/favicon.png"
    />

    <style>
      * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
      }

      body {
        font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #333;
      }

      .error-container {
        text-align: center;
        background: white;
        padding: 3rem 2rem;
        border-radius: 20px;
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
        max-width: 500px;
        width: 90%;
      }

      .error-code {
        font-size: 8rem;
        font-weight: bold;
        color: #667eea;
        margin-bottom: 1rem;
        text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
      }

      .error-title {
        font-size: 2rem;
        margin-bottom: 1rem;
        color: #333;
      }

      .error-message {
        font-size: 1.1rem;
        color: #666;
        margin-bottom: 2rem;
        line-height: 1.6;
      }

      .btn {
        display: inline-block;
        padding: 12px 30px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        text-decoration: none;
        border-radius: 25px;
        font-weight: 500;
        transition: transform 0.3s ease, box-shadow 0.3s ease;
      }

      .btn:hover {
        transform: translateY(-2px);
        box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
      }

      .home-link {
        margin-right: 1rem;
      }

      .back-link {
        background: transparent;
        color: #667eea;
        border: 2px solid #667eea;
      }

      .back-link:hover {
        background: #667eea;
        color: white;
      }

      @media (max-width: 768px) {
        .error-code {
          font-size: 6rem;
        }

        .error-title {
          font-size: 1.5rem;
        }

        .btn {
          display: block;
          margin: 0.5rem 0;
          text-align: center;
        }
      }
    </style>
  </head>
  <body>
    <div class="error-container">
      <div class="error-code">404</div>
      <h1 class="error-title">Trang không tồn tại</h1>
      <p class="error-message">
        Xin lỗi, trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển. Vui
        lòng kiểm tra lại URL hoặc quay về trang chủ.
      </p>
      <div>
        <a href="/" class="btn home-link">Trang chủ</a>
        <a href="javascript:history.back()" class="btn back-link">Quay lại</a>
      </div>
    </div>
  </body>
</html>
