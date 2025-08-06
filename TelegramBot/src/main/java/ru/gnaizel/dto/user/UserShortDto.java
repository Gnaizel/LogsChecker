package ru.gnaizel.dto.user;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserShortDto {
    private String userName;
    private long telegramId;
}
