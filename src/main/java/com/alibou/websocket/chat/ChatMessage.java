package com.alibou.websocket.chat;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {

    @DocumentId
    private String id;

    private String chatId;
    private String senderId;
    private String recipientId;
    private String content;

    @JsonDeserialize(using = TimestampDeserializer.class)
    private Timestamp timestamp;

}
