package ru.gnaizel.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gnaizel.dto.user.UserCreateDto;
import ru.gnaizel.dto.user.UserShortDto;
import ru.gnaizel.exception.user.UserCreateError;
import ru.gnaizel.exception.user.UserValidationException;
import ru.gnaizel.mapper.user.UserMapper;
import ru.gnaizel.model.User;
import ru.gnaizel.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final UserMapper userMapper;

    @Override
    public UserShortDto createUser(UserCreateDto createDto) {
        if (repository.existsByTelegramId(createDto.getTelegramId())) {
            throw new  UserValidationException("User is exists");
        }

        User user = repository.save(User.builder()
                .userName(createDto.getUserName())
                .telegramId(createDto.getTelegramId())
                .build());
        return userMapper.toShort(user);
    }

    @Override
    public User getUserByTelegramId(long telegramId) {
        if (!repository.existsByTelegramId(telegramId)) {
            throw new  UserValidationException("User is not exists");
        }

        return repository.findByTelegramId(telegramId);
    }

    @Override
    public UserShortDto createUserByUpdateMassageTelegram(Update update) {
        org.telegram.telegrambots.meta.api.objects.User telegramUser = null;

        if (update.hasMessage()) {
            telegramUser = update.getMessage().getFrom();
        } else if (update.hasCallbackQuery()) {
            telegramUser = update.getCallbackQuery().getFrom();
        } else if (update.hasInlineQuery()) {
            telegramUser = update.getInlineQuery().getFrom();
        }

        if (telegramUser != null) {

            String telegramUserName = telegramUser.getUserName();

            return createUser(UserCreateDto.builder()
                    .userName(telegramUserName)
                    .telegramId(update.getMessage().getChatId())
                    .build());
        } else {
            throw new UserCreateError("TelegramUser is null !!! " +
                    "(createUserByUpdateMassageTelegram method problem)");
        }
    }

    @Override
    public boolean existCheckUser(long telegramId) {
        return repository.existsByTelegramId(telegramId);
    }
}
