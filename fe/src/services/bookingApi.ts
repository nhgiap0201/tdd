import { RoomResponse, BookingResponse, CreateBookingRequest } from '../types';

const API_BASE_URL = '/api/v1';

export const bookingApi = {
  async getRooms(): Promise<RoomResponse[]> {
    const res = await fetch(`${API_BASE_URL}/rooms`);
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw { response: { status: res.status, data: errorData } };
    }
    return res.json();
  },

  async getBookings(): Promise<BookingResponse[]> {
    const res = await fetch(`${API_BASE_URL}/bookings`);
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw { response: { status: res.status, data: errorData } };
    }
    return res.json();
  },

  async createBooking(request: CreateBookingRequest, idempotencyKey?: string): Promise<BookingResponse> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    if (idempotencyKey) {
      headers['X-Idempotency-Key'] = idempotencyKey;
    }

    const res = await fetch(`${API_BASE_URL}/bookings`, {
      method: 'POST',
      headers,
      body: JSON.stringify(request),
    });

    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw { response: { status: res.status, data } };
    }
    return data;
  },

  async checkHealth(): Promise<{ status: string; service: string }> {
    const res = await fetch('/api/health');
    return res.json();
  },
};
