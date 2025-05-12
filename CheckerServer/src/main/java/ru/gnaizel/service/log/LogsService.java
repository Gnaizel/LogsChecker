package ru.gnaizel.service.log;

import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogUploadDto;

public interface LogsService {

    LogUploadDto uploadLog(MultipartFile file, long telegramId);

    void checkFormat(String url);
}
