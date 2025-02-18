package com.example.SpringTelegramBot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
@Getter
@Setter
@Entity
public class BotEntity {
    @jakarta.persistence.Id
    @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String botName;

        @ManyToOne
        @JoinColumn(name = "telegram_id")
        private User user; // ✅ Здесь ManyToOne, т.к. один User может иметь много ботов
    }


