package ru.gnaizel.mapper.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.gnaizel.dto.user.UserCreateDto;
import ru.gnaizel.dto.user.UserShortDto;
import ru.gnaizel.model.User;
import ru.gnaizel.repository.user.UserRepository;

@Component
@RequiredArgsConstructor
public class UserMapper {

    public UserShortDto toShort(User user) {
        return UserShortDto.builder()
                .telegramId(user.getTelegramId())
                .userName(user.getUserName())
                .build();
    }
}
