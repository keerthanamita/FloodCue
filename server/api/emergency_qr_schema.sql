CREATE TABLE emergency_qr_tokens (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    token_hash CHAR(64) NOT NULL,
    owner_firebase_uid VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_emergency_qr_token_hash (token_hash),
    KEY idx_emergency_qr_owner (owner_firebase_uid),
    CONSTRAINT fk_emergency_qr_owner FOREIGN KEY (owner_firebase_uid)
        REFERENCES users(firebase_uid) ON DELETE CASCADE
);

CREATE TABLE emergency_qr_authorized_users (
    firebase_uid VARCHAR(128) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (firebase_uid)
);
