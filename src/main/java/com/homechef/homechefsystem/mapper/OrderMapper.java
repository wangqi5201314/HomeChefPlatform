package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.dto.OrderQueryDTO;
import com.homechef.homechefsystem.entity.Order;
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

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("""
            SELECT COUNT(1)
            FROM orders
            """)
    int countAll();

    @Select("""
            SELECT COUNT(1)
            FROM orders
            WHERE created_at >= #{startTime}
              AND created_at < #{endTime}
            """)
    int countCreatedBetween(@Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COUNT(1)
            FROM orders
            WHERE order_status = #{orderStatus}
            """)
    int countByOrderStatus(@Param("orderStatus") String orderStatus);

    @Insert("""
            INSERT INTO orders (
                order_no, user_id, chef_id, address_id, service_date, time_slot,
                service_start_time, service_end_time, people_count, taste_preference,
                taboo_food, special_requirement, ingredient_mode, ingredient_list,
                contact_name, contact_phone, full_address, longitude, latitude,
                confirm_code, total_amount, discount_amount, pay_amount, order_status,
                cancel_reason, refund_reason, user_deleted, chef_deleted, created_at, updated_at
            ) VALUES (
                #{orderNo}, #{userId}, #{chefId}, #{addressId}, #{serviceDate}, #{timeSlot},
                #{serviceStartTime}, #{serviceEndTime}, #{peopleCount}, #{tastePreference},
                #{tabooFood}, #{specialRequirement}, #{ingredientMode}, #{ingredientList},
                #{contactName}, #{contactPhone}, #{fullAddress}, #{longitude}, #{latitude},
                #{confirmCode}, #{totalAmount}, #{discountAmount}, #{payAmount}, #{orderStatus},
                #{cancelReason}, #{refundReason}, #{userDeleted}, #{chefDeleted}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Select("""
            SELECT id, order_no, user_id, chef_id, address_id, service_date, time_slot,
                   service_start_time, service_end_time, people_count, taste_preference,
                   taboo_food, special_requirement, ingredient_mode, ingredient_list,
                   contact_name, contact_phone, full_address, longitude, latitude,
                   confirm_code, total_amount, discount_amount, pay_amount, order_status,
                   cancel_reason, refund_reason, user_deleted, chef_deleted,
                   EXISTS(SELECT 1 FROM review r WHERE r.order_id = orders.id) AS reviewed,
                   created_at, updated_at
            FROM orders
            WHERE id = #{id}
            """)
    @Results(id = "orderResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "orderNo", column = "order_no"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "chefId", column = "chef_id"),
            @Result(property = "addressId", column = "address_id"),
            @Result(property = "serviceDate", column = "service_date"),
            @Result(property = "timeSlot", column = "time_slot"),
            @Result(property = "serviceStartTime", column = "service_start_time"),
            @Result(property = "serviceEndTime", column = "service_end_time"),
            @Result(property = "peopleCount", column = "people_count"),
            @Result(property = "tastePreference", column = "taste_preference"),
            @Result(property = "tabooFood", column = "taboo_food"),
            @Result(property = "specialRequirement", column = "special_requirement"),
            @Result(property = "ingredientMode", column = "ingredient_mode"),
            @Result(property = "ingredientList", column = "ingredient_list"),
            @Result(property = "contactName", column = "contact_name"),
            @Result(property = "contactPhone", column = "contact_phone"),
            @Result(property = "fullAddress", column = "full_address"),
            @Result(property = "longitude", column = "longitude"),
            @Result(property = "latitude", column = "latitude"),
            @Result(property = "confirmCode", column = "confirm_code"),
            @Result(property = "totalAmount", column = "total_amount"),
            @Result(property = "discountAmount", column = "discount_amount"),
            @Result(property = "payAmount", column = "pay_amount"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "cancelReason", column = "cancel_reason"),
            @Result(property = "refundReason", column = "refund_reason"),
            @Result(property = "userDeleted", column = "user_deleted"),
            @Result(property = "chefDeleted", column = "chef_deleted"),
            @Result(property = "reviewed", column = "reviewed"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Order selectById(@Param("id") Long id);

    @Select("""
            SELECT id, order_no, user_id, chef_id, address_id, service_date, time_slot,
                   service_start_time, service_end_time, people_count, taste_preference,
                   taboo_food, special_requirement, ingredient_mode, ingredient_list,
                   contact_name, contact_phone, full_address, longitude, latitude,
                   confirm_code, total_amount, discount_amount, pay_amount, order_status,
                   cancel_reason, refund_reason, user_deleted, chef_deleted,
                   EXISTS(SELECT 1 FROM review r WHERE r.order_id = orders.id) AS reviewed,
                   created_at, updated_at
            FROM orders
            WHERE id = #{id}
              AND chef_id = #{chefId}
              AND chef_deleted = 0
            """)
    @ResultMap("orderResultMap")
    Order selectByIdAndChefId(@Param("id") Long id, @Param("chefId") Long chefId);

    @Select("""
            SELECT id, order_no, user_id, chef_id, address_id, service_date, time_slot,
                   service_start_time, service_end_time, people_count, taste_preference,
                   taboo_food, special_requirement, ingredient_mode, ingredient_list,
                   contact_name, contact_phone, full_address, longitude, latitude,
                   confirm_code, total_amount, discount_amount, pay_amount, order_status,
                   cancel_reason, refund_reason, user_deleted, chef_deleted,
                   EXISTS(SELECT 1 FROM review r WHERE r.order_id = orders.id) AS reviewed,
                   created_at, updated_at
            FROM orders
            WHERE chef_id = #{chefId}
              AND chef_deleted = 0
              AND (#{orderStatus} IS NULL OR #{orderStatus} = '' OR order_status = #{orderStatus})
            ORDER BY created_at DESC
            """)
    @ResultMap("orderResultMap")
    List<Order> selectChefList(@Param("chefId") Long chefId, @Param("orderStatus") String orderStatus);

    @SelectProvider(type = OrderSqlProvider.class, method = "buildSelectListSql")
    @ResultMap("orderResultMap")
    List<Order> selectList(OrderQueryDTO queryDTO);

    @Update("""
            UPDATE orders
            SET order_status = #{orderStatus},
                cancel_reason = #{cancelReason},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int cancelById(@Param("id") Long id,
                   @Param("orderStatus") String orderStatus,
                   @Param("cancelReason") String cancelReason,
                   @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE orders
            SET order_status = #{orderStatus},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updatePaidStatusById(@Param("id") Long id,
                             @Param("orderStatus") String orderStatus,
                             @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE orders
            SET order_status = #{orderStatus},
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND chef_id = #{chefId}
              AND chef_deleted = 0
            """)
    int updateStatusByIdAndChefId(@Param("id") Long id,
                                  @Param("chefId") Long chefId,
                                  @Param("orderStatus") String orderStatus,
                                  @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE orders
            SET order_status = #{orderStatus},
                cancel_reason = #{cancelReason},
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND chef_id = #{chefId}
              AND chef_deleted = 0
            """)
    int updateStatusAndCancelReasonById(@Param("id") Long id,
                                        @Param("chefId") Long chefId,
                                        @Param("orderStatus") String orderStatus,
                                        @Param("cancelReason") String cancelReason,
                                        @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE orders
            SET order_status = #{orderStatus},
                refund_reason = #{refundReason},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateRefundStatusById(@Param("id") Long id,
                               @Param("orderStatus") String orderStatus,
                               @Param("refundReason") String refundReason,
                               @Param("updatedAt") LocalDateTime updatedAt);

    class OrderSqlProvider {

        public String buildSelectListSql(final OrderQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, order_no, user_id, chef_id, address_id, service_date, time_slot")
                    .SELECT("service_start_time, service_end_time, people_count, taste_preference, taboo_food")
                    .SELECT("special_requirement, ingredient_mode, ingredient_list, contact_name, contact_phone")
                    .SELECT("full_address, longitude, latitude, confirm_code, total_amount, discount_amount")
                    .SELECT("pay_amount, order_status, cancel_reason, refund_reason, user_deleted, chef_deleted")
                    .SELECT("EXISTS(SELECT 1 FROM review r WHERE r.order_id = orders.id) AS reviewed")
                    .SELECT("created_at, updated_at")
                    .FROM("orders");

            if (queryDTO != null) {
                if (queryDTO.getUserId() != null) {
                    sql.WHERE("user_id = #{userId}");
                }
                if (queryDTO.getOrderStatus() != null && !queryDTO.getOrderStatus().trim().isEmpty()) {
                    sql.WHERE("order_status = #{orderStatus}");
                }
            }

            return sql.ORDER_BY("id DESC").toString();
        }
    }
}
