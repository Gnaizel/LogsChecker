package ru.gnaizel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.gnaizel.client.Client;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {
    private final Client client;

    @Scheduled(fixedDelay = 10800000)
    @Override
    public void uploadFiles() {

    }
}
