# [PRD] Tiêu Đề Tính Năng / Yêu Cầu Sản Phẩm

- **Người yêu cầu (Owner):** [Product Owner / BA Name]
- **Trạng thái:** DRAFT | IN REVIEW | APPROVED
- **Ngày tạo:** YYYY-MM-DD
- **Phiên bản:** 1.0.0

---

## 1. Bối Cảnh & Vấn Đề (Problem Statement)
*Mô tả ngắn gọn nỗi đau của người dùng hoặc vấn đề kinh doanh cần giải quyết.*

## 2. Mục Tiêu Nghiệp Vụ (Goals & Metrics)
- **Mục tiêu chính:** [Ví dụ: Giảm 90% tình trạng tranh chấp phòng họp].
- **Chỉ số đo lường (KPI):** [Ví dụ: 100% các lượt đặt phòng không xảy ra xung đột thời gian].

## 3. Chân Dung Người Dùng & Kịch Bản Sử Dụng (User Stories)
- **US-01:** Là một [Vai trò người dùng], tôi muốn [Hành động] để [Lợi ích mang lại].
- **US-02:** ...

## 4. Tiêu Chí Nghiệm Thu (Acceptance Criteria - Gherkin Format)

### Kịch bản 1: Thành công (Happy Path)
- **Given:** Người dùng đã chọn phòng họp A còn trống lúc 09:00 - 10:00 ngày Thứ Ba.
- **When:** Người dùng điền email hợp lệ và nhấn "Xác Nhận Đặt Phòng".
- **Then:** Hệ thống tạo lịch đặt thành công với trạng thái `CONFIRMED` và trả về mã đặt phòng.

### Kịch bản 2: Trùng lịch (Conflict)
- **Given:** Phòng họp A đã có người đặt từ 09:00 - 10:00.
- **When:** Người dùng khác cố gắng đặt phòng A từ 09:30 - 10:30.
- **Then:** Hệ thống từ chối yêu cầu và báo lỗi `BOOKING_OVERLAPPED`.

## 5. Ranh Giới Tính Năng (Out of Scope)
- Những gì **KHÔNG** làm trong giai đoạn này (ví dụ: Chưa hỗ trợ đặt phòng lặp lại theo tuần, chưa tích hợp Google Calendar).
