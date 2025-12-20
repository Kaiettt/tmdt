document.addEventListener('DOMContentLoaded', function () {
    const chatMessages = document.getElementById('chat-messages');
    const messageInput = document.getElementById('messageInput');
    const sendBtn = document.getElementById('sendBtn');
    const typingIndicator = document.createElement('div');
    typingIndicator.id = 'typing-indicator';
    typingIndicator.className = 'typing-indicator';
    chatMessages.parentNode.insertBefore(typingIndicator, chatMessages.nextSibling);

    // Generate a random username for the guest
    const username = 'Guest-' + Math.floor(Math.random() * 1000);
    let socket = null;
    let isTyping = false;
    let typingTimeout;

    function connectWebSocket(attempt = 1) {
        try {
            // Use WebSocket server on port 3000 (matching NestJS server)
            const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsHost = window.location.hostname + ':3000';
            const wsUrl = `${wsProtocol}//${wsHost}/ws/chat`;

            console.log(`[${new Date().toISOString()}] [Attempt ${attempt}] Connecting to WebSocket:`, wsUrl);
            console.log(`[${new Date().toISOString()}] Current location:`, window.location.href);

            // Close any existing connection
            if (socket) {
                console.log(`[${new Date().toISOString()}] Closing existing WebSocket connection`);
                try {
                    socket.close();
                } catch (e) {
                    console.error(`[${new Date().toISOString()}] Error closing existing socket:`, e);
                }
            }

            // Create new WebSocket connection
            console.log(`[${new Date().toISOString()}] Creating new WebSocket connection...`);
            socket = new WebSocket(wsUrl);

            // Log WebSocket state changes
            const logState = () => {
                const state = getStateName(socket.readyState);
                console.log(`[${new Date().toISOString()}] WebSocket state changed: ${socket.readyState} (${state})`);
                return state;
            };

            const getStateName = (state) => {
                const states = {
                    0: 'CONNECTING',
                    1: 'OPEN',
                    2: 'CLOSING',
                    3: 'CLOSED'
                };
                return states[state] || 'UNKNOWN';
            };

            // Log initial state
            console.log(`[${new Date().toISOString()}] Initial WebSocket state:`, logState());

            // Connection opened
            socket.onopen = function (event) {
                const state = logState();
                console.log(`[${new Date().toISOString()}] ✅ WebSocket connection established successfully`);
                console.log(`[${new Date().toISOString()}] WebSocket URL: ${wsUrl}`);
                console.log(`[${new Date().toISOString()}] WebSocket protocol: ${socket.protocol || 'default'}`);
                console.log(`[${new Date().toISOString()}] WebSocket extensions: ${socket.extensions || 'none'}`);
                console.log(`[${new Date().toISOString()}] WebSocket binary type: ${socket.binaryType}`);

                // Send initial registration message
                try {
                    const registerMsg = JSON.stringify({
                        type: 'register',
                        username: username,
                        timestamp: new Date().toISOString()
                    });
                    console.log(`[${new Date().toISOString()}] Sending registration:`, registerMsg);
                    socket.send(registerMsg);
                } catch (e) {
                    console.error(`[${new Date().toISOString()}] Error sending registration:`, e);
                }
                logState();

                try {
                    // Register as guest
                    const registerData = {
                        event: 'register',
                        data: {
                            username: username,
                            isAdmin: false
                        }
                    };
                    console.log(`[${new Date().toISOString()}] Sending registration:`, registerData);
                    socket.send(JSON.stringify(registerData));
                } catch (error) {
                    console.error(`[${new Date().toISOString()}] Error during registration:`, error);
                }
            };

            // Handle incoming messages
            socket.onmessage = function (event) {
                const timestamp = new Date().toISOString();
                console.log(`[${timestamp}] 📩 Message from server:`, event.data);

                try {
                    const message = JSON.parse(event.data);
                    console.log(`[${timestamp}] 📦 Parsed message:`, message);

                    // Add your message handling logic here
                    // For example, you might want to update the UI with the new message

                } catch (e) {
                    console.error(`[${timestamp}] ❌ Error parsing message:`, e);
                    console.error(`[${timestamp}] Raw message data:`, event.data);
                }
                try {
                    const message = JSON.parse(event.data);

                    if (message.event === 'message') {
                        addMessage(
                            message.data.sender || 'Hỗ trợ viên',
                            message.data.content,
                            message.data.isAdmin
                        );
                    } else if (message.event === 'adminTyping') {
                        typingIndicator.textContent = message.data.isTyping
                            ? 'Nhân viên đang soạn tin nhắn...'
                            : '';
                    } else if (message.event === 'messageHistory') {
                        // Display message history
                        if (message.data && Array.isArray(message.data)) {
                            message.data.forEach(msg => {
                                addMessage(
                                    msg.sender || 'Hệ thống',
                                    msg.content,
                                    msg.isAdmin
                                );
                            });
                        }
                    }
                } catch (e) {
                    console.error('Error processing message:', e);
                    console.error('Raw message data:', event.data);
                }
            };

            socket.onclose = function (event) {
                const closeReason = {
                    code: event.code,
                    reason: event.reason || 'No reason provided',
                    wasClean: event.wasClean ? 'clean' : 'unclean',
                    timestamp: new Date().toISOString()
                };

                console.log(`[${new Date().toISOString()}] WebSocket connection closed:`, closeReason);
                console.log(`[${new Date().toISOString()}] Close event:`, event);

                // Common close codes and their meanings
                const closeCodes = {
                    1000: 'Normal closure',
                    1001: 'Going away',
                    1002: 'Protocol error',
                    1003: 'Unsupported data',
                    1005: 'No status received',
                    1006: 'Abnormal closure',
                    1007: 'Invalid frame payload data',
                    1008: 'Policy violation',
                    1009: 'Message too big',
                    1010: 'Missing extension',
                    1011: 'Internal error',
                    1012: 'Service restart',
                    1013: 'Try again later',
                    1014: 'Bad gateway',
                    1015: 'TLS handshake failed'
                };

                console.log(`[${new Date().toISOString()}] Close code ${event.code}: ${closeCodes[event.code] || 'Unknown code'}`);

                // Attempt to reconnect if this wasn't a normal closure
                if (event.code !== 1000) {
                    const delay = Math.min(1000 * Math.pow(2, attempt), 30000); // Exponential backoff, max 30s
                    console.log(`[${new Date().toISOString()}] Attempting to reconnect in ${delay / 1000} seconds...`);
                    setTimeout(() => connectWebSocket(attempt + 1), delay);
                }
            };

            socket.onerror = function (error) {
                console.error(`[${new Date().toISOString()}] WebSocket error:`, error);
                console.error(`[${new Date().toISOString()}] Error details:`, {
                    type: error.type,
                    message: error.message,
                    readyState: socket.readyState,
                    url: wsUrl
                });

                // Try to get more error details if available
                if (error.target && error.target.url) {
                    console.error(`[${new Date().toISOString()}] Error target URL:`, error.target.url);
                }
                logState();

                // Log additional error information if available
                if (error && error.type) {
                    console.error(`[${new Date().toISOString()}] Error type: ${error.type}`);
                }

                // Log WebSocket state
                const state = socket ? socket.readyState : -1;
                console.error(`[${new Date().toISOString()}] WebSocket ready state: ${state} (${getStateName(state)})`);

                let errorMessage = 'Đã xảy ra lỗi kết nối';
                if (state === WebSocket.CLOSED) {
                    errorMessage = 'Kết nối đã đóng trước đó';
                } else if (state === WebSocket.CONNECTING) {
                    errorMessage = 'Đang kết nối đến máy chủ...';
                } else if (state === WebSocket.CLOSING) {
                    errorMessage = 'Đang đóng kết nối...';
                }

                addMessage('Lỗi', `${errorMessage}. Mã lỗi: ${error.code || 'Không xác định'}`);

                // Log URL being used
                console.warn(`[${new Date().toISOString()}] WebSocket URL: ${wsUrl}`);

                // Check if the server is reachable
                console.log(`[${new Date().toISOString()}] Testing server reachability...`);
                fetch(wsUrl.replace('ws', 'http').replace('/ws/chat', '/health'))
                    .then(response => {
                        console.log(`[${new Date().toISOString()}] Server health check:`, response.status, response.statusText);
                    })
                    .catch(err => {
                        console.error(`[${new Date().toISOString()}] Server health check failed:`, err);
                    });
            };
        } catch (e) {
            console.error('Error initializing WebSocket connection:', e);
            console.error('Full error details:', {
                name: e.name,
                message: e.message,
                stack: e.stack
            });
            addMessage('Lỗi', 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');
        }
    }

    function addMessage(sender, message, isAdmin = false) {
        const messageElement = document.createElement('div');
        messageElement.className = `message ${isAdmin ? 'admin-message' : 'user-message'}`;

        const senderElement = document.createElement('div');
        senderElement.className = 'message-sender';
        senderElement.textContent = isAdmin ? 'Nhân viên hỗ trợ' : sender;

        const contentElement = document.createElement('div');
        contentElement.className = 'message-content';
        contentElement.textContent = message;

        messageElement.appendChild(senderElement);
        messageElement.appendChild(contentElement);

        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function sendMessage() {
        const message = messageInput.value.trim();
        if (message && socket && socket.readyState === WebSocket.OPEN) {
            try {
                socket.send(message);
                addMessage('Bạn', message);
                messageInput.value = '';
            } catch (e) {
                console.error('Error sending message:', e);
                console.error('Full error details:', {
                    name: e.name,
                    message: e.message,
                    stack: e.stack
                });
                addMessage('Lỗi', 'Không thể gửi tin nhắn. Vui lòng thử lại.');
            }
        } else if (!socket || socket.readyState !== WebSocket.OPEN) {
            console.warn('Cannot send message, WebSocket not open', socket);
            addMessage('Lỗi', 'Kết nối chưa sẵn sàng. Vui lòng thử lại.');
        }
    }

    sendBtn.addEventListener('click', sendMessage);
    messageInput.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            sendMessage();
        }
    });

    connectWebSocket();

    const style = document.createElement('style');
    style.textContent = `
        .chat-box { display: flex; flex-direction: column; height: 500px; max-width: 600px; margin: 0 auto; border: 1px solid #ccc; border-radius: 8px; overflow: hidden; }
        #chat-messages { flex: 1; padding: 15px; overflow-y: auto; background-color: #f9f9f9; }
        .message { margin-bottom: 15px; max-width: 80%; padding: 10px 15px; border-radius: 15px; word-wrap: break-word; }
        .user-message { margin-left: auto; background-color: #007bff; color: white; border-bottom-right-radius: 5px; }
        .admin-message { margin-right: auto; background-color: #e9ecef; color: #212529; border-bottom-left-radius: 5px; }
        .message-sender { font-size: 0.8em; margin-bottom: 3px; font-weight: bold; }
        .chat-input { display: flex; padding: 10px; background-color: #fff; border-top: 1px solid #ddd; }
        #messageInput { flex: 1; padding: 10px; border: 1px solid #ddd; border-radius: 20px; margin-right: 10px; outline: none; }
        #sendBtn { padding: 0 20px; background-color: #007bff; color: white; border: none; border-radius: 20px; cursor: pointer; }
        #sendBtn:hover { background-color: #0056b3; }
    `;
    document.head.appendChild(style);
});
