# UTE Phone Hub - Tài liệu Tổng quan Dự án

## 1. Giới thiệu Dự án

**UTE Phone Hub** là một hệ thống thương mại điện tử (E-commerce) chuyên về bán điện thoại di động và phụ kiện. Dự án được xây dựng bằng Java với kiến trúc web application sử dụng Jakarta Servlet API và JSP cho giao diện người dùng.

### Mục đích
- Cung cấp nền tảng bán hàng trực tuyến cho các sản phẩm điện thoại
- Quản lý kho hàng, đơn hàng, và khách hàng
- Hỗ trợ đầy đủ các chức năng của một cửa hàng điện tử hiện đại

## 2. Công nghệ và Công cụ Sử dụng

### Backend
- **Java 17**: Ngôn ngữ lập trình chính
- **Jakarta Servlet API 6.1.0**: Xử lý HTTP requests và responses
- **Jakarta JSP 3.1.1**: Template engine cho giao diện
- **Jakarta JSTL 2.0.0**: Thư viện tag cho JSP
- **Hibernate 6.4.1.Final**: ORM (Object-Relational Mapping) framework
- **Jakarta Persistence API 3.1.0**: API chuẩn cho JPA
- **PostgreSQL 15**: Hệ quản trị cơ sở dữ liệu
- **Redis 7**: In-memory data store cho caching và session management
- **Maven**: Công cụ quản lý dependencies và build

### Frontend
- **HTML5/CSS3**: Cấu trúc và styling
- **JavaScript (Vanilla)**: Xử lý logic phía client
- **JSP (JavaServer Pages)**: Server-side rendering

### Authentication & Security
- **JWT (JSON Web Token) 0.11.5**: Xác thực và phân quyền
- **BCrypt**: Mã hóa mật khẩu
- **Google OAuth2**: Đăng nhập bằng tài khoản Google

### Utilities & Libraries
- **Gson 2.10.1**: Xử lý JSON
- **Jakarta Mail 2.0.1**: Gửi email
- **Commons FileUpload 1.5**: Upload files
- **Jakarta Validation 3.0.2**: Validation dữ liệu
- **Log4j2 2.20.0**: Logging
- **Jedis 5.1.0**: Redis client

### Infrastructure
- **Tomcat 10.1.18**: Application server
- **Docker & Docker Compose**: Containerization
- **HikariCP 5.1.0**: Connection pooling
- **C3P0**: Alternative connection pooling

## 3. Kiến trúc Hệ thống

### Mô hình Kiến trúc
Dự án sử dụng kiến trúc **MVC (Model-View-Controller)** với các lớp sau:

#### Controller Layer
- Xử lý HTTP requests
- Validate input
- Gọi Service layer
- Trả về responses (JSON hoặc JSP views)

#### Service Layer
- Chứa business logic
- Xử lý nghiệp vụ
- Tương tác với Repository layer
- Xử lý exceptions

#### Repository Layer
- Truy cập cơ sở dữ liệu
- Sử dụng JPA/Hibernate
- Thực hiện các query

#### Entity Layer
- Định nghĩa các entity classes
- Mapping với database tables
- Quan hệ giữa các entities

#### DTO Layer
- Data Transfer Objects
- Request DTOs: Nhận dữ liệu từ client
- Response DTOs: Trả dữ liệu về client

### Authentication & Authorization
- **JWT Authentication Filter**: Lọc và xác thực requests
- **Role-based Access Control**: Phân quyền theo vai trò (customer/admin)
- **Token Blacklisting**: Quản lý token đã logout (sử dụng Redis)
- **Public/Protected Endpoints**: Phân loại endpoints công khai và yêu cầu xác thực

### Database Connection Management
- **Thread-local EntityManager**: Mỗi thread có EntityManager riêng
- **Connection Pooling**: Sử dụng HikariCP và C3P0
- **Transaction Management**: Quản lý transactions thủ công

### Caching Strategy
- **Redis Cache**: Cache sản phẩm, tokens, OTP
- **Token Storage**: Lưu refresh tokens trong Redis
- **Session Management**: Quản lý session bằng Redis

## 4. Cơ sở Dữ liệu

### Database Schema

#### Bảng Users (Người dùng)
- Thông tin người dùng: username, email, password, full_name
- Vai trò: customer, admin
- Trạng thái: active, locked, pending
- Quan hệ: addresses, orders, reviews

#### Bảng Products (Sản phẩm)
- Thông tin sản phẩm: name, description, price, stock_quantity
- Specifications: Lưu dưới dạng JSONB
- Quan hệ: category, brand, images, reviews
- Status: active/inactive (soft delete)

#### Bảng Orders (Đơn hàng)
- Thông tin đơn hàng: order_code, total_amount, status
- Thông tin giao hàng: recipient_name, phone_number, street_address, city
- Payment method: COD, STORE_PICKUP, BANK_TRANSFER
- Order status: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
- Quan hệ: user, voucher, order_items

#### Bảng Categories (Danh mục)
- Phân loại sản phẩm
- Hỗ trợ danh mục con (parent_id)

#### Bảng Brands (Thương hiệu)
- Thông tin thương hiệu: name, description, logo_url

#### Bảng Carts & CartItems (Giỏ hàng)
- Mỗi user có một giỏ hàng
- Cart items: product_id, quantity

#### Bảng Reviews (Đánh giá)
- Rating: 1-5 sao
- Comment: Bình luận chi tiết
- Quan hệ: user, product
- Review likes: Người dùng có thể like đánh giá

#### Bảng Vouchers (Mã giảm giá)
- Code: Mã voucher
- Discount type: PERCENTAGE, FIXED_AMOUNT
- Discount value: Giá trị giảm giá
- Max usage: Số lần sử dụng tối đa
- Min order value: Giá trị đơn hàng tối thiểu
- Expiry date: Ngày hết hạn
- Status: ACTIVE, INACTIVE, EXPIRED

#### Bảng Addresses (Địa chỉ)
- Lưu địa chỉ giao hàng của người dùng
- Hỗ trợ địa chỉ mặc định

#### Bảng Password Reset Tokens
- Lưu token reset mật khẩu
- Có thời gian hết hạn

### Database Indexes
- Indexes trên các cột thường xuyên query: email, order_code, product_id, category_id, brand_id
- Indexes trên các cột status để filter nhanh

## 5. Tính năng Hệ thống

### 5.1. Tính năng Dành cho Khách hàng

#### Quản lý Tài khoản
- Đăng ký tài khoản mới
- Đăng nhập/Đăng xuất
- Đăng nhập bằng Google OAuth2
- Quên mật khẩu (OTP qua email)
- Xác thực email
- Cập nhật thông tin cá nhân
- Đổi mật khẩu
- Quản lý địa chỉ giao hàng

#### Duyệt Sản phẩm
- Xem danh sách sản phẩm
- Tìm kiếm sản phẩm
- Lọc theo danh mục, thương hiệu
- Sắp xếp theo giá, ngày tạo
- Xem chi tiết sản phẩm
- Xem hình ảnh sản phẩm
- Xem thông số kỹ thuật
- Xem đánh giá và bình luận

#### Giỏ hàng & Đặt hàng
- Thêm sản phẩm vào giỏ hàng
- Cập nhật số lượng sản phẩm
- Xóa sản phẩm khỏi giỏ hàng
- Hiển thị số lượng giỏ hàng real-time
- Guest checkout (đặt hàng không cần đăng nhập)
- Áp dụng mã giảm giá (voucher)
- Chọn phương thức thanh toán (COD, tại cửa hàng, chuyển khoản)
- Tạo đơn hàng
- Kiểm tra tồn kho tự động

#### Quản lý Đơn hàng
- Xem lịch sử đơn hàng (người dùng đã đăng nhập)
- Xem chi tiết đơn hàng
- Tra cứu đơn hàng công khai (mã đơn + email)
- Theo dõi trạng thái đơn hàng

#### Đánh giá Sản phẩm
- Đánh giá sản phẩm (1-5 sao)
- Viết bình luận chi tiết
- Chỉ người đã mua mới được đánh giá
- Like/Unlike đánh giá của người khác
- Xem tổng số lượt thích

### 5.2. Tính năng Dành cho Quản trị viên

#### Dashboard & Thống kê
- Tổng quan doanh thu
- Số lượng đơn hàng
- Số lượng sản phẩm
- Số lượng người dùng
- Thống kê theo thời gian (ngày, tháng, năm)
- Đơn hàng gần đây
- Sản phẩm sắp hết hàng
- Biểu đồ và báo cáo

#### Quản lý Sản phẩm
- Thêm/Sửa/Xóa sản phẩm (CRUD)
- Upload ảnh sản phẩm
- Quản lý nhiều ảnh cho một sản phẩm
- Cập nhật tồn kho
- Cập nhật giá
- Cập nhật thông số kỹ thuật (JSON)
- Soft delete (ẩn sản phẩm thay vì xóa)
- Quản lý trạng thái sản phẩm (active/inactive)

#### Quản lý Danh mục & Thương hiệu
- Thêm/Sửa/Xóa danh mục
- Thêm/Sửa/Xóa thương hiệu
- Kiểm tra ràng buộc (ngăn xóa nếu còn sản phẩm)
- Quản lý danh mục con

#### Quản lý Đơn hàng
- Xem danh sách tất cả đơn hàng
- Lọc theo trạng thái
- Xem chi tiết đơn hàng
- Cập nhật trạng thái đơn hàng
- Xử lý đơn hàng (xác nhận, vận chuyển, hoàn thành)
- Hủy đơn hàng

#### Quản lý Người dùng
- Xem danh sách người dùng
- Xem thông tin chi tiết người dùng
- Khóa/Mở khóa tài khoản
- Phân quyền (customer/admin)
- Quản lý trạng thái người dùng

#### Quản lý Voucher
- Tạo mã giảm giá
- Cập nhật voucher
- Xóa voucher
- Quản lý trạng thái voucher
- Thiết lập điều kiện sử dụng
- Theo dõi số lần sử dụng

## 6. API Endpoints

### 6.1. Authentication APIs
- POST `/api/v1/auth/register` - Đăng ký tài khoản
- POST `/api/v1/auth/login` - Đăng nhập
- POST `/api/v1/auth/refresh` - Làm mới token
- POST `/api/v1/auth/logout` - Đăng xuất
- POST `/api/v1/auth/forgot-password/request` - Yêu cầu reset mật khẩu
- POST `/api/v1/auth/forgot-password/verify` - Xác thực OTP
- POST `/api/v1/auth/forgot-password/reset` - Reset mật khẩu
- POST `/api/v1/auth/verify-email` - Xác thực email

### 6.2. User APIs
- GET `/api/v1/user/me` - Lấy thông tin người dùng hiện tại
- POST `/api/v1/user/profile` - Cập nhật thông tin cá nhân
- POST `/api/v1/user/password` - Đổi mật khẩu
- GET `/api/v1/user/addresses` - Lấy danh sách địa chỉ
- POST `/api/v1/user/addresses` - Thêm địa chỉ
- PUT `/api/v1/user/addresses/{id}` - Cập nhật địa chỉ
- DELETE `/api/v1/user/addresses/{id}` - Xóa địa chỉ

### 6.3. Product APIs
- GET `/api/v1/products` - Lấy danh sách sản phẩm (có phân trang, lọc, sắp xếp)
- GET `/api/v1/products/{id}` - Lấy chi tiết sản phẩm
- GET `/api/v1/products/{id}/reviews` - Lấy danh sách đánh giá
- POST `/api/v1/products/{id}/reviews` - Thêm đánh giá (yêu cầu đăng nhập)

### 6.4. Category & Brand APIs
- GET `/api/v1/categories` - Lấy danh sách danh mục
- GET `/api/v1/categories/{id}` - Lấy chi tiết danh mục
- GET `/api/v1/brands` - Lấy danh sách thương hiệu
- GET `/api/v1/brands/{id}` - Lấy chi tiết thương hiệu

### 6.5. Cart APIs
- GET `/api/v1/cart` - Lấy giỏ hàng
- POST `/api/v1/cart/items` - Thêm sản phẩm vào giỏ hàng
- PUT `/api/v1/cart/items/{id}` - Cập nhật số lượng
- DELETE `/api/v1/cart/items/{id}` - Xóa sản phẩm khỏi giỏ hàng
- DELETE `/api/v1/cart/clear` - Xóa toàn bộ giỏ hàng

### 6.6. Order APIs
- POST `/api/v1/checkout` - Tạo đơn hàng
- GET `/api/v1/orders` - Lấy danh sách đơn hàng của người dùng
- GET `/api/v1/orders/{id}` - Lấy chi tiết đơn hàng
- POST `/api/v1/orders/lookup` - Tra cứu đơn hàng công khai

### 6.7. Voucher APIs
- GET `/api/v1/vouchers` - Lấy danh sách voucher
- GET `/api/v1/vouchers/{id}` - Lấy chi tiết voucher
- POST `/api/v1/vouchers/validate` - Xác thực voucher (công khai)

### 6.8. Review APIs
- GET `/api/v1/reviews/{id}` - Lấy chi tiết đánh giá
- POST `/api/v1/reviews/{id}/like` - Like đánh giá
- DELETE `/api/v1/reviews/{id}/like` - Unlike đánh giá

### 6.9. Location APIs
- GET `/api/v1/location/provinces` - Lấy danh sách tỉnh/thành phố
- GET `/api/v1/location/provinces/{code}` - Lấy chi tiết tỉnh/thành phố
- GET `/api/v1/location/wards` - Lấy danh sách phường/xã
- GET `/api/v1/location/wards/{code}` - Lấy chi tiết phường/xã
- GET `/api/v1/location/provinces/{code}/wards` - Lấy danh sách phường/xã theo tỉnh

### 6.10. Admin APIs

#### Dashboard
- GET `/api/v1/admin/dashboard/summary` - Tổng quan dashboard
- GET `/api/v1/admin/dashboard/stats` - Thống kê chi tiết
- GET `/api/v1/admin/dashboard/recent-orders` - Đơn hàng gần đây
- GET `/api/v1/admin/dashboard/low-stock` - Sản phẩm sắp hết hàng

#### Products Management
- GET `/api/v1/admin/products` - Lấy danh sách sản phẩm (admin)
- GET `/api/v1/admin/products/{id}` - Lấy chi tiết sản phẩm
- POST `/api/v1/admin/products` - Tạo sản phẩm mới
- PUT `/api/v1/admin/products/{id}` - Cập nhật sản phẩm
- DELETE `/api/v1/admin/products/{id}` - Xóa sản phẩm

#### Categories Management
- GET `/api/v1/admin/categories` - Lấy danh sách danh mục
- POST `/api/v1/admin/categories` - Tạo danh mục mới
- PUT `/api/v1/admin/categories/{id}` - Cập nhật danh mục
- DELETE `/api/v1/admin/categories/{id}` - Xóa danh mục

#### Brands Management
- GET `/api/v1/admin/brands` - Lấy danh sách thương hiệu
- POST `/api/v1/admin/brands` - Tạo thương hiệu mới
- PUT `/api/v1/admin/brands/{id}` - Cập nhật thương hiệu
- DELETE `/api/v1/admin/brands/{id}` - Xóa thương hiệu

#### Orders Management
- GET `/api/v1/admin/orders` - Lấy danh sách đơn hàng
- GET `/api/v1/admin/orders/{id}` - Lấy chi tiết đơn hàng
- PUT `/api/v1/admin/orders/{id}/status` - Cập nhật trạng thái đơn hàng

#### Users Management
- GET `/api/v1/admin/users` - Lấy danh sách người dùng
- GET `/api/v1/admin/users/{id}` - Lấy chi tiết người dùng
- PUT `/api/v1/admin/users/{id}/status` - Cập nhật trạng thái người dùng
- PUT `/api/v1/admin/users/{id}/role` - Cập nhật vai trò người dùng

#### Vouchers Management
- GET `/api/v1/admin/vouchers` - Lấy danh sách voucher
- POST `/api/v1/admin/vouchers` - Tạo voucher mới
- PUT `/api/v1/admin/vouchers/{id}` - Cập nhật voucher
- DELETE `/api/v1/admin/vouchers/{id}` - Xóa voucher

### 6.11. OAuth2 APIs
- GET `/oauth2/google` - Chuyển hướng đến Google OAuth
- GET `/oauth2/google/callback` - Callback từ Google OAuth

### 6.12. Health Check
- GET `/api/v1/health` - Kiểm tra trạng thái hệ thống

## 7. Bảo mật

### Authentication
- **JWT Tokens**: Sử dụng JWT cho authentication
- **Access Token**: Thời hạn 24 giờ
- **Refresh Token**: Thời hạn 7 ngày
- **Token Blacklisting**: Lưu token đã logout trong Redis
- **Password Hashing**: Sử dụng BCrypt để hash mật khẩu

### Authorization
- **Role-based Access Control**: Phân quyền theo vai trò (customer/admin)
- **JWT Filter**: Tự động kiểm tra token cho các protected endpoints
- **Admin Endpoints**: Chỉ admin mới có thể truy cập

### Security Features
- **CORS**: Cấu hình Cross-Origin Resource Sharing
- **Input Validation**: Validate tất cả input từ client
- **SQL Injection Prevention**: Sử dụng JPA/Hibernate parameterized queries
- **XSS Prevention**: Escape output trong JSP
- **CSRF Protection**: Sử dụng token-based authentication
- **Rate Limiting**: Giới hạn số lần gửi OTP (Redis)

### Email Security
- **OTP Expiration**: OTP có thời hạn 5 phút
- **OTP Attempts Limit**: Giới hạn số lần nhập sai OTP (5 lần)
- **Email Verification**: Xác thực email khi đăng ký

## 8. Infrastructure & Deployment

### Docker Configuration
- **Dockerfile**: Multi-stage build để tối ưu image size
- **docker-compose.yml**: Định nghĩa services (app, postgres, redis)
- **Environment Variables**: Cấu hình qua file .env

### Services
- **Application Service**: Java application chạy trên Tomcat
- **PostgreSQL Service**: Database server
- **Redis Service**: Cache và session storage

### Health Checks
- Health check cho tất cả services
- Application health endpoint: `/api/v1/health`

### Logging
- **Log4j2**: Logging framework
- **Log Files**: Lưu trong thư mục `/logs`
- **Log Rotation**: Tự động rotate log files

### Environment Variables
Các biến môi trường cần thiết:
- Database: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
- Redis: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, REDIS_USERNAME, REDIS_URL
- Email: MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM
- Google OAuth: GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GOOGLE_REDIRECT_URI
- Admin Email: ADMIN_EMAIL

## 9. Cấu trúc Dự án

### Thư mục Source Code
```
src/main/java/com/utephonehub/
├── config/          # Cấu hình (Database, Redis)
├── controller/      # Controllers (xử lý HTTP requests)
├── service/         # Services (business logic)
├── repository/      # Repositories (data access)
├── entity/          # Entities (database models)
├── dto/             # Data Transfer Objects
│   ├── request/     # Request DTOs
│   └── response/    # Response DTOs
├── exception/       # Custom exceptions
├── filter/          # Servlet filters
└── util/            # Utility classes
```

### Thư mục Web Resources
```
src/main/webapp/
├── WEB-INF/
│   ├── views/       # JSP views
│   │   ├── admin/   # Admin views
│   │   ├── auth/    # Authentication views
│   │   ├── cart/    # Cart views
│   │   ├── order/   # Order views
│   │   ├── product/ # Product views
│   │   ├── user/    # User views
│   │   └── common/  # Common components
│   └── web.xml      # Web application configuration
├── static/
│   ├── css/         # Stylesheets
│   ├── js/          # JavaScript files
│   └── images/      # Images
└── index.jsp        # Home page
```

### Thư mục Configuration
```
src/main/resources/
├── META-INF/
│   └── persistence.xml  # JPA configuration
└── log4j2.xml           # Logging configuration
```

### Thư mục Docker
```
docker/
└── postgres/
    ├── init.sql            # Database initialization
    ├── cloud-sql-init.sql  # Cloud SQL initialization
    ├── products-import.sql # Sample products
    └── sample-data.sql     # Sample data
```

## 10. Quy trình Làm việc

### Development Workflow
1. **Local Development**: Sử dụng Docker Compose để chạy local
2. **Database Migration**: Hibernate tự động tạo/update schema
3. **Testing**: Test các API endpoints
4. **Deployment**: Build WAR file và deploy lên Tomcat

### API Development
- Tất cả API trả về JSON format
- Sử dụng standard HTTP status codes
- Error handling với message rõ ràng
- API versioning: `/api/v1/`

### Frontend Development
- JSP cho server-side rendering
- JavaScript cho client-side logic
- AJAX calls để tương tác với API
- Responsive design

## 11. Tính năng Đặc biệt

### Guest Checkout
- Khách hàng có thể đặt hàng mà không cần đăng ký
- Chỉ cần cung cấp email và thông tin giao hàng

### Voucher System
- Hỗ trợ giảm giá theo phần trăm hoặc số tiền cố định
- Thiết lập điều kiện sử dụng (giá trị đơn hàng tối thiểu)
- Giới hạn số lần sử dụng
- Thời hạn sử dụng

### Review System
- Chỉ người đã mua sản phẩm mới được đánh giá
- Like/Unlike đánh giá
- Hiển thị rating trung bình

### Order Tracking
- Tra cứu đơn hàng công khai bằng mã đơn và email
- Theo dõi trạng thái đơn hàng
- Cập nhật trạng thái đơn hàng (admin)

### Email Notifications
- Email xác nhận đơn hàng
- Email thông báo đơn hàng mới (admin)
- Email reset mật khẩu
- Email xác thực email

### Google OAuth2
- Đăng nhập bằng tài khoản Google
- Tự động tạo tài khoản nếu chưa có
- Lấy thông tin từ Google (email, name, picture)

## 12. Tối ưu Hóa

### Performance
- **Connection Pooling**: Sử dụng HikariCP và C3P0
- **Redis Caching**: Cache sản phẩm và dữ liệu thường xuyên truy cập
- **Lazy Loading**: Sử dụng lazy loading cho relationships
- **Database Indexes**: Indexes trên các cột thường xuyên query

### Scalability
- **Stateless Authentication**: JWT tokens không lưu trữ trên server
- **Redis Session Management**: Quản lý session bằng Redis
- **Docker Containerization**: Dễ dàng scale horizontally

### Security
- **Password Hashing**: BCrypt với salt
- **Token Expiration**: Tokens có thời hạn
- **Input Validation**: Validate tất cả input
- **SQL Injection Prevention**: Sử dụng parameterized queries

## 13. Tài liệu Bổ sung

### Postman Collection
- File Postman collection: `postman/UTE-PhoneHub-Collection.json`
- Environment files: Local và Production
- Tài liệu API endpoints trong Postman

### Database Scripts
- Initialization script: `docker/postgres/init.sql`
- Sample data: `docker/postgres/sample-data.sql`
- Products import: `docker/postgres/products-import.sql`

## 14. Kết luận

UTE Phone Hub là một hệ thống thương mại điện tử hoàn chỉnh với đầy đủ các tính năng cần thiết cho việc bán hàng trực tuyến. Dự án sử dụng các công nghệ hiện đại và best practices để đảm bảo tính bảo mật, hiệu suất và khả năng mở rộng.

### Điểm mạnh
- Kiến trúc rõ ràng, dễ bảo trì
- Bảo mật tốt với JWT và role-based access control
- Hỗ trợ đầy đủ các tính năng e-commerce
- Tích hợp Google OAuth2
- Hỗ trợ guest checkout
- Hệ thống voucher linh hoạt
- Email notifications
- Admin dashboard với thống kê chi tiết

### Hướng phát triển
- Thêm payment gateway integration
- Thêm shipping integration
- Thêm inventory management nâng cao
- Thêm analytics và reporting
- Thêm mobile app
- Thêm multi-language support
- Thêm advanced search với Elasticsearch
- Thêm recommendation system

---

**Lưu ý**: Tài liệu này chỉ cung cấp tổng quan về dự án. Để biết chi tiết về implementation, vui lòng tham khảo source code và các tài liệu kỹ thuật khác.

