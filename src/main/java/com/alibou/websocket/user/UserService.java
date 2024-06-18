package com.alibou.websocket.user;

import com.alibou.websocket.account.Account;
import com.alibou.websocket.account.AccountRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final String USER_COLLECTION_NAME = "users";
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final AccountRepository accountRepository;

    public UserService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void syncUsersToFirestore() {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference usersCollection = db.collection(USER_COLLECTION_NAME);
        List<Account> accounts = accountRepository.findAll(); // Lấy tất cả các tài khoản từ SQL

        for (Account account : accounts) {
            // Tạo document với ID là ID của tài khoản từ SQL
            DocumentReference docRef = usersCollection.document(account.getId());

            // Chuẩn bị dữ liệu cho Firestore document
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", account.getId());
            userData.put("name", account.getUserInfo().getFirstName());
            userData.put("role", account.getRole().toString());
            userData.put("saleStaff", account.getSaleStaff());

            ApiFuture<WriteResult> result = docRef.set(userData);
            try {
                result.get();
                log.info("Syncing user {} to Firestore {}", account.getId(), docRef.getId());
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error writing document: " + e);
            }
        }
    }

    public void disconnect(User user) {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection(USER_COLLECTION_NAME).document(user.getId());
        ApiFuture<WriteResult> future = docRef.update("status", "OFFLINE");
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error disconnecting user", e);
        }
    }

    public List<User> findUsersByRole(String role) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<User> users = new ArrayList<>();
        ApiFuture<QuerySnapshot> future;

        if (role.equals("STAFF")) {
            // Lấy tất cả người dùng
            future = db.collection(USER_COLLECTION_NAME).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            // Lọc những người dùng có role kết thúc bằng 'STAFF'
            for (DocumentSnapshot document : documents) {
                String userRole = document.getString("role");
                if (userRole != null && userRole.endsWith("STAFF")) {
                    users.add(document.toObject(User.class));
                }
            }
        } else {
            // Lấy những người dùng có role chính xác
            future = db.collection(USER_COLLECTION_NAME)
                    .whereEqualTo("role", role)
                    .get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (DocumentSnapshot document : documents) {
                users.add(document.toObject(User.class));
            }
        }
        return users;
    }


    public User findUserById(String userId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection(USER_COLLECTION_NAME).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(User.class);
        } else {
            return null;
        }
    }

    public String findSaleStaffById(String id) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<DocumentSnapshot> future = db.collection(USER_COLLECTION_NAME).document(id).get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return document.getString("saleStaff");
        } else {
            throw new RuntimeException("User not found");
        }
    }

    @Scheduled(fixedRate = 60000) // Đồng bộ hóa mỗi 1 phút
    public void syncData() {
        syncUsersToFirestore();
    }

}
