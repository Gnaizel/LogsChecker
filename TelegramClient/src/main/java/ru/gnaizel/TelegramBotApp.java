package ru.gnaizel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.gnaizel.client.Client;
import ru.gnaizel.exception.UploadException;
import ru.gnaizel.model.CheckCommandState;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class TelegramBotApp extends TelegramLongPollingBot {
    @Value("${telegram.bot.token}")
    private String TELEGRAM_BOT_TOKEN;
    @Value("${telegram.bot.name}")
    private String TELEGRAM_BOT_USERNAME;
    @Value("${telegram.bot.download-path}")
    private String TELEGRAM_BOT_DOWNLOAD_PATH;

    private final Map<Long, CheckCommandState> userCheckCommandState = new HashMap<>();


    private Client client;

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TelegramBotApp.class, args);

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(context.getBean(TelegramBotApp.class));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        long chatId = update.getMessage().getChatId();

        if (userCheckCommandState.containsKey(chatId)) {
            handleCheckCommandContinuation(update);
        } else {
            handleGeneralMessage(update);
        }
    }

    private void handleGeneralMessage(Update update) {
        String message = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        if (message.startsWith("/")) {
            handleCommand(update);
        } else {
            sendMessage(chatId, "Бот пока что не может обрабатывать что то кроме команд");
        }
    }


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

    private java.io.File handleDocument(Update update) {
        long chatId = update.getMessage().getChatId();
        Document document = update.getMessage().getDocument();
        String fileName = document.getFileName();
        String fileId = document.getFileId();
        java.io.File downloadFile;
        log.info("fileId: {}, fileName: {}/n Dockument: {}", fileId, fileName, document.toString());
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


    private void handleCommand(Update update) {
        String message = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
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

    private void startCheckCommand(Update update) {
        long chatId = update.getMessage().getChatId();
        CheckCommandState state = new CheckCommandState();
        userCheckCommandState.put(chatId, state);

        sendMessage(chatId, "Пришлите мне URL, который нужно отпарить:");
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
