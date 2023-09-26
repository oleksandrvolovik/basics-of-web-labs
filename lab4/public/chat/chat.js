const socket = io();

const messages = document.getElementById('messages');
const form = document.getElementById('form');
const input = document.getElementById('input');

const nickname = localStorage.getItem("nickname")
const room = localStorage.getItem("room")

class MessageType {
  static ChatJoin = 'ChatJoin';
  static ChatMessage = 'ChatMessage';
  static ChatLeave = 'ChatLeave';
}

window.onload = function () {
  document.getElementById('nickname').innerHTML = nickname
  document.getElementById('room').innerHTML = room
  socket.emit(MessageType.ChatJoin, nickname, room);
};

form.addEventListener('submit', (e) => {
  e.preventDefault();
  if (input.value) {
    socket.emit(MessageType.ChatMessage, nickname, input.value);
    input.value = '';
  }
});

function createChatMessage(nickname, text, time) {
  const realTime = new Date(time);
  const item = document.createElement('li');
  const messageContainer = document.createElement('div');
  const messageAuthor = document.createElement('p');
  const messageText = document.createElement('p');

  messageAuthor.classList.add('message-author');
  messageAuthor.textContent = nickname
  messageContainer.appendChild(messageAuthor)

  const hours = ('0' + realTime.getHours()).slice(-2);
  const minutes = ('0' + realTime.getMinutes()).slice(-2);

  const days = ('0' + realTime.getDate()).slice(-2)
  const month = ('0' + (realTime.getMonth() + 1)).slice(-2)
  const year = realTime.getFullYear()

  messageText.textContent = text;
  messageContainer.appendChild(messageText);
  item.appendChild(messageContainer);

  // Add classes for sent and received messages
  if (nickname === localStorage.getItem('nickname')) {
    item.classList.add('sent-message');
    messageContainer.classList.add('sent-message-container');
  } else {
    item.classList.add('received-message');
    messageContainer.classList.add('received-message-container');
  }

  // Add a timestamp to the message
  const timestamp = document.createElement('span');
  timestamp.textContent = `${hours}:${minutes} ${days}.${month}.${year}`;
  timestamp.classList.add('message-timestamp');
  messageContainer.appendChild(timestamp);

  messages.appendChild(item);
  document.getElementById('messages').scrollTop = document.getElementById('messages').scrollHeight;
}

function createChatJoinMessage(nickname) {
  const item = document.createElement('li');
  const messageContainer = document.createElement('div');
  const messageAuthor = document.createElement('p');

  messageAuthor.classList.add('message-author');
  messageAuthor.textContent = nickname + " joined."
  messageContainer.appendChild(messageAuthor)

  item.appendChild(messageContainer);

  // Add classes for sent and received messages
  if (nickname === localStorage.getItem('nickname')) {
    item.classList.add('sent-message');
    messageContainer.classList.add('sent-message-container');
  } else {
    item.classList.add('received-message');
    messageContainer.classList.add('received-message-container');
  }

  messages.appendChild(item);
  document.getElementById('messages').scrollTop = document.getElementById('messages').scrollHeight;
}

function createChatLeaveMessage(nickname) {
  const item = document.createElement('li');
  const messageContainer = document.createElement('div');
  const messageAuthor = document.createElement('p');

  messageAuthor.classList.add('message-author');
  messageAuthor.textContent = nickname + " left."
  messageContainer.appendChild(messageAuthor)

  item.appendChild(messageContainer);

  // Add classes for sent and received messages
  if (nickname === localStorage.getItem('nickname')) {
    item.classList.add('sent-message');
    messageContainer.classList.add('sent-message-container');
  } else {
    item.classList.add('received-message');
    messageContainer.classList.add('received-message-container');
  }

  messages.appendChild(item);
  document.getElementById('messages').scrollTop = document.getElementById('messages').scrollHeight;
}

socket.on(MessageType.ChatJoin, (nickname, time) => {
  createChatJoinMessage(nickname)
});

socket.on(MessageType.ChatMessage, (nickname, msg, time) => {
  createChatMessage(nickname, msg, time);
});

socket.on(MessageType.ChatLeave, (nickname, time) => {
  createChatLeaveMessage(nickname);
});