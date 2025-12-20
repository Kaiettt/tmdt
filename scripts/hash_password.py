#!/usr/bin/env python3
"""
Script để hash password với BCrypt
⚠️  CẢNH BÁO: Hash $2b$ từ Python bcrypt KHÔNG tương thích với jbcrypt (Java)!
jbcrypt chỉ hỗ trợ format $2a$, không hỗ trợ $2b$.

Để hash password cho project Java này, hãy sử dụng:
- Java: mvn test -Dtest=PasswordHashTest#generateAdminPasswordHash
- Hoặc: com.utephonehub.util.HashPasswordTool

Script này chỉ để tham khảo, hash tạo ra sẽ KHÔNG hoạt động với jbcrypt!
"""

import bcrypt
import sys

def hash_password(password, rounds=12):
    """
    Hash password với BCrypt
    @param password: Mật khẩu gốc
    @param rounds: Số rounds (mặc định 12, giống PasswordUtil.java)
    @return: Mật khẩu đã hash
    """
    # Generate salt với số rounds chỉ định
    salt = bcrypt.gensalt(rounds=rounds)
    # Hash password
    hashed = bcrypt.hashpw(password.encode('utf-8'), salt)
    return hashed.decode('utf-8')

def main():
    if len(sys.argv) > 1:
        password = sys.argv[1]
    else:
        password = "admin123"
    
    if len(sys.argv) > 2:
        rounds = int(sys.argv[2])
    else:
        rounds = 12
    
    print("=" * 50)
    print("PASSWORD HASHING SCRIPT")
    print("=" * 50)
    print(f"Password: {password}")
    print(f"BCrypt Rounds: {rounds}")
    print("=" * 50)
    print()
    
    try:
        hashed_password = hash_password(password, rounds)
        print("[OK] Password hashed successfully!")
        print()
        print(f"Hash: {hashed_password}")
        print()
        print("=" * 50)
        print("SQL INSERT statement:")
        print("=" * 50)
        print(f"INSERT INTO users (username, full_name, email, password_hash, phone_number, role, status, created_at, updated_at)")
        print(f"VALUES ('admin123', 'Administrator', 'admin123@utephonehub.me', '{hashed_password}', '0901234567', 'admin', 'active', NOW(), NOW());")
        print("=" * 50)
        
    except Exception as e:
        print(f"[ERROR] {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()

