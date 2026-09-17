# [ADR-0002] Chiến Lược TDD, Xử Lý Đồng Thời In-Memory & Bảo Vệ 3 Quy Tắc Bất Biến

- **Trạng thái:** ACCEPTED
- **Người đề xuất:** Tech Lead / Architect
- **Ngày:** 2026-09-17
- **Người duyệt:** Architecture Team

---

## 1. Bối Cảnh (Context)
Hệ thống RoomSync phục vụ 200 nhân viên với 5 phòng họp. Tần suất đặt phòng cao có thể gây ra tranh chấp tài nguyên khi nhiều người cùng đặt 1 phòng trong cùng 1 tích tắc. Ngoài ra, việc vận hành cần bảo vệ nghiêm ngặt 3 quy tắc bất biến để tránh hỗn loạn lịch làm việc nội bộ:
1. Không trùng lặp giờ đặt của cùng 1 phòng (`NO_OVERLAP`).
2. Không đặt quá 2 giờ cho 1 cuộc họp (`MAX_2_HOURS`).
3. Chỉ đặt trong giờ làm việc Thứ 2 - Thứ 6 từ 08:00 - 18:00 (`BUSINESS_HOURS_ONLY`).

## 2. Quyết Định (Decision)
1. **Phương pháp TDD (Test-Driven Development):**
   - Viết toàn bộ bài test case cho 19 kịch bản biên (TC-01 -> TC-19) trước khi viết mã xử lý nghiệp vụ.
   - Test là hợp đồng bất biến. AI Coding Agent tuyệt đối không được sửa test để làm bài test pass.
2. **Xử lý đồng thời (Concurrency Control):**
   - Sử dụng kho lưu trữ In-Memory (`InMemoryBookingRepository`) kết hợp với `ConcurrentHashMap<Long, ReentrantLock>` để khóa luồng ở mức chi tiết (Fine-grained locking theo từng `roomId`).
   - Kiểm tra trùng lặp và lưu trữ đặt phòng được thực thi nguyên tử (Atomic) trong khối synchronized lock.
3. **Thẩm định nghiệp vụ tập trung:**
   - Đặt toàn bộ logic xác thực thời gian, kiểm tra thứ/ngày và tính toán khoảng cách giờ tại `BookingServiceImpl` trước khi ghi vào kho dữ liệu.

## 3. Hệ Quả & Đánh Đổi (Consequences & Trade-offs)
### Điểm tích cực:
- Khóa theo từng phòng giúp các yêu cầu đặt phòng khác nhau diễn ra song song hoàn toàn, không gây nghẽn cổ chai (Bottle-neck).
- Khả năng kiểm thử cực nhanh (bộ test chạy chỉ mất dưới 1 giây do không phụ thuộc DB ngoài).
- Bảo đảm 100% không xảy ra Race Condition khi đặt trùng phòng.

### Điểm đánh đổi:
- Dữ liệu in-memory sẽ mất khi restart server. Khi chuyển sang cơ sở dữ liệu quan hệ (PostgreSQL) ở giai đoạn tiếp theo, cơ chế ReentrantLock sẽ được nâng cấp lên Pessimistic Locking (`SELECT FOR UPDATE`) hoặc Distributed Lock (Redis Redlock).
