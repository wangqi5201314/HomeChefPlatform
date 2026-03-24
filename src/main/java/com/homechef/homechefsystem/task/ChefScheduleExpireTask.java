package com.homechef.homechefsystem.task;

import com.homechef.homechefsystem.service.ChefScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChefScheduleExpireTask {

    private final ChefScheduleService chefScheduleService;

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Shanghai")
    public void disableExpiredSchedules() {
        int updatedRows = chefScheduleService.disableExpiredAvailableSchedules();
        log.info("disable expired chef schedules finished, updatedRows={}", updatedRows);
    }
}
