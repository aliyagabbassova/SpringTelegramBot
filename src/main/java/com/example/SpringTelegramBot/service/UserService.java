package com.example.SpringTelegramBot.service;

import com.example.SpringTelegramBot.entity.User;
import com.example.SpringTelegramBot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.glassfish.grizzly.http.util.TimeStamp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User saveUser(Long telegramId, String firstName, String lastName, String userName, TimeStamp registeredAt) {
        Optional<User> existingUser = userRepository.findByTelegramId(telegramId);
        return existingUser.orElseGet(() -> userRepository.save(new User()));
    }
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> getUserByTelegramId(Long telegramId) {
        Optional<User> user = userRepository.findByTelegramId(telegramId);
        System.out.println("Найден пользователь: " + user);
        return user;
    }

}
