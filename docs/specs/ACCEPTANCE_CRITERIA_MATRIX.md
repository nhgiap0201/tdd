# Bảng Kế hoạch Kiểm thử (Acceptance Criteria Matrix)

> [!NOTE]
> Bảng tiêu chuẩn này đóng vai trò là "la bàn" để tiến hành quá trình TDD (Red ➔ Green ➔ Refactor). Nó đảm bảo Frontend và Backend được kiểm thử độc lập nhưng vẫn khớp nối chặt chẽ với nhau ở các điểm giao tiếp.

## PHẦN A: Kiểm thử Backend (Logic nghiệp vụ, Database, Concurrency Lock)

| Mã Test | Phân loại | Tên kịch bản hành vi | Dữ liệu đầu vào (Input DTO) | Kết quả kỳ vọng (Expected Output) | Tín hiệu lỗi nếu Fail |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-BE-01** | Happy Path | Đặt phòng thành công khi phòng trống và đủ sức chứa | DTO: roomId=R-01 (max 10 người), attendees=5, startTime, endTime (ở tương lai) | HTTP `201 Created`, Trạng thái `CONFIRMED`, trả về ID | Trả về `500 ISE` hoặc `400 Bad Request` |
| **TC-BE-02** | Validation | Từ chối yêu cầu thiếu thông tin bắt buộc | DTO: roomId=null, attendees=null | HTTP `400 Bad Request`, Trả về danh sách lỗi từ Jakarta Validation | HTTP `201` (Lưu data rác) hoặc `500 ISE` |
| **TC-BE-03** | Validation | Thời lượng cuộc họp dưới 15 phút (Invalid Duration) | DTO: startTime="09:00Z", endTime="09:10Z" (khoảng cách 10 phút) | HTTP `400 Bad Request`, `errorCode="INVALID_DURATION"` | HTTP `201 Created` (Vẫn cho phép) |
| **TC-BE-04** | Validation | Đặt phòng vào thời gian trong quá khứ | DTO: startTime và endTime bé hơn `NOW()` | HTTP `400 Bad Request`, `errorCode="PAST_TIME"` | HTTP `201 Created` (Vẫn cho phép) |
| **TC-BE-05** | Validation | Số lượng tham gia vượt quá mức sức chứa (Capacity Boundary) | DTO: roomId=R-02 (max 5 người), attendees=8 | HTTP `400 Bad Request`, `errorCode="CAPACITY_EXCEEDED"` | HTTP `201 Created` (Vượt sức chứa) |
| **TC-BE-06** | Overlap | Từ chối đặt phòng trùng lịch (giao thoa một phần hoặc toàn phần) | Đã có sẵn lịch A (09:00Z - 10:00Z) trong DB. DTO yêu cầu: 09:30Z - 10:30Z | HTTP `409 Conflict`, `errorCode="OVERLAPPING_BOOKING"` | HTTP `201 Created` (Xảy ra thảm họa Double-booking) |
| **TC-BE-07** | Overlap | Cho phép đặt phòng sát giờ (liền kề nhưng không trùng) | Đã có sẵn lịch A (09:00Z - 10:00Z) trong DB. DTO yêu cầu: 10:00Z - 11:00Z | HTTP `201 Created`, Trạng thái `CONFIRMED` | HTTP `409 Conflict` (Lỗi Logic False Positive) |
| **TC-BE-08** | Concurrency| Hai yêu cầu đặt cùng phòng, cùng thời gian xảy ra đồng thời (Race Condition) | 2 HTTP request B và C được bắn song song tới Controller (cùng payload) | Một request nhận HTTP `201`, request còn lại bắt buộc nhận HTTP `409 Conflict` | Cả hai request đều nhận HTTP `201` (Race Condition xảy ra) |
| **TC-BE-09** | Validation | Đặt một phòng không tồn tại trong hệ thống | DTO: roomId=R-999 (không có trong DB) | HTTP `404 Not Found`, `errorCode="ROOM_NOT_FOUND"` | HTTP `500 ISE` (Crash DB) |

---

## PHẦN B: Kiểm thử Frontend (Validate form, Disabled nút bấm khi Loading, Báo lỗi từ API)

| Mã Test | Phân loại | Tên kịch bản hành vi | Dữ liệu đầu vào (Form Input) | Kết quả kỳ vọng (UI State) | Tín hiệu lỗi nếu Fail |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-FE-01** | UI State | Render form mặc định | Người dùng mở trang /booking | Form trống, Nút Submit hiển thị (Active) bình thường | Blank screen, UI vỡ, Nút Submit bị mờ vô lý |
| **TC-FE-02** | Validation | Chặn Submit khi bỏ trống thông tin (Client-side) | Để trống toàn bộ Form và bấm Submit | Nút Submit không kích hoạt gọi API, các ô text viền đỏ kèm thông báo bắt buộc nhập | Trình duyệt vẫn gọi Axios/API với DTO rác |
| **TC-FE-03** | Validation | Chặn nhập số lượng người không hợp lệ | Ô "Số lượng": nhập `0` hoặc `-5` | Input báo đỏ "Số người tham gia phải lớn hơn 0" | Trình duyệt vẫn đẩy số âm xuống Backend |
| **TC-FE-04** | UI State | Vô hiệu hóa nút Submit khi đang gọi API (Chống Double-click spam) | Điền form hợp lệ, bấm nút Submit | Ngay lập tức Nút Submit chuyển sang trạng thái `Disabled` (hoặc mờ đi) kèm icon Loading / Spinner | Nút Submit không khóa, bấm 3 lần gọi API 3 lần |
| **TC-FE-05** | UI State | Xử lý Happy Path: Nhận kết quả thành công | API Backend trả về HTTP `201` | Hiển thị Toast thông báo xanh "Đặt phòng thành công", Form tự động reset trống | Màn hình kẹt Loading mãi mãi |
| **TC-FE-06** | UI State | Hiển thị Toast lỗi khi bị trùng lịch (Stale State) | Cố tình Submit, API Backend trả về HTTP `409` | Hiển thị Toast thông báo đỏ "Phòng đã bị đặt, vui lòng chọn khung giờ khác", Gọi hàm tự động tải lại danh sách phòng | Không có Toast, người dùng không biết tại sao fail |
| **TC-FE-07** | UI State | Hiển thị lỗi từ chối do quá mức sức chứa | API Backend trả về HTTP `400` với mã `CAPACITY_EXCEEDED` | Hiển thị Toast thông báo đỏ "Số lượng người vượt sức chứa tối đa của phòng" | Trình duyệt báo lỗi "Có lỗi không xác định" hoặc im lặng |
