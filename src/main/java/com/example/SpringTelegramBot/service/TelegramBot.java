package com.example.SpringTelegramBot.service;

import com.example.SpringTelegramBot.config.BotConfig;
import com.example.SpringTelegramBot.entity.User;
import com.example.SpringTelegramBot.repository.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;


@Slf4j
@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final BotConfig config;
    private final UserService userService;
    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command \n\n" +
            "Type /start to see a welcome message \n\n" +
            "Type /mydata to see data stored about yourself \n\n" +
            "Type /help to see this help message";

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        updates.forEach(this::onUpdateReceived);
    }

    public void onUpdateReceived(Update update) {
        if (update != null && update.hasMessage()) {
            Message message = update.getMessage();
            if (message != null && message.hasText()) {
                System.out.println("Получено обновление: " + update);
            }

            String messageText = update.getMessage().getText();
            Long telegramId = update.getMessage().getFrom().getId();
            if (telegramId == null) {
                log.error("Ошибка: telegramId is null");
                return;
            }

            String firstName = update.getMessage().getChat().getFirstName();
            System.out.println("Сообщение от: " + firstName + " | Текст: " + messageText);

            // Создаем объект User перед вызовом userService
            User user = new User();
            user.setTelegramId(telegramId);
            user.setFirstName(firstName);
            user.setLastName(user.getLastName());
            user.setUserName(update.getMessage().getFrom().getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            switch (messageText) {
                case "/start":
                    userService.registerOrUpdateUser(user);
                    String answer = EmojiParser.parseToUnicode("Hi, " + firstName + "! Nice to meet you!" + ":blush:");
                    log.info("Replied to user {} with answer: {}", firstName, answer);
                    sendMessage(telegramId, answer);
                    break;

                case "/help":
                    sendMessage(telegramId, HELP_TEXT);
                    break;

                default:
                    sendMessage(telegramId, "Sorry, command was not recognized.");
                    break;
            }
        }
    }

    private void sendMessage(Long telegramId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(telegramId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: {}", e.getMessage());

        }
    }
}


