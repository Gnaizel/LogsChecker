package ru.gnaizel.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gnaizel.client.Client;
import ru.gnaizel.model.LogFileShortDto;
import ru.gnaizel.telegram.TelegramBot;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandHandler {
    private final Client client;

    private HashMap<Long, String> processMap = new HashMap<>();

    public void handleCommand(Update update, TelegramBot telegramBot) {
        long chatId = update.getMessage().getChatId();
        String message = update.getMessage().getText();

        if (processMap.containsKey(chatId)) {
            switch (processMap.get(chatId)) {
                case "/start":
                    telegramBot.sendMessage("Привет %s \uD83E\uDDD1\u200D\uD83D\uDCBB \nПрогресс и тд сброшены"
                                    .formatted(update.getMessage().getFrom().getFirstName()),
                            chatId);
                    processMap.remove(chatId);
                    break;
                case "check":
                    if (message.equals("stop")) {
                        processMap.remove(chatId);
                        return;
                    }
                    List<LogFileShortDto> logsInfo = client.sendCheckQuery(message);
                    long clearLine = logsInfo.stream().mapToLong(LogFileShortDto::getCleanLineCount).sum();
                    long allLine = logsInfo.stream().mapToLong(LogFileShortDto::getAllLineCount).sum();
                    telegramBot.sendMessage("Всего строк: %s \nНайдено строк: %s"
                            .formatted(allLine, clearLine),
                            chatId);
                    processMap.remove(chatId);
                    break;
            }
            return;
        }

        switch (message) {
            case "/check":
                telegramBot.sendMessage("Напишите мне url \n по которому надо парсить: ", chatId);
                processMap.put(chatId, "check");
                break;
            case "/start":
                telegramBot.sendMessage("Привет %s \uD83E\uDDD1\u200D\uD83D\uDCBB \nПрогресс и тд сброшены"
                        .formatted(update.getMessage().getFrom().getFirstName()),
                        chatId);
                break;
            default:
                telegramBot.sendMessage("Неизвестная команда",chatId);
        }

    }
}
