package ru.gnaizel.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.gnaizel.client.Client;
import ru.gnaizel.dto.user.UserCreateDto;
import ru.gnaizel.dto.user.UserShortDto;
import ru.gnaizel.model.CheckCommandState;
import ru.gnaizel.repository.user.UserRepository;
import ru.gnaizel.service.user.UserService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final UserService userService;

    @Value("${telegram.bot.token}")
    private String TELEGRAM_BOT_TOKEN;
    @Value("${telegram.bot.name}")
    private String TELEGRAM_BOT_USERNAME;
    @Value("${telegram.bot.download-path}")
    private String TELEGRAM_BOT_DOWNLOAD_PATH;

    private final Map<Long, CheckCommandState> userCheckCommandState = new HashMap<>();


    private final Client client;

    @Override
    public void onUpdateReceived(Update update) {
        setTelegramCommandList();

        handleGeneralMessage(update);
    }

    //Это будет отдельная кнопка для справки о командах бота
    private void setTelegramCommandList() {
        log.info("Иницилизация списка команд");
        List<BotCommand> botCommands = new ArrayList<>();

        botCommands.add(new BotCommand("/command", "command description"));

        try {
            execute(new SetMyCommands(botCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    // Обработка команды
    private void handleCommand(Update update) {
        String message = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        if (!userService.existCheckUser(chatId)) {
            UserShortDto newUser = userService.createUserByUpdateMassageTelegram(update);
            log.debug("New user has created \n User: userName- {}, telegramId- {}",
                    newUser.getUserName(),
                    newUser.getTelegramId());

            String helloMessage = String.format("Welcome %s !!!",
                    newUser.getUserName());
            sendMessage(chatId, helloMessage);
        }

        message = message.replace("/", "");

        switch (message) {
            case "check":
                startCheckCommand(update);
                break;
            case "status":
                sendMessage(chatId, "Функция статистики еще не реализована.");
                break;
            default:
                sendMessage(chatId, "Команда не поддерживается");
        }
    }

    // Глобальный обработчие сообщения (Проверки)
    private void handleGeneralMessage(Update update) {
        String message = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        //Проверяет находится ли пользователь уже в процессе
        if (userCheckCommandState.containsKey(chatId)) {
            handleCheckCommandContinuation(update);
        } else if (message.startsWith("/")) {
            handleCommand(update);
        } else {
            sendMessage(chatId, "Бот пока что не может обрабатывать что то кроме команд");
        }
    }

    // Начало обработки (/check)
    private void startCheckCommand(Update update) {
        long chatId = update.getMessage().getChatId();
        CheckCommandState state = new CheckCommandState();
        userCheckCommandState.put(chatId, state);

        sendMessage(chatId, "Пришлите мне URL, который нужно отпарить:");
    }


    // Процедура залива логов (/check)
    private void handleCheckCommandContinuation(Update update) {
        long chatId = update.getMessage().getChatId();
        CheckCommandState state = userCheckCommandState.get(chatId);

        switch (state.getStep()) {
            case 0:
                if (!update.getMessage().hasText()) {
                    sendMessage(chatId, "Пожалуйста, отправьте URL в текстовом формате");
                    return;
                }
                state.setUrl(update.getMessage().getText());
                state.setStep(1);
                sendMessage(chatId, "Теперь пришлите мне файл в формате txt:");
                break;

            case 1:
                if (!update.getMessage().hasDocument()) {
                    sendMessage(chatId, "Пожалуйста, отправьте файл как документ (не как фото или другой формат)");
                    return;
                }
                try {
                    java.io.File downloadFile = handleDocument(update);
                    log.info("Файл получен: {}", downloadFile.getAbsolutePath());

                    client.uploadLog(new FileSystemResource(downloadFile), chatId);
                    sendMessage(chatId, "Сервер 'Checker' получил ваш файл");

                    state.setFile(downloadFile.getName());
                    state.setStep(2);

                    sendMessage(chatId, "Обработка данных...\nURL: " + state.getUrl() + "\nFile: " + state.getFile());
                    client.checkRequest(state.getUrl(), chatId);

                } catch (Exception e) {
                    log.error("Ошибка обработки файла", e);
                    sendMessage(chatId, "Ошибка при обработке файла: " + e.getMessage());
                } finally {
                    userCheckCommandState.remove(chatId);
                }
                break;
        }
    }

    // Обработка докумета (как сообщения)
    private java.io.File handleDocument(Update update) {
        long chatId = update.getMessage().getChatId();
        Document document = update.getMessage().getDocument();
        String fileName = document.getFileName();
        String fileId = document.getFileId();
        java.io.File downloadFile;
        log.info("fileId: {}, fileName: {}/n Dockument: {}", fileId, fileName, document);
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        try {
            File file = execute(getFile);
            String filePathInTelegram = file.getFilePath();
            downloadFile = downloadFile(filePathInTelegram, fileName);
            sendMessage(chatId, "Файл " + fileName + " успешно получен.");
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return downloadFile;
    }

    //Это нада ПЕРЕНЕСТИ В СЕРВИС (ОТВЕЧАЕТ ЗА СКАЧИВАНИЕ ФАЙЛА С СЕРВЕРА ТЕЛЕГРАММ)
    private java.io.File downloadFile(String filePath, String fileName) {
        String fullPath = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;
        java.io.File downloadFile = new java.io.File(TELEGRAM_BOT_DOWNLOAD_PATH + fileName);
        try {
            if (Files.exists(downloadFile.toPath())) {
                Files.createDirectory(downloadFile.toPath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (InputStream is = new URL(fullPath).openStream();
             FileOutputStream fos = new FileOutputStream(downloadFile)) {

            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при скачивании файла: " + e.getMessage());
            e.printStackTrace();
        }
        return downloadFile;
    }


    private void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotToken() {
        return TELEGRAM_BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return TELEGRAM_BOT_USERNAME;
    }

}
