CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    deleted_at      TIMESTAMP,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    token           VARCHAR(512) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    ip_address      VARCHAR(45),
    device_info     VARCHAR(255),
    user_agent      VARCHAR(512),
    expires_at      TIMESTAMP NOT NULL,
    last_used_at    TIMESTAMP,
    revoked_at      TIMESTAMP,
    status          VARCHAR(20) NOT NULL,

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_refresh_tokens_user_id      ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_status       ON refresh_tokens(status);
CREATE INDEX idx_refresh_tokens_expires_at   ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_user_status  ON refresh_tokens(user_id, status);
