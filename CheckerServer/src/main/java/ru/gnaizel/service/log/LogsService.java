package ru.gnaizel.service.log;

import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogUploadDto;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface LogsService {

    LogUploadDto uploadLog(MultipartFile file, long telegramId);

    long check(String url, long telegramId) throws IOException, InterruptedException, ExecutionException;
}
