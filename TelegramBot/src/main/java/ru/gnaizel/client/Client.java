package ru.gnaizel.client;

import ru.gnaizel.model.LogFileShortDto;

import java.util.List;

public interface Client {
    List<LogFileShortDto> sendCheckQuery(String url);
}
