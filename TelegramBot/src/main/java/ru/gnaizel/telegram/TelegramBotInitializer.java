package ru.gnaizel.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotInitializer {

    private final TelegramBot bot;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            log.info("Telegram bot successfully registered");
        } catch (TelegramApiException e) {
            log.error("Failed to register bot", e);
            throw new IllegalStateException("Bot registration failed", e);
        }
    }
}
