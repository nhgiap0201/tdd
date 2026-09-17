# [ADR-0003] Chuyển Đổi Sang Cơ Sở Dữ Liệu PostgreSQL & Spring Data JPA

- **Trạng thái:** ACCEPTED
- **Người đề xuất:** Tech Lead / Developer
- **Ngày:** 2026-09-17
- **Người duyệt:** Architecture Team

---

## 1. Bối Cảnh (Context)
Ban đầu, hệ thống RoomSync sử dụng mô hình In-Memory (`ConcurrentHashMap`, `CopyOnWriteArrayList`) kết hợp với `ReentrantLock` trong bộ nhớ JVM để đạt tốc độ kiểm thử TDD tối đa. Tuy nhiên, khi chuyển sang giai đoạn sẵn sàng sản xuất (Production-Ready), dữ liệu cần được lưu trữ bền vững (Persistence) và có khả năng mở rộng (Scalability) khi triển khai đa instance hoặc container.

## 2. Quyết Định (Decision)
1. **Hệ Quản trị Cơ sở Dữ liệu:** Sử dụng **PostgreSQL 16** làm cơ sở dữ liệu quan hệ chính.
2. **Khung truy xuất dữ liệu:** Sử dụng **Spring Data JPA** kết hợp Hibernate ORM.
3. **Mô hình Entity & Table:**
   - Bảng `rooms`: Lưu thông tin phòng họp, sức chứa và cờ kích hoạt (`is_active`).
   - Bảng `bookings`: Lưu thông tin đặt phòng, khoảng thời gian `[start_time, end_time)` kiểu `TIMESTAMP WITH TIME ZONE (Instant)`, `attendees`, `status` và `idempotency_key`. Đánh composite index trên `(room_id, start_time, end_time)` để tối ưu hóa truy vấn trùng lịch.
4. **Kiểm soát đồng thời (Concurrency & Race Condition):**
   - Áp dụng **Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)** trên hàng của `Room` khi bắt đầu giao dịch đặt phòng (`findByIdWithLock`).
   - Mọi giao dịch đặt phòng cùng phòng sẽ được xếp hàng tuần tự hóa (Serialized) tại tầng Database, bảo vệ tuyệt đối quy tắc bất biến `NO_OVERLAP` kể cả khi có 200 nhân viên bấm cùng 1 tích tắc.
5. **Chiến lược Dual-Profile cho TDD:**
   - **Profile Test (`src/test/resources`):** Nạp cơ sở dữ liệu nhúng H2 (chế độ PostgreSQL) in-memory. Bộ test JUnit 5 và Context Test chạy hoàn toàn độc lập, không cần bật trước PostgreSQL daemon.
   - **Profile Runtime (`src/main/resources`):** Kết nối đến PostgreSQL thật qua biến môi trường `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## 3. Hệ Quả & Đánh Đổi (Consequences & Trade-offs)
### Điểm tích cực:
- Dữ liệu lịch đặt phòng được bảo toàn vĩnh viễn khi restart server.
- Khả năng scale horizontally (nhiều instance backend) nhờ cơ chế khóa tại Database thay vì phụ thuộc RAM một JVM.
- Tốc độ chạy test nội bộ (`npm run verify`) vẫn siêu tốc (~5s) nhờ H2 mode.
- Cung cấp `docker-compose.yml` giúp lập trình viên setup DB trong 5 giây.

### Điểm đánh đổi:
- Cần vận hành thêm 1 container hoặc service PostgreSQL cho môi trường chạy thử nghiệm hoặc sản xuất.
