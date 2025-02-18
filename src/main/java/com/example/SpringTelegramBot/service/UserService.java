package com.example.SpringTelegramBot.service;

import com.example.SpringTelegramBot.entity.User;
import com.example.SpringTelegramBot.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.sql.Timestamp;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void registerUser(Update update) {
        Long telegramId = update.getMessage().getFrom().getId();
        Optional<User> optionalUser = userRepository.findByTelegramId(telegramId);

        User user = optionalUser.orElseGet(() -> {
            User newUser = new User();
            newUser.setTelegramId(telegramId);
            newUser.setFirstName(update.getMessage().getFrom().getFirstName());
            newUser.setLastName(update.getMessage().getFrom().getLastName());
            newUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            newUser.setVersion(0);  // Устанавливаем начальную версию
            return newUser;
        });

// Теперь сохраняем только если данные изменились
        userRepository.save(user);
        log.info("Попытка сохранить пользователя: {}", user);
    }


    @Transactional
    public void registerOrUpdateUser(User user) {

        log.info("Запуск метода registerOrUpdateUser() с пользователем: {}", user);

        Optional<User> existingUser = userRepository.findByTelegramId(user.getTelegramId());

        if (existingUser.isPresent()) {

            log.info("Пользователь найден в базе: {}", existingUser.get());
            User userToUpdate = existingUser.get();
            userToUpdate.setFirstName(user.getFirstName());
            userToUpdate.setLastName(user.getLastName());
            userToUpdate.setUserName(user.getUserName());

            userRepository.save(userToUpdate);
            log.info("Обновлены данные пользователя: {}", userToUpdate);
        } else {
            log.info("Пользователь не найден, создаём нового: {}", user);
            userRepository.save(user);  // <-- Проверить, доходит ли выполнение сюда
            log.info("Пользователь успешно сохранён!");
        }
    }
}
