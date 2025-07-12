package ru.gnaizel.service;

import java.io.File;

public interface Client {
    int uploadFile(String filename, File file); // Возвращает код ответа ота основного сервиса
}
