CREATE TABLE app_user (
    id           UUID         PRIMARY KEY,
    subject      VARCHAR(255) NOT NULL UNIQUE,
    email        VARCHAR(320) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    role         VARCHAR(16)  NOT NULL,
    status       VARCHAR(16)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE page (
    id         UUID         PRIMARY KEY,
    slug       VARCHAR(255) NOT NULL UNIQUE,
    title      VARCHAR(200) NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    updated_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_page_title ON page (title);
