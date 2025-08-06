package ru.gnaizel.service.user;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gnaizel.dto.user.UserCreateDto;
import ru.gnaizel.dto.user.UserShortDto;
import ru.gnaizel.model.User;

public interface UserService {
    UserShortDto createUser(UserCreateDto createDto);

    boolean existCheckUser(long telegramId);

    UserShortDto createUserByUpdateMassageTelegram(Update update);

    User getUserByTelegramId(long telegramId);
}
