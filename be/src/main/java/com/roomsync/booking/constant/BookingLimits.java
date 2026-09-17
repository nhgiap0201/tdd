package com.roomsync.booking.constant;

public final class BookingLimits {
    private BookingLimits() {}

    public static final int MIN_ATTENDEES = 1;
    public static final int MAX_ATTENDEES = 100;
    public static final long MIN_DURATION_MINUTES = 15;
    public static final long MAX_DURATION_HOURS = 4;
    public static final int MAX_TITLE_LENGTH = 150;
}
