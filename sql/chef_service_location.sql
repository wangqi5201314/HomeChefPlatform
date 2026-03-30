-- Migration SQL for existing chef_service_location table
-- 1. Drop the old unique index on chef_id
ALTER TABLE chef_service_location
    DROP INDEX uk_chef_id;

-- 2. Add location_name
ALTER TABLE chef_service_location
    ADD COLUMN location_name VARCHAR(100) DEFAULT NULL COMMENT 'location name' AFTER chef_id;

-- 3. Add is_active
ALTER TABLE chef_service_location
    ADD COLUMN is_active TINYINT NOT NULL DEFAULT 0 COMMENT 'active flag: 0=inactive, 1=active' AFTER latitude;

-- 4. Add normal indexes
ALTER TABLE chef_service_location
    ADD INDEX idx_chef_id (chef_id),
    ADD INDEX idx_chef_active (chef_id, is_active);

-- Full create SQL if the table does not exist yet
CREATE TABLE `chef_service_location` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `chef_id` BIGINT NOT NULL COMMENT 'chef id',
  `location_name` VARCHAR(100) DEFAULT NULL COMMENT 'location name',
  `province` VARCHAR(50) DEFAULT NULL COMMENT 'province',
  `city` VARCHAR(50) DEFAULT NULL COMMENT 'city',
  `district` VARCHAR(50) DEFAULT NULL COMMENT 'district',
  `town` VARCHAR(50) DEFAULT NULL COMMENT 'town or street',
  `detail_address` VARCHAR(255) DEFAULT NULL COMMENT 'detail address',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT 'longitude',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT 'latitude',
  `is_active` TINYINT NOT NULL DEFAULT 0 COMMENT 'active flag: 0=inactive, 1=active',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`),
  KEY `idx_chef_id` (`chef_id`),
  KEY `idx_chef_active` (`chef_id`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='chef service locations';
