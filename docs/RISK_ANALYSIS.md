# Báo cáo Phân tích Rủi ro Kỹ thuật & Kịch bản Biên (Chaos & Architecture)

> [!WARNING]
> Tài liệu này được phân tích dưới góc độ Chaos Engineering và Full-stack Architecture, không chứa mã nguồn sản phẩm. Trọng tâm của tài liệu là nhận diện, bẻ gãy hệ thống ở các kịch bản cực đoan và đưa ra đối sách (Mitigation).

---

## 1. Rủi ro lệch múi giờ (Timezone Mismatch)

**Nguyên nhân:**
JavaScript (Frontend) sử dụng đối tượng `Date` và render mặc định theo Local Time của trình duyệt. Trong khi đó, Backend (Java) thường thao tác với thời gian tuyệt đối (`UTC`). Nếu việc truyền tải qua lại giữa hai môi trường không có chuẩn chung, dữ liệu sẽ bị lệch giờ.

**Kịch bản biên (Edge Case):**
* Một nhân viên dùng laptop thiết lập múi giờ Tokyo (UTC+9) đặt lịch phòng lúc 10:00 sáng theo giờ Việt Nam (UTC+7). JS tạo chuỗi ISO: `2026-09-16T10:00:00+09:00`.
* Nếu Java Backend bất cẩn sử dụng `LocalDateTime.parse()`, nó sẽ vứt bỏ offset `+09:00` và lưu thẳng giá trị `10:00` vào Database. Hậu quả là lệch 2 tiếng so với thực tế.
* Khi query trả về, Frontend ở Việt Nam nhận `10:00Z` và tự động cộng 7 tiếng thành `17:00` (sai hoàn toàn giờ đặt thực tế).

> [!TIP]
> **Giải pháp Architect:**
> 1. **Frontend:** Mọi API request phải chuyển thời gian về chuẩn ISO-8601 UTC (VD: dùng `date.toISOString()`).
> 2. **Backend:** **TUYỆT ĐỐI KHÔNG** dùng `LocalDateTime`. Bắt buộc sử dụng `Instant` hoặc `OffsetDateTime` (chỉ định UTC) tại Controller và Entity.
> 3. **Database:** Cấu hình Data Type cột thời gian phải lưu giữ múi giờ (ví dụ trong PostgreSQL là `TIMESTAMP WITH TIME ZONE`).

---

## 2. Rủi ro Concurrency trong Spring Boot (Race Condition / Double-Booking)

**Nguyên nhân:**
Hai HTTP request độc lập cùng đặt một phòng `Room A`, cùng khung giờ `09:00 - 10:00`, và chạm vào Controller Spring Boot gần như tại cùng một mili-giây.

**Kịch bản biên:**
Cả hai thread `Thread-1` và `Thread-2` cùng thực hiện lệnh SELECT để kiểm tra: "Phòng A có trống lúc 09:00 không?". Cả hai đều nhận kết quả "Trống" (vì chưa ai lưu vào DB). Sau đó, cả hai cùng thực thi `INSERT INTO Booking` thành công. Hậu quả: **Double-booking thảm họa**.

> [!CAUTION]
> **Phân tích đối sách:**
> * **Pessimistic Lock (Khóa bi quan):** Sử dụng `@Lock(LockModeType.PESSIMISTIC_WRITE)` khi truy vấn. Tuy nhiên, nó khóa dòng dữ liệu, có thể dẫn tới Deadlock khi tải cao hoặc làm giảm Throughput trầm trọng.
> * **Optimistic Lock (Khóa lạc quan):** Dùng cột `@Version` trong JPA. Khó áp dụng trực tiếp cho hành động `INSERT` dòng mới (vì ta không cập nhật `Room` entity mỗi khi có booking mới, làm thế sẽ gây tranh chấp không đáng có).
> * **Database Exclusion Constraint (Giải pháp hoàn hảo):** Sử dụng sức mạnh của PostgreSQL với giới hạn ngoại trừ (Exclusion Constraint).
>   * SQL: `EXCLUDE USING gist (room_id WITH =, booking_range WITH &&)` (Đảm bảo không bao giờ có 2 khoảng thời gian `&&` - giao nhau cho cùng một `room_id`).
>   * Ở phía Spring Boot, chỉ cần catch ngoại lệ `DataIntegrityViolationException` và map thành lỗi HTTP `409 Conflict`. Hệ thống được bảo vệ ở tầng thấp nhất.

---

## 3. Rủi ro Frontend (React & TanStack Query)

**Nguyên nhân:** Thao tác người dùng không lường trước (Human Errors), độ trễ mạng (Network Latency), và cơ chế bộ nhớ đệm (Caching).

**Các rủi ro cụ thể:**
1. **Lỗi Double-Click (Multi-submissions):**
   * *Edge Case:* Người dùng điền xong form, mạng lag nên click nút "Đặt phòng" 3 lần liên tục.
   * *Mitigation:* Trong React, sử dụng `isPending` của TanStack Query (`useMutation`) để ngay lập tức `disabled` nút submit và hiển thị spinner. Có thể triển khai thêm `Idempotency-Key` (UUID sinh từ FE) ở Header gửi xuống BE.
2. **Lỗi Stale State (Dữ liệu "Ôi thiu"):**
   * *Edge Case:* Màn hình người dùng hiển thị danh sách phòng trống lúc `08:00`. Họ treo máy đi uống cafe, quay lại lúc `08:15` và bấm đặt. Tuy nhiên, lúc `08:10` đã có người khác đặt phòng đó.
   * *Mitigation:* Phải handle lỗi HTTP `409 Conflict` trả về từ Backend thật mềm mỏng, báo toast message "Phòng vừa được đặt bởi người khác. Vui lòng chọn phòng khác.". Sau đó, ngay lập tức gọi `queryClient.invalidateQueries({ queryKey: ['rooms'] })` để làm mới danh sách tức thì.

---

## 4. State Machine của Booking (Vòng đời trạng thái)

Việc quản lý trạng thái lỏng lẻo sẽ sinh ra bug khi người dùng cố "Hủy" một cuộc họp đang diễn ra. Bảng State Machine sau giới hạn cứng các hành vi được phép:

| Trạng thái hiện tại | Kích hoạt (Trigger) | Trạng thái tiếp theo | Ràng buộc (Guards) |
| :--- | :--- | :--- | :--- |
| `[NULL]` | Khởi tạo | `DRAFT` | Đang trên form UI, chưa gọi API lưu DB. |
| `DRAFT` | `SUBMIT` | `CONFIRMED` | Validate chống trùng lịch thành công & Lưu DB thành công. |
| `CONFIRMED` | `TIME_ARRIVED` | `IN_PROGRESS` | Thời gian thực (NOW) $\ge$ `startTime` và < `endTime`. (Thường do cronjob hoặc lazy evaluation xử lý) |
| `IN_PROGRESS` | `TIME_PASSED` | `COMPLETED` | Thời gian thực (NOW) $\ge$ `endTime`. |
| `CONFIRMED` | `CANCEL_REQUEST` | `CANCELLED` | Chỉ Owner/Admin được hủy **TRƯỚC** khi giờ họp bắt đầu. |
| `IN_PROGRESS`| `CANCEL_REQUEST` | *(Block - Lỗi)* | Đang trong giờ họp thì không thể hủy, chỉ hỗ trợ tính năng "Kết thúc sớm" (Cập nhật lại `endTime = NOW`). |

---

## 5. Ba (3) Quy tắc Bất biến toán học (Invariants)

Đây là 3 định lý **TUYỆT ĐỐI** không được vi phạm ở bất kỳ tầng nào (Frontend / Backend / Database). Dù hacker có by-pass Frontend, Backend và Database vẫn phải chặn lại.

1. **Bất biến thời gian hợp lệ (Time Validity Invariant)**
   * Một Booking hợp lệ thì thời gian kết thúc phải luôn lớn hơn thời gian bắt đầu một lượng tối thiểu.
   * Ký hiệu toán học: $\forall \text{Booking } b : b.endTime - b.startTime \ge \text{MIN\_DURATION}$ (VD: 15 phút). Đồng thời, lúc khởi tạo: $b.startTime > \text{NOW()}$.

2. **Bất biến không giao thoa thời gian (Non-overlapping Invariant)**
   * Đối với bất kỳ 2 booking nào ($b_1, b_2$) có **cùng một phòng** (cùng `roomId`) và đang ở trạng thái hiệu lực (`CONFIRMED` hoặc `IN_PROGRESS`), khoảng thời gian của chúng không được phép cắt nhau.
   * Ký hiệu toán học: $(b_1.startTime, b_1.endTime) \cap (b_2.startTime, b_2.endTime) = \emptyset$.

3. **Bất biến sức chứa (Capacity Boundary Invariant)**
   * Số lượng người tham dự khai báo không bao giờ được vượt quá sức chứa vật lý thiết kế của phòng đó.
   * Ký hiệu toán học: $\forall \text{Booking } b : b.attendees \le \text{Room}(b.roomId).maxCapacity$.
