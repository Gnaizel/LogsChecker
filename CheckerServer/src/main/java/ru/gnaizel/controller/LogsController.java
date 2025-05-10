package ru.gnaizel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogDto;
import ru.gnaizel.service.log.LogsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/logs")
public class LogsController {
    private final LogsService logsService;

    @PostMapping("/upload")
    public LogDto uploadLog(@RequestParam("file") MultipartFile file, @RequestParam("telegram-id") long telegramId) {
        return logsService.uploadLog(file, telegramId);
    }
}
