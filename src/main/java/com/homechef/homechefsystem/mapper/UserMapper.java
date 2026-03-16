package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, openid, unionid, phone, nickname, avatar, gender, birthday,
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
