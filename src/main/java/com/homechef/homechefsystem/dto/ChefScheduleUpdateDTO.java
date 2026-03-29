package com.homechef.homechefsystem.dto;

import com.homechef.homechefsystem.common.enums.TimeSlotEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefScheduleUpdateDTO {

    private LocalDate serviceDate;

    @NotBlank(message = TimeSlotEnum.INVALID_MESSAGE)
    @Pattern(regexp = TimeSlotEnum.VALIDATION_REGEXP, message = TimeSlotEnum.INVALID_MESSAGE)
    private String timeSlot;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer isAvailable;

    private String remark;
}
