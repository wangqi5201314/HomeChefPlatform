package com.homechef.homechefsystem.entity;

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
public class ChefSchedule {

    private Long id;

    private Long chefId;

    private LocalDate serviceDate;

    private String timeSlot;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer isAvailable;

    private Long lockedOrderId;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
