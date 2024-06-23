import React, {useState, useEffect, useRef, useCallback} from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import axios from 'axios';

const ChatComponent = () => {
    const [stompClient, setStompClient] = useState(null);
    const [currentUser, setCurrentUser] = useState(null);
    const [userId, setUserId] = useState(null);
    const [userSaleStaff, setUserSaleStaff] = useState(null);
    const [selectedUserId, setSelectedUserId] = useState(null);

    const selectedUserIdRef = useRef(null);

    const usernamePageRef = useRef(null);
    const chatPageRef = useRef(null);
    const usernameFormRef = useRef(null);
    const messageFormRef = useRef(null);
    const messageInputRef = useRef(null);
    const imageInputRef = useRef(null);
    const connectingElementRef = useRef(null);
    const chatAreaRef = useRef(null);
    const roleSelectListRef = useRef(null);
    const roleSelectRef = useRef(null);

    useEffect(() => {
        // componentDidMount equivalent
        usernameFormRef.current.addEventListener('submit', connect);
        messageFormRef.current.addEventListener('submit', sendMessage);
        window.addEventListener('beforeunload', onLogout);
        roleSelectRef.current.addEventListener('change', onRoleChange);

        return () => {
            // componentWillUnmount equivalent
            if (stompClient) {
                stompClient.publish({
                    destination: "/app/user.disconnectUser",
                    body: JSON.stringify({ id: userId }),
                });
                stompClient.deactivate();
            }
        };
    }, [stompClient, userId]);

    useEffect(() => {
        if (stompClient) {
            stompClient.onConnect = onConnected;
            stompClient.onStompError = onError;
            stompClient.activate();
        }
    }, [onConnected, stompClient]);

    useEffect(() => {
        selectedUserIdRef.current = selectedUserId;
        if (selectedUserId !== null) {
            fetchAndDisplayUserChat();
        }
    }, [selectedUserId]);

    const connect = useCallback(async (event) => {
        event.preventDefault();
        const enteredUserId = usernameFormRef.current.querySelector('#id').value.trim();
        if (enteredUserId) {
            try {
                const [saleStaffResponse, userResponse] = await Promise.all([
                    axios.get(`http://localhost:8083/${enteredUserId}/sale-staff`),
                    axios.get(`http://localhost:8083/user/check/${enteredUserId}`)
                ]);
                setUserSaleStaff(saleStaffResponse.data);
                setCurrentUser(userResponse.data);
                setUserId(userResponse.data.id);
                onUserFound(userResponse.data);
            } catch (error) {
                handleConnectError(error);
            }
        } else {
            alert('Please enter a user ID.');
        }
    }, []);


    const onUserFound = useCallback(() => {
        usernamePageRef.current.classList.add('hidden');
        chatPageRef.current.classList.remove('hidden');

        const socket = new SockJS('http://localhost:8083/ws', null, { withCredentials: true });
        const client = new Client({
            webSocketFactory: () => socket,
            onConnect: onConnected,
            onStompError: onError,
        });
        setStompClient(client);
        client.activate();
    }, []);

    async function onConnected() {
        console.log('Connected to WebSocket');
        console.log('stompClient value:', stompClient);

        if (stompClient) {
            // Subscribe to necessary channels
            stompClient.subscribe(`/user/${userId}/queue/messages`, onMessageReceived);
            stompClient.subscribe(`/topic/public`, onMessageReceived);

            // Perform operations that depend on stompClient being ready
            document.querySelector('#connected-user-fullname').textContent = currentUser.name;
            await findAndDisplayConnectedUsers();
            await fetchUnreadMessages();
        } else {
            console.error('stompClient is null in onConnected. WebSocket connection may not be properly established.');
        }
    }

    async function fetchUnreadMessages() {
        try {
            const unreadMessagesResponse = await axios.get(`http://localhost:8083/unread-messages/${userId}`);
            const unreadMessages  = unreadMessagesResponse.data;
            if (unreadMessages && unreadMessages.length > 0) {
                unreadMessages.forEach(message => {
                    const notifiedUser = document.querySelector(`#${message.senderId}`);
                    if (notifiedUser) {
                        const nbrMsg = notifiedUser.querySelector('.nbr-msg');
                        if (nbrMsg) {
                            nbrMsg.classList.remove('hidden');
                            nbrMsg.textContent = parseInt(nbrMsg.textContent) + 1;
                        }
                    }
                });
            } else {
                console.log('No unread messages found.');
            }
        } catch (error) {
            if (error.response && error.response.status === 204) {
                console.log('No content found for unread messages.');
            } else {
                console.error('Failed to fetch unread messages:', error.message);
            }
        }
    }


    async function findAndDisplayConnectedUsers() {
        if (currentUser) {
            try {
                if (currentUser.role === "CUSTOMER") {
                    roleSelectListRef.current.classList.add('hidden');
                    if (userSaleStaff !== "") {
                        const response = await axios.get(`http://localhost:8083/user/check/${userSaleStaff}`);
                        const user = response.data;
                        await renderConnectedUsers([user]);
                    }
                } else {
                    roleSelectListRef.current.classList.remove('hidden');
                    const response = await axios.get(`http://localhost:8083/users/${roleSelectRef.current.value}`);
                    const users = response.data;
                    await renderConnectedUsers(users.filter(user => user.id !== userId));
                }
            } catch (error) {
                console.error('Error fetching and displaying connected users:', error);
            }
        }
    }

    function renderConnectedUsers(users) {
        const connectedUsersList = document.getElementById('connectedUsers');
        connectedUsersList.innerHTML = '';

        if (users.length === 0) {
            const noUsersMessage = document.createElement('p');
            noUsersMessage.textContent = 'No users connected';
            connectedUsersList.appendChild(noUsersMessage);
        } else {
            users.forEach(user => {
                const listItem = createUserElement(user);
                connectedUsersList.appendChild(listItem);
            });
        }
    }

    const createUserElement = (user) => {
        const listItem = document.createElement('li');
        listItem.classList.add('user-item');
        listItem.id = user.id;

        const userImage = document.createElement('img');
        userImage.src = '../img/user_icon.png';
        userImage.alt = user.id;

        const usernameSpan = document.createElement('span');
        usernameSpan.textContent = user.name;

        const receivedMsgs = document.createElement('span');
        receivedMsgs.textContent = '0';
        receivedMsgs.classList.add('nbr-msg', 'hidden');

        listItem.appendChild(userImage);
        listItem.appendChild(usernameSpan);
        listItem.appendChild(receivedMsgs);

        listItem.addEventListener('click', userItemClick);

        return listItem;
    }

    function onRoleChange() {
        findAndDisplayConnectedUsers().then(() => {
            if (chatAreaRef.current) {
                chatAreaRef.current.classList.add('hidden');
            }
        });
    }

    function markMessagesAsRead(recipientId) {
        try {
            axios.post(`http://localhost:8083/mark-messages-as-read/${recipientId}`);
            console.log(`Messages for ${recipientId} marked as read.`);
        } catch (error) {
            console.error(`Failed to mark messages as read for ${recipientId}:`, error.message);
        }
    }


    function userItemClick(event) {
        document.querySelectorAll('.user-item').forEach(item => {
            item.classList.remove('active');
        });
        messageFormRef.current.classList.remove('hidden');

        const clickedUser = event.currentTarget;
        clickedUser.classList.add('active');

        setSelectedUserId(clickedUser.getAttribute('id'));

        markMessagesAsRead(userId);

        const nbrMsg = clickedUser.querySelector('.nbr-msg');
        nbrMsg.classList.add('hidden');
        nbrMsg.textContent = '0';
    }

    function displayMessage(senderId, content) {
        const messageContainer = document.createElement('div');
        messageContainer.classList.add('message');
        if (senderId === userId) {
            messageContainer.classList.add('sender');
        } else {
            messageContainer.classList.add('receiver');
        }

        let messageElement;
        if (content.startsWith('https://')) {
            messageElement = document.createElement('img');
            messageElement.src = content;
            messageElement.alt = 'Uploaded image';
            messageElement.classList.add('uploaded-image');
        } else {
            messageElement = document.createElement('p');
            messageElement.textContent = content;
        }

        messageContainer.appendChild(messageElement);
        chatAreaRef.current.appendChild(messageContainer);
        chatAreaRef.current.scrollTop = chatAreaRef.current.scrollHeight;
    }

    async function fetchAndDisplayUserChat() {
        try {
            const response = await axios.get(`http://localhost:8083/messages/${userId}/${selectedUserId}`);
            const userChat = response.data;
            chatAreaRef.current.innerHTML = '';
            userChat.forEach(chat => {
                displayMessage(chat.senderId, chat.content);
            });
        } catch (error) {
            if (error.response && error.response.status === 404) {
                chatAreaRef.current.innerHTML = '';
                console.log('No chat messages found.');
            } else {
                console.error('Failed to fetch user chat:', error.message);
            }
        }
    }

    function onError(error) {
        connectingElementRef.current.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
        connectingElementRef.current.style.color = 'red';
        console.error('WebSocket connection error:', error);
    }

    function sendMessage(event) {
        event.preventDefault();
        const messageContent = messageInputRef.current.value.trim();
        if (messageContent && stompClient) {
            const chatMessage = {
                senderId: userId,
                recipientId: selectedUserIdRef.current,
                content: messageContent,
                timestamp: new Date()
            };
            stompClient.publish({
                destination: "/app/chat",
                body: JSON.stringify(chatMessage)
            });
            console.log('Message sent:', chatMessage);
            displayMessage(userId, messageContent);
            messageInputRef.current.value = '';
        }


        chatAreaRef.current.scrollTop = chatAreaRef.current.scrollHeight;
    }

    function onMessageReceived(payload) {
        console.log('PAYLOAD', payload);
        const message = JSON.parse(payload.body);

        console.log("selectedUserId: ", selectedUserId);
        if (selectedUserIdRef.current && selectedUserIdRef.current === message.senderId) {
            displayMessage(message.senderId, message.content);
            console.log('Message received and displayed.');
            chatAreaRef.current.scrollTop = chatAreaRef.current.scrollHeight;
        } else {
            const notifiedUser = document.querySelector(`#${message.senderId}`);
            if (notifiedUser && !notifiedUser.classList.contains('active')) {
                const nbrMsg = notifiedUser.querySelector('.nbr-msg');
                if (nbrMsg) {
                    nbrMsg.classList.remove('hidden');
                    nbrMsg.textContent = parseInt(nbrMsg.textContent) + 1;
                }
                console.log('Message notification sent:', nbrMsg);
            }
        }
    }

    function onLogout() {
        if (stompClient) {
            stompClient.publish({
                destination: "http://localhost:8083/app/user.disconnectUser",
                body: JSON.stringify({ id: userId, status: 'OFFLINE' })
            });
            stompClient.deactivate();
        }
        window.location.reload();
    }

    async function handleImageUpload(event) {
        const imageFile = event.target.files[0];
        if (imageFile) {
            try {
                const formData = new FormData();
                formData.append('senderId', userId);
                formData.append('recipientId', selectedUserIdRef.current);
                formData.append('message', messageInputRef.current.value);

                const resizedImageFile = await resizeImage(imageFile);
                formData.append('file', resizedImageFile);

                const response = await axios.post('http://localhost:8083/chat/upload', formData, {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                });

                const imageURL = response.data;
                displayMessage(userId, imageURL);

                messageInputRef.current.value = '';
                imageInputRef.current.value = '';
            } catch (error) {
                console.error('Error uploading image:', error);
                alert('Failed to upload image. Please try again later.');
            }
        }
    }

    function resizeImage(imageFile) {
        const maxSize = 1024;
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.readAsDataURL(imageFile);
            reader.onload = function (event) {
                const img = new Image();
                img.src = event.target.result;
                img.onload = function () {
                    let width = img.width;
                    let height = img.height;

                    if (width > height) {
                        if (width > maxSize) {
                            height *= maxSize / width;
                            width = maxSize;
                        }
                    } else {
                        if (height > maxSize) {
                            width *= maxSize / height;
                            height = maxSize;
                        }
                    }

                    const canvas = document.createElement('canvas');
                    canvas.width = width;
                    canvas.height = height;
                    const ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);

                    canvas.toBlob((blob) => {
                        resolve(new File([blob], imageFile.name, { type: imageFile.type }));
                    }, imageFile.type);
                };
            };
            reader.onerror = reject;
        });
    }

    const handleConnectError = useCallback((error) => {
        if (error.response) {
            if (error.response.status === 404) {
                alert('User not found. Please enter a valid ID.');
            } else {
                console.error('Error:', error.response.statusText);
                alert('Error. Please try again later.');
            }
        } else {
            console.error('Error:', error.message);
            alert('An unexpected error occurred. Please try again later.');
        }
    }, []);

    return (
        <div>
            <div className="user-form" ref={usernamePageRef}>
                <h2>Enter Chatroom</h2>
                <form ref={usernameFormRef}>
                    <label htmlFor="id">UserId:</label>
                    <input type="text" id="id" name="id" required />
                    <button type="submit">Enter Chatroom</button>
                </form>
            </div>

            <div className="chat-container hidden" ref={chatPageRef}>
                <div className="users-list">
                    <div className="users-list-container">
                        <h2>Online Users</h2>
                        <label htmlFor="role-select"></label>
                        <div className="role-select-list hidden" ref={roleSelectListRef}>
                            <select id="role-select" ref={roleSelectRef}>
                                <option value="CUSTOMER">Customer</option>
                                <option value="STAFF">Staff</option>
                                <option value="MANAGER">Manager</option>
                            </select>
                        </div>
                        <ul id="connectedUsers"></ul>
                    </div>
                    <div>
                        <p id="connected-user-fullname"></p>
                        <button className="logout" onClick={onLogout}>Logout</button>
                    </div>
                </div>

                <div className="chat-area">
                    <div className="chat-area" id="chat-messages" ref={chatAreaRef}></div>

                    <form id="messageForm" name="messageForm" className="hidden" ref={messageFormRef}>
                        <div className="message-input">
                            <input
                                autoComplete="off"
                                type="text"
                                id="message"
                                placeholder="Type your message..."
                                writingsuggestions="enabled"
                                ref={messageInputRef}
                            />
                            <input onChange={handleImageUpload} type="file" id="imageInput" accept="image/*"
                                   ref={imageInputRef}/>
                            <button>Send</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default ChatComponent;

