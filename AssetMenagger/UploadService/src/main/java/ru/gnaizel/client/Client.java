package ru.gnaizel.client;

import org.springframework.http.HttpStatusCode;

import java.io.File;

public interface Client {
    HttpStatusCode uploadLog(File file);
}
