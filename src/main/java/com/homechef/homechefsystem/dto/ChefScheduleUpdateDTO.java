package com.homechef.homechefsystem.dto;

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

    private String timeSlot;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer isAvailable;

    private String remark;
}
