package ru.gnaizel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogUploadDto;
import ru.gnaizel.service.log.LogsService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/logs")
public class LogsController {
    private final LogsService logsService;

    @PostMapping("/uploads")
    public LogUploadDto uploadLog(@RequestParam("file") MultipartFile file,
                                  @RequestParam("telegram-id") long telegramId) {
        return logsService.uploadLog(file, telegramId);
    }

    @PatchMapping("/checks")
    public long check(@RequestParam("url") String url, @RequestParam("telegram-id") long telegramId) throws IOException, ExecutionException, InterruptedException {
        return logsService.check(url, telegramId);
    }
}
