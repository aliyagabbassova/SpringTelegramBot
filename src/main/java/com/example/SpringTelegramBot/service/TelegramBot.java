package com.example.SpringTelegramBot.service;

import com.example.SpringTelegramBot.config.BotConfig;
import com.example.SpringTelegramBot.entity.User;
import com.example.SpringTelegramBot.repository.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final BotConfig config;
    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command \n\n"+
            "Type /start to see a welcome message \n\n" +
            "Type /mydata to see data stored about yourself \n\n" +
            "Type /help to see this help message";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    public TelegramBot(UserRepository userRepository, BotConfig config) {
        this.userRepository = userRepository;
        this.config = config;

        List<BotCommand> listOfCommands = List.of(
                new BotCommand("/start", "get a welcome message"),
                new BotCommand("/mydata", "get your data stored"),
                new BotCommand("/deletedata", "delete my data"),
                new BotCommand("/help", "info how to use this bot"),
                new BotCommand("/settings", "set your preferences")
        );

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: {}", e.getMessage());
        }
    }

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
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            System.out.println("Получено обновление: " + update);
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String firstName = update.getMessage().getChat().getFirstName();
            System.out.println("Сообщение от: " + firstName + " | Текст: " + messageText);

            switch (messageText) {
                case "/start":
                    registerUser(update);
                    String answer = EmojiParser.parseToUnicode("Hi, " + firstName + "! Nice to meet you!" + ":blush:" );
                    log.info("Replied to user" + firstName + " with answer: " + answer);
                    sendMessage(chatId, answer);
                    break;

                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;

                default:
                    sendMessage(chatId, "Sorry, command was not recognized.");
                    break;
            }
        }
    }

    private void registerUser(Update update) {
        long telegramId = update.getMessage().getFrom().getId();
        System.out.println("Начало регистрации пользователя с ID: " + telegramId);
        Optional<User> existingUser = userRepository.findByTelegramId(telegramId);
        System.out.println("Результат поиска: " + existingUser.orElse(null));

        if (existingUser.isPresent()) {
            System.out.println("Пользователь уже зарегистрирован: " + existingUser.get());
            return;
        }

        if (existingUser.isEmpty()) {
            User user = new User();
            user.setTelegramId(telegramId);
            user.setFirstName(update.getMessage().getFrom().getFirstName());
            user.setLastName(update.getMessage().getFrom().getLastName());
            user.setUserName(update.getMessage().getFrom().getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            registerUserData(user);

        }
    }
    private void sendMessage(long telegramId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(telegramId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: {}", e.getMessage());

        }
    }
    @Transactional
    public void registerUserData(User user) {
        userRepository.save(user);
        System.out.println("Пользователь сохранен в базу данных: " + user);
        log.info("User saved: {}", user);
    }
//    @Transactional
//    public void registerOrUpdateUser(User user) {
//        Optional<User> existingUser = userRepository.findById(user.getTelegramId());
//
//        if (existingUser.isPresent()) {
//            User existing = existingUser.get();
//            existing.setFirstName(user.getFirstName());
//            existing.setLastName(user.getLastName());
//            existing.setUserName(user.getUserName());
//            existing.setRegisteredAt(user.getRegisteredAt());
//            user = existing; // Используем загруженный объект
//        }
//
//        userRepository.save(user); // Теперь Hibernate не выбросит ошибку
//    }

}
