package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.dto.ChefQueryDTO;
import com.homechef.homechefsystem.entity.Chef;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
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
public interface ChefMapper {

    @SelectProvider(type = ChefSqlProvider.class, method = "buildSelectListSql")
    @Results(id = "chefResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "password", column = "password"),
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
    List<Chef> selectList(ChefQueryDTO queryDTO);

    @Select("""
            SELECT id, name, phone, password, avatar, gender, age, introduction, specialty_cuisine,
                   specialty_tags, years_of_experience, service_radius_km, service_mode,
                   rating_avg, order_count, on_time_rate, good_review_rate, cert_status,
                   status, created_at, updated_at
            FROM chef
            WHERE id = #{id}
            """)
    @Results(id = "chefDetailResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "password", column = "password"),
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
    Chef selectById(@Param("id") Long id);

    @Select("""
            SELECT id, name, phone, password, avatar, gender, age, introduction, specialty_cuisine,
                   specialty_tags, years_of_experience, service_radius_km, service_mode,
                   rating_avg, order_count, on_time_rate, good_review_rate, cert_status,
                   status, created_at, updated_at
            FROM chef
            WHERE phone = #{phone}
            ORDER BY id DESC
            LIMIT 1
            """)
    @Results(id = "chefLoginResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "password", column = "password"),
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
    Chef selectByPhone(@Param("phone") String phone);

    @Insert("""
            INSERT INTO chef (
                name, phone, password, avatar, gender, age, introduction, specialty_cuisine,
                specialty_tags, years_of_experience, service_radius_km, service_mode,
                rating_avg, order_count, on_time_rate, good_review_rate, cert_status,
                status, created_at, updated_at
            ) VALUES (
                #{name}, #{phone}, #{password}, #{avatar}, #{gender}, #{age}, #{introduction}, #{specialtyCuisine},
                #{specialtyTags}, #{yearsOfExperience}, #{serviceRadiusKm}, #{serviceMode},
                #{ratingAvg}, #{orderCount}, #{onTimeRate}, #{goodReviewRate}, #{certStatus},
                #{status}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Chef chef);

    @Update("""
            UPDATE chef
            SET name = #{name},
                phone = #{phone},
                avatar = #{avatar},
                gender = #{gender},
                age = #{age},
                introduction = #{introduction},
                specialty_cuisine = #{specialtyCuisine},
                specialty_tags = #{specialtyTags},
                years_of_experience = #{yearsOfExperience},
                service_radius_km = #{serviceRadiusKm},
                service_mode = #{serviceMode},
                status = #{status},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateById(Chef chef);

    @Update("""
            UPDATE chef
            SET password = #{password},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updatePasswordById(@Param("id") Long id,
                           @Param("password") String password,
                           @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE chef
            SET order_count = COALESCE(order_count, 0) + 1,
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int incrementOrderCountById(@Param("id") Long id,
                                @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE chef
            SET cert_status = #{certStatus},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateCertStatusById(@Param("id") Long id,
                             @Param("certStatus") Integer certStatus,
                             @Param("updatedAt") LocalDateTime updatedAt);

    class ChefSqlProvider {

        public String buildSelectListSql(final ChefQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, name, phone, password, avatar, gender, age, introduction, specialty_cuisine")
                    .SELECT("specialty_tags, years_of_experience, service_radius_km, service_mode")
                    .SELECT("rating_avg, order_count, on_time_rate, good_review_rate, cert_status")
                    .SELECT("status, created_at, updated_at")
                    .FROM("chef");

            if (queryDTO != null) {
                if (queryDTO.getName() != null && !queryDTO.getName().trim().isEmpty()) {
                    sql.WHERE("name LIKE CONCAT('%', #{name}, '%')");
                }
                if (queryDTO.getSpecialtyCuisine() != null && !queryDTO.getSpecialtyCuisine().trim().isEmpty()) {
                    sql.WHERE("specialty_cuisine = #{specialtyCuisine}");
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
    }
}
