package ru.gnaizel.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.gnaizel.model.LogFileShortDto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Slf4j
@Service
public class ClientImpl implements Client {
    private final RestTemplate template = new RestTemplate();

    @Value("${checker.url}")
    private String checkerUrl;

    @Override
    public List<LogFileShortDto> sendCheckQuery(String url) {
        try {
            LogFileShortDto[] resultArray =
                    template.getForObject(checkerUrl + "/logs/check/" + url, LogFileShortDto[].class);
            return resultArray != null ? Arrays.asList(resultArray) : Collections.emptyList();
        } catch (RestClientException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
