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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
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
            String lastName = update.getMessage().getChat().getLastName();
            System.out.println("Сообщение от: " + firstName + " | Текст: " + messageText);

            // Создаем объект User перед вызовом userService
            User user = new User();
            user.setTelegramId(telegramId);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUserName(update.getMessage().getFrom().getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            switch (messageText) {
                case "/start":
                    userService.registerOrUpdateUser(user);
                    String answer = EmojiParser.parseToUnicode("Здравствуйте, " + firstName + "!" + ":blush:");
                    log.info("Replied to user {} with answer: {}", firstName, answer);
                    sendMessage(telegramId, answer);
                    break;

                case "/mydata":
                    Optional<User> userData = userService.getUserByTelegramId(telegramId);
                    if (userData.isPresent()) {

                        String userInfo = "Ваши данные:\n" +
                                "Имя: " + user.getFirstName() + "\n" +
                                "Фамилия: " + (user.getLastName() != null ? user.getLastName() : "Не указана") + "\n" +
                                "Username: " + (user.getUserName() != null ? user.getUserName() : "Не указан") + "\n" +
                                "Дата регистрации: " + user.getRegisteredAt();
                        sendMessage(telegramId, userInfo);
                    } else {
                        sendMessage(telegramId, "Ваши данные не найдены.");
                    }
                    break;
                case "/deletedata":
                    boolean deleted = userService.deleteUserByTelegramId(telegramId);
                    if (deleted) {
                        sendMessage(telegramId, "Ваши данные были успешно удалены.");
                    } else {
                        sendMessage(telegramId, "Ваши данные не найдены.");
                    }
                    break;

                case "/help":
                    sendMessage(telegramId, HELP_TEXT);
                    break;

                case "/phone":
                    requestPhoneNumber(telegramId);  // Вызывает метод с кнопкой для отправки номера
                    break;

                default:
                    sendMessage(telegramId, "Sorry, command was not recognized.");
                    break;
            }
        }
        if (update.hasMessage()) {
            Message message = update.getMessage();

            if (message.hasContact() && message.getContact() != null) {
                if (message.hasContact()) {
                    String phoneNumber = message.getContact().getPhoneNumber();
                    Long telegramId = message.getFrom().getId();

                    userService.savePhoneNumber(telegramId, phoneNumber);
                    sendMessage(telegramId, "Спасибо! Ваш номер сохранён: " + phoneNumber);
                    return;
                }
            }
            String messageText = message.getText();
            Long telegramId = message.getFrom().getId();
            String phoneNumber = update.getMessage().getContact().getPhoneNumber();
            Optional<User> userOptional = userRepository.findByTelegramId(telegramId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setPhoneNumber(phoneNumber);
                userRepository.save(user);
                sendMessage(telegramId, "Ваш номер телефона успешно сохранён! 📲");
            } else {
                sendMessage(telegramId, "Сначала используйте /start для регистрации.");
            }

            if ("/phone".equals(messageText)) {
                requestPhoneNumber(telegramId);
            }
        }

    }

    private void sendMessage(Long telegramId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(telegramId));
        message.setText(textToSend);
        message.setReplyMarkup(getMainKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: {}", e.getMessage());

        }
    }

    public void requestPhoneNumber(Long telegramId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(telegramId));
        message.setText("Пожалуйста, отправьте свой номер телефона:");

        KeyboardButton phoneButton = new KeyboardButton("📞 Отправить номер");
        phoneButton.setRequestContact(true);  // Важный параметр для запроса контакта

        KeyboardRow row = new KeyboardRow();
        row.add(phoneButton);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при запросе номера телефона: {}", e.getMessage());
        }
    }
    public ReplyKeyboardMarkup getMainKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("/phone"));


        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row1));
        keyboardMarkup.setResizeKeyboard(true); // Автоматическая подгонка клавиатуры
        keyboardMarkup.setOneTimeKeyboard(false); // Клавиатура будет оставаться активной

        return keyboardMarkup;
    }

}



