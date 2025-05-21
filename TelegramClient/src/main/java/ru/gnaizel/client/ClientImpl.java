package ru.gnaizel.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.gnaizel.exception.CheckedException;
import ru.gnaizel.exception.UploadException;
import ru.gnaizel.model.UploadLog;

@Service
@RequiredArgsConstructor
public class ClientImpl implements Client {
    @Value("${logs-service.url}")
    private static String URL;

    RestTemplate restTemplate = new RestTemplate();

    @Override
    public UploadLog uploadLog(Resource file, long telegramId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);
        body.add("telegram-id", telegramId);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<UploadLog> response = restTemplate.postForEntity(URL, request, UploadLog.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            throw new UploadException("Upload error, status: " + response.getStatusCode());
        }
        return response.getBody();
    }

    @Override
    public void checkRequest(String url, long telegramId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("url", url);
        body.add("telegram-id", telegramId);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Object> response = restTemplate.postForEntity(URL, request, Object.class);

        if (response.getStatusCode() == HttpStatus.ACCEPTED) {
            throw new CheckedException("Check request failed, status: " + response.getStatusCode());
        }
    }
}
