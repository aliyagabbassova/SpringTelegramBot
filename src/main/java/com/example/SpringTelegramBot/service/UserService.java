package com.example.SpringTelegramBot.service;

import com.example.SpringTelegramBot.entity.User;
import com.example.SpringTelegramBot.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
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
    public void registerOrUpdateUser(User user) {

        log.info("Запуск метода registerOrUpdateUser() с пользователем: {}", user);

        Optional<User> existingUser = userRepository.findByTelegramId(user.getTelegramId());

        if (existingUser.isPresent()) {

            log.info("Пользователь найден в базе: {}", existingUser.get());
            User userToUpdate = existingUser.get();
            userToUpdate.setFirstName(user.getFirstName());
            userToUpdate.setLastName(user.getLastName());
            userToUpdate.setUserName(user.getUserName());
            userToUpdate.setPhoneNumber(user.getPhoneNumber());
            userToUpdate.setPersonalAccount(user.getPersonalAccount());

            userRepository.save(userToUpdate);
            log.info("Обновлены данные пользователя: {}", userToUpdate);
        } else {
            log.info("Пользователь не найден, создаём нового: {}", user);
            userRepository.save(user);  // <-- Проверить, доходит ли выполнение сюда
            log.info("Пользователь успешно сохранён!");
        }
    }
    public Optional<User> getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    @Transactional
    public boolean deleteUserByTelegramId(Long telegramId) {
            Optional<User> user = userRepository.findByTelegramId(telegramId);
            if (user.isPresent()) {
                userRepository.delete(user.get());
                log.info("Удалён пользователь с telegramId: {}", telegramId);
                return true;
            } else {
                log.warn("Не найден пользователь с telegramId: {}", telegramId);
                return false;
            }
        }

    @Transactional
    public void savePhoneNumber(Long telegramId, String phoneNumber) {
        Optional<User> userOptional = userRepository.findByTelegramId(telegramId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPhoneNumber(phoneNumber);
            userRepository.save(user);
            log.info("Сохранён номер телефона для пользователя {}: {}", telegramId, phoneNumber);
        } else {
            log.warn("Не удалось найти пользователя с telegramId: {}", telegramId);
        }
    }
    @Transactional
    public void savePersonalAccount(Long telegramId, Integer personalAccount) {
        Optional<User> userOptional = userRepository.findByTelegramId(telegramId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPersonalAccount(personalAccount);
            userRepository.save(user);
            log.info("Сохранён номер лицевого счета {}: {}", telegramId, personalAccount);
        } else {
            log.warn("Не удалось найти пользователя с telegramId: {}", telegramId);
        }
    }
    @Transactional
    public void saveLastMeterReading(Long telegramId, Integer lastMeterReading) {
        Optional<User> userOptional = userRepository.findByTelegramId(telegramId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setLastMeterReading(lastMeterReading);
            userRepository.save(user);
            log.info("Последние показания счетчика сохранены {}: {}", telegramId, lastMeterReading);
        } else {
            log.warn("Не удалось найти пользователя с telegramId: {}", telegramId);
        }
    }
    public boolean hasPersonalAccount(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .map(user -> user.getPersonalAccount() != null)
                .orElse(false);
    }
}

