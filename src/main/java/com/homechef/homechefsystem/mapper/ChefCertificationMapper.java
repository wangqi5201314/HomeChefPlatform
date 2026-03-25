package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.dto.ChefCertificationQueryDTO;
import com.homechef.homechefsystem.entity.ChefCertification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.jdbc.SQL;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChefCertificationMapper {

    @Select("""
            SELECT COUNT(1)
            FROM chef_certification
            WHERE audit_status = #{auditStatus}
            """)
    int countByAuditStatus(@Param("auditStatus") Integer auditStatus);

    @Insert("""
            INSERT INTO chef_certification (
                chef_id, real_name, id_card_no, health_cert_url, skill_cert_url,
                service_cert_url, advanced_cert_url, audit_status, audit_remark,
                submitted_at, audited_at
            ) VALUES (
                #{chefId}, #{realName}, #{idCardNo}, #{healthCertUrl}, #{skillCertUrl},
                #{serviceCertUrl}, #{advancedCertUrl}, #{auditStatus}, #{auditRemark},
                #{submittedAt}, #{auditedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChefCertification chefCertification);

    @Select("""
            SELECT id, chef_id, real_name, id_card_no, health_cert_url, skill_cert_url,
                   service_cert_url, advanced_cert_url, audit_status, audit_remark,
                   submitted_at, audited_at
            FROM chef_certification
            WHERE chef_id = #{chefId}
            ORDER BY id DESC
            LIMIT 1
            """)
    @Results(id = "chefCertificationResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "chefId", column = "chef_id"),
            @Result(property = "realName", column = "real_name"),
            @Result(property = "idCardNo", column = "id_card_no"),
            @Result(property = "healthCertUrl", column = "health_cert_url"),
            @Result(property = "skillCertUrl", column = "skill_cert_url"),
            @Result(property = "serviceCertUrl", column = "service_cert_url"),
            @Result(property = "advancedCertUrl", column = "advanced_cert_url"),
            @Result(property = "auditStatus", column = "audit_status"),
            @Result(property = "auditRemark", column = "audit_remark"),
            @Result(property = "submittedAt", column = "submitted_at"),
            @Result(property = "auditedAt", column = "audited_at")
    })
    ChefCertification selectByChefId(@Param("chefId") Long chefId);

    @Select("""
            SELECT id, chef_id, real_name, id_card_no, health_cert_url, skill_cert_url,
                   service_cert_url, advanced_cert_url, audit_status, audit_remark,
                   submitted_at, audited_at
            FROM chef_certification
            WHERE id = #{id}
            """)
    @ResultMap("chefCertificationResultMap")
    ChefCertification selectById(@Param("id") Long id);

    @SelectProvider(type = ChefCertificationSqlProvider.class, method = "buildSelectListSql")
    @ResultMap("chefCertificationResultMap")
    List<ChefCertification> selectList(ChefCertificationQueryDTO queryDTO);

    @Update("""
            UPDATE chef_certification
            SET real_name = #{realName},
                id_card_no = #{idCardNo},
                health_cert_url = #{healthCertUrl},
                skill_cert_url = #{skillCertUrl},
                service_cert_url = #{serviceCertUrl},
                advanced_cert_url = #{advancedCertUrl},
                audit_status = #{auditStatus},
                audit_remark = #{auditRemark},
                submitted_at = #{submittedAt},
                audited_at = #{auditedAt}
            WHERE chef_id = #{chefId}
            """)
    int updateByChefId(ChefCertification chefCertification);

    @Update("""
            UPDATE chef_certification
            SET audit_status = #{auditStatus},
                audit_remark = #{auditRemark},
                audited_at = #{auditedAt}
            WHERE id = #{id}
            """)
    int updateAuditById(@Param("id") Long id,
                        @Param("auditStatus") Integer auditStatus,
                        @Param("auditRemark") String auditRemark,
                        @Param("auditedAt") LocalDateTime auditedAt);

    class ChefCertificationSqlProvider {

        public String buildSelectListSql(final ChefCertificationQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, chef_id, real_name, id_card_no, health_cert_url, skill_cert_url")
                    .SELECT("service_cert_url, advanced_cert_url, audit_status, audit_remark")
                    .SELECT("submitted_at, audited_at")
                    .FROM("chef_certification");

            if (queryDTO != null) {
                if (queryDTO.getAuditStatus() != null) {
                    sql.WHERE("audit_status = #{auditStatus}");
                }
                if (queryDTO.getChefId() != null) {
                    sql.WHERE("chef_id = #{chefId}");
                }
                if (queryDTO.getRealName() != null && !queryDTO.getRealName().trim().isEmpty()) {
                    sql.WHERE("real_name LIKE CONCAT('%', #{realName}, '%')");
                }
            }

            return sql.ORDER_BY("submitted_at DESC").toString();
        }
    }
}
