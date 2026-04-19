ALTER TABLE messages
    ADD COLUMN shared_story_id UUID;

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_shared_story
    FOREIGN KEY (shared_story_id) REFERENCES stories(id) ON DELETE SET NULL;

CREATE INDEX idx_messages_shared_story_id ON messages (shared_story_id);
