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
    Admin selectByUsername(@Param("username") String username);

    @Select("""
            SELECT id, username, password, real_name, role, status, last_login_time, created_at, updated_at
            FROM admin
            WHERE id = #{id}
            """)
    Admin selectById(@Param("id") Long id);

    @Update("""
            UPDATE admin
            SET last_login_time = #{lastLoginTime},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateLoginTimeById(@Param("id") Long id,
                            @Param("lastLoginTime") LocalDateTime lastLoginTime,
                            @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE admin
            SET password = #{password},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updatePasswordById(@Param("id") Long id,
                           @Param("password") String password,
                           @Param("updatedAt") LocalDateTime updatedAt);

    @SelectProvider(type = AdminSqlProvider.class, method = "buildSelectUserListSql")
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

