package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.dto.AdminChefQueryDTO;
import com.homechef.homechefsystem.dto.AdminOrderQueryDTO;
import com.homechef.homechefsystem.dto.AdminUserQueryDTO;
import com.homechef.homechefsystem.entity.Admin;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.Order;
import com.homechef.homechefsystem.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.jdbc.SQL;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminMapper {

    @Select("""
            SELECT id, username, password, real_name, role, status, last_login_time, created_at, updated_at
            FROM admin
            WHERE username = #{username}
            """)
    @Results(id = "adminResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"),
            @Result(property = "realName", column = "real_name"),
            @Result(property = "role", column = "role"),
            @Result(property = "status", column = "status"),
            @Result(property = "lastLoginTime", column = "last_login_time"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Admin selectByUsername(@Param("username") String username);

    @Update("""
            UPDATE admin
            SET last_login_time = #{lastLoginTime},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateLoginTimeById(@Param("id") Long id,
                            @Param("lastLoginTime") LocalDateTime lastLoginTime,
                            @Param("updatedAt") LocalDateTime updatedAt);

    @SelectProvider(type = AdminSqlProvider.class, method = "buildSelectUserListSql")
    @Results(id = "adminUserResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "openid", column = "openid"),
            @Result(property = "unionid", column = "unionid"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "nickname", column = "nickname"),
            @Result(property = "avatar", column = "avatar"),
            @Result(property = "gender", column = "gender"),
            @Result(property = "birthday", column = "birthday"),
            @Result(property = "tastePreference", column = "taste_preference"),
            @Result(property = "allergyInfo", column = "allergy_info"),
            @Result(property = "emergencyContactName", column = "emergency_contact_name"),
            @Result(property = "emergencyContactPhone", column = "emergency_contact_phone"),
            @Result(property = "status", column = "status"),
            @Result(property = "lastLoginTime", column = "last_login_time"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<User> selectUserList(AdminUserQueryDTO queryDTO);

    @Update("""
            UPDATE `user`
            SET status = #{status},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateUserStatusById(@Param("id") Long id,
                             @Param("status") Integer status,
                             @Param("updatedAt") LocalDateTime updatedAt);

    @SelectProvider(type = AdminSqlProvider.class, method = "buildSelectChefListSql")
    @Results(id = "adminChefResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "avatar", column = "avatar"),
            @Result(property = "gender", column = "gender"),
            @Result(property = "age", column = "age"),
            @Result(property = "introduction", column = "introduction"),
            @Result(property = "specialtyCuisine", column = "specialty_cuisine"),
            @Result(property = "specialtyTags", column = "specialty_tags"),
            @Result(property = "yearsOfExperience", column = "years_of_experience"),
            @Result(property = "serviceRadiusKm", column = "service_radius_km"),
            @Result(property = "serviceMode", column = "service_mode"),
            @Result(property = "ratingAvg", column = "rating_avg"),
            @Result(property = "orderCount", column = "order_count"),
            @Result(property = "onTimeRate", column = "on_time_rate"),
            @Result(property = "goodReviewRate", column = "good_review_rate"),
            @Result(property = "certStatus", column = "cert_status"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Chef> selectChefList(AdminChefQueryDTO queryDTO);

    @Update("""
            UPDATE chef
            SET status = #{status},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateChefStatusById(@Param("id") Long id,
                             @Param("status") Integer status,
                             @Param("updatedAt") LocalDateTime updatedAt);

    @SelectProvider(type = AdminSqlProvider.class, method = "buildSelectOrderListSql")
    @Results(id = "adminOrderResultMap", value = {
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
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Order> selectOrderList(AdminOrderQueryDTO queryDTO);

    class AdminSqlProvider {

        public String buildSelectUserListSql(final AdminUserQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, openid, unionid, phone, nickname, avatar, gender, birthday")
                    .SELECT("taste_preference, allergy_info, emergency_contact_name, emergency_contact_phone")
                    .SELECT("status, last_login_time, created_at, updated_at")
                    .FROM("`user`");

            if (queryDTO != null) {
                if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
                    sql.WHERE("(phone LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%'))");
                }
                if (queryDTO.getStatus() != null) {
                    sql.WHERE("status = #{status}");
                }
            }

            return sql.ORDER_BY("id DESC").toString();
        }

        public String buildSelectChefListSql(final AdminChefQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, name, phone, avatar, gender, age, introduction, specialty_cuisine")
                    .SELECT("specialty_tags, years_of_experience, service_radius_km, service_mode")
                    .SELECT("rating_avg, order_count, on_time_rate, good_review_rate, cert_status")
                    .SELECT("status, created_at, updated_at")
                    .FROM("chef");

            if (queryDTO != null) {
                if (queryDTO.getName() != null && !queryDTO.getName().trim().isEmpty()) {
                    sql.WHERE("name LIKE CONCAT('%', #{name}, '%')");
                }
                if (queryDTO.getCertStatus() != null) {
                    sql.WHERE("cert_status = #{certStatus}");
                }
                if (queryDTO.getStatus() != null) {
                    sql.WHERE("status = #{status}");
                }
            }

            return sql.ORDER_BY("id DESC").toString();
        }

        public String buildSelectOrderListSql(final AdminOrderQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, order_no, user_id, chef_id, address_id, service_date, time_slot")
                    .SELECT("service_start_time, service_end_time, people_count, taste_preference, taboo_food")
                    .SELECT("special_requirement, ingredient_mode, ingredient_list, contact_name, contact_phone")
                    .SELECT("full_address, longitude, latitude, confirm_code, total_amount, discount_amount")
                    .SELECT("pay_amount, order_status, cancel_reason, refund_reason, user_deleted, chef_deleted")
                    .SELECT("created_at, updated_at")
                    .FROM("orders");

            if (queryDTO != null) {
                if (queryDTO.getOrderNo() != null && !queryDTO.getOrderNo().trim().isEmpty()) {
                    sql.WHERE("order_no LIKE CONCAT('%', #{orderNo}, '%')");
                }
                if (queryDTO.getOrderStatus() != null && !queryDTO.getOrderStatus().trim().isEmpty()) {
                    sql.WHERE("order_status = #{orderStatus}");
                }
            }

            return sql.ORDER_BY("id DESC").toString();
        }
    }
}
