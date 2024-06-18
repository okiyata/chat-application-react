package com.alibou.websocket.image;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/storage")
public class StorageController {

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Lấy bucket của Firebase Storage
            Bucket bucket = StorageClient.getInstance().bucket(); // Sử dụng StorageClient từ Firebase SDK

            String fileName = "P-" + System.currentTimeMillis() + "-" + file.getOriginalFilename();

            // Tạo blob từ file và lưu vào bucket
            Blob blob = bucket.create(fileName, file.getBytes(), file.getContentType());

            // Trả về URL của file đã tải lên
            return blob.getMediaLink();
        } catch (IOException e) {
            e.printStackTrace();
            return "Upload failed";
        }
    }
}
