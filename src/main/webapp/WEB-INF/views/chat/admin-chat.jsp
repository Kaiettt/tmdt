<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <!DOCTYPE html>
    <html>

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý hỗ trợ khách hàng</title>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
        <style>
            :root {
                --primary-color: #4361ee;
                --primary-hover: #3a56d4;
                --secondary-color: #f8f9fa;
                --border-color: #dee2e6;
                --text-color: #212529;
                --light-text: #6c757d;
                --white: #ffffff;
                --success: #28a745;
                --warning: #ffc107;
                --danger: #dc3545;
            }

            * {
                box-sizing: border-box;
                margin: 0;
                padding: 0;
            }

            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background-color: #f1f3f9;
                color: var(--text-color);
                line-height: 1.6;
                height: 100vh;
                display: flex;
                flex-direction: column;
            }

            .admin-header {
                background-color: var(--primary-color);
                color: white;
                padding: 15px 25px;
                display: flex;
                justify-content: space-between;
                align-items: center;
                box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
            }

            .admin-header h1 {
                font-size: 1.5rem;
                font-weight: 600;
            }

            .admin-header .user-info {
                display: flex;
                align-items: center;
                gap: 10px;
            }

            .admin-header .user-avatar {
                width: 36px;
                height: 36px;
                border-radius: 50%;
                background-color: rgba(255, 255, 255, 0.2);
                display: flex;
                align-items: center;
                justify-content: center;
            }

            .admin-container {
                display: flex;
                flex: 1;
                overflow: hidden;
            }

            .user-list-container {
                width: 300px;
                background-color: var(--white);
                border-right: 1px solid var(--border-color);
                display: flex;
                flex-direction: column;
                overflow: hidden;
            }

            .user-list-header {
                padding: 15px;
                border-bottom: 1px solid var(--border-color);
                font-weight: 600;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }

            .user-count {
                background-color: var(--primary-color);
                color: white;
                border-radius: 12px;
                padding: 2px 8px;
                font-size: 0.8rem;
            }

            .user-search {
                padding: 10px 15px;
                border-bottom: 1px solid var(--border-color);
            }

            .user-search input {
                width: 100%;
                padding: 8px 12px;
                border: 1px solid var(--border-color);
                border-radius: 20px;
                outline: none;
                font-size: 0.9rem;
            }

            .user-search input:focus {
                border-color: var(--primary-color);
                box-shadow: 0 0 0 0.2rem rgba(67, 97, 238, 0.25);
            }

            #userList {
                flex: 1;
                overflow-y: auto;
            }

            .user-item {
                padding: 15px;
                border-bottom: 1px solid var(--border-color);
                cursor: pointer;
                transition: background-color 0.2s;
                display: flex;
                align-items: center;
                gap: 12px;
            }

            .user-item:hover {
                background-color: rgba(67, 97, 238, 0.05);
            }

            .user-item.active {
                background-color: rgba(67, 97, 238, 0.1);
                border-left: 3px solid var(--primary-color);
            }

            .user-item.has-new-message {
                background-color: #fff8e6;
            }

            .user-avatar {
                width: 40px;
                height: 40px;
                border-radius: 50%;
                background-color: #e9ecef;
                display: flex;
                align-items: center;
                justify-content: center;
                color: var(--light-text);
                font-weight: 600;
            }

            .user-info {
                flex: 1;
                min-width: 0;
            }

            .user-name {
                font-weight: 600;
                margin-bottom: 3px;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }

            .user-status {
                display: flex;
                align-items: center;
                font-size: 0.8rem;
                color: var(--light-text);
            }

            .status-dot {
                width: 8px;
                height: 8px;
                border-radius: 50%;
                background-color: #28a745;
                margin-right: 5px;
            }

            .chat-container {
                flex: 1;
                display: flex;
                flex-direction: column;
                background-color: var(--white);
                border-radius: 8px 0 0 0;
                box-shadow: -1px 0 5px rgba(0, 0, 0, 0.05);
            }

            .chat-header {
                padding: 15px 25px;
                border-bottom: 1px solid var(--border-color);
                display: flex;
                align-items: center;
                justify-content: space-between;
            }

            .chat-partner-info {
                display: flex;
                align-items: center;
                gap: 12px;
            }

            .chat-actions {
                display: flex;
                gap: 15px;
            }

            .chat-action-btn {
                background: none;
                border: none;
                color: var(--light-text);
                font-size: 1.1rem;
                cursor: pointer;
                transition: color 0.2s;
            }

            .chat-action-btn:hover {
                color: var(--primary-color);
            }

            #chatMessages {
                flex: 1;
                padding: 20px;
                overflow-y: auto;
                background-color: #f5f7fb;
                background-image: url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M11 18c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm48 25c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm-43-7c1.657 0 3-1.343 3-3s-.895-3-2-3-3 1.343-3 3 1.343 3 3 3zm63 31c1.657 0 3-1.343 3-3s-.895-3-2-3-3 1.343-3 3 1.343 3 3 3zM34 90c1.657 0 3-1.343 3-3s-.895-3-2-3-3 1.343-3 3 1.343 3 3 3zm56-76c1.657 0 3-1.343 3-3s-.895-3-2-3-3 1.343-3 3 1.343 3 3 3zM12 86c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm28-65c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm23-11c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-6 60c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm29 22c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zM32 63c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm57-13c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-9-21c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM60 91c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM35 41c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM12 60c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2z' fill='%23e9ecef' fill-opacity='0.4' fill-rule='evenodd'/%3E%3C/svg%3E");
            }

            .message {
                margin-bottom: 15px;
                max-width: 70%;
                position: relative;
                padding: 10px 15px;
                border-radius: 15px;
                word-wrap: break-word;
            }

            .message.sent {
                margin-left: auto;
                background-color: var(--primary-color);
                color: white;
                border-bottom-right-radius: 5px;
            }

            .message.received {
                margin-right: auto;
                background-color: var(--white);
                color: var(--text-color);
                border-bottom-left-radius: 5px;
                box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
            }

            .message-time {
                font-size: 0.7rem;
                color: var(--light-text);
                margin-top: 3px;
                text-align: right;
            }

            .message.sent .message-time {
                color: rgba(255, 255, 255, 0.7);
            }

            .system-message {
                margin: 15px auto;
                padding: 5px 15px;
                background-color: #e9ecef;
                border-radius: 15px;
                font-size: 0.85rem;
                color: var(--light-text);
                max-width: 80%;
                text-align: center;
            }

            .chat-input-container {
                padding: 15px;
                border-top: 1px solid var(--border-color);
                background-color: var(--white);
            }

            .message-input-container {
                display: flex;
                align-items: center;
                gap: 10px;
            }

            #adminInput {
                flex: 1;
                padding: 12px 20px;
                border: 1px solid var(--border-color);
                border-radius: 25px;
                outline: none;
                font-size: 1rem;
                transition: border-color 0.2s, box-shadow 0.2s;
            }

            #adminInput:focus {
                border-color: var(--primary-color);
                box-shadow: 0 0 0 0.2rem rgba(67, 97, 238, 0.25);
            }

            #adminSend {
                width: 45px;
                height: 45px;
                border: none;
                border-radius: 50%;
                background-color: var(--primary-color);
                color: white;
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
                transition: background-color 0.2s;
            }

            #adminSend:hover {
                background-color: var(--primary-hover);
            }

            #adminSend:disabled {
                background-color: #adb5bd;
                cursor: not-allowed;
            }

            .typing-indicator {
                display: flex;
                align-items: center;
                gap: 5px;
                margin: 5px 0 15px 15px;
                font-size: 0.85rem;
                color: var(--light-text);
                display: none;
            }

            .typing-dot {
                width: 8px;
                height: 8px;
                background-color: var(--light-text);
                border-radius: 50%;
                opacity: 0.4;
                animation: typingAnimation 1.4s infinite ease-in-out;
            }

            .typing-dot:nth-child(1) {
                animation-delay: 0s;
            }

            .typing-dot:nth-child(2) {
                animation-delay: 0.2s;
            }

            .typing-dot:nth-child(3) {
                animation-delay: 0.4s;
            }

            @keyframes typingAnimation {

                0%,
                60%,
                100% {
                    transform: translateY(0);
                    opacity: 0.4;
                }

                30% {
                    transform: translateY(-5px);
                    opacity: 1;
                }
            }

            .no-chat-selected {
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                height: 100%;
                color: var(--light-text);
                text-align: center;
                padding: 20px;
            }

            .no-chat-selected i {
                font-size: 3rem;
                margin-bottom: 15px;
                color: #e9ecef;
            }

            .no-chat-selected h3 {
                margin-bottom: 10px;
                color: var(--text-color);
            }

            /* Scrollbar styling */
            ::-webkit-scrollbar {
                width: 8px;
                height: 8px;
            }

            ::-webkit-scrollbar-track {
                background: #f1f1f1;
                border-radius: 4px;
            }

            ::-webkit-scrollbar-thumb {
                background: #c1c1c1;
                border-radius: 4px;
            }

            ::-webkit-scrollbar-thumb:hover {
                background: #a8a8a8;
            }

            /* Responsive styles */
            @media (max-width: 992px) {
                .admin-container {
                    flex-direction: column;
                }

                .user-list-container {
                    width: 100%;
                    max-height: 300px;
                    border-right: none;
                    border-bottom: 1px solid var(--border-color);
                }

                .chat-container {
                    height: calc(100vh - 300px - 60px);
                }
            }

            @media (max-width: 576px) {
                .admin-header h1 {
                    font-size: 1.2rem;
                }

                .message {
                    max-width: 85%;
                }

                .chat-input-container {
                    padding: 10px;
                }

                #adminInput {
                    padding: 10px 15px;
                }

                #adminSend {
                    width: 40px;
                    height: 40px;
                }
            }
        </style>
    </head>

    <body>
        <header class="admin-header">
            <h1>Quản lý hỗ trợ khách hàng</h1>
            <div class="user-info">
                <span>${sessionScope.user != null ? sessionScope.user.fullName : 'Quản trị viên'}</span>
                <div class="user-avatar">
                    <i class="fas fa-user-shield"></i>
                </div>
            </div>
        </header>

        <div class="admin-container">
            <aside class="user-list-container">
                <div class="user-list-header">
                    <span>Khách hàng đang trực tuyến</span>
                    <span class="user-count" id="onlineCount">0</span>
                </div>
                <div class="user-search">
                    <input type="text" id="searchUser" placeholder="Tìm kiếm khách hàng...">
                </div>
                <div id="userList">
                    <!-- User items will be added here dynamically -->
                    <div class="no-users">Không có khách hàng nào đang trực tuyến</div>
                </div>
            </aside>

            <main class="chat-container">
                <div id="chatArea" style="display: none;">
                    <div class="chat-header">
                        <div class="chat-partner-info">
                            <div class="user-avatar">
                                <i class="fas fa-user"></i>
                            </div>
                            <div>
                                <div class="user-name" id="chatPartnerName">Khách hàng</div>
                                <div class="user-status">
                                    <span class="status-dot"></span>
                                    <span>Đang hoạt động</span>
                                </div>
                            </div>
                        </div>
                        <div class="chat-actions">
                            <button class="chat-action-btn" title="Xem thông tin" id="viewInfoBtn">
                                <i class="fas fa-info-circle"></i>
                            </button>
                            <button class="chat-action-btn" title="Tải xuống lịch sử chat" id="downloadChatBtn">
                                <i class="fas fa-download"></i>
                            </button>
                            <button class="chat-action-btn" title="Đóng chat" id="endChatBtn">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                    </div>

                    <div id="chatMessages">
                        <!-- Messages will be added here dynamically -->
                    </div>

                    <div class="typing-indicator" id="typingIndicator">
                        <div class="typing-dot"></div>
                        <div class="typing-dot"></div>
                        <div class="typing-dot"></div>
                        <span>Đang nhập...</span>
                    </div>

                    <div class="chat-input-container">
                        <div class="message-input-container">
                            <input type="text" id="adminInput" placeholder="Nhập tin nhắn..." autocomplete="off"
                                disabled>
                            <button id="adminSend" disabled>
                                <i class="fas fa-paper-plane"></i>
                            </button>
                        </div>
                    </div>
                </div>

                <div id="noChatSelected" class="no-chat-selected">
                    <i class="fas fa-comment-dots"></i>
                    <h3>Chọn một cuộc trò chuyện</h3>
                    <p>Chọn một khách hàng từ danh sách bên trái để bắt đầu trò chuyện</p>
                </div>
            </main>
        </div>

        <script src="${pageContext.request.contextPath}/static/js/pages/admin-chat.js"></script>
    </body>

    </html>