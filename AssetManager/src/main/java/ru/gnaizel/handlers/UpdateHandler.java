package ru.gnaizel.handlers;

package com.telegramfiledownloader.handlers;

import ca.denisab85.tdlight.Client;
import ca.denisab85.tdlight.jni.TdApi;
import ca.denisab85.tdlight.Client.ResultHandler;
import com.telegramfiledownloader.utils.FileDownloader;

public class UpdateHandler implements ResultHandler {
    private final Client client;
    private final String downloadDir;

    public UpdateHandler(Client client, String downloadDir) {
        this.client = client;
        this.downloadDir = downloadDir;
    }

    @Override
    public void onResult(TdApi.Object object) {
        if (object.getConstructor() != TdApi.UpdateNewMessage.CONSTRUCTOR) return;

        TdApi.Message message = ((TdApi.UpdateNewMessage) object).message;

        if (message.content.getConstructor() == TdApi.MessageDocument.CONSTRUCTOR) {
            TdApi.Document document = ((TdApi.MessageDocument) message.content).document;
            FileDownloader.downloadFile(client, document.document, downloadDir);
        }
        else if (message.content.getConstructor() == TdApi.MessagePhoto.CONSTRUCTOR) {
            TdApi.Photo photo = ((TdApi.MessagePhoto) message.content).photo;
            TdApi.PhotoSize largest = getLargestPhotoSize(photo.sizes);
            if (largest != null) {
                FileDownloader.downloadFile(client, largest.photo, downloadDir);
            }
        }
    }

    private TdApi.PhotoSize getLargestPhotoSize(TdApi.PhotoSize[] sizes) {
        TdApi.PhotoSize largest = null;
        for (TdApi.PhotoSize size : sizes) {
            if (largest == null || size.width > largest.width) {
                largest = size;
            }
        }
        return largest;
    }
}
