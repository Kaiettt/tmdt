# Postman Collection & Environment

Thư mục này chứa Postman collection và environments cho UTE Phone Hub API.

## Files

- **UTE-PhoneHub-Collection.json**: Full API collection với tất cả endpoints
- **UTE-PhoneHub-Local.postman_environment.json**: Environment cho local development (localhost:8080)
- **UTE-PhoneHub-Production.postman_environment.json**: Environment cho production

## Import vào Postman

### Cách 1: Import Collection

1. Mở Postman
2. Click **Import** ở góc trên bên trái
3. Chọn file `UTE-PhoneHub-Collection.json`
4. Click **Import**

### Cách 2: Import Environment

1. Mở Postman
2. Click **Import**
3. Chọn file environment (.postman_environment.json)
4. Click **Import**

## Sử dụng

### 1. Chọn Environment

- Click dropdown ở góc phải trên
- Chọn **UTE PhoneHub - Local** hoặc **UTE PhoneHub - Production**

### 2. Authentication Flow

1. **Register** hoặc **Login** → Tự động lưu `accessToken` vào environment
2. Các request khác sẽ tự động dùng `{{accessToken}}` từ environment
3. Khi token hết hạn, dùng **Refresh Token** để lấy token mới

### 3. Test API

- Tất cả request đều có pre-configured với variables
- Authorization header tự động thêm từ environment
- Response tự động parse và lưu token

## API Endpoints

### Authentication

- `POST /api/v1/auth/register` - Đăng ký tài khoản
- `POST /api/v1/auth/login` - Đăng nhập
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/logout` - Đăng xuất
- `POST /api/v1/auth/forgot-password` - Quên mật khẩu
- `POST /api/v1/auth/reset-password` - Reset mật khẩu

### User Profile

- `GET /api/v1/user/profile` - Lấy thông tin profile
- `PUT /api/v1/user/profile` - Cập nhật profile
- `POST /api/v1/user/change-password` - Đổi mật khẩu

### Addresses

- `GET /api/v1/user/addresses` - Lấy tất cả địa chỉ
- `GET /api/v1/user/addresses/:id` - Lấy địa chỉ theo ID
- `POST /api/v1/user/addresses` - Tạo địa chỉ mới
- `PUT /api/v1/user/addresses/:id` - Cập nhật địa chỉ
- `DELETE /api/v1/user/addresses/:id` - Xóa địa chỉ
- `POST /api/v1/user/addresses/:id/set-default` - Set địa chỉ mặc định

### Location (Public)

- `GET /api/v1/location/provinces` - Lấy tất cả tỉnh/thành
- `GET /api/v1/location/provinces/:code` - Lấy tỉnh theo code
- `GET /api/v1/location/provinces/:code/wards` - Lấy xã/phường theo tỉnh
- `GET /api/v1/location/wards` - Lấy tất cả xã/phường
- `GET /api/v1/location/wards/:code` - Lấy xã theo code

### Health Check

- `GET /api/v1/health` - Kiểm tra trạng thái API

## Environment Variables

### Local Environment

```
baseUrl: http://localhost:8080
accessToken: (auto-saved after login)
refreshToken: (auto-saved after login)
```

### Production Environment

```
baseUrl: https://api.utephonehub.com
accessToken: (auto-saved after login)
refreshToken: (auto-saved after login)
```

## Notes

- **Authorization**: Bearer token tự động thêm vào header cho protected endpoints
- **Auto-save tokens**: Scripts tự động lưu access token sau login/register/refresh
- **HTTP-only cookies**: Refresh token được lưu trong HTTP-only cookie, không cần manual handle
- **Public endpoints**: Location APIs không cần authentication
