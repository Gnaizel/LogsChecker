package ru.gnaizel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gnaizel.model.Log;

@Repository
public interface LogsRepository extends JpaRepository<Log, Long> {
}
