package com.alibou.websocket.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        FileInputStream serviceAccount =
                new FileInputStream("E:\\Hoc\\Ky 5\\SWP391\\firebase\\chat-d8802-firebase-adminsdk-v44xe-3e2f56978a.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket("chat-d8802.appspot.com")
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public StorageClient storageClient() throws IOException {
        FirebaseApp firebaseApp = firebaseApp();
        return StorageClient.getInstance(firebaseApp);
    }

}
