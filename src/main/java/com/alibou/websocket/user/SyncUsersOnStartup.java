package com.alibou.websocket.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SyncUsersOnStartup implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SyncUsersOnStartup.class);
    private final UserService userService;

    public SyncUsersOnStartup(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Syncing users to Firestore on application startup");
        userService.syncUsersToFirestore();
        log.info("Users synced to Firestore successfully");
    }
}
