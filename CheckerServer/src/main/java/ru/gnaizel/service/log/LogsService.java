package ru.gnaizel.service.log;

import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogDto;

public interface LogsService {

    LogDto uploadLog(MultipartFile file, long telegramId);
}
