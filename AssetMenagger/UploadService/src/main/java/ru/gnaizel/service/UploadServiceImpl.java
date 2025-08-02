package ru.gnaizel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.gnaizel.client.Client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {
    private final Client client;

    @Value("${upload-value.file-path}")
    private String filePath;

    @Scheduled(fixedDelay = 60) //10800000
    @Override
    public void uploadFiles() {
        Path path = Paths.get(filePath);
        List<Path> files;
        try  {
            files = Files.list(path).toList();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        if (files.isEmpty()) {
            throw new RuntimeException(filePath + " is empty");
        }

        for (Path pathFile : files) {
            File file = pathFile.toFile();

            if (client.uploadLog(file).equals(HttpStatusCode.valueOf(201))) {
                file.delete();
            }
        }
    }
}
