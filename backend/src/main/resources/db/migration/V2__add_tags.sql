CREATE TABLE page_tag (
    page_id UUID        NOT NULL REFERENCES page (id) ON DELETE CASCADE,
    tag     VARCHAR(64) NOT NULL,
    PRIMARY KEY (page_id, tag)
);

CREATE INDEX idx_page_tag_tag ON page_tag (tag);
