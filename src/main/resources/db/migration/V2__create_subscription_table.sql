CREATE TABLE subscription (
    id BIGSERIAL PRIMARY KEY,

    customer_name VARCHAR(255) NOT NULL,

    customer_email VARCHAR(255) NOT NULL,

    plan_id BIGINT NOT NULL,

    amount NUMERIC(10,2) NOT NULL,

    start_date DATE NOT NULL,

    end_date DATE,

    status VARCHAR(50) NOT NULL,

    created_at TIMESTAMP,

    created_by VARCHAR(255),

    last_modified_at TIMESTAMP,

    last_modified_by VARCHAR(255),

    CONSTRAINT fk_subscription_plan
        FOREIGN KEY (plan_id)
        REFERENCES plan(id)
);