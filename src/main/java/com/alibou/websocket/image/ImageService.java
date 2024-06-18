package com.alibou.websocket.image;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class ImageService {

    public String uploadImage(MultipartFile file) throws IOException {
        // Lấy bucket mặc định từ Firebase Storage
        Bucket bucket = StorageClient.getInstance().bucket();

        // Kiểm tra xem bucket có null hay không để tránh lỗi NullPointerException
        if (bucket == null) {
            throw new IOException("Unable to access default bucket. Check your Firebase configuration.");
        }

        // Tạo tên file với đường dẫn "folder" chat-images
        String fileName = "chat-images/" + generateFileName(file.getOriginalFilename());

        // Tạo blob từ file và lưu vào bucket
        Blob blob = bucket.create(fileName, file.getBytes(), file.getContentType());

        // Tạo URL truy cập tệp đã tải lên
        String imageUrl = String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media", bucket.getName(), fileName.replace("/", "%2F"));

        // Trả về URL của file đã tải lên
        return imageUrl;
    }

    private String generateFileName(String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uniqueID = UUID.randomUUID().toString().replace("-", "");
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return timestamp + "-" + uniqueID + extension;
    }
}
