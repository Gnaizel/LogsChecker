package ru.gnaizel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.http.HttpHeaders;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class ClientImpl implements Client {
    private final RestTemplate restTemplate;

    @Override
    public int uploadFile(String filename, File file) {
        return 0;
    }
}
