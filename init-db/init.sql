CREATE TABLE IF NOT EXISTS app_user (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO app_user (name, email)
SELECT 'user-' || i, 'user' || i || '@example.com'
FROM generate_series(1, 500) AS i;
