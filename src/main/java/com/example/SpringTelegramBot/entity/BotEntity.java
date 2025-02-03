package com.example.SpringTelegramBot.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
@Data
@Entity
public class BotEntity {
    @jakarta.persistence.Id
    @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String botName;

        @ManyToOne
        @JoinColumn(name = "telegram_id", nullable = false)
        private User user; // ✅ Здесь ManyToOne, т.к. один User может иметь много ботов

    }


