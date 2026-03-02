CREATE TABLE token (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token_number VARCHAR(10),
    status VARCHAR(20),
    created_time TIMESTAMP,
    served_time TIMESTAMP
);