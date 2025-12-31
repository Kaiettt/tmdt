<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <!DOCTYPE html>
    <html lang="vi">

    <head>
        <meta charset="UTF-8">
        <title>Guest Chat</title>

        <style>
            body {
                font-family: Arial, sans-serif;
                background: #f5f5f5;
                padding: 20px;
            }

            .chat-container {
                max-width: 600px;
                margin: auto;
                background: #fff;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0, 0, 0, .1);
                overflow: hidden;
            }

            .chat-header {
                background: #007bff;
                color: #fff;
                padding: 15px;
                font-size: 18px;
            }

            #chat-messages {
                height: 350px;
                padding: 15px;
                overflow-y: auto;
                background: #f9f9f9;
            }

            .message {
                margin-bottom: 10px;
                padding: 10px 14px;
                border-radius: 12px;
                max-width: 80%;
            }

            .user {
                background: #007bff;
                color: #fff;
                margin-left: auto;
            }

            .server {
                background: #e9ecef;
                color: #333;
            }

            .chat-input {
                display: flex;
                padding: 10px;
                border-top: 1px solid #ddd;
            }

            #messageInput {
                flex: 1;
                padding: 10px;
                border-radius: 20px;
                border: 1px solid #ccc;
                outline: none;
            }

            #sendBtn {
                margin-left: 10px;
                padding: 0 20px;
                border: none;
                background: #007bff;
                color: #fff;
                border-radius: 20px;
                cursor: pointer;
            }
        </style>
    </head>

    <body>

        <div class="chat-container">
            <div class="chat-header">Guest Chat</div>

            <div id="chat-messages"></div>

            <div class="chat-input">
                <input type="text" id="messageInput" placeholder="Type message..." />
                <button id="sendBtn">Send</button>
            </div>
        </div>

        <script>
            window.CHAT_CONFIG = {
                userId: "guest-001",
                role: "customer",
                roomId: "room-001",
                wsUrl: "ws://localhost:5000/ws"
            };
        </script>

        <script src="${pageContext.request.contextPath}/static/js/pages/guest-chat.js"></script>

    </body>

    </html>