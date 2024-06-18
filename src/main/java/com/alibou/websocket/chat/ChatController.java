package com.alibou.websocket.chat;

import com.alibou.websocket.image.ImageService;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final ImageService imageService;

    @PostMapping("/chat/upload")
    public ResponseEntity<String> handleImageMessage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senderId") String senderId,
            @RequestParam("recipientId") String recipientId,
            @RequestParam(value = "message", required = false) String message
    ) {
        try {
            String imageUrl = imageService.uploadImage(file);

            // Create and save the chat message
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSenderId(senderId);
            chatMessage.setRecipientId(recipientId);
            chatMessage.setContent(imageUrl); // Use the URL of the uploaded image
            chatMessage.setTimestamp(Timestamp.of(new Date()));

            processMessage(chatMessage);

            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image");
        }
    }

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        ChatMessage savedMsg = chatMessageService.save(chatMessage);
        String recipientID = savedMsg.getRecipientId();

        log.info("Saved message: {}", savedMsg);
        ChatNotification chatNotification = ChatNotification.builder()
                .senderId(savedMsg.getSenderId())
                .recipientId(recipientID)
                .content(savedMsg.getContent())
                .build();

        log.info("Attempting to send notification to user {}: {}", chatMessage.getRecipientId(), chatNotification);

        messagingTemplate.convertAndSendToUser(
                chatMessage.getRecipientId(), "/queue/messages", chatNotification
        );

        log.info("Notification sent to user {}: {}", chatMessage.getRecipientId(), chatNotification);
    }

    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<ChatMessage>> findChatMessages(@PathVariable String senderId,
                                                              @PathVariable String recipientId) {
        List<ChatMessage> messages = chatMessageService.findChatMessages(senderId, recipientId);
        if (messages.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/unread-messages/{recipientId}")
    public ResponseEntity<List<ChatMessage>> findUnreadMessages(@PathVariable String recipientId) {
        List<ChatMessage> messages = chatMessageService.findUnreadMessages(recipientId);
        if (messages.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/mark-messages-as-read/{recipientId}")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable String recipientId) {
        chatMessageService.markMessagesAsRead(recipientId);
        return ResponseEntity.ok().build();
    }
}

