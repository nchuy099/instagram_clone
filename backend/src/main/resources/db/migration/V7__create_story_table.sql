CREATE TABLE stories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    media_url TEXT NOT NULL,
    media_type VARCHAR(20),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_stories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_stories_user_expires ON stories (user_id, expires_at);
CREATE INDEX idx_stories_expires ON stories (expires_at);
