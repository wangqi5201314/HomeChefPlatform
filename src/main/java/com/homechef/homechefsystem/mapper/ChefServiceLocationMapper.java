package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.entity.ChefServiceLocation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChefServiceLocationMapper {

    @Select("""
            SELECT id, chef_id, location_name, province, city, district, town, detail_address,
                   longitude, latitude, is_active, created_at, updated_at
            FROM chef_service_location
            WHERE id = #{id}
            """)
    ChefServiceLocation selectById(@Param("id") Long id);

    @Select("""
            SELECT id, chef_id, location_name, province, city, district, town, detail_address,
                   longitude, latitude, is_active, created_at, updated_at
            FROM chef_service_location
            WHERE chef_id = #{chefId}
            ORDER BY is_active DESC, id DESC
            """)
    List<ChefServiceLocation> selectListByChefId(@Param("chefId") Long chefId);

    @Select("""
            SELECT id, chef_id, location_name, province, city, district, town, detail_address,
                   longitude, latitude, is_active, created_at, updated_at
            FROM chef_service_location
            WHERE chef_id = #{chefId}
              AND id = #{id}
            """)
    ChefServiceLocation selectByChefIdAndId(@Param("chefId") Long chefId, @Param("id") Long id);

    @Select("""
            SELECT id, chef_id, location_name, province, city, district, town, detail_address,
                   longitude, latitude, is_active, created_at, updated_at
            FROM chef_service_location
            WHERE chef_id = #{chefId}
              AND is_active = 1
            ORDER BY id DESC
            LIMIT 1
            """)
    ChefServiceLocation selectActiveByChefId(@Param("chefId") Long chefId);

    @Select({
            "<script>",
            "SELECT id, chef_id, location_name, province, city, district, town, detail_address,",
            "       longitude, latitude, is_active, created_at, updated_at",
            "FROM chef_service_location",
            "WHERE is_active = 1",
            "  AND chef_id IN",
            "  <foreach collection='chefIds' item='chefId' open='(' separator=',' close=')'>",
            "    #{chefId}",
            "  </foreach>",
            "ORDER BY id DESC",
            "</script>"
    })
    List<ChefServiceLocation> selectActiveListByChefIds(@Param("chefIds") List<Long> chefIds);

    @Insert("""
            INSERT INTO chef_service_location (
                chef_id, location_name, province, city, district, town, detail_address,
                longitude, latitude, is_active, created_at, updated_at
            ) VALUES (
                #{chefId}, #{locationName}, #{province}, #{city}, #{district}, #{town}, #{detailAddress},
                #{longitude}, #{latitude}, #{isActive}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChefServiceLocation chefServiceLocation);

    @Update("""
            UPDATE chef_service_location
            SET location_name = #{locationName},
                province = #{province},
                city = #{city},
                district = #{district},
                town = #{town},
                detail_address = #{detailAddress},
                longitude = #{longitude},
                latitude = #{latitude},
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND chef_id = #{chefId}
            """)
    int updateById(ChefServiceLocation chefServiceLocation);

    @Delete("""
            DELETE FROM chef_service_location
            WHERE id = #{id}
              AND chef_id = #{chefId}
            """)
    int deleteById(@Param("id") Long id, @Param("chefId") Long chefId);

    @Update("""
            UPDATE chef_service_location
            SET is_active = 0,
                updated_at = #{updatedAt}
            WHERE chef_id = #{chefId}
            """)
    int resetActiveByChefId(@Param("chefId") Long chefId, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE chef_service_location
            SET is_active = 1,
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND chef_id = #{chefId}
            """)
    int activateById(@Param("id") Long id,
                     @Param("chefId") Long chefId,
                     @Param("updatedAt") LocalDateTime updatedAt);
}

