package ru.gnaizel.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gnaizel.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByTelegramId(long telegramId);

    boolean existsByTelegramId(long telegramId);
}
