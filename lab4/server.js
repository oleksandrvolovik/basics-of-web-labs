const express = require('express');
const { createServer } = require('node:http');
const { Server } = require('socket.io');

const app = express();
const server = createServer(app);
const io = new Server(server);

app.use(express.static('public'))

var rooms = new Set() // Set назв кімнат
const socketIdsToUsernames = new Map(); // Map<socket.id, nickname>
const usernamesToSocketIds = new Map(); // Map<nickname, socket.id>
const messages = new Map(); // Map<roomName, Array<{ type, nickname, timestamp }>>

class MessageType {
  static ChatJoin = 'ChatJoin';
  static ChatMessage = 'ChatMessage';
  static ChatLeave = 'ChatLeave';
  static GetRoomInfo = "GetRoomInfo";
  static RoomInfo = "RoomInfo";
  static CheckNicknameAvailability = "CheckNicknameAvailability";
  static NicknameAvailable = "NicknameAvailable";
  static NicknameTaken = 'NicknameTaken';
}

io.on('connection', (socket) => {
  socket.on(MessageType.GetRoomInfo, () => {
    const realRoomNames = new Array()
    const populations = new Array()
    const curRooms = io.of("/").adapter.rooms
    // io.of("/").adapter.rooms містить кімнати, до яких підключений хоча б один сокет

    curRooms.forEach((socketsSet, roomName) => {
      if (rooms.has(roomName)) {
        realRoomNames.push(roomName)
        populations.push(socketsSet.size)
      }
    })

    // Очищення списку кімнат від пустих
    rooms = new Set(realRoomNames)
    io.to(socket.id).emit(MessageType.RoomInfo, realRoomNames, populations)
  });

  socket.on(MessageType.CheckNicknameAvailability, (nickname) => {
    if (usernamesToSocketIds.get(nickname)) {
      io.to(socket.id).emit(MessageType.NicknameTaken)
    } else {
      io.to(socket.id).emit(MessageType.NicknameAvailable)
    }
  })

  socket.on(MessageType.ChatJoin, (nickname, room) => {
    if (usernamesToSocketIds.get(nickname)) {
      // Nickname is already taken
      console.log(`${socket.id} tried to join as ${nickname}`)
      socket.disconnect()
    }

    socket.join(room);
    socketIdsToUsernames.set(socket.id, nickname);
    usernamesToSocketIds.set(nickname, socket.id)

    // Check if there are stored messages for the room
    if (messages.has(room)) {
      const storedMessages = messages.get(room);

      // Send stored messages to the new user
      storedMessages.forEach((message) => {
        emitMessage(socket.id, message);
      });
    }

    // Store the ChatJoin message
    const timestamp = new Date();
    const chatJoinMessage = { type: MessageType.ChatJoin, nickname, timestamp };
    storeMessage(room, chatJoinMessage);

    // Emit the ChatJoin message for users in the room
    emitMessage(room, chatJoinMessage);
    rooms.add(room);
    logMessage(room, chatJoinMessage);
  });

  socket.on(MessageType.ChatMessage, (nickname, msg) => {
    const rooms = Array.from(socket.rooms)
    const timestamp = new Date();

    // Create a new message object
    const message = { type: MessageType.ChatMessage, nickname, msg, timestamp };

    rooms.forEach(connectedRoom => {
      // Store the message in the messages map
      storeMessage(connectedRoom, message);
    });

    // Emit the message to all users in the room
    emitMessage(rooms, message);
    logMessage(rooms, message)
  });

  socket.on('disconnecting', () => {
    const nickname = socketIdsToUsernames.get(socket.id);
    const roomsArray = Array.from(socket.rooms);
    const timestamp = new Date();

    roomsArray.forEach((room) => {
      const chatLeaveMessage = { type: MessageType.ChatLeave, nickname, timestamp };
      storeMessage(room, chatLeaveMessage);
      emitMessage(room, chatLeaveMessage)
      logMessage(room, chatLeaveMessage)
    });

    socketIdsToUsernames.delete(socket.id);
    usernamesToSocketIds.delete(nickname)
  });
});

server.listen(3000, () => {
  console.log('Server running at http://localhost:3000');
});

function storeMessage(room, message) {
  if (!messages.has(room)) {
    messages.set(room, []);
  }
  messages.get(room).push(message);
}

function emitMessage(receiver, message) {
  if (message.type === MessageType.ChatMessage) {
    const { nickname, msg, timestamp } = message;
    io.to(receiver).emit(MessageType.ChatMessage, nickname, msg, timestamp);
  } else if (message.type === MessageType.ChatJoin) {
    const { nickname, timestamp } = message;
    io.to(receiver).emit(MessageType.ChatJoin, nickname, timestamp);
  } else if (message.type === MessageType.ChatLeave) {
    const { nickname, timestamp } = message;
    io.to(receiver).emit(MessageType.ChatLeave, nickname, timestamp);
  }
}

function logMessage(receiver, message) {
  if (message.type === MessageType.ChatMessage) {
    const { nickname, msg, timestamp } = message;
    console.log(`[${timestamp}] ${nickname} : ${msg}`);
  } else if (message.type === MessageType.ChatJoin) {
    const { nickname, timestamp } = message;
    console.log(`[${timestamp}] ${nickname} joined ${receiver}`);
  } else if (message.type === MessageType.ChatLeave) {
    const { nickname, timestamp } = message;
    if (nickname && receiver.length < 11) {
      console.log(`[${timestamp}] ${nickname} left ${receiver}`);
    }
  }
}