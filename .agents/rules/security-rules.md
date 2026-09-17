# Rào Chắn Bảo Mật & Phòng Thủ Hệ Thống (Security Rules)

Tài liệu này quy định các nguyên tắc an ninh bảo mật và phòng thủ rủi ro bắt buộc đối với RoomSync. Mọi dòng mã do AI Agent hoặc con người tạo ra đều phải tuân thủ nghiêm ngặt.

---

## 1. Kiểm Soát Thông Tin Nhạy Cảm (Secrets & Credentials)

- **Cấm Tuyệt Đối Hardcode:** Không bao giờ đưa mật khẩu, JWT secret key, API token hoặc thông tin cấu hình nhạy cảm trực tiếp vào mã nguồn Java hoặc TypeScript.
- **Biến Môi Trường (Environment Variables):** Sử dụng `application.properties` (hoặc `application-*.properties`) nạp qua `${ENV_VAR}` hoặc file cấu hình cục bộ không bị track vào Git.
- **Git Leaks:** Không commit file `.env`, file private key (`*.key`, `*.pem`) hoặc file cấu hình cá nhân.

---

## 2. Kiểm Soát Đầu Vào & Phòng Chống Tấn Công (Input Sanitization)

### 2.1. Bean Validation Bắt Buộc (Backend)
Mọi Request DTO gửi lên từ client đều phải được thẩm định nghiêm ngặt qua Hibernate Validator / Spring Validation:
- `@NotNull`: Kiểm tra các trường bắt buộc (`roomId`, `startTime`, `endTime`, `employeeEmail`).
- `@Email`: Đảm bảo email nhân viên đúng định dạng nội bộ.
- `@Size(max = ...)`: Chặn Payload Flooding (giới hạn độ dài tiêu đề cuộc họp tối đa 150 ký tự).
- Chống XSS: Thoát các ký tự HTML đặc biệt khi hiển thị tiêu đề cuộc họp lên giao diện người dùng.

### 2.2. Validate Khung Giờ Đặt Phòng
- Chặn đặt phòng trong quá khứ (`startTime.isBefore(Instant.now())` hoặc quy định biên).
- Chặn thời gian đảo ngược (`endTime.isBefore(startTime)` hoặc `endTime.equals(startTime)`).
- Chặn thời lượng âm hoặc thời lượng vượt quá ngưỡng 120 phút.

---

## 3. Phòng Ngừa Tấn Công Đua Luồng (Race Condition & Concurrency Attacks)

- **Nguy cơ:** Khi mở cổng đặt phòng cho 200 nhân sự, hai nhân viên có thể bấm nút cùng 1 tích tắc để giành cùng một phòng trong cùng một khung giờ.
- **Giải pháp Phòng Thủ:**
  - Áp dụng kỹ thuật phân tách khóa theo tài nguyên (`Fine-grained locking by Room ID` sử dụng `ConcurrentHashMap<Long, ReentrantLock>`).
  - Toàn bộ chu trình `Kiểm tra trùng lịch -> Lưu lịch mới` phải nằm trong khối `try { lock.lock(); ... } finally { lock.unlock(); }`.
  - Không khóa toàn bộ hệ thống (tránh bottle-neck trên toàn bộ 5 phòng), chỉ khóa phòng đang được yêu cầu đặt.

---

## 4. Bảo Vệ API Endpoints & CORS
- Thiết lập CORS an toàn trong Spring Boot (`WebMvcConfigurer`), chỉ cho phép các origin hợp lệ của Frontend (ví dụ: `http://localhost:5173`).
- Chuẩn hóa mã phản hồi lỗi: Không bao giờ trả về nguyên vẹn Stack Trace ra ngoài môi trường Production nhằm tránh lộ thông tin nội bộ hệ thống (Information Disclosure). Luôn trả về cấu trúc lỗi chuẩn dạng `ErrorResponse { code, message, timestamp }`.
