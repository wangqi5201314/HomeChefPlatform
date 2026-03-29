-- 1) 保持字段类型为 VARCHAR(20)，更新注释
ALTER TABLE chef_schedule
    MODIFY COLUMN time_slot VARCHAR(20) NOT NULL COMMENT '时段：BREAKFAST/LUNCH/DINNER/LATE_NIGHT';

ALTER TABLE orders
    MODIFY COLUMN time_slot VARCHAR(20) NOT NULL COMMENT '时段：BREAKFAST/LUNCH/DINNER/LATE_NIGHT';

-- 2) 历史中文数据迁移
UPDATE chef_schedule
SET time_slot = CASE time_slot
    WHEN '早餐' THEN 'BREAKFAST'
    WHEN '午餐' THEN 'LUNCH'
    WHEN '晚餐' THEN 'DINNER'
    WHEN '夜宵' THEN 'LATE_NIGHT'
    ELSE time_slot
END
WHERE time_slot IN ('早餐', '午餐', '晚餐', '夜宵');

UPDATE orders
SET time_slot = CASE time_slot
    WHEN '早餐' THEN 'BREAKFAST'
    WHEN '午餐' THEN 'LUNCH'
    WHEN '晚餐' THEN 'DINNER'
    WHEN '夜宵' THEN 'LATE_NIGHT'
    ELSE time_slot
END
WHERE time_slot IN ('早餐', '午餐', '晚餐', '夜宵');

-- 3) 可选：为 time_slot 增加 CHECK 约束（MySQL 8.0.16+ 可考虑）
-- ALTER TABLE chef_schedule
--     ADD CONSTRAINT chk_chef_schedule_time_slot
--     CHECK (time_slot IN ('BREAKFAST', 'LUNCH', 'DINNER', 'LATE_NIGHT'));
--
-- ALTER TABLE orders
--     ADD CONSTRAINT chk_orders_time_slot
--     CHECK (time_slot IN ('BREAKFAST', 'LUNCH', 'DINNER', 'LATE_NIGHT'));
