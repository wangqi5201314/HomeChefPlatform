package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, openid, unionid, phone, password, nickname, avatar, gender, birthday,
                   taste_preference, allergy_info, emergency_contact_name, emergency_contact_phone,
                   status, last_login_time, created_at, updated_at
            FROM `user`
            WHERE id = #{id}
            """)
    @Results(id = "userResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "openid", column = "openid"),
            @Result(property = "unionid", column = "unionid"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "password", column = "password"),
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
    @Results(id = "userLoginResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "openid", column = "openid"),
            @Result(property = "unionid", column = "unionid"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "password", column = "password"),
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
    User selectByPhone(@Param("phone") String phone);

    @Insert("""
            INSERT INTO `user` (
                phone, password, nickname, avatar, gender, status,
                created_at, updated_at
            ) VALUES (
                #{phone}, #{password}, #{nickname}, #{avatar}, #{gender}, #{status},
                #{createdAt}, #{updatedAt}
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
}
