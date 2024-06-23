package com.alibou.websocket.firebase;

import com.alibou.websocket.account.Account;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FirestoreService {

    private static final Logger log = LoggerFactory.getLogger(FirestoreService.class);

    public void syncAccountToFirestore(Account account) {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference usersCollection = db.collection("users");
        DocumentReference docRef = usersCollection.document(account.getId());

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", account.getId());
        userData.put("name", account.getUserInfo().getFirstName());
        userData.put("role", account.getRole().toString());
        userData.put("saleStaff", account.getSaleStaff());

        ApiFuture<WriteResult> result = docRef.set(userData);
        try {
            result.get();
            log.info("Synced user {} to Firestore {}", account.getId(), docRef.getId());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error writing document: ", e);
        }
    }
}
