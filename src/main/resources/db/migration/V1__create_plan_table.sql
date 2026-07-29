CREATE TABLE plan (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) NOT NULL UNIQUE,

    price NUMERIC(10,2) NOT NULL,

    description VARCHAR(500),

    billing_cycle VARCHAR(50) NOT NULL,

    active BOOLEAN NOT NULL
);