# [ADR-0001] Sử Dụng Cấu Trúc Monorepo Với Java Spring Boot & React TypeScript

- **Trạng thái:** ACCEPTED
- **Người đề xuất:** Tech Lead
- **Ngày:** 2026-09-17
- **Người duyệt:** Architecture Team

---

## 1. Bối Cảnh (Context)
Dự án RoomSync cần phát triển một hệ thống đặt phòng họp nội bộ hoàn chỉnh gồm Backend phục vụ logic nghiệp vụ, tính toán thời gian, kiểm soát đồng thời và Frontend tương tác người dùng thời gian thực. Việc tách thành 2 kho lưu trữ (Repo) riêng biệt trong giai đoạn này tạo ra chi phí quản trị (overhead), khó khăn trong việc đồng bộ hóa hợp đồng API (Spec-First) và quản lý quy trình kiểm thử liên thông (End-to-End).

## 2. Quyết Định (Decision)
Chúng tôi quyết định cấu trúc dự án dưới dạng **Monorepo**:
- Thư mục `be/`: Java 21, Spring Boot 4, Maven Wrapper, JUnit 5 + Mockito cho quy trình TDD. Chạy độc lập trên cổng `8080`.
- Thư mục `fe/`: React 18+, TypeScript, Vite, Vitest. Chạy trên cổng `5173`.
- Cấu hình Vite Proxy: Chuyển tiếp toàn bộ các yêu cầu bắt đầu bằng `/api` từ cổng `5173` sang `http://localhost:8080`.
- Root Orchestrator: File `package.json` và `Makefile` tại thư mục gốc quản lý các script chạy đồng thời (`concurrently`) và điều phối kiểm thử tập trung (`npm run verify`).

## 3. Hệ Quả & Đánh Đổi (Consequences & Trade-offs)
### Điểm tích cực:
- **Đồng nhất hợp đồng:** AI Coding Agent và lập trình viên có thể đọc trực tiếp tài liệu Spec (`docs/specs/`) và kiểm tra tính tương thích giữa DTO Java và Interface TypeScript cùng lúc.
- **Tự động hóa đơn giản:** Pipeline CI duy nhất có thể kiểm tra cả Backend và Frontend trong một Pull Request.
- **Tiện lợi khi phát triển cục bộ:** Chỉ cần một lệnh `npm run dev` để chạy toàn bộ hệ thống.

### Điểm đánh đổi:
- Quá trình build CI cần cài đặt cả JDK 21 và Node.js 20. Đã được giải quyết bằng cấu hình Matrix / Parallel jobs trong GitHub Actions.
