package ru.gnaizel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogFileShortDto;
import ru.gnaizel.dto.log.LogFileUploadInfoDto;
import ru.gnaizel.service.log.LogsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/logs")
public class LogsController {
    private final LogsService logsService;

    @PostMapping("/uploads")
    public LogFileUploadInfoDto uploadLog(@RequestParam("file") MultipartFile file,
                                          @RequestParam("telegram-id") long telegramId) {
        return logsService.uploadLog(file, telegramId);
    }

    @PatchMapping("/checks")
    public List<LogFileShortDto> check(@RequestParam("url") String url, @RequestParam("telegram-id") long telegramId) {
        return logsService.check(url, telegramId);
    }
}
