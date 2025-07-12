package ru.gnaizel.handlers;

package com.telegramfiledownloader.handlers;

import ca.denisab85.tdlight.jni.TdApi;
import ca.denisab85.tdlight.Client;
import ca.denisab85.tdlight.Client.ResultHandler;
import java.nio.file.Path;

public class FileHandler implements ResultHandler {
    private final Path filePath;

    public FileHandler(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void onResult(TdApi.Object object) {
        if (object.getConstructor() == TdApi.File.CONSTRUCTOR) {
            TdApi.File file = (TdApi.File) object;
            if (file.local.isDownloadingCompleted) {
                System.out.println("📥 Downloaded: " + filePath);
            }
        } else if (object.getConstructor() == TdApi.Error.CONSTRUCTOR) {
            TdApi.Error error = (TdApi.Error) object;
            System.err.println("Download failed: " + error.code + " - " + error.message);
        }
    }
}
