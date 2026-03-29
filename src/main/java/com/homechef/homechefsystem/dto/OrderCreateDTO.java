package com.homechef.homechefsystem.dto;

import com.homechef.homechefsystem.common.enums.TimeSlotEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDTO {

    private Long userId;

    private Long chefId;

    private Long addressId;

    private LocalDate serviceDate;

    @NotBlank(message = TimeSlotEnum.INVALID_MESSAGE)
    @Pattern(regexp = TimeSlotEnum.VALIDATION_REGEXP, message = TimeSlotEnum.INVALID_MESSAGE)
    private String timeSlot;

    private LocalDateTime serviceStartTime;

    private LocalDateTime serviceEndTime;

    private Integer peopleCount;

    private String tastePreference;

    private String tabooFood;

    private String specialRequirement;

    private Integer ingredientMode;

    private String ingredientList;

    private String contactName;

    private String contactPhone;

    private String fullAddress;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;
}
