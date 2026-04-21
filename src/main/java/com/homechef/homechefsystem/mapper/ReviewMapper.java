package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.dto.AdminReviewQueryDTO;
import com.homechef.homechefsystem.dto.ReviewQueryDTO;
import com.homechef.homechefsystem.entity.Review;
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
public interface ReviewMapper {

    @Insert("""
            INSERT INTO review (
                order_id, user_id, chef_id, dish_score, service_score, skill_score,
                environment_score, overall_score, content, image_urls, is_anonymous,
                reply_content, reply_at, created_at
            ) VALUES (
                #{orderId}, #{userId}, #{chefId}, #{dishScore}, #{serviceScore}, #{skillScore},
                #{environmentScore}, #{overallScore}, #{content}, #{imageUrls}, #{isAnonymous},
                #{replyContent}, #{replyAt}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Review review);

    @Select("""
            SELECT id, order_id, user_id, chef_id, dish_score, service_score, skill_score,
                   environment_score, overall_score, content, image_urls, is_anonymous,
                   reply_content, reply_at, created_at
            FROM review
            WHERE id = #{id}
            """)
    Review selectById(@Param("id") Long id);

    @Select("""
            SELECT id, order_id, user_id, chef_id, dish_score, service_score, skill_score,
                   environment_score, overall_score, content, image_urls, is_anonymous,
                   reply_content, reply_at, created_at
            FROM review
            WHERE order_id = #{orderId}
            LIMIT 1
            """)
    Review selectByOrderId(@Param("orderId") Long orderId);

    @Select("""
            SELECT COUNT(1)
            FROM review
            WHERE order_id = #{orderId}
            """)
    int countByOrderId(@Param("orderId") Long orderId);

    @SelectProvider(type = ReviewSqlProvider.class, method = "buildSelectListSql")
    List<Review> selectList(ReviewQueryDTO queryDTO);

    @SelectProvider(type = ReviewSqlProvider.class, method = "buildSelectAdminListSql")
    List<Review> selectAdminList(AdminReviewQueryDTO queryDTO);

    @Update("""
            UPDATE review
            SET reply_content = #{replyContent},
                reply_at = #{replyAt}
            WHERE id = #{id}
            """)
    int updateReplyById(@Param("id") Long id,
                        @Param("replyContent") String replyContent,
                        @Param("replyAt") LocalDateTime replyAt);

    class ReviewSqlProvider {

        public String buildSelectListSql(final ReviewQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, order_id, user_id, chef_id, dish_score, service_score, skill_score")
                    .SELECT("environment_score, overall_score, content, image_urls, is_anonymous")
                    .SELECT("reply_content, reply_at, created_at")
                    .FROM("review");

            if (queryDTO != null) {
                if (queryDTO.getChefId() != null) {
                    sql.WHERE("chef_id = #{chefId}");
                }
                if (queryDTO.getUserId() != null) {
                    sql.WHERE("user_id = #{userId}");
                }
            }

            return sql.ORDER_BY("created_at DESC").toString();
        }

        public String buildSelectAdminListSql(final AdminReviewQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, order_id, user_id, chef_id, dish_score, service_score, skill_score")
                    .SELECT("environment_score, overall_score, content, image_urls, is_anonymous")
                    .SELECT("reply_content, reply_at, created_at")
                    .FROM("review");

            if (queryDTO != null) {
                if (queryDTO.getOrderId() != null) {
                    sql.WHERE("order_id = #{orderId}");
                }
                if (queryDTO.getUserId() != null) {
                    sql.WHERE("user_id = #{userId}");
                }
                if (queryDTO.getChefId() != null) {
                    sql.WHERE("chef_id = #{chefId}");
                }
            }

            return sql.ORDER_BY("created_at DESC").toString();
        }
    }
}

