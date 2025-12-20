-- ===================================================
-- UTE Phone Hub - Cloud SQL Database Initialization
-- Created from Entity classes - October 2025
-- ===================================================

-- Drop all tables if exists (CASCADE để xóa tất cả dependencies)
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
DROP TABLE IF EXISTS review_likes CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS cart_items CASCADE;
DROP TABLE IF EXISTS carts CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS product_images CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS vouchers CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS brands CASCADE;
DROP TABLE IF EXISTS categories CASCADE;

-- ===================================================
-- CREATE TABLES (theo đúng Entity classes)
-- ===================================================

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(50) NOT NULL DEFAULT 'customer',
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT users_role_check CHECK (role IN ('customer', 'admin')),
    CONSTRAINT users_status_check CHECK (status IN ('active', 'locked', 'pending'))
);

-- Addresses table
CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    street_address TEXT NOT NULL,
    ward VARCHAR(100) NOT NULL,
    ward_code VARCHAR(10),
    province VARCHAR(100) NOT NULL,
    province_code VARCHAR(10),
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Categories table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE RESTRICT
);

-- Brands table
CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Products table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price NUMERIC(12,2) NOT NULL CHECK (price > 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    thumbnail_url VARCHAR(500),
    specifications JSONB,
    status BOOLEAN NOT NULL DEFAULT true,
    category_id BIGINT NOT NULL,
    brand_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE RESTRICT
);

-- Product Images table
CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(200),
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Vouchers table
CREATE TABLE vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_type VARCHAR(50) NOT NULL,
    discount_value NUMERIC(12,2) NOT NULL CHECK (discount_value > 0),
    max_usage INTEGER,
    min_order_value NUMERIC(12,2),
    expiry_date TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT vouchers_discount_type_check CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT vouchers_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'))
);

-- Orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_code VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT,
    email VARCHAR(100) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    street_address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount > 0),
    voucher_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL,
    CONSTRAINT orders_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT orders_payment_method_check CHECK (payment_method IN ('COD', 'BANK_TRANSFER', 'STORE_PICKUP','VNPAY'))
);

-- Order Items table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price NUMERIC(12,2) NOT NULL CHECK (price > 0),
    created_at TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

-- Carts table
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Cart Items table
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE(cart_id, product_id)
);

-- Reviews table
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE(user_id, product_id)
);

-- Review Likes table
CREATE TABLE review_likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    review_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
    UNIQUE(user_id, review_id)
);

-- Password Reset Tokens table
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ===================================================
-- CREATE INDEXES for Performance
-- ===================================================

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);

CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_status ON products(status);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_email ON orders(email);
CREATE INDEX idx_orders_order_code ON orders(order_code);
CREATE INDEX idx_orders_status ON orders(status);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);

CREATE INDEX idx_vouchers_code ON vouchers(code);
CREATE INDEX idx_vouchers_status ON vouchers(status);

-- ===================================================
-- INSERT INITIAL DATA
-- ===================================================

-- Insert Admin User (password: Admin@123)
-- Hash generated with BCrypt: $2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/8Kz8KzK
INSERT INTO users (username, full_name, email, password_hash, phone_number, role, status, created_at, updated_at) 
VALUES ('admin', 'Administrator', 'admin@utephonehub.me', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/8Kz8KzK', '0901234567', 'admin', 'active', NOW(), NOW());

-- Insert Admin User (password: admin123)
-- Hash generated with BCrypt (jbcrypt) rounds 12: $2a$12$1BjrT76KBt/oeyVY2sGakO2wERCO.iYQwbNbhCol.k9Zn0sxfW6XG
INSERT INTO users (username, full_name, email, password_hash, phone_number, role, status, created_at, updated_at) 
VALUES ('admin123', 'Administrator', 'admin123@utephonehub.me', '$2a$12$1BjrT76KBt/oeyVY2sGakO2wERCO.iYQwbNbhCol.k9Zn0sxfW6XG', '0901234567', 'admin', 'active', NOW(), NOW());

-- Insert Sample Customers (password: User@123)
INSERT INTO users (username, full_name, email, password_hash, phone_number, role, status, created_at, updated_at) VALUES
('nguyenvana', 'Nguyễn Văn An', 'nguyenvana@gmail.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/8Kz8KzK', '0912345678', 'customer', 'active', NOW(), NOW()),
('tranthib', 'Trần Thị Bình', 'tranthib@gmail.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/8Kz8KzK', '0923456789', 'customer', 'active', NOW(), NOW()),
('levanc', 'Lê Văn Cường', 'levanc@gmail.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/8Kz8KzK', '0934567890', 'customer', 'active', NOW(), NOW()),
('phamthid', 'Phạm Thị Dung', 'phamthid@gmail.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/8Kz8KzK', '0945678901', 'customer', 'active', NOW(), NOW());

-- Insert Categories
INSERT INTO categories (name, description, parent_id, created_at, updated_at) VALUES
('Điện thoại', 'Các loại điện thoại thông minh', NULL, NOW(), NOW()),
('Tablet', 'Máy tính bảng', NULL, NOW(), NOW()),
('Laptop', 'Máy tính xách tay', NULL, NOW(), NOW()),
('Phụ kiện', 'Phụ kiện điện tử', NULL, NOW(), NOW()),
('Đồng hồ thông minh', 'Smartwatch và wearables', NULL, NOW(), NOW()),
('Tai nghe', 'Tai nghe và loa', NULL, NOW(), NOW()),
('Sạc dự phòng', 'Pin sạc dự phòng', 4, NOW(), NOW()),
('Ốp lưng', 'Ốp lưng điện thoại', 4, NOW(), NOW()),
('Cáp sạc', 'Cáp và đầu sạc', 4, NOW(), NOW()),
('Kính cường lực', 'Miếng dán bảo vệ màn hình', 4, NOW(), NOW());

-- Insert Brands
INSERT INTO brands (name, description, logo_url, created_at, updated_at) VALUES
('Apple', 'Thương hiệu công nghệ hàng đầu từ Mỹ', 'https://upload.wikimedia.org/wikipedia/commons/f/fa/Apple_logo_black.svg', NOW(), NOW()),
('Samsung', 'Tập đoàn điện tử hàng đầu Hàn Quốc', 'https://upload.wikimedia.org/wikipedia/commons/2/24/Samsung_Logo.svg', NOW(), NOW()),
('Xiaomi', 'Thương hiệu công nghệ từ Trung Quốc', 'https://upload.wikimedia.org/wikipedia/commons/2/29/Xiaomi_logo.svg', NOW(), NOW()),
('OPPO', 'Thương hiệu điện thoại thông minh', 'https://upload.wikimedia.org/wikipedia/commons/8/89/Oppo_Logo.svg', NOW(), NOW()),
('Vivo', 'Thương hiệu điện thoại phổ biến', 'https://upload.wikimedia.org/wikipedia/commons/3/33/Vivo_logo.svg', NOW(), NOW()),
('Realme', 'Thương hiệu smartphone giá rẻ', NULL, NOW(), NOW()),
('Huawei', 'Tập đoàn công nghệ Trung Quốc', NULL, NOW(), NOW()),
('Nokia', 'Thương hiệu điện thoại lâu đời', NULL, NOW(), NOW()),
('Sony', 'Tập đoàn công nghệ Nhật Bản', NULL, NOW(), NOW()),
('Asus', 'Thương hiệu máy tính và di động', NULL, NOW(), NOW());

-- Insert Products (iPhone)
-- Insert Products (iPhone)
INSERT INTO products (name, description, price, stock_quantity, thumbnail_url, specifications, status, category_id, brand_id, created_at, updated_at) VALUES
('iPhone 15 Pro Max 256GB', 
'iPhone 15 Pro Max với chip A17 Pro mạnh mẽ nhất, camera 48MP chuyên nghiệp, khung Titan cao cấp, màn hình Super Retina XDR 6.7 inch với ProMotion 120Hz. Pin trâu, sạc nhanh, hỗ trợ 5G.',
32990000, 50, 
'https://s13emagst.akamaized.net/products/60458/60457155/images/res_bc44fa0bbdf0de65a3c655809352aba9.jpg',
'{"ram": "8GB", "chip": "A17 Pro", "camera": "48MP", "screen": "6.7 inch", "battery": "4422mAh", "storage": "256GB", "os": "iOS 17"}',
true, 1, 1, NOW(), NOW()),

('iPhone 15 Pro 128GB',
'iPhone 15 Pro với chip A17 Pro, camera 48MP, Dynamic Island, khung Titan, thiết kế cao cấp. Hiệu năng đỉnh cao cho mọi tác vụ.',
27990000, 60,
'https://i5.walmartimages.com/seo/Verizon-iPhone-15-Pro-Max-256GB-Natural-Titanium_43b58742-b6ad-491a-a795-e6c2eb6bcc6f.d6fa026332e1941b129f607c592844ab.jpeg',
'{"ram": "8GB", "chip": "A17 Pro", "camera": "48MP", "screen": "6.1 inch", "battery": "3274mAh", "storage": "128GB", "os": "iOS 17"}',
true, 1, 1, NOW(), NOW()),

('iPhone 15 Plus 128GB',
'iPhone 15 Plus màn hình lớn 6.7 inch, camera 48MP, chip A16 Bionic mạnh mẽ, pin trâu, thiết kế sang trọng.',
24990000, 70,
'https://tse1.mm.bing.net/th/id/OIP.BEQP1y8KVzPy8JHSMb99aAAAAA?cb=ucfimg2&ucfimg=1&w=380&h=380&rs=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "6GB", "chip": "A16 Bionic", "camera": "48MP", "screen": "6.7 inch", "battery": "4383mAh", "storage": "128GB", "os": "iOS 17"}',
true, 1, 1, NOW(), NOW()),

('iPhone 14 128GB',
'iPhone 14 với chip A15 Bionic, camera kép 12MP, màn hình 6.1 inch sắc nét, thiết kế bền bỉ, hỗ trợ 5G.',
18990000, 80,
'https://tse2.mm.bing.net/th/id/OIP.KByJEBmV7q2XoDzxNeiHVgHaHZ?cb=ucfimg2&ucfimg=1&w=882&h=881&rs=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "6GB", "chip": "A15 Bionic", "camera": "12MP", "screen": "6.1 inch", "battery": "3279mAh", "storage": "128GB", "os": "iOS 16"}',
true, 1, 1, NOW(), NOW()),

('iPhone 13 128GB',
'iPhone 13 giá tốt nhất, hiệu năng mạnh mẽ với chip A15 Bionic, camera kép 12MP, pin trâu, màn hình OLED 6.1 inch.',
15990000, 100,
'https://tse3.mm.bing.net/th/id/OIP.7VccDGB_hwsgH4boLGOFSAHaJ6?cb=ucfimg2&ucfimg=1&rs=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "4GB", "chip": "A15 Bionic", "camera": "12MP", "screen": "6.1 inch", "battery": "3240mAh", "storage": "128GB", "os": "iOS 15"}',
true, 1, 1, NOW(), NOW());

-- Insert Products (Samsung)
INSERT INTO products (name, description, price, stock_quantity, thumbnail_url, specifications, status, category_id, brand_id, created_at, updated_at) VALUES
('Samsung Galaxy S24 Ultra 12GB/256GB',
'Galaxy S24 Ultra với bút S Pen, camera 200MP siêu nét, chip Snapdragon 8 Gen 3 mạnh nhất, màn hình Dynamic AMOLED 2X 6.8 inch, pin 5000mAh.',
29990000, 40,
'https://tse4.mm.bing.net/th/id/OIP.q2UwyZGSwq-0ZdORQpa51AHaHc?cb=ucfimg2&ucfimg=1&rs=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "12GB", "chip": "Snapdragon 8 Gen 3", "camera": "200MP", "screen": "6.8 inch", "battery": "5000mAh", "storage": "256GB", "os": "Android 14"}',
true, 1, 2, NOW(), NOW()),

('Samsung Galaxy S23 Ultra 8GB/256GB',
'Galaxy S23 Ultra camera 200MP, hiệu năng đỉnh cao với Snapdragon 8 Gen 2, màn hình 6.8 inch QHD+, S Pen tích hợp.',
25990000, 35,
'https://tse4.mm.bing.net/th/id/OIP.q2UwyZGSwq-0ZdORQpa51AHaHc?cb=ucfimg2&ucfimg=1&rs=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "8GB", "chip": "Snapdragon 8 Gen 2", "camera": "200MP", "screen": "6.8 inch", "battery": "5000mAh", "storage": "256GB", "os": "Android 13"}',
true, 1, 2, NOW(), NOW()),

('Samsung Galaxy Z Fold5 12GB/256GB',
'Điện thoại gập cao cấp nhất với màn hình Dynamic AMOLED 2X 7.6 inch, chip Snapdragon 8 Gen 2, camera 50MP.',
40990000, 20,
'https://media.cnn.com/api/v1/images/stellar/prod/galaxy-s24ultra-titaniumblack-device-spen-front-hi.jpg?c=16x9&q=h_720,w_1280,c_fill',
'{"ram": "12GB", "chip": "Snapdragon 8 Gen 2", "camera": "50MP", "screen": "7.6 inch", "battery": "4400mAh", "storage": "256GB", "os": "Android 13"}',
true, 1, 2, NOW(), NOW()),

('Samsung Galaxy Z Flip5 8GB/256GB',
'Điện thoại gập nhỏ gọn, thời trang với màn hình phụ lớn 3.4 inch, chip Snapdragon 8 Gen 2, camera kép 12MP.',
23990000, 25,
'https://i.ebayimg.com/images/g/FOkAAOSwmSRnAdyv/s-l400.jpg',
'{"ram": "8GB", "chip": "Snapdragon 8 Gen 2", "camera": "12MP", "screen": "6.7 inch", "battery": "3700mAh", "storage": "256GB", "os": "Android 13"}',
true, 1, 2, NOW(), NOW()),

('Samsung Galaxy A54 5G 8GB/128GB',
'Galaxy A54 camera 50MP OIS, pin 5000mAh, màn hình Super AMOLED 6.4 inch 120Hz, chip Exynos 1380.',
9990000, 120,
'https://tse3.mm.bing.net/th/id/OIP.IBM4o7WsGlGWP18kDmb-GAHaJP?cb=ucfimg2&ucfimg=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "8GB", "chip": "Exynos 1380", "camera": "50MP", "screen": "6.4 inch", "battery": "5000mAh", "storage": "128GB", "os": "Android 13"}',
true, 1, 2, NOW(), NOW());

-- Insert Products (Xiaomi)
INSERT INTO products (name, description, price, stock_quantity, thumbnail_url, specifications, status, category_id, brand_id, created_at, updated_at) VALUES
('Xiaomi 14 Pro 12GB/512GB',
'Xiaomi 14 Pro với camera Leica 50MP, chip Snapdragon 8 Gen 3, màn hình AMOLED 6.73 inch 120Hz, sạc nhanh 120W.',
21990000, 50,
'https://tse1.mm.bing.net/th/id/OIP.2LUXP7OLqohrkYnryGz5SwHaHa?cb=ucfimg2&ucfimg=1&rs=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "12GB", "chip": "Snapdragon 8 Gen 3", "camera": "50MP Leica", "screen": "6.73 inch", "battery": "4880mAh", "storage": "512GB", "os": "Android 14"}',
true, 1, 3, NOW(), NOW()),

('Xiaomi 13T Pro 12GB/256GB',
'Xiaomi 13T Pro camera 50MP, chip Dimensity 9200+, màn hình AMOLED 144Hz, sạc nhanh 120W, thiết kế cao cấp.',
13990000, 80,
'https://tse3.mm.bing.net/th/id/OIP.rKgXu0G-ATdDAm2q7WUQhgAAAA?cb=ucfimg2&ucfimg=1&rs=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "12GB", "chip": "Dimensity 9200+", "camera": "50MP", "screen": "6.67 inch", "battery": "5000mAh", "storage": "256GB", "os": "Android 13"}',
true, 1, 3, NOW(), NOW()),

('Xiaomi Redmi Note 13 Pro 8GB/128GB',
'Redmi Note 13 Pro camera 200MP siêu nét, chip Snapdragon 7s Gen 2, màn hình AMOLED 120Hz, pin 5100mAh.',
7490000, 150,
'https://tse3.mm.bing.net/th/id/OIP.8FrtDjtcFk796bWKQByXKwAAAA?cb=ucfimg2&ucfimg=1&w=400&h=400&rs=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "8GB", "chip": "Snapdragon 7s Gen 2", "camera": "200MP", "screen": "6.67 inch", "battery": "5100mAh", "storage": "128GB", "os": "Android 13"}',
true, 1, 3, NOW(), NOW()),

('Xiaomi Redmi 12 8GB/256GB',
'Redmi 12 pin khủng 5000mAh, camera 50MP, chip Helio G88, màn hình lớn 6.79 inch, giá rẻ nhất.',
4490000, 200,
'https://tse2.mm.bing.net/th/id/OIP.lAUVXu-IcK_B1amtAU3PfwHaHa?cb=ucfimg2&ucfimg=1&w=500&h=500&rs=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "8GB", "chip": "Helio G88", "camera": "50MP", "screen": "6.79 inch", "battery": "5000mAh", "storage": "256GB", "os": "Android 13"}',
true, 1, 3, NOW(), NOW());

-- Insert Products (OPPO)
INSERT INTO products (name, description, price, stock_quantity, thumbnail_url, specifications, status, category_id, brand_id, created_at, updated_at) VALUES
('OPPO Find N3 Flip 12GB/256GB',
'OPPO Find N3 Flip điện thoại gập dọc, camera 50MP, chip Dimensity 9200, màn hình AMOLED 6.8 inch.',
22990000, 30,
'https://tse4.mm.bing.net/th/id/OIP.4W2tp7jusT3sIDHsV0SS0QHaHa?cb=ucfimg2&ucfimg=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "12GB", "chip": "Dimensity 9200", "camera": "50MP", "screen": "6.8 inch", "battery": "4300mAh", "storage": "256GB", "os": "Android 13"}',
true, 1, 4, NOW(), NOW()),

('OPPO Reno11 F 5G 8GB/256GB',
'OPPO Reno11 F camera 64MP chân dung, chip Dimensity 7050, màn hình AMOLED 6.7 inch, thiết kế đẹp mắt.',
8990000, 99,
'https://tse3.mm.bing.net/th/id/OIP.PaUs8NbNrashRYo3chrO7wHaHa?cb=ucfimg2&ucfimg=1&w=500&h=500&rs=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "8GB", "chip": "Dimensity 7050", "camera": "64MP", "screen": "6.7 inch", "battery": "5000mAh", "storage": "256GB", "os": "Android 14"}',
true, 1, 4, NOW(), NOW()),

('OPPO A78 8GB/256GB',
'OPPO A78 pin 5000mAh, sạc nhanh SUPERVOOC 67W, camera 50MP, chip Snapdragon 680, giá tốt.',
6490000, 149,
'https://tse4.mm.bing.net/th/id/OIP.lUzAgUVJ-SmA37cftvrIHQHaHa?cb=ucfimg2&ucfimg=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "8GB", "chip": "Snapdragon 680", "camera": "50MP", "screen": "6.43 inch", "battery": "5000mAh", "storage": "256GB", "os": "Android 13"}',
true, 1, 4, NOW(), NOW());

-- Insert Products (Vivo)
INSERT INTO products (name, description, price, stock_quantity, thumbnail_url, specifications, status, category_id, brand_id, created_at, updated_at) VALUES
('Vivo V29e 5G 12GB/256GB',
'Vivo V29e camera 64MP OIS chống rung, chip Snapdragon 695, màn hình AMOLED 6.67 inch, thiết kế mỏng nhẹ 7.57mm.',
8490000, 87,
'https://smartphoneshop.co.za/cdn/shop/files/VivoY36DualSim128GB-Blue_1024x1024.png?v=1696499513',
'{"ram": "12GB", "chip": "Snapdragon 695", "camera": "64MP OIS", "screen": "6.67 inch", "battery": "4800mAh", "storage": "256GB", "os": "Android 13"}',
true, 1, 5, NOW(), NOW()),

('Vivo Y36 8GB/128GB',
'Vivo Y36 pin 5000mAh, sạc nhanh 44W, camera 50MP, chip Snapdragon 680, hiệu năng tốt trong tầm giá.',
5990000, 112,
'https://tse4.mm.bing.net/th/id/OIP.RJHPp3YRNmIsuSNTYLP1OwHaHa?cb=ucfimg2&ucfimg=1&pid=ImgDetMain&o=7&rm=3',
'{"ram": "8GB", "chip": "Snapdragon 680", "camera": "50MP", "screen": "6.64 inch", "battery": "5000mAh", "storage": "128GB", "os": "Android 13"}',
true, 1, 5, NOW(), NOW());


-- Insert Vouchers
INSERT INTO vouchers (code, discount_type, discount_value, max_usage, min_order_value, expiry_date, status, created_at, updated_at) VALUES
('WELCOME10', 'PERCENTAGE', 10, 1000, 1000000, '2025-12-31 23:59:59', 'ACTIVE', NOW(), NOW()),
('SALE50K', 'FIXED_AMOUNT', 50000, 500, 2000000, '2025-12-31 23:59:59', 'ACTIVE', NOW(), NOW()),
('NEWUSER', 'PERCENTAGE', 15, 100, 500000, '2025-12-31 23:59:59', 'ACTIVE', NOW(), NOW()),
('FLASH100K', 'FIXED_AMOUNT', 100000, 200, 5000000, '2025-11-30 23:59:59', 'ACTIVE', NOW(), NOW()),
('VIP20', 'PERCENTAGE', 20, 50, 10000000, '2025-12-31 23:59:59', 'ACTIVE', NOW(), NOW());

-- Insert Sample Addresses
INSERT INTO addresses (user_id, recipient_name, phone_number, street_address, ward, ward_code, province, province_code, is_default, created_at, updated_at) VALUES
(2, 'Nguyễn Văn An', '0912345678', '123 Nguyễn Huệ', 'Phường Bến Nghé', '27259', 'TP. Hồ Chí Minh', '79', true, NOW(), NOW()),
(3, 'Trần Thị Bình', '0923456789', '456 Lê Lợi', 'Phường Bến Thành', '27262', 'TP. Hồ Chí Minh', '79', true, NOW(), NOW()),
(4, 'Lê Văn Cường', '0934567890', '789 Trần Hưng Đạo', 'Phường 1', '26794', 'TP. Hồ Chí Minh', '79', true, NOW(), NOW()),
(5, 'Phạm Thị Dung', '0945678901', '321 Hai Bà Trưng', 'Phường Đa Kao', '27274', 'TP. Hồ Chí Minh', '79', true, NOW(), NOW());

-- Insert Carts for users
INSERT INTO carts (user_id, created_at, updated_at) VALUES
(2, NOW(), NOW()),
(3, NOW(), NOW()),
(4, NOW(), NOW()),
(5, NOW(), NOW());

-- ===================================================
-- GRANT PERMISSIONS
-- ===================================================

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO utephonehub_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO utephonehub_user;

-- ===================================================
-- SUMMARY
-- ===================================================
-- Tables created: 13
-- - users, addresses, categories, brands, products, product_images
-- - vouchers, orders, order_items, carts, cart_items
-- - reviews, review_likes, password_reset_tokens
-- 
-- Initial data:
-- - 1 admin user
-- - 4 customer users
-- - 10 categories
-- - 10 brands
-- - 20+ products
-- - 5 vouchers
-- - 4 addresses
-- - 4 carts
-- ===================================================
