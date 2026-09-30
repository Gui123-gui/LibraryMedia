CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE media (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    type            VARCHAR(10)   NOT NULL,
    provider        VARCHAR(30)   NOT NULL,
    external_id     VARCHAR(100)  NOT NULL,
    title           VARCHAR(500)  NOT NULL,
    release_year    SMALLINT      NULL,
    genre           VARCHAR(255)  NULL,
    synopsis        TEXT          NULL,
    cover_url       VARCHAR(1000) NULL,
    external_rating DECIMAL(3,1)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_media_provider_external UNIQUE (provider, external_id),
    CONSTRAINT ck_media_type CHECK (type IN ('BOOK', 'MOVIE', 'SERIES'))
);

CREATE TABLE lists (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    name               VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    is_favorites       BOOLEAN      NOT NULL DEFAULT FALSE,
    favorites_owner_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN is_favorites THEN user_id ELSE NULL END
    ) VIRTUAL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_lists_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_lists_one_favorites_per_user UNIQUE (favorites_owner_id),
    CONSTRAINT uq_lists_user_name UNIQUE (user_id, name)
);
CREATE INDEX idx_lists_user ON lists (user_id);

CREATE TABLE user_media (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    media_id   BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    rating     TINYINT     NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_media_user  FOREIGN KEY (user_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_media_media FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE RESTRICT,
    CONSTRAINT uq_user_media UNIQUE (user_id, media_id),
    CONSTRAINT ck_user_media_status CHECK (status IN ('WANT', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT ck_user_media_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 10)
);

CREATE TABLE list_items (
    list_id       BIGINT    NOT NULL,
    user_media_id BIGINT    NOT NULL,
    added_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (list_id, user_media_id),
    CONSTRAINT fk_list_items_list       FOREIGN KEY (list_id)       REFERENCES lists (id)      ON DELETE CASCADE,
    CONSTRAINT fk_list_items_user_media FOREIGN KEY (user_media_id) REFERENCES user_media (id) ON DELETE CASCADE
);
CREATE INDEX idx_list_items_user_media ON list_items (user_media_id);

CREATE TABLE shares (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    list_id    BIGINT      NOT NULL,
    token      VARCHAR(64) NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_shares_list FOREIGN KEY (list_id) REFERENCES lists (id) ON DELETE CASCADE,
    CONSTRAINT uq_shares_list  UNIQUE (list_id),
    CONSTRAINT uq_shares_token UNIQUE (token)
);
