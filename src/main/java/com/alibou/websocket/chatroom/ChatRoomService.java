package com.alibou.websocket.chatroom;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class ChatRoomService {

    private static final String CHAT_ROOM_COLLECTION_NAME = "chat_rooms";

    @Transactional
    public Optional<String> getChatRoomId(String senderId, String recipientId, boolean createNewRoomIfNotExists) {
        Firestore db = FirestoreClient.getFirestore();
        String chatId1 = createChatId(senderId, recipientId);
        String chatId2 = createChatId(recipientId, senderId);

        try {
            // Check for the existence of both possible chat IDs
            DocumentReference docRef1 = db.collection(CHAT_ROOM_COLLECTION_NAME).document(chatId1);
            ApiFuture<DocumentSnapshot> future1 = docRef1.get();
            DocumentSnapshot document1 = future1.get();

            if (document1.exists()) {
                return Optional.of(chatId1);
            }

            DocumentReference docRef2 = db.collection(CHAT_ROOM_COLLECTION_NAME).document(chatId2);
            ApiFuture<DocumentSnapshot> future2 = docRef2.get();
            DocumentSnapshot document2 = future2.get();

            if (document2.exists()) {
                return Optional.of(chatId2);
            }

            // If neither chat ID exists and createNewRoomIfNotExists is true, create a new chat room
            if (createNewRoomIfNotExists) {
                ChatRoom chatRoom = ChatRoom.builder()
                        .chatId(chatId1)
                        .senderId(senderId)
                        .recipientId(recipientId)
                        .build();
                db.collection(CHAT_ROOM_COLLECTION_NAME).document(chatId1).set(chatRoom);
                return Optional.of(chatId1);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error getting chat room", e);
        }

        return Optional.empty();
    }

    public String createChatId(String senderId, String recipientId) {
        return String.format("%s_%s", senderId, recipientId);
    }
}
