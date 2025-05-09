CREATE TABLE IF NOT EXISTS USER
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    NOT
    NULL,
    name
    VARCHAR
(
    100
) NOT NULL,
    registration TIMESTAMP NOT NULL,
    telegram_id BIGINT NOT NULL
    );

CREATE TABLE IF NOT EXISTS LOG
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    NOT
    NULL,
    file_name
    TEXT
    NOT
    NULL,
    file_size_in_megabyte
    BIGINT
    NOT
    NULL,
    owner_id
    BIGINT
);