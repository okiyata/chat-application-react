package com.alibou.websocket.listener;

import com.alibou.websocket.account.Account;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class GlobalEntityListener {

    private static final Logger log = LoggerFactory.getLogger(GlobalEntityListener.class);

    private final ApplicationEventPublisher eventPublisher;

    public GlobalEntityListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostPersist
    @PostUpdate
    @PostRemove
    public void onChange(Object entity) {
        log.info("Entity changed: {}", entity);

        if (entity instanceof Account) {
            eventPublisher.publishEvent(new AccountChangedEvent((Account) entity));
        }
    }

    public static class AccountChangedEvent {
        private final Account account;

        public AccountChangedEvent(Account account) {
            this.account = account;
        }

        public Account getAccount() {
            return account;
        }
    }
}
