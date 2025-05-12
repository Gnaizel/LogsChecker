package ru.gnaizel.service.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogUploadDto;
import ru.gnaizel.exceptions.FileUploadError;
import ru.gnaizel.exceptions.FileValidationException;
import ru.gnaizel.mapper.LogMapper;
import ru.gnaizel.model.FileInExpect;
import ru.gnaizel.model.FileUpload;
import ru.gnaizel.repository.LogsRepository;
import ru.gnaizel.repository.QueueRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogsServiceImpl implements LogsService {
    private final LogsRepository logsRepository;
    private final QueueRepository queueRepository;

    @Value("${upload_dir}")
    private String UPLOAD_DIR;


    @Override
    @Transactional
    public LogUploadDto uploadLog(MultipartFile file, long telegramId) {
        if (file.isEmpty()) throw new FileValidationException("File is empty");
        FileUpload fileLog;

        try {
            Path uploadDir = Paths.get(UPLOAD_DIR);

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String fileName = file.getOriginalFilename() + Timestamp.valueOf(LocalDateTime.now());
            if (fileName == null) throw new FileValidationException("File name can't be null");
            Path filePath = uploadDir.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File: {}, ({}) is upload in server", fileName, file.getSize() / 1048576);

            fileLog = FileUpload.builder()
                    .fileName(fileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileSizeInMegabyte(file.getSize() / 1048576)
                    .ownerId(telegramId)
                    .build();
        } catch (IOException e) {
            throw new FileUploadError(e.getMessage());
        }

        FileUpload fileUpload = logsRepository.save(fileLog);
        queueRepository.save(FileInExpect.builder()
                .ownerTelegramId(fileUpload.getOwnerId())
                .file(fileUpload)
                .build());

        return LogMapper.toDto(fileUpload);
    }

    @Override
    public void checkFormat(String url) {

    }
}
