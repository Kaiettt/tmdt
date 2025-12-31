document.addEventListener('DOMContentLoaded', () => {
    const chatMessages = document.getElementById('chat-messages');
    const messageInput = document.getElementById('messageInput');
    const sendBtn = document.getElementById('sendBtn');

    const config = window.CHAT_CONFIG;
    let ws;

    function addMessage(text, type) {
        const div = document.createElement('div');
        div.className = `message ${type}`;
        div.textContent = text;
        chatMessages.appendChild(div);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function connectWebSocket() {
        const wsUrl =
            `${config.wsUrl}?userId=${config.userId}&role=${config.role}`;

        console.log('Connecting to:', wsUrl);

        ws = new WebSocket(wsUrl);

        ws.onopen = () => {
            console.log('WebSocket connected');

            // gửi init sau khi handshake OK
            ws.send(JSON.stringify({
                type: 'init',
                userId: config.userId,
                role: config.role,
                roomId: config.roomId
            }));

            addMessage('✅ Connected to chat server', 'server');
        };

        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                addMessage(
                    `${data.sender || 'Server'}: ${data.message}`,
                    'server'
                );
            } catch (e) {
                addMessage(event.data, 'server');
            }
        };

        ws.onerror = () => {
            addMessage('❌ WebSocket error', 'server');
        };

        ws.onclose = () => {
            addMessage('🔌 Connection closed', 'server');
        };
    }

    sendBtn.addEventListener('click', () => {
        const message = messageInput.value.trim();
        if (!message) return;

        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({
                type: 'message',
                roomId: config.roomId,
                message: message
            }));

            addMessage('You: ' + message, 'user');
            messageInput.value = '';
        }
    });

    messageInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') sendBtn.click();
    });

    connectWebSocket();
});
