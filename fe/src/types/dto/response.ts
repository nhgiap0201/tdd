import { BookingStatus, BookingErrorCode } from '../enums';

export interface ApiResponse<T = unknown> {
  status?: number;
  code?: number;
  error?: string;
  errorCode?: BookingErrorCode | string;
  message?: string;
  data?: T;
  path?: string;
  timestamp?: string;
}

export interface RoomResponse {
  id: string;
  name: string;
  capacity: number;
  isActive: boolean;
  active?: boolean;
}

export interface BookingResponse {
  id: string;
  roomId: string;
  bookedByUserId?: string;
  title: string;
  startTime: string;
  endTime: string;
  attendees: number;
  status: BookingStatus;
  createdAt: string;
  updatedAt?: string;
}

export interface ApiErrorResponse {
  status?: number;
  error?: string;
  errorCode?: BookingErrorCode | string;
  message?: string;
  path?: string;
  timestamp?: string;
}
