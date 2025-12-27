document.addEventListener('DOMContentLoaded', () => {
  const chatHistory = document.getElementById('chat-history');
  const messageInput = document.getElementById('message-input');
  const sendBtn = document.getElementById('send-btn');
  const resetBtn = document.getElementById('reset-btn');
  const typingIndicator = document.getElementById('typing-indicator');
  const conversationList = document.getElementById('conversation-list');

  let conversationId = null;

  const generateUUID = () => {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return crypto.randomUUID();
    }
    return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
      (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
  };

  const getUserId = () => {
    let userId = localStorage.getItem('app_user_id');
    if (!userId) {
      userId = generateUUID();
      localStorage.setItem('app_user_id', userId);
    }
    return userId;
  };

  const initialWelcomeMessage = `
    <div class="message agent">
      <div class="msg-label">> SYSTEM</div>
      <div class="msg-content">Welcome to the Multi-Agent Support Gateway. 
All agents are online and ready to assist you with Technical or Billing inquiries.
Type your request below to begin.</div>
    </div>
  `;

  const loadConversations = async () => {
    try {
      const response = await fetch('/api/chat/conversations', {
        headers: {
          'X-User-ID': getUserId()
        }
      });
      if (!response.ok) return;
      const summaries = await response.json();

      conversationList.innerHTML = '';
      summaries.forEach(c => {
        const div = document.createElement('div');
        div.className = `conv-item ${c.id === conversationId ? 'active' : ''}`;
        div.textContent = c.title;
        div.onclick = () => loadHistory(c.id);
        conversationList.appendChild(div);
      });
    } catch (error) {
      console.error('Failed to load conversations', error);
    }
  };

  const loadHistory = async (id) => {
    try {
      const response = await fetch(`/api/chat/conversations/${id}`, {
        headers: {
          'X-User-ID': getUserId()
        }
      });
      if (!response.ok) return;
      const history = await response.json();

      conversationId = id;
      chatHistory.innerHTML = '';

      history.forEach(msg => {
        let role = msg.role.toLowerCase();
        if (role === 'assistant') {
          role = 'agent';
        }
        const agentName = role === 'agent' ? 'AGENT' : null;
        appendMessage(role, msg.content, agentName);
      });

      loadConversations();

    } catch (error) {
      console.error('Failed to load history', error);
    }
  };

  const resetConversation = () => {
    conversationId = null;
    chatHistory.innerHTML = initialWelcomeMessage;
    typingIndicator.style.display = 'none';
    messageInput.disabled = false;
    sendBtn.disabled = false;
    messageInput.value = '';
    messageInput.style.height = 'auto';
    messageInput.focus();
    loadConversations();
  };

  if (resetBtn) {
    resetBtn.addEventListener('click', resetConversation);
  }

  loadConversations();

  messageInput.addEventListener('input', () => {
    messageInput.style.height = 'auto';
    messageInput.style.height = messageInput.scrollHeight + 'px';
  });

  const appendMessage = (role, text, agentName = null) => {
    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${role}`;

    const label = document.createElement('div');
    label.className = 'msg-label';
    label.textContent = role === 'user' ? 'USER' : `> ${agentName || 'AGENT'}`;

    const content = document.createElement('div');
    content.className = 'msg-content';
    content.textContent = text;

    msgDiv.appendChild(label);
    msgDiv.appendChild(content);
    chatHistory.appendChild(msgDiv);

    chatHistory.scrollTo({
      top: chatHistory.scrollHeight,
      behavior: 'smooth'
    });
  };

  const sendMessage = async () => {
    const message = messageInput.value.trim();
    if (!message) return;

    if (!conversationId) {
      conversationId = generateUUID();
    }

    appendMessage('user', message);
    messageInput.value = '';
    messageInput.style.height = 'auto';
    messageInput.disabled = true;
    sendBtn.disabled = true;
    typingIndicator.style.display = 'block';
    chatHistory.scrollTo({ top: chatHistory.scrollHeight, behavior: 'smooth' });

    try {
      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-ID': getUserId()
        },
        body: JSON.stringify({
          message: message,
          conversationId: conversationId
        }),
      });

      if (!response.ok) {
        throw new Error('Network response was not ok');
      }

      const data = await response.json();
      conversationId = data.conversationId;

      typingIndicator.style.display = 'none';
      appendMessage('agent', data.reply, data.agent);

      loadConversations();

    } catch (error) {
      console.error('Error:', error);
      typingIndicator.style.display = 'none';
      appendMessage('agent', 'ERROR: Failed to connect to the agent gateway. Please try again.', 'SYSTEM');
    } finally {
      messageInput.disabled = false;
      sendBtn.disabled = false;
      messageInput.focus();
    }
  };

  sendBtn.addEventListener('click', sendMessage);

  messageInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });
});
