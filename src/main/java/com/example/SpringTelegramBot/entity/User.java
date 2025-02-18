package com.example.SpringTelegramBot.entity;

import com.example.SpringTelegramBot.service.TelegramBot;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "telegram_bot")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)

public class User {

    @Id
    @Column(unique = true,name = "telegramId")
    private Long telegramId;

    @Column(name = "firstName")
    private String firstName;

    @Column(name = "lastName")
    private String lastName;

    @Column(name = "userName")
    private String userName;

    @Column(name = "registeredAt")
    private Timestamp registeredAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<BotEntity> bots = new ArrayList<>();

    @Version
    private Integer version;

    @Override
    public String toString() {
        return "User{" +
                "telegramId=" + telegramId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", registeredAt=" + registeredAt +
                '}';
    }

}
