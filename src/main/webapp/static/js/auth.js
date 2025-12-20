// Toggle between login and register forms (only on auth pages)
const container = document.querySelector(".container");
const registerBtn = document.querySelector(".register-btn");
const loginBtn = document.querySelector(".login-btn");

if (registerBtn && loginBtn && container) {
  registerBtn.addEventListener("click", () => {
    container.classList.add("active");
  });

  loginBtn.addEventListener("click", () => {
    container.classList.remove("active");
  });
}

// ============================================
// TOAST NOTIFICATION SYSTEM
// ============================================

// Create toast container if it doesn't exist
function ensureToastContainer() {
  let container = document.querySelector(".toast-container");
  if (!container) {
    container = document.createElement("div");
    container.className = "toast-container";
    document.body.appendChild(container);
  }
  return container;
}

// Show toast notification
function showToast(message, type = "info", title = null) {
  const container = ensureToastContainer();

  const icons = {
    success: "✓",
    error: "✕",
    warning: "⚠",
    info: "ℹ",
  };

  const titles = {
    success: title || "Thành công",
    error: title || "Lỗi",
    warning: title || "Cảnh báo",
    info: title || "Thông báo",
  };

  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <div class="toast-icon">${icons[type]}</div>
    <div class="toast-content">
      <div class="toast-title">${titles[type]}</div>
      <div class="toast-message">${message}</div>
    </div>
    <button class="toast-close">&times;</button>
  `;

  container.appendChild(toast);

  // Auto close after 5 seconds
  const autoCloseTimer = setTimeout(() => {
    closeToast(toast);
  }, 5000);

  // Close button handler
  toast.querySelector(".toast-close").addEventListener("click", () => {
    clearTimeout(autoCloseTimer);
    closeToast(toast);
  });
}

// Close toast with animation
function closeToast(toast) {
  toast.classList.add("hiding");
  setTimeout(() => {
    toast.remove();
  }, 300);
}

// Utility functions
function showMessage(elementId, message, isError = false) {
  const element = document.getElementById(elementId);
  element.textContent = message;
  element.style.display = "block";

  // Hide after 5 seconds
  setTimeout(() => {
    element.style.display = "none";
  }, 5000);
}

function hideMessage(elementId) {
  const element = document.getElementById(elementId);
  if (element) {
    element.style.display = "none";
  }
}

// Login form handler
const loginForm = document.getElementById("loginForm");
if (loginForm) {
  loginForm.addEventListener("submit", async function (e) {
    e.preventDefault();

    hideMessage("errorMessage");
    hideMessage("successMessage");

    const formData = new FormData(this);
    const loginData = {
      username: formData.get("username"),
      password: formData.get("password"),
    };

    // Validate inputs
    if (!loginData.username || !loginData.password) {
      showMessage("errorMessage", "Vui lòng nhập đầy đủ thông tin!", true);
      return;
    }

    try {
      const response = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(loginData),
      });

      const data = await response.json();

      if (data.success) {
        // Store access token and user info (refresh token is in HttpOnly cookie)
        localStorage.setItem("accessToken", data.data.accessToken);
        localStorage.setItem("user", JSON.stringify(data.data.user));

        showMessage("successMessage", "Đăng nhập thành công!");

        // Redirect to previous page or home after 1 second
        setTimeout(() => {
          const returnUrl = new URLSearchParams(window.location.search).get(
            "returnUrl"
          );
          window.location.href = returnUrl || "/";
        }, 1000);
      } else {
        showMessage(
          "errorMessage",
          data.message || "Đăng nhập thất bại!",
          true
        );
      }
    } catch (error) {
      console.error("Login error:", error);
      showMessage("errorMessage", "Có lỗi xảy ra, vui lòng thử lại!", true);
    }
  });
}

// Register form handler
const registerForm = document.getElementById("registerForm");
if (registerForm) {
  registerForm.addEventListener("submit", async function (e) {
    e.preventDefault();

    hideMessage("regErrorMessage");
    hideMessage("regSuccessMessage");

    const formData = new FormData(this);
    const registerData = {
      username: formData.get("username"),
      fullName: formData.get("fullName"),
      email: formData.get("email"),
      password: formData.get("password"),
      confirmPassword: formData.get("confirmPassword"),
    };

    // Validate inputs
    if (
      !registerData.username ||
      !registerData.fullName ||
      !registerData.email ||
      !registerData.password ||
      !registerData.confirmPassword
    ) {
      showMessage("regErrorMessage", "Vui lòng nhập đầy đủ thông tin!", true);
      return;
    }

    if (registerData.password !== registerData.confirmPassword) {
      showMessage("regErrorMessage", "Mật khẩu xác nhận không khớp!", true);
      return;
    }

    if (registerData.password.length < 6) {
      showMessage("regErrorMessage", "Mật khẩu phải có ít nhất 6 ký tự!", true);
      return;
    }

    // Password must contain at least one letter and one number
    const hasLetter = /[a-zA-Z]/.test(registerData.password);
    const hasNumber = /\d/.test(registerData.password);

    if (!hasLetter || !hasNumber) {
      showMessage(
        "regErrorMessage",
        "Mật khẩu phải chứa ít nhất một chữ cái và một số!",
        true
      );
      return;
    }

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(registerData.email)) {
      showMessage("regErrorMessage", "Email không hợp lệ!", true);
      return;
    }

    try {
      const response = await fetch("/api/v1/auth/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username: registerData.username,
          fullName: registerData.fullName,
          email: registerData.email,
          password: registerData.password,
        }),
      });

      const data = await response.json();

      if (data.success) {
        showMessage(
          "regSuccessMessage",
          "Đăng ký thành công! Chuyển sang đăng nhập..."
        );

        // Clear form
        this.reset();

        // Switch to login form after 2 seconds
        setTimeout(() => {
          container.classList.remove("active");
        }, 2000);
      } else {
        showMessage(
          "regErrorMessage",
          data.message || "Đăng ký thất bại!",
          true
        );
      }
    } catch (error) {
      console.error("Register error:", error);
      showMessage("regErrorMessage", "Có lỗi xảy ra, vui lòng thử lại!", true);
    }
  });
}

// Auto-hide messages when user starts typing
document.querySelectorAll("input").forEach((input) => {
  input.addEventListener("input", function () {
    const form = this.closest("form");
    if (form && form.id === "loginForm") {
      hideMessage("errorMessage");
      hideMessage("successMessage");
    } else if (form && form.id === "registerForm") {
      hideMessage("regErrorMessage");
      hideMessage("regSuccessMessage");
    }
  });
});

// ============================================
// FORGOT PASSWORD MODAL
// ============================================

const forgotPasswordModal = document.getElementById("forgotPasswordModal");
const forgotPasswordLink = document.getElementById("forgotPasswordLink");
const closeForgotPassword = document.querySelector(".close-forgot-password");
let currentStep = 1;
let forgotEmail = "";

// Open modal - only if element exists
if (forgotPasswordLink) {
  forgotPasswordLink.addEventListener("click", function (e) {
    e.preventDefault();
    if (forgotPasswordModal) {
      forgotPasswordModal.classList.add("show");
      resetForgotPasswordModal();
    }
  });
}

// Close modal - only if element exists
if (closeForgotPassword) {
  closeForgotPassword.addEventListener("click", function () {
    if (forgotPasswordModal) {
      forgotPasswordModal.classList.remove("show");
    }
  });
}

// Close on outside click - only if modal exists
if (forgotPasswordModal) {
  window.addEventListener("click", function (e) {
    if (e.target === forgotPasswordModal) {
      forgotPasswordModal.classList.remove("show");
    }
  });
}

// Reset modal to step 1
function resetForgotPasswordModal() {
  currentStep = 1;
  showStep(1);
  document.getElementById("forgotPasswordForm").reset();
  document.getElementById("verifyOtpForm").reset();
  document.getElementById("resetPasswordForm").reset();
  hideMessage("forgotErrorMessage");
  hideMessage("otpErrorMessage");
  hideMessage("resetErrorMessage");
  hideMessage("resetSuccessMessage");
}

// Show specific step
function showStep(step) {
  document.getElementById("step1").style.display =
    step === 1 ? "block" : "none";
  document.getElementById("step2").style.display =
    step === 2 ? "block" : "none";
  document.getElementById("step3").style.display =
    step === 3 ? "block" : "none";
  currentStep = step;
}

// Step 1: Send OTP
const forgotPasswordForm = document.getElementById("forgotPasswordForm");
if (forgotPasswordForm) {
  forgotPasswordForm.addEventListener("submit", async function (e) {
    e.preventDefault();

    const email = document.getElementById("forgotEmail").value;
    const sendOtpBtn = document.getElementById("sendOtpBtn");
    const btnText = sendOtpBtn.querySelector(".btn-text");
    const btnLoading = sendOtpBtn.querySelector(".btn-loading");

    hideMessage("forgotErrorMessage");

    // Show loading
    btnText.style.display = "none";
    btnLoading.style.display = "inline-flex";
    sendOtpBtn.disabled = true;

    try {
      const response = await fetch("/api/v1/auth/forgot-password/request", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email: email }),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        forgotEmail = email;
        showToast(
          data.message || "Mã xác nhận đã được gửi đến email của bạn",
          "success"
        );
        showStep(2);
      } else {
        showToast(data.message || "Có lỗi xảy ra, vui lòng thử lại!", "error");
        showMessage(
          "forgotErrorMessage",
          data.message || "Có lỗi xảy ra, vui lòng thử lại!",
          true
        );
      }
    } catch (error) {
      console.error("Error:", error);
      showToast("Không thể kết nối đến server!", "error");
      showMessage("forgotErrorMessage", "Không thể kết nối đến server!", true);
    } finally {
      btnText.style.display = "inline";
      btnLoading.style.display = "none";
      sendOtpBtn.disabled = false;
    }
  });
}

// Step 2: Verify OTP
const verifyOtpForm = document.getElementById("verifyOtpForm");
if (verifyOtpForm) {
  verifyOtpForm.addEventListener("submit", async function (e) {
    e.preventDefault();

    const otp = document.getElementById("otpCode").value;
    const verifyOtpBtn = document.getElementById("verifyOtpBtn");
    const btnText = verifyOtpBtn.querySelector(".btn-text");
    const btnLoading = verifyOtpBtn.querySelector(".btn-loading");

    hideMessage("otpErrorMessage");

    // Validate OTP format
    if (!/^\d{6}$/.test(otp)) {
      showToast("Mã xác nhận phải gồm 6 chữ số!", "error");
      showMessage("otpErrorMessage", "Mã xác nhận phải gồm 6 chữ số!", true);
      return;
    }

    // Show loading
    btnText.style.display = "none";
    btnLoading.style.display = "inline-flex";
    verifyOtpBtn.disabled = true;

    try {
      const response = await fetch("/api/v1/auth/forgot-password/verify", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: forgotEmail,
          otp: otp,
        }),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        showToast("Xác nhận mã thành công!", "success");
        showStep(3);
      } else {
        showToast(data.message || "Mã xác nhận không hợp lệ!", "error");
        showMessage(
          "otpErrorMessage",
          data.message || "Mã xác nhận không hợp lệ!",
          true
        );
      }
    } catch (error) {
      console.error("Error:", error);
      showToast("Không thể kết nối đến server!", "error");
      showMessage("otpErrorMessage", "Không thể kết nối đến server!", true);
    } finally {
      btnText.style.display = "inline";
      btnLoading.style.display = "none";
      verifyOtpBtn.disabled = false;
    }
  });
}

// Resend OTP
const resendOtpBtn = document.getElementById("resendOtp");
if (resendOtpBtn) {
  resendOtpBtn.addEventListener("click", async function (e) {
    e.preventDefault();

    const resendButton = this;

    // Check if button is disabled (cooldown active)
    if (resendButton.disabled) {
      return;
    }

    hideMessage("otpErrorMessage");
    showToast("Đang gửi lại mã xác nhận...", "info");

    // Disable button temporarily
    resendButton.disabled = true;

    try {
      const response = await fetch("/api/v1/auth/forgot-password/request", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email: forgotEmail }),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        showToast("Mã xác nhận mới đã được gửi đến email của bạn!", "success");
        // Clear OTP input
        document.getElementById("otpCode").value = "";

        // Start 60 second cooldown
        let countdown = 60;
        const originalText = resendButton.textContent;

        const countdownInterval = setInterval(() => {
          resendButton.textContent = `Gửi lại (${countdown}s)`;
          countdown--;

          if (countdown < 0) {
            clearInterval(countdownInterval);
            resendButton.textContent = originalText;
            resendButton.disabled = false;
          }
        }, 1000);
      } else {
        showToast(data.message || "Không thể gửi lại mã!", "error");
        showMessage(
          "otpErrorMessage",
          data.message || "Không thể gửi lại mã!",
          true
        );
        resendButton.disabled = false;
      }
    } catch (error) {
      console.error("Error:", error);
      showToast("Không thể kết nối đến server!", "error");
      showMessage("otpErrorMessage", "Không thể kết nối đến server!", true);
      resendButton.disabled = false;
    }
  });
}

// Step 3: Reset Password
const resetPasswordForm = document.getElementById("resetPasswordForm");
if (resetPasswordForm) {
  resetPasswordForm.addEventListener("submit", async function (e) {
    e.preventDefault();

    const newPassword = document.getElementById("newPassword").value;
    const confirmPassword = document.getElementById(
      "resetConfirmPassword"
    ).value;
    const resetPasswordBtn = document.getElementById("resetPasswordBtn");
    const btnText = resetPasswordBtn.querySelector(".btn-text");
    const btnLoading = resetPasswordBtn.querySelector(".btn-loading");

    hideMessage("resetErrorMessage");
    hideMessage("resetSuccessMessage");

    // Validate passwords
    if (newPassword.length < 6) {
      showToast("Mật khẩu phải có ít nhất 6 ký tự!", "error");
      showMessage(
        "resetErrorMessage",
        "Mật khẩu phải có ít nhất 6 ký tự!",
        true
      );
      return;
    }

    // Password must contain at least one letter and one number
    const hasLetter = /[a-zA-Z]/.test(newPassword);
    const hasNumber = /\d/.test(newPassword);

    if (!hasLetter || !hasNumber) {
      showToast("Mật khẩu phải chứa ít nhất một chữ cái và một số!", "error");
      showMessage(
        "resetErrorMessage",
        "Mật khẩu phải chứa ít nhất một chữ cái và một số!",
        true
      );
      return;
    }

    if (newPassword !== confirmPassword) {
      showToast("Mật khẩu xác nhận không khớp!", "error");
      showMessage("resetErrorMessage", "Mật khẩu xác nhận không khớp!", true);
      return;
    }

    // Show loading
    btnText.style.display = "none";
    btnLoading.style.display = "inline-flex";
    resetPasswordBtn.disabled = true;

    try {
      const response = await fetch("/api/v1/auth/forgot-password/reset", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: forgotEmail,
          otp: document.getElementById("otpCode").value,
          newPassword: newPassword,
        }),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        showToast(
          "Đặt lại mật khẩu thành công! Đang chuyển hướng đến trang đăng nhập...",
          "success"
        );
        showMessage(
          "resetSuccessMessage",
          "Đặt lại mật khẩu thành công!",
          false
        );

        // Close modal and redirect to login after 2 seconds
        setTimeout(() => {
          forgotPasswordModal.classList.remove("show");
          resetForgotPasswordModal();

          // Redirect to home page (which shows login)
          window.location.href = "/login";
        }, 2000);
      } else {
        showToast(data.message || "Có lỗi xảy ra, vui lòng thử lại!", "error");
        showMessage(
          "resetErrorMessage",
          data.message || "Có lỗi xảy ra, vui lòng thử lại!",
          true
        );
      }
    } catch (error) {
      console.error("Error:", error);
      showToast("Không thể kết nối đến server!", "error");
      showMessage("resetErrorMessage", "Không thể kết nối đến server!", true);
    } finally {
      btnText.style.display = "inline";
      btnLoading.style.display = "none";
      resetPasswordBtn.disabled = false;
    }
  });
}

// ============================================
// LOGOUT AND TOKEN MANAGEMENT
// ============================================

/**
 * Logout function - Call API to delete refresh token and blacklist access token
 */
async function logout() {
  const token = localStorage.getItem("accessToken");

  // Show loading toast
  showToast("Đang đăng xuất...", "info");

  // IMPORTANT: Call API FIRST, then redirect
  if (token) {
    try {
      const response = await fetch("/api/v1/auth/logout", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ accessToken: token }),
      });

      const data = await response.json();
      console.log("Logout response:", data);
    } catch (error) {
      console.error("Logout error:", error);
      // Continue with cleanup even if API fails
    }
  }

  // Clear local storage (access token and user info only)
  localStorage.removeItem("accessToken");
  localStorage.removeItem("user");

  // Legacy cleanup: remove refreshToken from localStorage if it exists
  // (Refresh token should be in HttpOnly cookie, not localStorage)
  localStorage.removeItem("refreshToken");

  // Clear refresh token cookie (the proper location for refresh token)
  // NOTE: This is redundant - server already clears it via Set-Cookie header
  document.cookie =
    "refreshToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; Secure; SameSite=Strict";

  // Show success toast
  showToast("Đăng xuất thành công!", "success");

  // Redirect to home page AFTER cleanup completes (with small delay for toast)
  setTimeout(() => {
    window.location.href = contextPath + "/";
  }, 800);
}

/**
 * Refresh access token using refresh token from HttpOnly cookie
 */
async function refreshAccessToken() {
  // Refresh token is sent automatically via HttpOnly cookie
  // No need to read from localStorage

  try {
    const response = await fetch("/api/v1/auth/refresh", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include", // Important: include cookies in request
    });

    const data = await response.json();

    if (data.success && data.data.accessToken) {
      // Update access token
      localStorage.setItem("accessToken", data.data.accessToken);
      console.log("Access token refreshed successfully");
      return data.data.accessToken;
    } else {
      console.error("Failed to refresh token:", data.message);
      // Refresh token invalid, logout
      logout();
      return null;
    }
  } catch (error) {
    console.error("Error refreshing token:", error);
    logout();
    return null;
  }
}

// Make logout function globally available
window.logout = logout;
window.refreshAccessToken = refreshAccessToken;
