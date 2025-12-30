CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL
);

INSERT INTO products (name, description, price) VALUES
('Laptop', 'High performance laptop', 1200.00),
('Mouse', 'Wireless mouse', 25.50),
('Keyboard', 'Mechanical keyboard', 80.00),
('Monitor', '27 inch 4K monitor', 350.00),
('Headphones', 'Noise cancelling headphones', 150.00);
