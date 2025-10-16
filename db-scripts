create table customers (
    cust_id BIGINT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) ,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    nationality VARCHAR(50) NOT NULL,
    marital_status VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

create table contact (
    contact_id BIGINT PRIMARY KEY,
    phone VARCHAR(15) NOT NULL,
    secondary_phone VARCHAR(15),
    whatsapp_phone VARCHAR(15),
    email VARCHAR(255) NOT NULL,
    cust_id BIGINT NOT NULL,

    CONSTRAINT fk_contact
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
        ON DELETE CASCADE
);

create table address (
    address_id BIGINT PRIMARY KEY,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    cust_id BIGINT NOT NULL,

    CONSTRAINT fk_address
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
        ON DELETE CASCADE
);

create table id_proof (
    id_proof_id BIGINT PRIMARY KEY,
    cust_id BIGINT NOT NULL,
    id_type VARCHAR(50) NOT NULL,
    id_number VARCHAR(100) NOT NULL,
    image_url VARCHAR(255),

    CONSTRAINT fk_id_proof
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
        ON DELETE CASCADE
);

create table relatives (
    relative_id BIGINT PRIMARY KEY,
    cust_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    contact_number VARCHAR(15),

    CONSTRAINT fk_relatives
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_contact_cust_id ON contact(cust_id);
CREATE INDEX idx_address_cust_id ON address(cust_id);
CREATE INDEX idx_id_proof_cust_id ON id_proof(cust_id);
CREATE INDEX idx_relatives_cust_id ON relatives(cust_id);

