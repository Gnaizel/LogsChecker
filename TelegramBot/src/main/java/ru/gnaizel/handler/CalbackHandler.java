package ru.gnaizel.handler;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gnaizel.telegram.TelegramBot;

@Service
public class CalbackHandler {
    public void handleCalback(Update update, TelegramBot telegramBot) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();

        switch (data) {
            case "":
        }
    }
}
