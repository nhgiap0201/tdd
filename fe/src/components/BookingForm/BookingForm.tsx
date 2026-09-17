import React, { useState } from 'react';
import axios from 'axios';
import { BookingErrorCode, BookingRequest } from '../../types/booking';

interface FormErrors {
  roomId?: string;
  time?: string;
  attendees?: string;
  title?: string;
}

export default function BookingForm() {
  const [roomId, setRoomId] = useState<string>('');
  const [attendees, setAttendees] = useState<number | string>('');
  const [title, setTitle] = useState<string>('');
  const [startTime, setStartTime] = useState<string>('');
  const [endTime, setEndTime] = useState<string>('');
  const [bookedBy, setBookedBy] = useState<string>('UserA');

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [errors, setErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handleRoomChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    setRoomId(val);

    // Tự động thiết lập khung giờ hợp lệ ở tương lai (ngày mai) nếu người dùng chưa chọn giờ
    if (val && !startTime) {
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      const yyyy = tomorrow.getFullYear();
      const mm = String(tomorrow.getMonth() + 1).padStart(2, '0');
      const dd = String(tomorrow.getDate()).padStart(2, '0');
      setStartTime(`${yyyy}-${mm}-${dd}T09:00`);
      setEndTime(`${yyyy}-${mm}-${dd}T10:00`);
    }
  };

  const validate = (): boolean => {
    const newErrors: FormErrors = {};
    let isValid = true;

    if (!roomId) {
      newErrors.roomId = 'Vui lòng chọn phòng';
      isValid = false;
    }

    if (
      !startTime ||
      !endTime ||
      new Date(startTime) <= new Date() ||
      new Date(endTime) <= new Date(startTime)
    ) {
      newErrors.time = 'Thời gian không hợp lệ';
      isValid = false;
    }

    const attendeesNum = Number(attendees);
    if (!attendees || isNaN(attendeesNum) || attendeesNum <= 0) {
      newErrors.attendees = 'Số người tham gia phải lớn hơn 0';
      isValid = false;
    }

    setErrors(newErrors);
    return isValid;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Chống submit trùng lặp khi đang tải (loading lock)
    if (isLoading) {
      return;
    }

    setServerError(null);
    setSuccessMessage(null);

    if (!validate()) {
      return;
    }

    setIsLoading(true);

    try {
      const payload: BookingRequest = {
        roomId,
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        attendees: Number(attendees),
        title: title || 'Cuộc họp nhóm',
        bookedBy: bookedBy || 'UserA',
      };

      await axios.post('/api/v1/bookings', payload);
      setSuccessMessage('Đặt phòng thành công!');
    } catch (err: any) {
      const errCode = err?.response?.data?.errorCode;
      if (errCode === BookingErrorCode.OVERLAPPING_BOOKING || err?.response?.status === 409) {
        setServerError('Phòng đã có người đặt trong khung giờ này!');
      } else if (errCode === BookingErrorCode.CAPACITY_EXCEEDED || errCode === BookingErrorCode.EXCEEDS_CAPACITY) {
        setServerError('Số lượng người tham gia vượt quá sức chứa phòng!');
      } else if (errCode === BookingErrorCode.ROOM_NOT_FOUND) {
        setServerError('Phòng họp không tồn tại!');
      } else if (errCode === BookingErrorCode.OUTSIDE_BUSINESS_HOURS) {
        setServerError('Chỉ được đặt phòng trong khung giờ hành chính (08:00 - 18:00)!');
      } else if (errCode === BookingErrorCode.WEEKEND_NOT_ALLOWED) {
        setServerError('Không thể đặt phòng vào Thứ Bảy hoặc Chủ Nhật!');
      } else if (errCode === BookingErrorCode.DURATION_INVALID) {
        setServerError('Thời lượng cuộc họp không hợp lệ (phải từ 15 đến 120 phút)!');
      } else if (errCode === BookingErrorCode.PAST_TIME || errCode === BookingErrorCode.PAST_TIME_INVALID) {
        setServerError('Thời gian bắt đầu cuộc họp phải ở thời điểm tương lai!');
      } else if (errCode === BookingErrorCode.ROOM_INACTIVE) {
        setServerError('Phòng họp đang ở trạng thái bảo trì, không thể đặt lịch!');
      } else {
        setServerError(err?.response?.data?.message || 'Có lỗi xảy ra khi đặt phòng!');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="booking-form-container" style={{ maxWidth: '540px', margin: '0 auto', padding: '24px' }}>
      <h2>Đặt Lịch Phòng Họp</h2>

      {serverError && (
        <div
          role="alert"
          style={{
            padding: '12px 16px',
            marginBottom: '16px',
            borderRadius: '8px',
            backgroundColor: '#fee2e2',
            color: '#b91c1c',
            fontWeight: 500,
          }}
        >
          {serverError}
        </div>
      )}

      {successMessage && (
        <div
          style={{
            padding: '12px 16px',
            marginBottom: '16px',
            borderRadius: '8px',
            backgroundColor: '#dcfce7',
            color: '#15803d',
            fontWeight: 500,
          }}
        >
          {successMessage}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        {/* Chọn phòng */}
        <div style={{ marginBottom: '16px' }}>
          <label htmlFor="roomId" style={{ display: 'block', marginBottom: '6px', fontWeight: 600 }}>
            Chọn phòng
          </label>
          <select
            id="roomId"
            value={roomId}
            onChange={handleRoomChange}
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          >
            <option value="">-- Chọn phòng họp --</option>
            <option value="R-01">Phòng Alpha (R-01) - Sức chứa 10</option>
            <option value="R-02">Phòng Beta (R-02) - Sức chứa 5</option>
            <option value="R-03">Phòng Hội thảo (R-03) - Sức chứa 30</option>
          </select>
          {errors.roomId && (
            <span style={{ color: '#dc2626', fontSize: '13px', display: 'block', marginTop: '4px' }}>
              {errors.roomId}
            </span>
          )}
        </div>

        {/* Số người tham gia */}
        <div style={{ marginBottom: '16px' }}>
          <label htmlFor="attendees" style={{ display: 'block', marginBottom: '6px', fontWeight: 600 }}>
            Số người
          </label>
          <input
            id="attendees"
            type="number"
            min="1"
            value={attendees}
            onChange={(e) => setAttendees(e.target.value)}
            placeholder="Nhập số người tham gia"
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          />
          {errors.attendees && (
            <span style={{ color: '#dc2626', fontSize: '13px', display: 'block', marginTop: '4px' }}>
              {errors.attendees}
            </span>
          )}
        </div>

        {/* Tiêu đề cuộc họp */}
        <div style={{ marginBottom: '16px' }}>
          <label htmlFor="title" style={{ display: 'block', marginBottom: '6px', fontWeight: 600 }}>
            Tiêu đề cuộc họp
          </label>
          <input
            id="title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Ví dụ: Họp Sprint Planning"
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          />
        </div>

        {/* Người đặt phòng */}
        <div style={{ marginBottom: '16px' }}>
          <label htmlFor="bookedBy" style={{ display: 'block', marginBottom: '6px', fontWeight: 600 }}>
            Người đặt phòng
          </label>
          <input
            id="bookedBy"
            type="text"
            value={bookedBy}
            onChange={(e) => setBookedBy(e.target.value)}
            placeholder="Tên người đặt"
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          />
        </div>

        {/* Thời gian bắt đầu */}
        <div style={{ marginBottom: '16px' }}>
          <label htmlFor="startTime" style={{ display: 'block', marginBottom: '6px', fontWeight: 600 }}>
            Thời gian bắt đầu
          </label>
          <input
            id="startTime"
            type="datetime-local"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          />
        </div>

        {/* Thời gian kết thúc */}
        <div style={{ marginBottom: '16px' }}>
          <label htmlFor="endTime" style={{ display: 'block', marginBottom: '6px', fontWeight: 600 }}>
            Thời gian kết thúc
          </label>
          <input
            id="endTime"
            type="datetime-local"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          />
          {errors.time && (
            <span style={{ color: '#dc2626', fontSize: '13px', display: 'block', marginTop: '4px' }}>
              {errors.time}
            </span>
          )}
        </div>

        {/* Nút submit */}
        <button
          type="submit"
          disabled={isLoading}
          style={{
            width: '100%',
            padding: '12px 20px',
            borderRadius: '6px',
            backgroundColor: isLoading ? '#9ca3af' : '#2563eb',
            color: '#fff',
            fontWeight: 600,
            fontSize: '16px',
            border: 'none',
            cursor: isLoading ? 'not-allowed' : 'pointer',
            transition: 'background-color 0.2s',
          }}
        >
          {isLoading ? 'Đang xử lý...' : 'Đặt phòng'}
        </button>
      </form>
    </div>
  );
}
