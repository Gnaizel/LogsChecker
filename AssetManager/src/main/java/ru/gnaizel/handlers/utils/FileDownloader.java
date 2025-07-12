package ru.gnaizel.handlers.utils;

package com.telegramfiledownloader.utils;

import ca.denisab85.tdlight.Client;
import ca.denisab85.tdlight.jni.TdApi;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.telegramfiledownloader.handlers.FileHandler;

public class FileDownloader {
    public static void downloadFile(Client client, TdApi.File file, String downloadDir) {
        Path dir = Paths.get(downloadDir);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (Exception e) {
                System.err.println("Failed to create directory: " + e.getMessage());
                return;
            }
        }

        Path filePath = dir.resolve(file.remote.uniqueId);
        client.send(new TdApi.DownloadFile(file.id, 1, 0, 0, true), new FileHandler(filePath));
    }
}
