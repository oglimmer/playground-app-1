CREATE TABLE page_attachment (
    id           UUID         PRIMARY KEY,
    page_id      UUID         NOT NULL REFERENCES page (id) ON DELETE CASCADE,
    filename     VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size         BIGINT       NOT NULL,
    data         BYTEA        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_page_attachment_page_id ON page_attachment (page_id);
