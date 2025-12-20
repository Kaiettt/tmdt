document.addEventListener('DOMContentLoaded', function () {
    const userList = document.getElementById('userList');
    const chatMessages = document.getElementById('chatMessages');
    const adminInput = document.getElementById('adminInput');
    const adminSend = document.getElementById('adminSend');

    let socket = null;
    let currentChatUserId = null;
    const adminUsername = 'Nhân viên hỗ trợ';
    const typingTimeout = 1000; // 1 second delay for typing indicator
    let typingTimer;

    const userMessages = new Map(); // Store messages for each user
    const users = new Map(); // Store connected users

    function connectWebSocket() {
        try {
            // Use WebSocket server on port 3000
            const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsHost = window.location.hostname + ':3000';
            const wsUrl = `${wsProtocol}//${wsHost}/ws/chat`;

            console.log('Connecting to WebSocket:', wsUrl);
            socket = new WebSocket(wsUrl);

            socket.onopen = function (event) {
                console.log('WebSocket connected:', event);
                // Register as admin
                socket.send(JSON.stringify({
                    event: 'register',
                    data: {
                        username: adminUsername,
                        isAdmin: true
                    }
                }));
            };

            socket.onmessage = function (event) {
                const message = JSON.parse(event.data);
                console.log('Message from server:', message);

                // Handle different event types
                if (message.event === 'userList') {
                    updateUserList(message.data);
                } else if (message.event === 'message') {
                    handleIncomingMessage(message.data);
                } else if (message.event === 'userConnected') {
                    addUserToList(message.data.userId, message.data.username);
                } else if (message.event === 'userDisconnected') {
                    removeUserFromList(message.data.userId);
                } else if (message.event === 'userTyping') {
                    // Handle typing indicator
                    const typingIndicator = document.getElementById('typing-indicator');
                    if (typingIndicator) {
                        typingIndicator.textContent = message.data.isTyping
                            ? `${message.data.username} is typing...`
                            : '';
                    }
                } else if (message.event === 'messageHistory') {
                    // Handle message history
                    if (message.data && Array.isArray(message.data)) {
                        message.data.forEach(msg => {
                            addMessageToChat(msg.sender, msg.content, msg.isAdmin);
                        });
                    }
                    console.error('Raw message data:', event.data);
                    console.error('Full details:', { name: e.name, message: e.message, stack: e.stack });
                    addSystemMessage('Lỗi: Không thể xử lý tin nhắn từ máy chủ.');
                }
            };

            socket.onclose = function (event) {
                console.warn('Admin WebSocket disconnected', event);
                addSystemMessage('Kết nối đã bị ngắt. Vui lòng tải lại trang để thử lại.');
            };

            socket.onerror = function (error) {
                console.error('WebSocket error occurred:', error);
                console.error('Socket state:', socket ? socket.readyState : 'No socket');
                addSystemMessage('Đã xảy ra lỗi kết nối. Vui lòng thử lại sau.');
            };

        } catch (e) {
            console.error('Error initializing WebSocket connection:', e);
            console.error('Full details:', { name: e.name, message: e.message, stack: e.stack });
            addSystemMessage('Lỗi: Không thể kết nối đến máy chủ.');
        }
    }

    function updateUserList(userListData) {
        try {
            const users = userListData.substring(9).split(',').filter(Boolean);
            userList.innerHTML = '';

            users.forEach(userInfo => {
                const [userId, username] = userInfo.split('|');
                const userElement = document.createElement('div');
                userElement.className = 'user-item';
                userElement.dataset.userId = userId;
                userElement.textContent = username || 'Khách';

                userElement.addEventListener('click', () => {
                    document.querySelectorAll('.user-item').forEach(el => el.classList.remove('active'));
                    userElement.classList.add('active');
                    userElement.classList.remove('has-new-message');

                    currentChatUserId = userId;
                    displayMessagesForUser(userId);
                });

                userList.appendChild(userElement);

                if (!currentChatUserId && userList.children.length === 1) {
                    userElement.click();
                }
            });
        } catch (e) {
            console.error('Error updating user list:', e);
            console.error('Raw data:', userListData);
            console.error('Full details:', { name: e.name, message: e.message, stack: e.stack });
        }
    }

    function displayMessagesForUser(userId) {
        try {
            const userData = userMessages.get(userId);
            chatMessages.innerHTML = '';

            if (userData && userData.messages.length > 0) {
                userData.messages.forEach(msg => addMessage(msg.from, msg.message, msg.isAdmin));
            } else {
                addSystemMessage('Bắt đầu cuộc trò chuyện với khách hàng');
            }

            chatMessages.scrollTop = chatMessages.scrollHeight;
        } catch (e) {
            console.error('Error displaying messages for user:', e);
            console.error('Full details:', { name: e.name, message: e.message, stack: e.stack });
        }
    }

    function addMessage(sender, message, isAdmin = false) {
        try {
            const messageElement = document.createElement('div');
            messageElement.className = `message ${isAdmin ? 'admin-message' : 'user-message'}`;

            const senderElement = document.createElement('div');
            senderElement.className = 'message-sender';
            senderElement.textContent = isAdmin ? 'Bạn' : sender;

            const contentElement = document.createElement('div');
            contentElement.className = 'message-content';
            contentElement.textContent = message;

            messageElement.appendChild(senderElement);
            messageElement.appendChild(contentElement);
            chatMessages.appendChild(messageElement);
        } catch (e) {
            console.error('Error adding message to DOM:', e);
            console.error('Full details:', { name: e.name, message: e.message, stack: e.stack });
        }
    }

    function addSystemMessage(message) {
        try {
            const messageElement = document.createElement('div');
            messageElement.className = 'system-message';
            messageElement.textContent = message;
            chatMessages.appendChild(messageElement);
        } catch (e) {
            console.error('Error adding system message:', e);
        }
    }

    function sendMessage() {
        try {
            const message = adminInput.value.trim();
            if (message && socket && socket.readyState === WebSocket.OPEN && currentChatUserId) {
                socket.send(`${currentChatUserId}|${message}`);

                if (!userMessages.has(currentChatUserId)) {
                    userMessages.set(currentChatUserId, { username: currentChatUserId, userId: currentChatUserId, messages: [] });
                }

                const userData = userMessages.get(currentChatUserId);
                userData.messages.push({ from: adminUsername, message, isAdmin: true, timestamp: new Date() });
                displayMessagesForUser(currentChatUserId);
                adminInput.value = '';
            } else {
                console.warn('Cannot send message: Socket not ready or no user selected');
            }
        } catch (e) {
            console.error('Error sending message:', e);
            console.error('Full details:', { name: e.name, message: e.message, stack: e.stack });
            addSystemMessage('Lỗi: Không thể gửi tin nhắn.');
        }
    }

    adminSend.addEventListener('click', sendMessage);
    adminInput.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') sendMessage();
    });

    connectWebSocket();
});
