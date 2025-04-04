package com.example.SpringTelegramBot.service;

import com.example.SpringTelegramBot.config.BotConfig;
import com.example.SpringTelegramBot.entity.User;
import com.vdurmont.emoji.EmojiParser;
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
import java.util.*;

@Slf4j
@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final UserService userService;
    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command \n\n" +
            "Type /start to see a welcome message \n\n" +
            "Type /mydata to see data stored about yourself \n\n" +
            "Type /help to see this help message";
    private final Map<Long, String> userStates = new HashMap<>();

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    public void onUpdateReceived(Update update) {
        if (update != null && update.hasMessage()) {
            Message message = update.getMessage();
            Long telegramId = message.getFrom().getId();
//            String text = message.getText();
            String text = update.getMessage().getText();

            if (telegramId == null) {
                log.error("Ошибка: telegramId is null");
                return;
            }

            // ✅ Если пользователь отправляет контакт
            if (message.hasContact()) {
                String phoneNumber = message.getContact().getPhoneNumber();
                userService.savePhoneNumber(telegramId, phoneNumber);  // Сохраняем номер в базе
                sendMessage(telegramId, "Спасибо! Ваш номер сохранён: " + phoneNumber + ". Для дальнейшей работы необходимо ввести номер лицевого счета");
                return; // Прекращаем выполнение после сохранения контакта
            }
            if (update.hasMessage()) {
                if (update.getMessage().hasText()) {
//                    String textToSend = update.getMessage().getText();
                    log.info("Получено текстовое сообщение: {}", text);
                    sendMessage(update.getMessage().getChatId(), text);
                } else {
                    log.warn("Получено не текстовое сообщение от {}. Отправляем предупреждение.", update.getMessage().getChatId());
                    sendMessage(update.getMessage().getChatId(), "Пожалуйста, отправьте текстовое сообщение.");
                }
            } else {
                log.warn("Получено сообщение без текста от {}", update.getMessage().getChatId());
            }
//
            if (update.hasMessage() && update.getMessage().hasText()) {
//
                Long chatId = update.getMessage().getChatId();

                if (text!= null && !text.isEmpty()) {
                    SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
                    try {
                        execute(sendMessage);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    System.out.println("Ошибка: пустой текст в update.getMessage().getText(), messageText = " + text);
                }
                if (text == null || text.isBlank()) {
                    log.error("Ошибка: text = null или пуст. Сообщение не будет отправлено.");
                    return; // Прерываем выполнение, чтобы избежать исключения
                }
            }

            if (message.hasText()) {

                if ("Введите номер лицевого счета".equals(text)) {
//                    sendMessage(telegramId, "📝 Введите номер лицевого счета");
                    userStates.put(telegramId, "AWAITING_ACCOUNT_NUMBER"); // Сохраняем состояние
                    return;
                }

                // Проверяем, ожидает ли пользователь ввод лицевого счета
                log.info("User state for {}: {}", telegramId, userStates.get(telegramId));
                if ("AWAITING_ACCOUNT_NUMBER".equals(userStates.get(telegramId))) {
                    if (text.matches("\\d+")) {
                        Integer personalAccount = Integer.valueOf(text);
                        userService.savePersonalAccount(telegramId, personalAccount);

                        sendMessage(telegramId, "✅ Спасибо! Ваш лицевой счет сохранен: " + personalAccount + ". Следующие действия можете выбрать из списка");
                        userStates.remove(telegramId); // Сбрасываем состояние
                    } else {
                        sendMessage(telegramId, "❌ Пожалуйста, введите только цифры.");
                    }
                    return;
                }
                if ("Ввести показания счетчика".equals(text)) {
                    userStates.put(telegramId, "AWAITING_METER_READING"); // Сохраняем состояние
                    return;
                }

                // Проверяем, ожидает ли пользователь ввод показаний счетчика
                log.info("User state for {}: {}", telegramId, userStates.get(telegramId));
                if ("AWAITING_METER_READING".equals(userStates.get(telegramId))) {
                    if (text.matches("\\d+")) {
                        Integer lastMeterReading = Integer.valueOf(text);
                        userService.saveLastMeterReading(telegramId, lastMeterReading);

                        sendMessage(telegramId, "✅ Спасибо! Последние показания счетчика сохранены: " + lastMeterReading);
                        userStates.remove(telegramId); // Сбрасываем состояние
                    } else {
                        sendMessage(telegramId, "❌ Пожалуйста, введите только цифры.");
                    }
                    return;
                }
            }
            if (update.hasMessage() && update.getMessage().hasText()) {

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(String.valueOf(telegramId));

                // Добавляем клавиатуру
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                keyboardMarkup.setOneTimeKeyboard(true);

                List<KeyboardRow> keyboard = new ArrayList<>();

                keyboardMarkup.setKeyboard(keyboard);
                sendMessage.setReplyMarkup(keyboardMarkup);

                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }


            // ✅ Если пользователь отправляет текст
            if (message.hasText()) {
                String firstName = message.getChat().getFirstName();
                String lastName = message.getChat().getLastName();

                User user = new User();
                user.setTelegramId(telegramId);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setUserName(message.getFrom().getUserName());
                user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));


                switch (text) {
                    case "/start":
                        userService.registerOrUpdateUser(user);
                        String answer = EmojiParser.parseToUnicode(
                                "Здравствуйте, " + firstName + " :blush:! Добро пожаловать в телеграм бот Алматы Су. " +
                                        "Для успешной работы с ботом оставьте пожалуйста свой телефон. Нажмите на кнопку /phone"
                        );
                        sendMessage(telegramId, answer);
                        break;

                    case "/mydata":
                        userService.getUserByTelegramId(telegramId).ifPresentOrElse(
                                userData -> {
                                    String userInfo = "Ваши данные:\n" +
                                            "Имя: " + userData.getFirstName() + "\n" +
                                            "Фамилия: " + (userData.getLastName() != null ? userData.getLastName() : "Не указана") + "\n" +
                                            "Username: " + (userData.getUserName() != null ? userData.getUserName() : "Не указан") + "\n" +
                                            "Телефон: " + (userData.getPhoneNumber() != null ? userData.getPhoneNumber() : "Не указан") + "\n" +
                                            "Дата регистрации: " + userData.getRegisteredAt() + "\n" +
                                            "Номер лицевого счета: " + userData.getPersonalAccount();
                                    sendMessage(telegramId, userInfo);
                                },
                                () -> sendMessage(telegramId, "Ваши данные не найдены.")
                        );
                        break;

                    case "/deletedata":
                        boolean deleted = userService.deleteUserByTelegramId(telegramId);
                        sendMessage(telegramId, deleted ? "Ваши данные были успешно удалены." : "Ваши данные не найдены.");
                        break;

                    case "/help":
                        sendMessage(telegramId, HELP_TEXT);
                        break;

                    case "/phone":
                        requestPhoneNumber(telegramId);  // Кнопка для отправки номера
                        break;

                    case "/personal account":
                        requestPersonalAccount(telegramId);  //Кнопка для отправки лицевого счета
                        break;

                    case "/meter":
                        sendMessage(telegramId, "Введите новые показания счетчика воды:");
                        requestLastMeterReading(telegramId);
                        userStates.put(telegramId, "AWAITING_METER_READING");
                        break;



                    default:
                        sendMessage(telegramId, "Извините, команда не распознана. Используйте /help для просмотра доступных команд.");
                        break;
                }
            }
        }
    }

    private void sendMessage(Long telegramId, String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Попытка отправить пустое сообщение пользователю {}. Подставляем текст по умолчанию.", telegramId);
            text = "Ошибка: текст сообщения не может быть пустым!";
        }

        // Проверяем, есть ли состояние пользователя, и устанавливаем значение по умолчанию
        userStates.putIfAbsent(telegramId, "DEFAULT_STATE");

        log.info("Отправка сообщения пользователю {}: {}", telegramId, text);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(telegramId));
        message.setText(text);
        message.setReplyMarkup(getMainKeyboard(telegramId));

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения пользователю {}: {}", telegramId, e.getMessage(), e);
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

    private void requestLastMeterReading(Long telegramId) {
        SendMessage message = new SendMessage();
        message.setChatId(telegramId.toString());
        message.setText("Последние показания счетчика: Введите новые показания, если хотите обновить."); // Добавляем текст
//        // Создаём кнопку "Ввести показания"
//        KeyboardRow row7 = new KeyboardRow();
//
        // Добавляем строку кнопок в клавиатуру
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
//        keyboardMarkup.setKeyboard(List.of(row7));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        keyboardMarkup.setSelective(true);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);  // Отправляем сообщение с кнопкой
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке клавиатуры: {}", e.getMessage());
        }
    }

    private void requestPersonalAccount(Long telegramId) {
        SendMessage message = new SendMessage();
        message.setChatId(telegramId.toString());

        // Добавляем кнопку в ряд
        KeyboardRow row = new KeyboardRow();

        // Формируем клавиатуру
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row));
        keyboardMarkup.setResizeKeyboard(true);      // Автоматическое подстраивание клавиатуры
        keyboardMarkup.setOneTimeKeyboard(false);    // Если true, клавиатура исчезнет после нажатия
        keyboardMarkup.setSelective(true);           // Показывает клавиатуру только выбранным пользователям

        message.setReplyMarkup(keyboardMarkup);      // Прикрепляем клавиатуру к сообщению

        try {
            execute(message);                         // Отправляем сообщение
        } catch (TelegramApiException e) {
            log.error("Ошибка при запросе лицевого счета: {}", e.getMessage());
        }
    }

    public ReplyKeyboardMarkup getMainKeyboard(Long telegramId) {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("/phone"));

        KeyboardRow row6 = new KeyboardRow();
        row6.add(new KeyboardButton("Введите номер лицевого счета"));

        // Проверяем, есть ли у пользователя номер лицевого счета
        boolean hasPersonalAccount = userService.hasPersonalAccount(telegramId);

        List<KeyboardRow> keyboardRows = new ArrayList<>(List.of(row1, row6));

        if (hasPersonalAccount) {
            KeyboardRow row7 = new KeyboardRow();
            row7.add(new KeyboardButton("Ввести показания счетчика"));

            KeyboardRow row5 = new KeyboardRow();
            row5.add(new KeyboardButton("Подсчитать расход воды"));

//            KeyboardRow row2 = new KeyboardRow();
//            row2.add(new KeyboardButton("Установка прибора учета"));
//
//            KeyboardRow row3 = new KeyboardRow();
//            row3.add(new KeyboardButton("Подключение к сетям водоотведения"));

//            KeyboardRow row4 = new KeyboardRow();
//            row4.add(new KeyboardButton("Подключение к сетям водоснабжения"));

            keyboardRows.addAll(List.of(row5, row7));
        }

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        return keyboardMarkup;
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}




