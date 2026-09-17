import React, { useState, useEffect, useMemo } from 'react';
import { RoomEntity, BookingEntity, CreateBookingRequest } from './types';

interface Toast {
  id: string;
  message: string;
  type: 'success' | 'error';
}

const DEFAULT_ROOMS: RoomEntity[] = [
  { id: 'ROOM_01', name: 'Phòng Tokyo (Room A - Lầu 2)', capacity: 10, isActive: true },
  { id: 'ROOM_02', name: 'Phòng Seoul (Room B - Lầu 2)', capacity: 4, isActive: true },
  { id: 'ROOM_03', name: 'Phòng Singapore (Room C - Lầu 3)', capacity: 6, isActive: true },
  { id: 'ROOM_04', name: 'Phòng London (Room D - Lầu 3)', capacity: 8, isActive: true },
  { id: 'ROOM_05', name: 'Phòng Maintenance (Bảo trì)', capacity: 8, isActive: false },
];

export default function App() {
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [rooms, setRooms] = useState<RoomEntity[]>(DEFAULT_ROOMS);
  const [selectedRoomId, setSelectedRoomId] = useState<string>('ROOM_01');
  const [bookings, setBookings] = useState<BookingEntity[]>([]);
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [presetOpen, setPresetOpen] = useState(false);

  // Set theme attribute on root element
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  // Form inputs
  const todayStr = useMemo(() => new Date().toISOString().split('T')[0], []);
  const [title, setTitle] = useState('');
  const [date, setDate] = useState(todayStr);
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('10:00');
  const [attendees, setAttendees] = useState<number>(5);

  // Clock
  const [clockStr, setClockStr] = useState('');

  useEffect(() => {
    const update = () => {
      const now = new Date();
      setClockStr(now.toISOString().substring(11, 19) + 'Z');
    };
    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, []);

  // Check if room is active (handles both isActive and active boolean property)
  const isRoomActive = (r?: RoomEntity): boolean => {
    if (!r) return true;
    if (r.isActive !== undefined) return Boolean(r.isActive);
    if (r.active !== undefined) return Boolean(r.active);
    return true;
  };

  // Fetch Rooms & Bookings
  const loadRooms = async () => {
    try {
      const res = await fetch('/api/v1/rooms');
      if (res.ok) {
        const data: any[] = await res.json();
        const mapped: RoomEntity[] = data.map((r) => {
          const activeVal =
            r.isActive !== undefined
              ? Boolean(r.isActive)
              : r.active !== undefined
              ? Boolean(r.active)
              : true;
          return {
            ...r,
            isActive: activeVal,
            active: activeVal,
          };
        });
        setRooms(mapped);
      }
    } catch {
      console.warn('Backend Spring Boot chưa sẵn sàng, dùng danh sách phòng mặc định.');
    }
  };

  const loadBookings = async () => {
    try {
      const res = await fetch('/api/v1/bookings');
      if (res.ok) {
        const data = await res.json();
        setBookings(data);
      }
    } catch (err) {
      console.error('Lỗi tải danh sách bookings:', err);
    }
  };

  useEffect(() => {
    loadRooms();
    loadBookings();
  }, []);

  const addToast = (message: string, type: 'success' | 'error') => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  };

  // Selected Room & Active status
  const currentRoom = useMemo(() => {
    return rooms.find((r) => r.id === selectedRoomId) || rooms[0];
  }, [rooms, selectedRoomId]);

  const currentRoomActive = useMemo(() => isRoomActive(currentRoom), [currentRoom]);

  // Validation calculations
  const validation = useMemo(() => {
    const startMs = new Date(`${date}T${startTime}:00`).getTime();
    const endMs = new Date(`${date}T${endTime}:00`).getTime();
    const durationMinutes = (endMs - startMs) / (1000 * 60);

    const isTimeOrderValid = endMs > startMs;
    // Invariant 2: MAX_2_HOURS (15 to 120 minutes)
    const isDurationValid = durationMinutes >= 15 && durationMinutes <= 120;

    // Invariant 3: BUSINESS_HOURS_ONLY (08:00 - 18:00)
    const [startH, startM] = startTime.split(':').map(Number);
    const [endH, endM] = endTime.split(':').map(Number);
    const startMin = (startH || 0) * 60 + (startM || 0);
    const endMin = (endH || 0) * 60 + (endM || 0);
    const isBusinessHours = startMin >= 8 * 60 && endMin <= 18 * 60;

    // Invariant 3: WEEKEND_NOT_ALLOWED (Monday = 1 to Friday = 5)
    const [y, m, d] = date.split('-').map(Number);
    const selectedDate = new Date(y, m - 1, d);
    const dayOfWeek = selectedDate.getDay();
    const isWeekday = dayOfWeek >= 1 && dayOfWeek <= 5;

    // Capacity & active status
    const isCapacityValid =
      attendees >= 1 &&
      attendees <= (currentRoom ? currentRoom.capacity : 10) &&
      currentRoomActive;

    const isFutureTime = startMs > Date.now();

    const isValid =
      isTimeOrderValid &&
      isDurationValid &&
      isBusinessHours &&
      isWeekday &&
      isCapacityValid;

    return {
      isTimeOrderValid,
      isDurationValid,
      isBusinessHours,
      isWeekday,
      isCapacityValid,
      isFutureTime,
      durationMinutes,
      isValid,
    };
  }, [date, startTime, endTime, attendees, currentRoom, currentRoomActive]);

  const formatLocalDate = (d: Date) => {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  // Date helpers for presets
  const getUpcomingWeekday = (daysAhead = 0) => {
    const d = new Date();
    d.setDate(d.getDate() + daysAhead);
    if (d.getDay() === 0) d.setDate(d.getDate() + 1); // Chủ Nhật -> Thứ Hai
    if (d.getDay() === 6) d.setDate(d.getDate() + 2); // Thứ Bảy -> Thứ Hai
    return formatLocalDate(d);
  };

  const getUpcomingWeekend = () => {
    const d = new Date();
    const day = d.getDay();
    const daysUntilSat = (6 - day + 7) % 7 || 7;
    d.setDate(d.getDate() + daysUntilSat);
    return formatLocalDate(d);
  };

  // Friendly error message mapper
  const getFriendlyErrorMessage = (code?: string, defaultMsg?: string) => {
    switch (code) {
      case 'OVERLAPPING_BOOKING':
        return 'Phòng đã có người đặt trong khung giờ này!';
      case 'DURATION_INVALID':
        return 'Thời lượng cuộc họp không hợp lệ (phải từ 15 đến 120 phút)!';
      case 'OUTSIDE_BUSINESS_HOURS':
        return 'Chỉ được đặt phòng trong khung giờ hành chính (08:00 - 18:00)!';
      case 'WEEKEND_NOT_ALLOWED':
        return 'Không thể đặt phòng vào Thứ Bảy hoặc Chủ Nhật!';
      case 'PAST_TIME':
      case 'PAST_TIME_INVALID':
        return 'Thời gian bắt đầu cuộc họp phải ở thời điểm tương lai!';
      case 'CAPACITY_EXCEEDED':
      case 'EXCEEDS_CAPACITY':
        return 'Số lượng người tham gia vượt quá sức chứa tối đa của phòng!';
      case 'ROOM_NOT_FOUND':
        return 'Phòng họp không tồn tại trong hệ thống!';
      case 'ROOM_INACTIVE':
        return 'Phòng họp đang ở trạng thái bảo trì, không thể đặt lịch!';
      default:
        return defaultMsg || 'Có lỗi xảy ra khi đặt phòng!';
    }
  };

  // Quick Preset Handlers
  const applyPreset = (preset: string) => {
    setPresetOpen(false);

    switch (preset) {
      case 'TC01':
        setSelectedRoomId('ROOM_01');
        setTitle('Sprint Planning Team A');
        setDate(getUpcomingWeekday(0));
        setStartTime('10:00');
        setEndTime('11:00');
        setAttendees(8);
        addToast('Đã nạp TC-01: Happy Path hợp lệ [10:00 - 11:00]', 'success');
        break;

      case 'TC02':
        setSelectedRoomId('ROOM_01');
        setTitle('Early Sync [Touching Head]');
        setDate(getUpcomingWeekday(0));
        setStartTime('08:00');
        setEndTime('09:00');
        setAttendees(6);
        addToast('Đã nạp TC-02: Chạm ranh giới đầu [08:00 - 09:00]', 'success');
        break;

      case 'TC05':
        setSelectedRoomId('ROOM_01');
        setTitle('Past Time Attempt');
        setDate('2020-01-01');
        setStartTime('09:00');
        setEndTime('10:00');
        setAttendees(4);
        addToast('Đã nạp TC-05: Đặt lịch trong quá khứ (2020)', 'error');
        break;

      case 'TC07':
        setSelectedRoomId('ROOM_01');
        setTitle('Micro 5-min Meeting');
        setDate(getUpcomingWeekday(0));
        setStartTime('14:00');
        setEndTime('14:05');
        setAttendees(3);
        addToast('Đã nạp TC-07: Lỗi thời lượng 5 phút (< 15p)', 'error');
        break;

      case 'TC08':
        setSelectedRoomId('ROOM_01');
        setTitle('Over 2 Hours Meeting');
        setDate(getUpcomingWeekday(0));
        setStartTime('10:00');
        setEndTime('13:00');
        setAttendees(5);
        addToast('Đã nạp TC-08: Lỗi thời lượng 3 tiếng (> 120p)', 'error');
        break;

      case 'TC09':
        setSelectedRoomId('ROOM_01');
        setTitle('Night Meeting (Outside Hours)');
        setDate(getUpcomingWeekday(0));
        setStartTime('18:30');
        setEndTime('19:30');
        setAttendees(5);
        addToast('Đã nạp TC-09: Lỗi ngoài giờ hành chính (sau 18:00)', 'error');
        break;

      case 'TC10':
        setSelectedRoomId('ROOM_01');
        setTitle('Weekend Meeting');
        setDate(getUpcomingWeekend());
        setStartTime('10:00');
        setEndTime('11:00');
        setAttendees(5);
        addToast('Đã nạp TC-10: Lỗi đặt vào Thứ Bảy / Chủ Nhật', 'error');
        break;

      case 'TC11':
        setSelectedRoomId('ROOM_02'); // max 4
        setTitle('Over Capacity Crowd');
        setDate(getUpcomingWeekday(0));
        setStartTime('14:00');
        setEndTime('15:00');
        setAttendees(12);
        addToast('Đã nạp TC-11: Vượt sức chứa (12/4)', 'error');
        break;

      case 'TC13':
        setSelectedRoomId('ROOM_01');
        setTitle('Overlap Conflict Attempt');
        setDate(getUpcomingWeekday(0));
        setStartTime('10:00');
        setEndTime('11:00');
        setAttendees(5);
        addToast('Đã nạp TC-13: Thử đặt trùng khung giờ có sẵn', 'error');
        break;
    }
  };

  // Submit Handler
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validation.isValid) return;

    setIsSubmitting(true);
    const idempotencyKey = 'IDEM-' + Date.now() + '-' + Math.random().toString(36).substring(2, 7);

    const payload: CreateBookingRequest = {
      roomId: selectedRoomId,
      title: title.trim(),
      startTime: new Date(`${date}T${startTime}:00`).toISOString(),
      endTime: new Date(`${date}T${endTime}:00`).toISOString(),
      attendees,
    };

    try {
      const res = await fetch('/api/v1/bookings', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Idempotency-Key': idempotencyKey,
        },
        body: JSON.stringify(payload),
      });

      const data = await res.json();

      if (res.status === 201) {
        addToast(`Đặt phòng thành công! Mã: ${data.id}`, 'success');
        setBookings((prev) => [...prev, data]);
        setTitle('');
      } else {
        const friendlyMsg = getFriendlyErrorMessage(data.errorCode || data.error, data.message);
        addToast(`Lỗi [${data.errorCode || data.error || '400'}]: ${friendlyMsg}`, 'error');
      }
    } catch {
      addToast('Lỗi kết nối tới Backend Spring Boot API.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const roomBookings = useMemo(() => {
    return bookings.filter((b) => b.roomId === selectedRoomId);
  }, [bookings, selectedRoomId]);

  return (
    <div className="app-container">
      {/* HEADER */}
      <header className="app-header">
        <div className="brand-area">
          <div className="logo-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
              <line x1="16" y1="2" x2="16" y2="6"></line>
              <line x1="8" y1="2" x2="8" y2="6"></line>
              <line x1="3" y1="10" x2="21" y2="10"></line>
            </svg>
          </div>
          <div>
            <h1 className="brand-title">
              RoomSync <span className="brand-badge">React + Spring Boot</span>
            </h1>
            <p className="brand-subtitle">
              Đặt lịch phòng họp nội bộ • Java Spring Boot 4 Backend & React TypeScript Frontend
            </p>
          </div>
        </div>

        <div className="header-status">
          <button
            type="button"
            className="theme-toggle-btn"
            onClick={() => setTheme((t) => (t === 'light' ? 'dark' : 'light'))}
            title="Chuyển đổi giao diện Sáng / Tối"
          >
            {theme === 'light' ? '🌙 Chế độ Tối' : '☀️ Chế độ Sáng'}
          </button>
          <div className="status-chip chip-success">
            <span className="status-dot"></span>
            <span>Collision Rate: 0%</span>
          </div>
          <div className="status-chip chip-info">
            <span className="mono-text">UTC: {clockStr}</span>
          </div>
        </div>
      </header>

      {/* MAIN CONTENT */}
      <main className="main-layout">
        {/* LEFT COLUMN: FORM */}
        <section className="booking-section">
          {/* ROOM CARDS */}
          <div className="card-box">
            <div className="box-header">
              <h2 className="box-title">1. Chọn phòng họp</h2>
              <span className="box-note">{rooms.length} phòng khả dụng</span>
            </div>
            <div className="room-grid">
              {rooms.map((room) => {
                const isSelected = room.id === selectedRoomId;
                const active = isRoomActive(room);
                return (
                  <div
                    key={room.id}
                    className={`room-card ${isSelected ? 'selected' : ''} ${!active ? 'maintenance-card' : ''}`}
                    onClick={() => setSelectedRoomId(room.id)}
                  >
                    <div className="room-name">
                      {room.name}{' '}
                      {!active && (
                        <span className="badge-maintenance">(Bảo trì)</span>
                      )}
                    </div>
                    <div className="room-capacity-badge">Sức chứa: {room.capacity} người</div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* FORM */}
          <div className="card-box">
            <div className="box-header">
              <h2 className="box-title">2. Thông tin cuộc họp</h2>
              <div className="preset-dropdown-container">
                <button
                  type="button"
                  className="btn-sm btn-ghost"
                  onClick={() => setPresetOpen(!presetOpen)}
                >
                  ⚡ Nạp Test Case Mẫu
                </button>
                {presetOpen && (
                  <div className="preset-menu">
                    <div className="preset-item" onClick={() => applyPreset('TC01')}>
                      TC-01: Happy Path [10:00 - 11:00] (Hợp lệ)
                    </div>
                    <div className="preset-item" onClick={() => applyPreset('TC02')}>
                      TC-02: Chạm ranh giới đầu [08:00 - 09:00]
                    </div>
                    <div className="preset-item" onClick={() => applyPreset('TC05')}>
                      TC-05: Lỗi đặt trong quá khứ (2020)
                    </div>
                    <div className="preset-item" onClick={() => applyPreset('TC07')}>
                      TC-07: Lỗi thời lượng &lt; 15 phút
                    </div>
                    <div className="preset-item" onClick={() => applyPreset('TC08')}>
                      TC-08: Lỗi thời lượng &gt; 2 giờ (180p)
                    </div>
                    <div className="preset-item" onClick={() => applyPreset('TC09')}>
                      TC-09: Lỗi ngoài giờ hành chính (sau 18:00)
                    </div>
                    <div className="preset-item" onClick={() => applyPreset('TC10')}>
                      TC-10: Lỗi đặt vào Thứ Bảy / Chủ Nhật
                    </div>
                    <div className="preset-item" onClick={() => applyPreset('TC11')}>
                      TC-11: Lỗi vượt sức chứa (12/4)
                    </div>
                    <div className="preset-item" onClick={() => applyPreset('TC13')}>
                      TC-13: Lỗi trùng lịch 100%
                    </div>
                  </div>
                )}
              </div>
            </div>

            <form className="form-layout" onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Tiêu đề cuộc họp</label>
                <input
                  type="text"
                  placeholder="VD: Sprint Planning, Tech Grooming..."
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  maxLength={150}
                  required
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Ngày họp</label>
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Số người tham dự</label>
                  <div className="number-input-wrapper">
                    <input
                      type="number"
                      min={1}
                      max={50}
                      value={attendees}
                      onChange={(e) => setAttendees(parseInt(e.target.value, 10) || 0)}
                      required
                    />
                    <span className="input-suffix">người</span>
                  </div>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Giờ bắt đầu</label>
                  <input
                    type="time"
                    step={900}
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Giờ kết thúc</label>
                  <input
                    type="time"
                    step={900}
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    required
                  />
                </div>
              </div>

              {/* LIVE INVARIANT PRE-CHECK */}
              <div className="validation-summary">
                <div className={`val-item ${validation.isTimeOrderValid ? 'valid' : 'invalid'}`}>
                  <span className="val-icon">{validation.isTimeOrderValid ? '✔' : '✖'}</span>{' '}
                  Thời gian kết thúc phải sau thời gian bắt đầu
                </div>
                <div className={`val-item ${validation.isDurationValid ? 'valid' : 'invalid'}`}>
                  <span className="val-icon">{validation.isDurationValid ? '✔' : '✖'}</span>{' '}
                  Thời lượng họp:{' '}
                  {validation.durationMinutes > 0 ? `${validation.durationMinutes} phút` : 'không hợp lệ'} (15 phút - 2 giờ)
                </div>
                <div className={`val-item ${validation.isBusinessHours ? 'valid' : 'invalid'}`}>
                  <span className="val-icon">{validation.isBusinessHours ? '✔' : '✖'}</span>{' '}
                  Khung giờ hành chính: 08:00 - 18:00
                </div>
                <div className={`val-item ${validation.isWeekday ? 'valid' : 'invalid'}`}>
                  <span className="val-icon">{validation.isWeekday ? '✔' : '✖'}</span>{' '}
                  Ngày làm việc: Thứ Hai - Thứ Sáu (không nhận Thứ Bảy / Chủ Nhật)
                </div>
                <div className={`val-item ${validation.isCapacityValid ? 'valid' : 'invalid'}`}>
                  <span className="val-icon">{validation.isCapacityValid ? '✔' : '✖'}</span>{' '}
                  Số người: {attendees}/{currentRoom ? currentRoom.capacity : 10} người{' '}
                  {!currentRoomActive ? '(Phòng đang bảo trì)' : ''}
                </div>
              </div>

              <button
                type="submit"
                className="btn-primary btn-block"
                disabled={!validation.isValid || isSubmitting}
              >
                <span className="btn-text">
                  {isSubmitting ? 'Đang xử lý đặt phòng...' : 'Xác nhận đặt phòng'}
                </span>
              </button>
            </form>
          </div>
        </section>

        {/* RIGHT COLUMN: INVARIANTS & TIMELINE */}
        <section className="schedule-section">
          {/* INVARIANTS CARD */}
          <div className="card-box glass-card">
            <h2 className="box-title">🛡️ Quy tắc Bất biến Hệ thống (Invariants)</h2>
            <div className="invariants-list">
              <div className="invariant-card">
                <div className="inv-header">
                  <span className="inv-badge">INVARIANT 1 (NO_OVERLAP)</span>
                  <span className="inv-name">Phân tách Không - Thời gian</span>
                </div>
                <p className="inv-desc">
                  [B1.start, B1.end) ∩ [B2.start, B2.end) = ∅. Chặn 100% trùng lịch tại Backend.
                </p>
              </div>
              <div className="invariant-card">
                <div className="inv-header">
                  <span className="inv-badge">INVARIANT 2 (MAX_2_HOURS)</span>
                  <span className="inv-name">Giới hạn Thời lượng</span>
                </div>
                <p className="inv-desc">Now &lt; Start &lt; End, 15 phút ≤ Thời lượng ≤ 2 giờ (120 phút).</p>
              </div>
              <div className="invariant-card">
                <div className="inv-header">
                  <span className="inv-badge">INVARIANT 3 (BUSINESS_HOURS)</span>
                  <span className="inv-name">Khung giờ & Ngày làm việc</span>
                </div>
                <p className="inv-desc">Chỉ từ 08:00 - 18:00 các ngày Thứ Hai đến Thứ Sáu (chặn Thứ Bảy, Chủ Nhật).</p>
              </div>
              <div className="invariant-card">
                <div className="inv-header">
                  <span className="inv-badge">SỨC CHỨA VẬT LÝ</span>
                  <span className="inv-name">Bảo toàn Sức chứa Phòng</span>
                </div>
                <p className="inv-desc">1 ≤ Số người ≤ Sức chứa phòng đang ở trạng thái Hoạt động (Active).</p>
              </div>
            </div>
          </div>

          {/* SCHEDULE TIMELINE */}
          <div className="card-box">
            <div className="box-header">
              <h2 className="box-title">📅 Lịch phòng đã đặt hôm nay</h2>
              <button type="button" className="btn-sm btn-outline" onClick={loadBookings}>
                Làm mới
              </button>
            </div>
            <div className="bookings-timeline">
              {roomBookings.length === 0 ? (
                <div className="empty-state">Chưa có lịch đặt nào cho phòng này.</div>
              ) : (
                roomBookings.map((b) => {
                  const startDate = new Date(b.startTime);
                  const endDate = new Date(b.endTime);
                  const start = startDate.toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: false,
                  });
                  const end = endDate.toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: false,
                  });
                  const dateStr = startDate.toLocaleDateString([], {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric',
                  });
                  return (
                    <div key={b.id} className="booking-item">
                      <div>
                        <div className="booking-time">
                          {start} - {end} <span style={{ fontSize: '0.8rem', color: '#64748b', marginLeft: '6px' }}>({dateStr})</span>
                        </div>
                        <div className="booking-title">
                          {b.title} ({b.attendees} người)
                        </div>
                      </div>
                      <span className="booking-badge">{b.status}</span>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </section>
      </main>

      {/* TOASTS */}
      <div className="toast-container">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast-${toast.type}`}>
            <span>{toast.type === 'success' ? '✅' : '⚠️'}</span>
            <span>{toast.message}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
