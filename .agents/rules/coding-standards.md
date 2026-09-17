# Quy Chuẩn Lập Trình Dự Án RoomSync (Coding Standards)

Tài liệu này định nghĩa các nguyên tắc và phong cách lập trình bắt buộc đối với cả mã nguồn Backend (Java 21 Spring Boot) và Frontend (React TypeScript).

---

## 1. Backend: Java 21 & Spring Boot Conventions

### 1.1. Kiến Trúc Phân Lớp (Layered Architecture)
Mã nguồn phải tuân thủ nghiêm ngặt ranh giới giữa các tầng:
- **`domain/`**: Chứa Core Entities, Enums (`BookingStatus`, `BookingLimits`). Tuyệt đối không phụ thuộc vào Spring Web hay Database driver.
- **`dto/`**: Data Transfer Objects (`BookingRequest`, `BookingResponse`, `ErrorResponse`). Sử dụng Java Records hoặc immutable POJOs kết hợp Lombok `@Getter`, `@Builder`. Bắt buộc khai báo Bean Validation (`@NotNull`, `@NotBlank`, `@FutureOrPresent`).
- **`repository/`**: Chứa Interface (`BookingRepository`, `RoomRepository`) và các triển khai cụ thể (`InMemoryBookingRepository`). Thao tác dữ liệu nội tại phải đảm bảo tính Thread-safe (`ConcurrentHashMap`, `CopyOnWriteArrayList`).
- **`service/`**: Chứa nghiệp vụ cốt lõi (`BookingService`, `BookingServiceImpl`). Đây là nơi duy nhất thẩm định 3 Quy tắc Bất biến và kiểm soát `ReentrantLock`.
- **`controller/`**: REST Endpoints. Chỉ làm nhiệm vụ nhận Request, validate qua `@Valid`, ủy quyền cho Service và trả về `ResponseEntity`. Xử lý lỗi tập trung tại `GlobalExceptionHandler` qua `@RestControllerAdvice`.

### 1.2. Naming Conventions & Clean Code
- **Class / Interface / Enum:** `PascalCase` (ví dụ: `BookingServiceImpl`, `RoomRepository`, `BookingErrorCode`).
- **Method / Variable:** `camelCase` (ví dụ: `createBooking`, `isOverlapping`, `lockMap`).
- **Constant:** `UPPER_SNAKE_CASE` (ví dụ: `MAX_BOOKING_MINUTES`, `DEFAULT_ROOM_CAPACITY`).
- **Xử lý Exception:** Không bao giờ nuốt ngoại lệ (Catch and swallow). Bắn ra Custom Exception (ví dụ: `BookingConflictException`, `InvalidBookingTimeException`) kèm mã lỗi cụ thể.

---

## 2. Frontend: React 18+ & TypeScript Standards

### 2.1. Cấu Trúc Thành Phần (Components)
- Sử dụng **Functional Components** với TypeScript typing tường minh (`React.FC<Props>` hoặc explicit parameter types).
- Tách biệt UI (Presentational) và Logic/State (Container / Custom Hooks).
- Tránh monolithic component: Nếu một component vượt quá 150 dòng, hãy cân nhắc tách nhỏ thành sub-components hoặc custom hook.

### 2.2. TypeScript Typing
- Bật `strict: true` trong `tsconfig.json`. Tuyệt đối không dùng `any`. Sử dụng `unknown` nếu chưa rõ kiểu và ép kiểu an toàn (Type Guard).
- Định nghĩa kiểu dùng chung trong `src/types.ts` hoặc theo feature module `src/components/.../types.ts`.
- Các DTO phải khớp 100% với schema API Backend (`BookingRequest`, `BookingResponse`, `Room`).

### 2.3. Styling & Giao Diện (Rich Aesthetics)
- Áp dụng phong cách **Modern Glassmorphism** nhất quán: gradient nền dịu mắt, hiệu ứng kính mờ `backdrop-filter: blur()`, viền trong suốt nhẹ `border: 1px solid rgba(255, 255, 255, 0.1)`.
- Phản hồi tương tác (Micro-animations): Các nút bấm khi hover/active phải có hiệu ứng chuyển động mượt mà (`transition: all 0.2s ease`).
- Trạng thái loading: Khi người dùng gửi form đặt phòng, nút bấm phải hiển thị spinner/loading và chuyển sang trạng thái `disabled` để chống double-click (thỏa mãn kịch bản kiểm thử TC-FE-04).

---

## 3. Quy Chuẩn Kiểm Thử (Test-Driven Development - TDD)

- **Nguyên lý Red-Green-Refactor:**
  1. **Red:** Viết test trước, đảm bảo test thất bại vì tính năng chưa được hiện thực.
  2. **Green:** Viết lượng mã tối thiểu để bài test vượt qua (Pass).
  3. **Refactor:** Tối ưu hóa mã, loại bỏ trùng lặp, cải thiện hiệu năng nhưng vẫn giữ toàn bộ bài test màu xanh.
- **Tên bài test tường minh (Given - When - Then):**
  - Ví dụ: `shouldThrowConflictException_whenBookingTimeOverlapsWithExistingBooking()`
  - Ví dụ: `shouldDisableSubmitButton_whenApiCallIsInProgress()`
