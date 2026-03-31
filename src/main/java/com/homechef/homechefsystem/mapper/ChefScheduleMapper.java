package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.dto.ChefScheduleQueryDTO;
import com.homechef.homechefsystem.entity.ChefSchedule;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.jdbc.SQL;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChefScheduleMapper {

    @SelectProvider(type = ChefScheduleSqlProvider.class, method = "buildSelectListSql")
    @Results(id = "chefScheduleResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "chefId", column = "chef_id"),
            @Result(property = "serviceDate", column = "service_date"),
            @Result(property = "timeSlot", column = "time_slot"),
            @Result(property = "startTime", column = "start_time"),
            @Result(property = "endTime", column = "end_time"),
            @Result(property = "isAvailable", column = "is_available"),
            @Result(property = "lockedOrderId", column = "locked_order_id"),
            @Result(property = "remark", column = "remark"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<ChefSchedule> selectList(ChefScheduleQueryDTO queryDTO);

    @Select("""
            SELECT id, chef_id, service_date, time_slot, start_time, end_time,
                   is_available, locked_order_id, remark, created_at, updated_at
            FROM chef_schedule
            WHERE id = #{id}
            """)
    @ResultMap("chefScheduleResultMap")
    ChefSchedule selectById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM chef_schedule
            WHERE chef_id = #{chefId}
              AND service_date = #{serviceDate}
              AND time_slot = #{timeSlot}
            """)
    int countDuplicate(@Param("chefId") Long chefId,
                       @Param("serviceDate") LocalDate serviceDate,
                       @Param("timeSlot") String timeSlot);

    @Select("""
            SELECT COUNT(1)
            FROM chef_schedule
            WHERE chef_id = #{chefId}
              AND service_date = #{serviceDate}
              AND time_slot = #{timeSlot}
              AND id <> #{id}
            """)
    int countDuplicateExcludeId(@Param("chefId") Long chefId,
                                @Param("serviceDate") LocalDate serviceDate,
                                @Param("timeSlot") String timeSlot,
                                @Param("id") Long id);

    @Select("""
            SELECT DISTINCT chef_id
            FROM chef_schedule
            WHERE service_date = #{serviceDate}
              AND time_slot = #{timeSlot}
              AND is_available = 1
            """)
    List<Long> selectAvailableChefIdsByDateAndTimeSlot(@Param("serviceDate") LocalDate serviceDate,
                                                       @Param("timeSlot") String timeSlot);

    @Insert("""
            INSERT INTO chef_schedule (
                chef_id, service_date, time_slot, start_time, end_time,
                is_available, locked_order_id, remark, created_at, updated_at
            ) VALUES (
                #{chefId}, #{serviceDate}, #{timeSlot}, #{startTime}, #{endTime},
                #{isAvailable}, #{lockedOrderId}, #{remark}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChefSchedule chefSchedule);

    @Update("""
            UPDATE chef_schedule
            SET service_date = #{serviceDate},
                time_slot = #{timeSlot},
                start_time = #{startTime},
                end_time = #{endTime},
                is_available = #{isAvailable},
                remark = #{remark},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateById(ChefSchedule chefSchedule);

    @Update("""
            UPDATE chef_schedule
            SET is_available = #{isAvailable},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateAvailabilityById(@Param("id") Long id,
                               @Param("isAvailable") Integer isAvailable,
                               @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE chef_schedule
            SET is_available = 0,
                updated_at = #{updatedAt}
            WHERE service_date < #{currentDate}
              AND is_available = 1
            """)
    int disableExpiredAvailableSchedules(@Param("currentDate") LocalDate currentDate,
                                         @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE chef_schedule
            SET is_available = 0,
                updated_at = #{updatedAt}
            WHERE chef_id = #{chefId}
              AND end_time < #{currentTime}
              AND is_available = 1
            """)
    int disableExpiredAvailableSchedulesByChefId(@Param("chefId") Long chefId,
                                                 @Param("currentTime") LocalDateTime currentTime,
                                                 @Param("updatedAt") LocalDateTime updatedAt);

    @Delete("""
            DELETE FROM chef_schedule
            WHERE id = #{id}
            """)
    int deleteById(@Param("id") Long id);

    class ChefScheduleSqlProvider {

        public String buildSelectListSql(final ChefScheduleQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, chef_id, service_date, time_slot, start_time, end_time")
                    .SELECT("is_available, locked_order_id, remark, created_at, updated_at")
                    .FROM("chef_schedule");

            if (queryDTO != null) {
                if (queryDTO.getChefId() != null) {
                    sql.WHERE("chef_id = #{chefId}");
                }
                if (queryDTO.getStartDate() != null) {
                    sql.WHERE("service_date >= #{startDate}");
                }
                if (queryDTO.getEndDate() != null) {
                    sql.WHERE("service_date <= #{endDate}");
                }
            }

            return sql.ORDER_BY("service_date ASC, time_slot ASC").toString();
        }
    }
}
