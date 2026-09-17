# CONCEPT PAPER: MODULE ĐẶT LỊCH PHÒNG HỌP (MEETING ROOM BOOKING - MVP)

**Dự án:** Hệ thống Quản trị & Đặt phòng họp nội bộ (RoomSync)  
**Quy mô:** 200 nhân sự | 5 phòng họp  
**Giai đoạn:** MVP (Minimum Viable Product)  
**Vai trò tư vấn:** Product Consultant ➔ Tech Lead / Product Owner  
**Vị trí tài liệu:** `docs/CONCEPT_PAPER.md` (Tài liệu định hình bài toán kinh doanh trước khi viết Test/Code)

---

## 1. Vấn đề cốt lõi (Problem Statement)
Với tỷ lệ cạnh tranh cao (**200 nhân sự / 5 phòng họp**, trung bình 40 người/phòng), việc chưa có công cụ quản trị tập trung đang gây ra 3 nút thắt nghiêm trọng:
* **Tranh chấp phòng & Trùng lịch (Double-booking):** Việc thỏa thuận lịch qua tin nhắn chat hoặc thỏa thuận miệng dẫn đến tình huống 2-3 nhóm cùng kéo vào một phòng tại một thời điểm, gây gián đoạn công việc và tạo sự bức xúc không đáng có.
* **Lãng phí thời gian họp:** Nhân viên mất từ 10–15 phút đầu mỗi buổi họp chỉ để đi tìm phòng trống hoặc buộc phải hủy/dời lịch đột xuất vì phòng đã bị nhóm khác "chiếm" trước.
* **Lãng phí sức chứa (Capacity Mismatch):** Thiếu thông tin trực quan khiến nhóm 2–3 người thường xuyên giữ phòng lớn (15–20 người), trong khi các buổi họp dự án đông người lại không tìm được không gian phù hợp.

---

## 2. Mục tiêu MVP (MVP Goals)
Xây dựng một giải pháp tối giản, vận hành ổn định với các mục tiêu cụ thể:
* **Triệt tiêu 100% tình trạng trùng lịch:** Cung cấp nguồn dữ liệu duy nhất (Single Source of Truth) với cơ chế kiểm tra và khóa lịch theo thời gian thực (Real-time conflict validation).
* **Tối ưu hóa hiệu suất sử dụng phòng:** Minh bạch hóa trạng thái phòng và giới hạn sức chứa, ghép nối chính xác nhu cầu họp với tài nguyên sẵn có.
* **Tối giản hóa thao tác người dùng:** Đảm bảo toàn bộ quy trình từ lúc tra cứu đến khi giữ chỗ hoàn tất trong **dưới 60 giây** với tối đa **4 thao tác**.
* **Làm tiền đề vững chắc cho TDD (Test-Driven Development):** Xác định ranh giới nghiệp vụ chuẩn xác để Tech Lead dễ dàng phân rã thành các kịch bản kiểm thử BDD/Unit Test (Red Phase) trước khi viết mã nguồn.

---

## 3. Giới hạn phạm vi (Scope Boundaries)

### In-Scope (Bắt buộc phải có trong phiên bản MVP này)
* **Xem & Chọn phòng:** Hiển thị danh sách 5 phòng họp kèm thông tin sức chứa tối đa (Capacity) và trang thiết bị cơ bản.
* **Chọn khung giờ:** Cho phép chọn ngày, thời gian bắt đầu và thời gian kết thúc cuộc họp (bảo đảm ràng buộc logic thời gian hợp lệ).
* **Kiểm tra trùng lịch tự động (Conflict Check):** Chặn lập tức nếu khung giờ người dùng chọn có bất kỳ khoảng giao thoa thời gian nào với cuộc họp đã xác nhận.
* **Kiểm tra sức chứa (Capacity Check):** Chặn hoặc cảnh báo khi số lượng người tham gia vượt quá sức chứa thiết kế của phòng.
* **Lưu & Hiển thị trạng thái đặt phòng:** Ghi nhận thông tin người đặt, chủ đề cuộc họp và phản ánh tức thời trạng thái phòng (*Trống / Đã đặt / Đang sử dụng*) lên giao diện chung.

### Out-of-Scope (Tuyệt đối KHÔNG làm ở bản này để tránh phình to dự án)
* ❌ **Không** đặt lịch lặp lại định kỳ (Recurring booking: hàng ngày, hàng tuần, hàng tháng).
* ❌ **Không** tích hợp đồng bộ lịch bên thứ ba (Google Calendar, Microsoft Outlook, Teams, Slack bot).
* ❌ **Không** tính năng gọi dịch vụ đi kèm (đặt trà nước, coffee break, lễ tân, mượn thêm micro/máy chiếu).
* ❌ **Không** thanh toán phí sử dụng phòng hoặc hạch toán chi phí nội bộ giữa các phòng ban.

---

## 4. Hành trình người dùng cấp cao (High-level User Journey - Tối đa 4 bước)

```
[Bước 1: Nhập nhu cầu] ➔ [Bước 2: Chọn phòng] ➔ [Bước 3: Xác nhận đặt] ➔ [Bước 4: Nhận phòng thành công]
```

1. **Bước 1 - Khai báo nhu cầu:** Người dùng mở ứng dụng, chọn ngày, khoảng thời gian họp dự kiến và số người tham dự.
2. **Bước 2 - Lựa chọn phòng:** Hệ thống tự động lọc các phòng khả dụng (còn trống khung giờ đó & đủ sức chứa); người dùng chọn 1 phòng tối ưu.
3. **Bước 3 - Xác nhận thông tin:** Điền nhanh tiêu đề cuộc họp/tên người đặt và bấm nút **"Xác nhận đặt"** (Hệ thống thực thi validate chống xung đột lịch tức thì).
4. **Bước 4 - Nhận phòng thành công:** Nhận thông báo xác nhận thành công ngay trên màn hình; trạng thái phòng được cập nhật tức thời trên bảng điều khiển chung.

---

## 5. Chỉ số đo lường thành công (Success Metrics)

| Chỉ số | Mục tiêu MVP | Cách thức đo lường |
| :--- | :---: | :--- |
| **Tỷ lệ trùng lịch (Double-booking Rate)** | **0%** | Số vụ tranh chấp/trùng phòng được phản ánh sau khi ứng dụng đi vào hoạt động. |
| **Thời gian đặt phòng (Time-to-Book)** | **< 60s** | Thời gian trung bình nhân viên thao tác từ Bước 1 đến Bước 4. |
| **Tỷ lệ ứng dụng (Adoption Rate)** | **> 90%** | Tỷ lệ cuộc họp nội bộ được đặt qua module thay vì trao đổi miệng/chat sau 30 ngày. |

---

## 6. Định hướng cấu trúc tài liệu chuẩn cho quy trình TDD

Để phục vụ tốt nhất quy trình **Test-Driven Development (TDD)**, tài liệu dự án được tổ chức phân tầng từ tổng quan nghiệp vụ đến mã kiểm thử cụ thể:

```
learn-tdd/
├── docs/                                    # [Tầng 1 - Product Concept]
│   └── CONCEPT_PAPER.md                     # Tài liệu này: Định hình phạm vi, bài toán kinh doanh & User Journey
│
├── specs/                                   # [Tầng 2 - BDD Specification & Test Matrix]
│   ├── booking.spec.md                      # Đặc tả kịch bản Given-When-Then (Gherkin), Error Codes, Invariants
│   └── ACCEPTANCE_CRITERIA_MATRIX.md        # Ma trận kiểm thử & nghiệm thu (TC-01 -> TC-19)
│
├── be/src/test/java/.../booking/            # [Tầng 3 - TDD Automated Tests (Red Phase)]
│   └── BookingServiceTest.java              # Viết Unit Tests kiểm tra từng kịch bản nghiệp vụ trước khi code logic
│
└── be/src/main/java/.../booking/            # [Tầng 4 - Implementation (Green & Refactor Phase)]
    ├── domain/                              # Entities & Business Rules
    ├── service/                             # Business Logic thỏa mãn toàn bộ Test Cases
    └── controller/                          # REST API Endpoints phục vụ User Journey
```
