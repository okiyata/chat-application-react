package com.alibou.websocket.chat;

import com.alibou.websocket.chatroom.ChatRoomService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomService chatRoomService;

    private static final String CHAT_MESSAGES_COLLECTION_NAME = "chat_messages";
    private static final String UNREAD_MESSAGES_COLLECTION_NAME = "unread_messages";


    public ChatMessage save(ChatMessage chatMessage) {
        String chatId = chatRoomService
                .getChatRoomId(chatMessage.getSenderId(), chatMessage.getRecipientId(), true)
                .orElseThrow(() -> new RuntimeException("Failed to get chat room ID"));

        chatMessage.setChatId(chatId);

        Firestore db = FirestoreClient.getFirestore();
        CollectionReference chatMessages = db.collection(CHAT_MESSAGES_COLLECTION_NAME);

        // Assuming you want Firestore to auto-generate the document ID
        ApiFuture<DocumentReference> result = chatMessages.add(chatMessage);

        try {
            DocumentReference documentReference = result.get();
            String messageId = documentReference.getId();
            chatMessage.setId(messageId);

            // Save unread message status
            CollectionReference unreadMessages = db.collection(UNREAD_MESSAGES_COLLECTION_NAME);
            unreadMessages.add(chatMessage);

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving chat message", e);
        }
        return chatMessage;
    }

    public List<ChatMessage> findChatMessages(String senderId, String recipientId) {
        String chatId = chatRoomService.getChatRoomId(senderId, recipientId, true)
                .orElseThrow(() -> new IllegalArgumentException("Chat room ID not found"));

        Firestore db = FirestoreClient.getFirestore();
        CollectionReference chatMessages = db.collection(CHAT_MESSAGES_COLLECTION_NAME);

        List<ChatMessage> messages = new ArrayList<>();

        // Retrieve chat messages for the given chat ID
        ApiFuture<QuerySnapshot> future = chatMessages.whereEqualTo("chatId", chatId).get();
        try {
            QuerySnapshot documents = future.get();
            for (DocumentSnapshot document : documents) {
                ChatMessage message = document.toObject(ChatMessage.class);
                messages.add(message);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching chat messages", e);
        }

        messages.sort(Comparator.comparing(ChatMessage::getTimestamp));

        return messages;
    }

    public List<ChatMessage> findUnreadMessages(String recipientId) {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference unreadMessages = db.collection(UNREAD_MESSAGES_COLLECTION_NAME);

        List<ChatMessage> messages = new ArrayList<>();

        // Retrieve unread messages for the given recipient ID
        ApiFuture<QuerySnapshot> future = unreadMessages.whereEqualTo("recipientId", recipientId).get();
        try {
            QuerySnapshot documents = future.get();
            for (DocumentSnapshot document : documents) {
                ChatMessage message = document.toObject(ChatMessage.class);
                messages.add(message);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching unread messages", e);
        }

        messages.sort(Comparator.comparing(ChatMessage::getTimestamp));

        return messages;
    }

    public void markMessagesAsRead(String recipientId) {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference unreadMessages = db.collection(UNREAD_MESSAGES_COLLECTION_NAME);

        // Retrieve and delete unread messages for the given recipient ID
        ApiFuture<QuerySnapshot> future = unreadMessages.whereEqualTo("recipientId", recipientId).get();
        try {
            QuerySnapshot documents = future.get();
            WriteBatch batch = db.batch();
            for (DocumentSnapshot document : documents) {
                batch.delete(document.getReference());
            }
            batch.commit();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error marking messages as read", e);
        }
    }
}
