import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import BookingForm from './BookingForm'; // Sẽ báo lỗi đỏ do chưa có Component
import axios from 'axios';
import { BookingErrorCode } from '../../types/booking'; // Import type hợp đồng từ Bước 2

// Giả lập thư viện axios
vi.mock('axios');
const mockedAxios = vi.mocked(axios, true);

describe('BookingForm Component (TDD - Red Phase)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('TC-FE-02 & TC-FE-03: Validate form trống và giờ quá khứ -> Báo lỗi đỏ, KHÔNG gọi API', async () => {
    // Given
    render(<BookingForm />);
    const user = userEvent.setup();
    const submitBtn = screen.getByRole('button', { name: /đặt phòng/i });

    // When: Nhấn submit mà không điền thông tin (trống form)
    await user.click(submitBtn);

    // Then: API tuyệt đối không được gọi
    expect(mockedAxios.post).not.toHaveBeenCalled();

    // Và màn hình phải hiển thị thông báo lỗi màu đỏ tương ứng
    expect(await screen.findByText(/vui lòng chọn phòng/i)).toBeInTheDocument();
    expect(await screen.findByText(/thời gian không hợp lệ/i)).toBeInTheDocument();
    expect(await screen.findByText(/số người tham gia phải lớn hơn 0/i)).toBeInTheDocument();
  });

  it('TC-FE-04: Đang gọi API -> Nút Submit bị disabled & hiển thị Loading (Chống Double-click)', async () => {
    // Given: Giả lập API gọi mất 1 giây (1000ms) để quan sát UI
    mockedAxios.post.mockImplementationOnce(() => 
      new Promise(resolve => setTimeout(() => resolve({ status: 201 }), 1000))
    );
    render(<BookingForm />);
    const user = userEvent.setup();
    
    // Giả lập nhập toàn bộ form hợp lệ (bằng code hoặc placeholder testing)
    await user.selectOptions(screen.getByLabelText(/chọn phòng/i), 'R-01');
    await user.type(screen.getByLabelText(/số người/i), '5');
    await user.type(screen.getByLabelText(/tiêu đề cuộc họp/i), 'Họp Sync Team');
    // ... input các trường date time hợp lệ
    
    const submitBtn = screen.getByRole('button', { name: /đặt phòng/i });

    // When: Người dùng click đúp (hoặc bấm submit)
    await user.click(submitBtn);
    await user.click(submitBtn); // Cố tình double-click

    // Then: Ngay lập tức gọi API đúng 1 lần duy nhất (Nhờ nút đã bị block)
    expect(mockedAxios.post).toHaveBeenCalledTimes(1);
    
    // Nút phải chuyển sang trạng thái Loading và bị vô hiệu hóa
    expect(submitBtn).toBeDisabled();
    expect(submitBtn).toHaveTextContent(/đang xử lý/i); // Hoặc hiện loading spinner

    // Chờ qua 1s khi gọi API xong, form được reset hoặc nút hết khóa
    await waitFor(() => {
      expect(submitBtn).not.toBeDisabled();
    });
  });

  it('TC-FE-06: Báo lỗi Alert "Phòng đã có người đặt" khi API trả về 409 OVERLAPPING_BOOKING', async () => {
    // Given: Backend từ chối lịch do trùng giờ
    mockedAxios.post.mockRejectedValueOnce({
      response: {
        status: 409,
        data: {
          errorCode: BookingErrorCode.OVERLAPPING_BOOKING,
          message: 'Conflict time range',
          timestamp: new Date().toISOString()
        }
      }
    });

    render(<BookingForm />);
    const user = userEvent.setup();
    
    // Điền form
    await user.selectOptions(screen.getByLabelText(/chọn phòng/i), 'R-01');
    await user.type(screen.getByLabelText(/số người/i), '5');
    await user.type(screen.getByLabelText(/tiêu đề cuộc họp/i), 'Phỏng vấn');
    
    const submitBtn = screen.getByRole('button', { name: /đặt phòng/i });

    // When: Gửi yêu cầu đặt phòng
    await user.click(submitBtn);

    // Then: Trình duyệt phải móc đúng mã lỗi ra và hiện text đỏ thân thiện cho End-user
    expect(await screen.findByText(/phòng đã có người đặt trong khung giờ này!/i)).toBeInTheDocument();
  });
});
