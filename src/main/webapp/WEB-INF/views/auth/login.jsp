<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%> <%@ taglib prefix="c"
uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Đăng nhập - UTE Phone Hub</title>

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

    <!-- CSS -->
    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/static/css/components/auth.css"
    />

    <!-- Google Fonts - Roboto -->
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link
      href="https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,100;0,300;0,400;0,500;0,700;0,900;1,100;1,300;1,400;1,500;1,700;1,900&display=swap"
      rel="stylesheet"
    />

    <!-- Boxicons -->
    <link
      href="https://unpkg.com/boxicons@2.1.4/css/boxicons.min.css"
      rel="stylesheet"
    />

    <!-- Favicon -->
    <link
      rel="icon"
      type="image/x-icon"
      href="${pageContext.request.contextPath}/static/images/favicon.ico"
    />

    <!-- Check if already logged in and redirect -->
    <script>
      // Check if user is already logged in
      (function () {
        const token = localStorage.getItem("accessToken");
        const user = localStorage.getItem("user");

        if (token && user) {
          // User is already logged in, redirect to home
          console.log("User already logged in, redirecting to home...");
          window.location.href = "${pageContext.request.contextPath}/";
        }
      })();
    </script>
  </head>
  <body>
    <!-- Back to Home Link -->
    <div class="back-to-home">
      <a href="${pageContext.request.contextPath}/">
        <i class="bx bx-arrow-back"></i>
        Về trang chủ
      </a>
    </div>

    <div class="container">
      <!-- Login Form -->
      <div class="form-box login">
        <form id="loginForm">
          <h1>Đăng nhập</h1>

          <div class="input-box">
            <input
              type="text"
              id="username"
              name="username"
              placeholder="Tên đăng nhập hoặc email"
              required
            />
            <i class="bx bxs-user"></i>
          </div>

          <div class="input-box">
            <input
              type="password"
              id="password"
              name="password"
              placeholder="Mật khẩu"
              required
            />
            <i class="bx bxs-lock-alt"></i>
          </div>

          <div class="forgot-link">
            <a href="#" id="forgotPasswordLink">Quên mật khẩu?</a>
          </div>

          <button type="submit" class="btn">Đăng nhập</button>

          <div
            class="error-message"
            id="errorMessage"
            style="display: none"
          ></div>
          <div
            class="success-message"
            id="successMessage"
            style="display: none"
          ></div>

          <p>hoặc đăng nhập với</p>
          <div class="social-icons">
            <a href="<%= request.getContextPath() %>/oauth2/google"><i class="bx bxl-google"></i></a>
          </div>
        </form>
      </div>

      <!-- Register Form -->
      <div class="form-box register">
        <form id="registerForm">
          <h1>Đăng ký</h1>

          <div class="input-box">
            <input
              type="text"
              id="regUsername"
              name="username"
              placeholder="Tên đăng nhập"
              required
            />
            <i class="bx bxs-user"></i>
          </div>

          <div class="input-box">
            <input
              type="text"
              id="regFullName"
              name="fullName"
              placeholder="Họ và tên"
              required
            />
            <i class="bx bxs-user-detail"></i>
          </div>

          <div class="input-box">
            <input
              type="email"
              id="regEmail"
              name="email"
              placeholder="Email"
              required
            />
            <i class="bx bxs-envelope"></i>
          </div>

          <div class="input-box">
            <input
              type="password"
              id="regPassword"
              name="password"
              placeholder="Mật khẩu"
              required
            />
            <i class="bx bxs-lock-alt"></i>
          </div>

          <div class="input-box">
            <input
              type="password"
              id="registerConfirmPassword"
              name="confirmPassword"
              placeholder="Xác nhận mật khẩu"
              required
            />
            <i class="bx bxs-lock-alt"></i>
          </div>

          <button type="submit" class="btn">Đăng ký</button>

          <div
            class="error-message"
            id="regErrorMessage"
            style="display: none"
          ></div>
          <div
            class="success-message"
            id="regSuccessMessage"
            style="display: none"
          ></div>

          <p>hoặc đăng ký với</p>
          <div class="social-icons">
            <a href="<%= request.getContextPath() %>/oauth2/google"><i class="bx bxl-google"></i></a>
          </div>
        </form>
      </div>

      <!-- Toggle Panel -->
      <div class="toggle-box">
        <div class="toggle-panel toggle-left">
          <h1>Xin chào!</h1>
          <p>Chưa có tài khoản?</p>
          <button class="btn register-btn">Đăng ký</button>
        </div>

        <div class="toggle-panel toggle-right">
          <h1>Chào mừng trở lại!</h1>
          <p>Đã có tài khoản?</p>
          <button class="btn login-btn">Đăng nhập</button>
        </div>
      </div>
    </div>

    <!-- Forgot Password Modal -->
    <div id="forgotPasswordModal" class="forgot-password-modal">
      <div class="forgot-password-content">
        <span class="close-forgot-password">&times;</span>

        <!-- Step 1: Enter Email -->
        <div class="forgot-step" id="step1">
          <h2>Quên mật khẩu?</h2>
          <p>Nhập email của bạn để nhận mã xác nhận</p>

          <form id="forgotPasswordForm">
            <div class="input-box">
              <input
                type="email"
                id="forgotEmail"
                name="email"
                placeholder="Email của bạn"
                required
              />
              <i class="bx bxs-envelope"></i>
            </div>

            <button type="submit" class="btn" id="sendOtpBtn">
              <span class="btn-text">Gửi mã xác nhận</span>
              <span class="btn-loading" style="display: none">
                <i class="bx bx-loader-alt bx-spin"></i> Đang gửi...
              </span>
            </button>

            <div
              class="error-message"
              id="forgotErrorMessage"
              style="display: none"
            ></div>
          </form>
        </div>

        <!-- Step 2: Enter OTP -->
        <div class="forgot-step" id="step2" style="display: none">
          <h2>Nhập mã xác nhận</h2>
          <p>Chúng tôi đã gửi mã xác nhận đến email của bạn</p>

          <form id="verifyOtpForm">
            <div class="input-box">
              <input
                type="text"
                id="otpCode"
                name="otp"
                placeholder="Nhập mã 6 số"
                maxlength="6"
                required
              />
              <i class="bx bxs-lock"></i>
            </div>

            <button type="submit" class="btn" id="verifyOtpBtn">
              <span class="btn-text">Xác nhận</span>
              <span class="btn-loading" style="display: none">
                <i class="bx bx-loader-alt bx-spin"></i> Đang xác nhận...
              </span>
            </button>

            <div class="resend-otp">
              Không nhận được mã? <a href="#" id="resendOtp">Gửi lại</a>
            </div>

            <div
              class="error-message"
              id="otpErrorMessage"
              style="display: none"
            ></div>
          </form>
        </div>

        <!-- Step 3: Reset Password -->
        <div class="forgot-step" id="step3" style="display: none">
          <h2>Đặt lại mật khẩu</h2>
          <p>Nhập mật khẩu mới của bạn</p>

          <form id="resetPasswordForm">
            <div class="input-box">
              <input
                type="password"
                id="newPassword"
                name="newPassword"
                placeholder="Mật khẩu mới"
                required
              />
              <i class="bx bxs-lock-alt"></i>
            </div>

            <div class="input-box">
              <input
                type="password"
                id="resetConfirmPassword"
                name="confirmPassword"
                placeholder="Xác nhận mật khẩu"
                required
              />
              <i class="bx bxs-lock-alt"></i>
            </div>

            <button type="submit" class="btn" id="resetPasswordBtn">
              <span class="btn-text">Đặt lại mật khẩu</span>
              <span class="btn-loading" style="display: none">
                <i class="bx bx-loader-alt bx-spin"></i> Đang xử lý...
              </span>
            </button>

            <div
              class="error-message"
              id="resetErrorMessage"
              style="display: none"
            ></div>
            <div
              class="success-message"
              id="resetSuccessMessage"
              style="display: none"
            ></div>
          </form>
        </div>
      </div>
    </div>

    <!-- JavaScript for login page -->
    <script>
      const contextPath = '${pageContext.request.contextPath}';
    </script>
    <script src="${pageContext.request.contextPath}/static/js/utils.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/api.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/auth.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/pages/login.js"></script>
  </body>
</html>
