# AI AGENT BEHAVIOR CONTRACT & CONVENTIONS (RoomSync)

Tài liệu này là **Hợp đồng Hành vi (Behavior Contract)** có hiệu lực cao nhất đối với mọi AI Coding Agent (Antigravity, Cursor, Claude Code, GitHub Copilot,...) khi hoạt động trong repository RoomSync.

---

## 1. Lệnh Vận Hành Tiêu Chuẩn (Standard Commands)

Agent **BẮT BUỘC** sử dụng các lệnh chuẩn hóa sau đây:

- **Xác minh toàn diện (All Checks):** `npm run verify` hoặc `make verify` (Chạy cả Backend test + Frontend test).
- **Kiểm thử Backend (Java Spring Boot):** `npm run test:be` hoặc `make test-be` (Chạy JUnit 5 qua Maven Wrapper).
- **Kiểm thử Frontend (React Vitest):** `npm run test:fe` hoặc `make test-fe` (Chạy Vitest).
- **Khởi chạy ứng dụng cục bộ:** `npm run dev` (Khởi chạy đồng thời BE port 8080 & FE port 5173).
- **Đóng gói sản phẩm:** `npm run build` hoặc `make build`.

---

## 2. Rào Chắn Bất Khả Xâm Phạm (Guardrails & Red Lines)

1. 🚫 **NGHIÊM CẤM TỰ Ý SỬA TEST ĐỂ PASS:**
   - Tuyệt đối **KHÔNG ĐƯỢC** chỉnh sửa các file kiểm thử trong `be/src/test/` hoặc `fe/src/**/*.test.tsx` trừ khi có chỉ thị trực tiếp từ Tech Lead/Human Reviewer về việc cập nhật Spec.
   - Khi kiểm thử thất bại (Red), nguyên nhân **LUÔN NẰM Ở MÃ NGUỒN TRIỂN KHAI** (`be/src/main/` hoặc `fe/src/`). Nhiệm vụ của Agent là sửa mã nguồn để thỏa mãn bài test, không phải sửa bài test để che giấu lỗi.

2. 🛡️ **BẢO VỆ TUYỆT ĐỐI 3 QUY TẮC BẤT BIẾN NGHIỆP VỤ (System Invariants):**
   - **Bất biến 1 (NO_OVERLAP):** Tuyệt đối không cho phép 2 lịch đặt phòng của cùng một phòng họp bị chồng chéo thời gian (`[startA, endA)` giao với `[startB, endB)`).
   - **Bất biến 2 (MAX_2_HOURS):** Thời lượng một cuộc họp không được vượt quá 120 phút.
   - **Bất biến 3 (BUSINESS_HOURS_ONLY):** Chỉ được đặt phòng trong khung giờ 08:00 - 18:00 các ngày Thứ Hai đến Thứ Sáu. Không cho phép đặt vào Thứ Bảy, Chủ Nhật.

3. 🔒 **AN TOÀN ĐỒNG THỜI (Concurrency & Thread Safety):**
   - Vì RoomSync hiện tại vận hành mô hình In-Memory Repository, mọi thao tác ghi/đọc kiểm tra trùng lịch **BẮT BUỘC** phải được bọc trong cơ chế khóa luồng (`ReentrantLock` theo roomId hoặc Mutex) để chống Race Condition khi có 200 nhân viên đặt phòng cùng lúc.

4. 📜 **TUÂN THỦ HỢP ĐỒNG API (API Contract Compliance):**
   - Mọi DTO, Request Body, Response Schema, Http Status Code và Error Code phải khớp 100% với tài liệu hợp đồng tại `docs/specs/openapi/booking-service.yaml` và `specs/booking.spec.md`.

5. 📦 **KHÔNG TỰ TIỆN THÊM THƯ VIỆN LẠ:**
   - Không tự ý thêm dependencies vào `be/pom.xml` hoặc `fe/package.json` khi chưa tham vấn ý kiến của con người.

6. 🔄 **VÒNG LẶP TỰ SỬA LỖI & HOÀN THÀNH TASK (Inner Loop Completion):**
   - Sau khi hoàn thành việc viết code, Agent **PHẢI TỰ ĐỘNG CHẠY** `npm run verify` (hoặc `make verify`).
   - Chỉ được phép báo cáo hoàn thành cho lập trình viên khi **100% test suite chuyển sang màu XANH (Green)**. Nếu có lỗi, tự động phân tích stack trace và tự sửa.
