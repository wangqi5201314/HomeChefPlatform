package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.entity.ChefServiceLocation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChefServiceLocationMapper {

    @Select("""
            SELECT id, chef_id, province, city, district, town, detail_address,
                   longitude, latitude, created_at, updated_at
            FROM chef_service_location
            WHERE chef_id = #{chefId}
            """)
    @Results(id = "chefServiceLocationResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "chefId", column = "chef_id"),
            @Result(property = "province", column = "province"),
            @Result(property = "city", column = "city"),
            @Result(property = "district", column = "district"),
            @Result(property = "town", column = "town"),
            @Result(property = "detailAddress", column = "detail_address"),
            @Result(property = "longitude", column = "longitude"),
            @Result(property = "latitude", column = "latitude"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    ChefServiceLocation selectByChefId(@Param("chefId") Long chefId);

    @Insert("""
            INSERT INTO chef_service_location (
                chef_id, province, city, district, town, detail_address,
                longitude, latitude, created_at, updated_at
            ) VALUES (
                #{chefId}, #{province}, #{city}, #{district}, #{town}, #{detailAddress},
                #{longitude}, #{latitude}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChefServiceLocation chefServiceLocation);

    @Update("""
            UPDATE chef_service_location
            SET province = #{province},
                city = #{city},
                district = #{district},
                town = #{town},
                detail_address = #{detailAddress},
                longitude = #{longitude},
                latitude = #{latitude},
                updated_at = #{updatedAt}
            WHERE chef_id = #{chefId}
            """)
    int updateByChefId(ChefServiceLocation chefServiceLocation);
}
