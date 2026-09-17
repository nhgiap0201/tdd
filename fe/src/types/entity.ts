import { BookingStatus } from './enums';

export interface RoomEntity {
  id: string;
  name: string;
  capacity: number;
  isActive: boolean;
  active?: boolean;
}

export interface BookingEntity {
  id: string;
  roomId: string;
  bookedByUserId: string;
  title: string;
  startTime: string;
  endTime: string;
  attendees: number;
  status: BookingStatus;
  createdAt: string;
  updatedAt?: string;
}
