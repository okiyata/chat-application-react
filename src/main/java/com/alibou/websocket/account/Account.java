package com.alibou.websocket.account;

import com.alibou.websocket.enums.AccountStatus;
import com.alibou.websocket.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "is_staff", columnDefinition = "bit")
@DiscriminatorValue("0")
public class Account {
    @Id
    @Column(length = 8, nullable = false, updatable = false, unique = true)
    private String id;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;
    @Column
    private String saleStaff;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id", referencedColumnName = "id")
    private UserInfo userInfo;

}
