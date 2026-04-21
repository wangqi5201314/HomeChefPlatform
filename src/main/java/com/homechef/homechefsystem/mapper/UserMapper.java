package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.dto.AdminUserQueryDTO;
import com.homechef.homechefsystem.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.jdbc.SQL;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT COUNT(1)
            FROM `user`
            """)
    int countAll();

    @SelectProvider(type = UserSqlProvider.class, method = "buildSelectAdminUserListSql")
    List<User> selectAdminList(AdminUserQueryDTO queryDTO);

    @Select("""
            SELECT id, openid, unionid, phone, password, nickname, avatar, gender, birthday,
                   taste_preference, allergy_info, emergency_contact_name, emergency_contact_phone,
                   status, last_login_time, created_at, updated_at
            FROM `user`
            WHERE id = #{id}
            """)
    User selectById(@Param("id") Long id);

    @Select("""
            SELECT id, openid, unionid, phone, password, nickname, avatar, gender, birthday,
                   taste_preference, allergy_info, emergency_contact_name, emergency_contact_phone,
                   status, last_login_time, created_at, updated_at
            FROM `user`
            WHERE phone = #{phone}
            ORDER BY id DESC
            LIMIT 1
            """)
    User selectByPhone(@Param("phone") String phone);

    @Select("""
            SELECT id, openid, unionid, phone, password, nickname, avatar, gender, birthday,
                   taste_preference, allergy_info, emergency_contact_name, emergency_contact_phone,
                   status, last_login_time, created_at, updated_at
            FROM `user`
            WHERE emergency_contact_phone = #{emergencyContactPhone}
            ORDER BY id DESC
            LIMIT 1
            """)
    User selectByEmergencyContactPhone(@Param("emergencyContactPhone") String emergencyContactPhone);

    @Select("""
            SELECT id, openid, unionid, phone, password, nickname, avatar, gender, birthday,
                   taste_preference, allergy_info, emergency_contact_name, emergency_contact_phone,
                   status, last_login_time, created_at, updated_at
            FROM `user`
            WHERE openid = #{openid}
            ORDER BY id DESC
            LIMIT 1
            """)
    User selectByOpenid(@Param("openid") String openid);

    @Insert("""
            INSERT INTO `user` (
                openid, unionid, phone, password, nickname, avatar, gender, status,
                last_login_time, created_at, updated_at
            ) VALUES (
                #{openid}, #{unionid}, #{phone}, #{password}, #{nickname}, #{avatar}, #{gender}, #{status},
                #{lastLoginTime}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("""
            UPDATE `user`
            SET last_login_time = #{lastLoginTime},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateLoginTimeById(@Param("id") Long id,
                            @Param("lastLoginTime") LocalDateTime lastLoginTime,
                            @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE `user`
            SET password = #{password},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updatePasswordById(@Param("id") Long id,
                           @Param("password") String password,
                           @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE `user`
            SET status = #{status},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateStatusById(@Param("id") Long id,
                         @Param("status") Integer status,
                         @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE `user`
            SET phone = #{phone},
                nickname = #{nickname},
                avatar = #{avatar},
                gender = #{gender},
                birthday = #{birthday},
                taste_preference = #{tastePreference},
                allergy_info = #{allergyInfo},
                emergency_contact_name = #{emergencyContactName},
                emergency_contact_phone = #{emergencyContactPhone},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateProfileById(User user);

    class UserSqlProvider {

        public String buildSelectAdminUserListSql(final AdminUserQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, phone, nickname, avatar, gender, taste_preference, status, created_at")
                    .FROM("`user`");

            if (queryDTO != null) {
                if (queryDTO.getPhone() != null && !queryDTO.getPhone().trim().isEmpty()) {
                    sql.WHERE("phone LIKE CONCAT('%', #{phone}, '%')");
                }
                if (queryDTO.getNickname() != null && !queryDTO.getNickname().trim().isEmpty()) {
                    sql.WHERE("nickname LIKE CONCAT('%', #{nickname}, '%')");
                }
                if (queryDTO.getStatus() != null) {
                    sql.WHERE("status = #{status}");
                }
            }

            return sql.ORDER_BY("id DESC").toString();
        }
    }
}

