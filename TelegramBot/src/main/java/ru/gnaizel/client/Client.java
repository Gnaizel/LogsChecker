package ru.gnaizel.client;

import org.springframework.core.io.Resource;
import ru.gnaizel.model.UploadLog;

public interface Client {
    UploadLog uploadLog(Resource file, long telegramId);

    void checkRequest(String url, long telegramId);
}