# Đặc tả nghiệp vụ Đặt phòng họp (Booking Spec - BDD)

## Feature: Đặt lịch phòng họp mới
**In order to** sử dụng phòng họp một cách hợp lý, tối ưu không gian và tránh tranh chấp  
**As a** nhân viên công ty (người dùng hệ thống)  
**I want to** đặt lịch một phòng họp dựa trên khung giờ và sức chứa phù hợp, và nhận được kết quả lập tức

### Scenario: Đặt phòng thành công (Happy Path)
**Given** phòng họp "Phòng Alpha" (`roomId: R-01`) có sức chứa 10 người đang trống từ 09:00Z đến 10:00Z  
**And** thời gian hiện tại là trước 09:00Z  
**When** tôi gửi yêu cầu đặt phòng "R-01" với 5 người tham gia từ 09:00Z đến 10:00Z  
**Then** hệ thống ghi nhận thành công và trạng thái đặt phòng chuyển thành "CONFIRMED"  
**And** hệ thống trả về thông tin `BookingResponseDTO` có chứa ID hợp lệ

### Scenario: Đặt phòng thất bại do quá mức sức chứa (Capacity Boundary)
**Given** phòng họp "Phòng Beta" (`roomId: R-02`) có sức chứa tối đa là 5 người  
**When** tôi gửi yêu cầu đặt phòng "R-02" với số lượng tham gia là 8 người  
**Then** hệ thống từ chối yêu cầu  
**And** hệ thống trả về mã lỗi `CAPACITY_EXCEEDED`

### Scenario: Đặt phòng thất bại do trùng lịch (Overlapping Invariant)
**Given** phòng họp "Phòng Alpha" (`roomId: R-01`) đã có một lịch đặt (`CONFIRMED`) từ 09:00Z đến 10:00Z  
**When** tôi gửi yêu cầu đặt phòng "R-01" từ 09:30Z đến 10:30Z (hoặc bất kỳ khoảng nào giao thoa)  
**Then** hệ thống từ chối yêu cầu để bảo vệ Invariant  
**And** hệ thống trả về mã lỗi `OVERLAPPING_BOOKING`

### Scenario: Đặt phòng thất bại do lỗi thời gian trong quá khứ (Time Validity Invariant)
**Given** thời gian thực tại hệ thống đang là 08:00Z  
**When** tôi gửi yêu cầu đặt phòng cho khung giờ từ 07:00Z đến 07:30Z  
**Then** hệ thống từ chối yêu cầu  
**And** hệ thống trả về mã lỗi `PAST_TIME`

### Scenario: Đặt phòng thất bại do thời lượng không hợp lệ (Minimum Duration)
**Given** thời gian hiện tại là 08:00Z  
**When** tôi gửi yêu cầu đặt phòng với khung giờ từ 09:00Z đến 09:05Z (dưới mức tối thiểu 15 phút)  
**Then** hệ thống từ chối yêu cầu  
**And** hệ thống trả về mã lỗi `INVALID_DURATION`
