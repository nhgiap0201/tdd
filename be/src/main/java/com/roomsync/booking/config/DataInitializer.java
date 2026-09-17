package com.roomsync.booking.config;

import com.roomsync.booking.entity.Room;
import com.roomsync.booking.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(RoomRepository roomRepository) {
        return args -> {
            if (roomRepository.count() == 0) {
                log.info("Cơ sở dữ liệu phòng họp đang trống. Bắt đầu khởi tạo dữ liệu mẫu (Seed Rooms)...");
                List<Room> defaultRooms = List.of(
                    new Room("ROOM_01", "Tokyo (Room A)", 10, true),
                    new Room("ROOM_02", "Seoul (Room B)", 4, true),
                    new Room("ROOM_03", "Singapore (Room C)", 6, true),
                    new Room("ROOM_04", "London (Room D)", 8, true),
                    new Room("ROOM_05", "Maintenance (Room E)", 8, false)
                );
                defaultRooms.forEach(roomRepository::save);
                log.info("Khởi tạo thành công {} phòng họp mặc định vào cơ sở dữ liệu!", defaultRooms.size());
            } else {
                log.info("Cơ sở dữ liệu phòng họp đã có sẵn {} phòng.", roomRepository.count());
            }
        };
    }
}
