/**
 * Login Page JavaScript
 * Handles login and register form submissions
 */

// Toggle between login and register forms
const wrapper = document.querySelector(".wrapper");
const registerLink = document.querySelector(".register-link");
const loginLink = document.querySelector(".login-link");
const btnPopup = document.querySelector(".btnLogin-popup");
const iconClose = document.querySelector(".icon-close");

if (registerLink) {
  registerLink.addEventListener("click", () => {
    wrapper?.classList.add("active");
  });
}

if (loginLink) {
  loginLink.addEventListener("click", () => {
    wrapper?.classList.remove("active");
  });
}

if (btnPopup) {
  btnPopup.addEventListener("click", () => {
    wrapper?.classList.add("active-popup");
  });
}

if (iconClose) {
  iconClose.addEventListener("click", () => {
    wrapper?.classList.remove("active-popup");
  });
}

// Login form submission
const loginForm = document.getElementById("loginForm");
if (loginForm) {
  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPassword").value;

    // Validate
    if (!email || !password) {
      showMessage("errorMessage", "Vui lòng nhập đầy đủ thông tin", "error");
      return;
    }

    try {
      // Call API
      const response = await fetch(`${contextPath}/api/v1/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email, password }),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        // Save tokens and user info
        localStorage.setItem("accessToken", data.data.accessToken);
        localStorage.setItem("refreshToken", data.data.refreshToken);
        localStorage.setItem("user", JSON.stringify(data.data.user));

        // Show success message
        showMessage("successMessage", "Đăng nhập thành công! Đang chuyển hướng...", "success");

        // Redirect based on role
        setTimeout(() => {
          if (data.data.user.role === "admin") {
            window.location.href = `${contextPath}/admin/dashboard`;
          } else {
            window.location.href = `${contextPath}/`;
          }
        }, 1000);
      } else {
        showMessage("errorMessage", data.message || "Đăng nhập thất bại", "error");
      }
    } catch (error) {
      console.error("Login error:", error);
      showMessage("errorMessage", "Lỗi kết nối. Vui lòng thử lại sau.", "error");
    }
  });
}

// Register form submission
const registerForm = document.getElementById("registerForm");
if (registerForm) {
  registerForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const fullName = document.getElementById("registerName").value.trim();
    const email = document.getElementById("registerEmail").value.trim();
    const password = document.getElementById("registerPassword").value;
    const confirmPassword = document.getElementById("registerConfirmPassword").value;
    const termsCheckbox = document.getElementById("registerTerms");

    // Validate
    if (!fullName || !email || !password || !confirmPassword) {
      showMessage("registerErrorMessage", "Vui lòng nhập đầy đủ thông tin", "error");
      return;
    }

    if (password !== confirmPassword) {
      showMessage("registerErrorMessage", "Mật khẩu xác nhận không khớp", "error");
      return;
    }

    if (password.length < 8) {
      showMessage("registerErrorMessage", "Mật khẩu phải có ít nhất 8 ký tự", "error");
      return;
    }

    if (!termsCheckbox.checked) {
      showMessage("registerErrorMessage", "Vui lòng đồng ý với điều khoản", "error");
      return;
    }

    try {
      // Call API
      const response = await fetch(`${contextPath}/api/v1/auth/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ fullName, email, password }),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        showMessage("registerSuccessMessage", "Đăng ký thành công! Vui lòng đăng nhập.", "success");

        // Switch to login form after 2 seconds
        setTimeout(() => {
          wrapper?.classList.remove("active");
          registerForm.reset();
        }, 2000);
      } else {
        showMessage("registerErrorMessage", data.message || "Đăng ký thất bại", "error");
      }
    } catch (error) {
      console.error("Register error:", error);
      showMessage("registerErrorMessage", "Lỗi kết nối. Vui lòng thử lại sau.", "error");
    }
  });
}

// Forgot password handler
const forgotPasswordLink = document.getElementById("forgotPasswordLink");
if (forgotPasswordLink) {
  forgotPasswordLink.addEventListener("click", (e) => {
    e.preventDefault();
    alert("Chức năng quên mật khẩu đang được phát triển. Vui lòng liên hệ admin để được hỗ trợ.");
  });
}

// Show message function
function showMessage(elementId, message, type) {
  const messageElement = document.getElementById(elementId);
  if (messageElement) {
    messageElement.textContent = message;
    messageElement.style.display = "block";
    messageElement.className = type === "error" ? "error-message" : "success-message";

    // Hide after 5 seconds
    setTimeout(() => {
      messageElement.style.display = "none";
    }, 5000);
  }
}

