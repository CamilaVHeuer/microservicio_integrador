CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP NOT NULL
);

CREATE TABLE api_keys (
                          id BIGSERIAL PRIMARY KEY,
                          key_prefix VARCHAR(50) UNIQUE NOT NULL,
                          key_hash VARCHAR(255) NOT NULL,
                          expires_at TIMESTAMP NULL,
                          created_at TIMESTAMP NOT NULL ,
                          user_id BIGINT NOT NULL,

                          CONSTRAINT fk_api_keys_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users (id)
                                  ON DELETE CASCADE
);

CREATE INDEX idx_api_keys_prefix ON api_keys(key_prefix);