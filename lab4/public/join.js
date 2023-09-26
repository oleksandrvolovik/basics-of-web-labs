const socket = io();

const form = document.getElementById('form');
const nicknameInput = document.getElementById('nickname_input');
const roomInput = document.getElementById('room_input');
const rooms = document.getElementById('rooms');
const usernameTakenMessage = document.getElementById('username_taken_message');

window.onload = function () {
  nicknameInput.value = localStorage.getItem("nickname")
  roomInput.value = localStorage.getItem("room")
  socket.emit(MessageType.GetRoomInfo);
};

class MessageType {
  static GetRoomInfo = "GetRoomInfo";
  static RoomInfo = "RoomInfo";
  static CheckNicknameAvailability = "CheckNicknameAvailability";
  static NicknameAvailable = "NicknameAvailable";
  static NicknameTaken = 'NicknameTaken';
}

form.addEventListener('submit', (e) => {
  e.preventDefault();
  if (nicknameInput.value) {
    if (roomInput.value) {
      localStorage.setItem("room", roomInput.value);
    } else {
      localStorage.setItem("room", "Public");
    }
    socket.emit(MessageType.CheckNicknameAvailability, nicknameInput.value)
  }
});

socket.on(MessageType.RoomInfo, (roomNames, populations) => {
  roomNames.forEach((roomName, index) => {
    const item = document.createElement('li');
    item.textContent = `${roomName} : ${populations[index]}`;
    rooms.appendChild(item);
  });
});

socket.on(MessageType.NicknameAvailable, () => {
    localStorage.setItem("nickname", nicknameInput.value);
    window.location.href = "chat";
})

socket.on(MessageType.NicknameTaken, () => {
  usernameTakenMessage.style.display = 'block'
  nicknameInput.classList.add('error-input');
});