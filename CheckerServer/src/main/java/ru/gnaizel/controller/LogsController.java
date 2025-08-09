package ru.gnaizel.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogFileShortDto;
import ru.gnaizel.dto.log.LogFileUploadInfoDto;
import ru.gnaizel.service.log.LogsService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/logs")
public class LogsController {
    private final LogsService logsService;

    @PostMapping("/upload")
    public LogFileUploadInfoDto uploadLog(@RequestParam("file") MultipartFile file) {
        return logsService.uploadLog(file);
    }

    @GetMapping("/check/{url}")
    public List<LogFileShortDto> check(@PathVariable("url") String url) {
        log.info(url);
        return logsService.check(url);
    }
}
