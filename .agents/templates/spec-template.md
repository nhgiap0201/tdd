# [SPEC] Đặc Tả Kỹ Thuật: [Tên Tính Năng]

- **Tech Lead / Architect:** [Tên người phụ trách]
- **Trạng thái:** DRAFT | LOCKED (Hợp đồng đã đóng băng)
- **PRD Tham chiếu:** `docs/prd/...`
- **Ngày khóa Spec:** YYYY-MM-DD

---

## 1. Tổng Quan Kỹ Thuật (Technical Overview)
*Tóm tắt giải pháp kỹ thuật, phạm vi tác động đến Backend (Spring Boot) và Frontend (React TS).*

## 2. Các Quy Tắc Bất Biến Cần Bảo Vệ (Invariants)
- [ ] Invariant 1: ...
- [ ] Invariant 2: ...

## 3. Hợp Đồng Giao Diện API (API Contract)

### Endpoint: `POST /api/...`
- **Mô tả:** ...
- **Request Headers:** `Content-Type: application/json`
- **Request Body:**
```json
{
  "field1": "string",
  "field2": 123
}
```
- **Response 201 Created:**
```json
{
  "id": 1,
  "status": "CONFIRMED"
}
```
- **Response Error Codes:**
  - `400 Bad Request`: `INVALID_TIME_RANGE`
  - `409 Conflict`: `BOOKING_OVERLAPPED`

## 4. Mô Hình Dữ Liệu (Domain & DTO Schema)
- **Java DTO:** `com.roomsync.booking.dto...`
- **TypeScript Interface:** `src/types.ts...`

## 5. Kế Hoạch Kiểm Thử (Test Skeletons Plan)
Danh sách các bài test bắt buộc phải viết trước khi code (Red Phase):
- [ ] `TC-01`: ...
- [ ] `TC-02`: ...
