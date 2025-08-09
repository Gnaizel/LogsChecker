package ru.gnaizel.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gnaizel.telegram.TelegramBot;

import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class UpdateHandler {
    private final CommandHandler commandHandler;
    private final CalbackHandler calbackHandler;

    public void handleUpdate(Update update, TelegramBot telegramBot) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            commandHandler.handleCommand(update, telegramBot);
        } else if (update.hasCallbackQuery()) {
            calbackHandler.handleCalback(update, telegramBot);
        }
    }
}
