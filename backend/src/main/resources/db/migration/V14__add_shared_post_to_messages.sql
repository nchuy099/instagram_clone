ALTER TABLE messages
    ADD COLUMN shared_post_id UUID;

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_shared_post
    FOREIGN KEY (shared_post_id) REFERENCES posts(id) ON DELETE SET NULL;

CREATE INDEX idx_messages_shared_post_id ON messages (shared_post_id);
