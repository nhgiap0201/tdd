export interface CreateBookingRequest {
  roomId: string;
  title: string;
  startTime: string;
  endTime: string;
  attendees: number;
}

export interface BookingRequest {
  roomId: string;
  startTime: string;
  endTime: string;
  attendees: number;
  title: string;
  bookedBy?: string;
}
