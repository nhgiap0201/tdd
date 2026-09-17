package com.roomsync.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin phòng họp")
public class RoomResponse {
    @Schema(description = "Mã định danh phòng họp", example = "ROOM_01")
    private String id;

    @Schema(description = "Tên phòng họp", example = "Tokyo (Room A)")
    private String name;

    @Schema(description = "Sức chứa tối đa (người)", example = "10")
    private int capacity;

    @JsonProperty("isActive")
    @Schema(description = "Trạng thái phòng (true: hoạt động, false: bảo trì)", example = "true")
    private boolean isActive;
}
