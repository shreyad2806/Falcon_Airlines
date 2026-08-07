CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    deleted_at      TIMESTAMP,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    token           VARCHAR(512) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    expires_at      TIMESTAMP NOT NULL,

    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_email_verification_tokens_user_id    ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_tokens_expires_at ON email_verification_tokens(expires_at);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
