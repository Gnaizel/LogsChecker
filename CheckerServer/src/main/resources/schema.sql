CREATE TABLE IF NOT EXISTS USERS
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    PRIMARY
    KEY,
    name
    VARCHAR
(
    100
) NOT NULL,
    registration TIMESTAMP NOT NULL,
    telegram_id BIGINT NOT NULL
    );

CREATE TABLE IF NOT EXISTS log
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    PRIMARY
    KEY,
    file_name
    TEXT
    NOT
    NULL,
    file_size_in_megabyte
    DOUBLE
    PRECISION
    NOT
    NULL, -- Use DOUBLE PRECISION for MB size
    owner_id
    BIGINT
    REFERENCES
    USERS
(
    id
) -- Add foreign key constraint
);