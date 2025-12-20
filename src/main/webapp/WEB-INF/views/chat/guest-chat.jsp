<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <!DOCTYPE html>
    <html>

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Hỗ trợ khách hàng</title>
        <style>
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                margin: 0;
                padding: 20px;
                background-color: #f5f5f5;
            }

            .chat-container {
                max-width: 800px;
                margin: 0 auto;
                background: white;
                border-radius: 10px;
                box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                overflow: hidden;
            }

            .chat-header {
                background-color: #007bff;
                color: white;
                padding: 15px 20px;
                font-size: 1.2em;
                font-weight: bold;
            }

            .chat-box {
                display: flex;
                flex-direction: column;
                height: 70vh;
            }

            #chat-messages {
                flex: 1;
                padding: 20px;
                overflow-y: auto;
                background-color: #f9f9f9;
            }

            .message {
                margin-bottom: 15px;
                max-width: 80%;
                padding: 10px 15px;
                border-radius: 15px;
                word-wrap: break-word;
                position: relative;
            }

            .user-message {
                margin-left: auto;
                background-color: #007bff;
                color: white;
                border-bottom-right-radius: 5px;
            }

            .admin-message {
                margin-right: auto;
                background-color: #e9ecef;
                color: #212529;
                border-bottom-left-radius: 5px;
            }

            .message-sender {
                font-size: 0.8em;
                margin-bottom: 3px;
                font-weight: bold;
            }

            .chat-input-container {
                display: flex;
                padding: 15px;
                background-color: #fff;
                border-top: 1px solid #ddd;
            }

            #messageInput {
                flex: 1;
                padding: 12px 15px;
                border: 1px solid #ddd;
                border-radius: 25px;
                margin-right: 10px;
                outline: none;
                font-size: 1em;
            }

            #sendBtn {
                padding: 0 25px;
                background-color: #007bff;
                color: white;
                border: none;
                border-radius: 25px;
                cursor: pointer;
                font-size: 1em;
                transition: background-color 0.2s;
            }

            #sendBtn:hover {
                background-color: #0056b3;
            }

            .typing-indicator {
                font-style: italic;
                color: #6c757d;
                margin: 5px 20px;
                display: none;
            }

            @media (max-width: 768px) {
                .chat-container {
                    margin: 0;
                    border-radius: 0;
                    height: 100vh;
                }

                .chat-box {
                    height: calc(100vh - 120px);
                }
            }
        </style>
    </head>

    <body>
        <div class="chat-container">
            <div class="chat-header">
                Hỗ trợ khách hàng
            </div>
            <div class="chat-box">
                <div id="chat-messages"></div>
                <div class="typing-indicator" id="typingIndicator">
                    Đang nhập...
                </div>
            </div>
            <div class="chat-input-container">
                <input type="text" id="messageInput" placeholder="Nhập tin nhắn..." autocomplete="off" />
                <button id="sendBtn">Gửi</button>
            </div>
        </div>
        <script src="${pageContext.request.contextPath}/static/js/pages/guest-chat.js"></script>
    </body>

    </html>