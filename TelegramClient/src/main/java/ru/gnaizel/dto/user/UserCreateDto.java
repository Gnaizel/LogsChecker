package ru.gnaizel.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;


@Builder
@Data
public class UserCreateDto {
    @NotNull
    private String userName;
    @NotNull
    private long telegramId;
}
