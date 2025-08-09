package ru.gnaizel.telegram;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.gnaizel.handler.CalbackHandler;
import ru.gnaizel.handler.CommandHandler;
import ru.gnaizel.handler.UpdateHandler;

@Service
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final UpdateHandler updateHandler;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.name}")
    private String botName;

    @Override
    public void onUpdateReceived(Update update) {
        updateHandler.handleUpdate(update, this);
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    public void sendMessage(String text, long chatId) {
        try {
            SendMessage send = new SendMessage();
            send.setText(text);
            send.setChatId(chatId);
            execute(send);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
