package com.homechef.homechefsystem.dto;

import com.homechef.homechefsystem.common.enums.TimeSlotEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefRecommendQueryDTO {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "addressId 不能为空")
    private Long addressId;

    @NotNull(message = "ingredientMode 不能为空")
    private Integer ingredientMode;

    @NotNull(message = "serviceDate 不能为空")
    private LocalDate serviceDate;

    @NotNull(message = "timeSlot 不能为空")
    @Pattern(regexp = TimeSlotEnum.VALIDATION_REGEXP, message = TimeSlotEnum.INVALID_MESSAGE)
    private String timeSlot;

    @Pattern(regexp = "DISTANCE|RATING|ORDER_COUNT|GOOD_REVIEW_RATE|DEFAULT", message = "sortType 取值非法，只能为 DISTANCE、RATING、ORDER_COUNT、GOOD_REVIEW_RATE、DEFAULT")
    private String sortType;
}
