package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.dto.UserAddressQueryDTO;
import com.homechef.homechefsystem.entity.UserAddress;
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
public interface UserAddressMapper {

    @Insert("""
            INSERT INTO user_address (
                user_id, contact_name, contact_phone, province, city, district, town,
                detail_address, longitude, latitude, is_default, status,
                created_at, updated_at
            ) VALUES (
                #{userId}, #{contactName}, #{contactPhone}, #{province}, #{city}, #{district}, #{town},
                #{detailAddress}, #{longitude}, #{latitude}, #{isDefault}, #{status},
                #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAddress userAddress);

    @Select("""
            SELECT id, user_id, contact_name, contact_phone, province, city, district, town,
                   detail_address, longitude, latitude, is_default, status,
                   created_at, updated_at
            FROM user_address
            WHERE id = #{id} AND status = 1
            """)
    UserAddress selectById(@Param("id") Long id);

    @Select("""
            SELECT id, user_id, contact_name, contact_phone, province, city, district, town,
                   detail_address, longitude, latitude, is_default, status,
                   created_at, updated_at
            FROM user_address
            WHERE id = #{id}
              AND user_id = #{userId}
              AND status = 1
            """)
    UserAddress selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @SelectProvider(type = UserAddressSqlProvider.class, method = "buildSelectListSql")
    List<UserAddress> selectList(UserAddressQueryDTO queryDTO);

    @Select("""
            SELECT id, user_id, contact_name, contact_phone, province, city, district, town,
                   detail_address, longitude, latitude, is_default, status,
                   created_at, updated_at
            FROM user_address
            WHERE user_id = #{userId} AND is_default = 1 AND status = 1
            ORDER BY id DESC
            LIMIT 1
            """)
    UserAddress selectDefaultByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(1)
            FROM user_address
            WHERE user_id = #{userId} AND status = 1
            """)
    int countActiveByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE user_address
            SET contact_name = #{contactName},
                contact_phone = #{contactPhone},
                province = #{province},
                city = #{city},
                district = #{district},
                town = #{town},
                detail_address = #{detailAddress},
                longitude = #{longitude},
                latitude = #{latitude},
                is_default = #{isDefault},
                updated_at = #{updatedAt}
            WHERE id = #{id} AND status = 1
            """)
    int updateById(UserAddress userAddress);

    @Update("""
            UPDATE user_address
            SET is_default = 0,
                updated_at = #{updatedAt}
            WHERE user_id = #{userId} AND status = 1
            """)
    int resetDefaultByUserId(@Param("userId") Long userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE user_address
            SET is_default = 1,
                updated_at = #{updatedAt}
            WHERE id = #{id} AND user_id = #{userId} AND status = 1
            """)
    int setDefaultById(@Param("id") Long id,
                       @Param("userId") Long userId,
                       @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE user_address
            SET status = 0,
                is_default = 0,
                updated_at = #{updatedAt}
            WHERE id = #{id} AND status = 1
            """)
    int logicDeleteById(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    class UserAddressSqlProvider {

        public String buildSelectListSql(final UserAddressQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, user_id, contact_name, contact_phone, province, city, district, town")
                    .SELECT("detail_address, longitude, latitude, is_default, status")
                    .SELECT("created_at, updated_at")
                    .FROM("user_address")
                    .WHERE("status = 1");

            if (queryDTO != null && queryDTO.getUserId() != null) {
                sql.WHERE("user_id = #{userId}");
            }

            return sql.ORDER_BY("id DESC").toString();
        }
    }
}

