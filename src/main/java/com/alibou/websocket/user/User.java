package com.alibou.websocket.user;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

    private String id;

    private String name;
    private String role;
    private String saleStaff;

}
