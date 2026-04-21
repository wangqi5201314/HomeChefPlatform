package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.dto.AdminPaymentQueryDTO;
import com.homechef.homechefsystem.entity.Payment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.jdbc.SQL;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentMapper {

    @Insert("""
            INSERT INTO payment (
                order_id, pay_no, pay_channel, pay_amount, pay_status,
                transaction_id, paid_at, refund_no, refund_amount, refund_status,
                refund_at, created_at, updated_at
            ) VALUES (
                #{orderId}, #{payNo}, #{payChannel}, #{payAmount}, #{payStatus},
                #{transactionId}, #{paidAt}, #{refundNo}, #{refundAmount}, #{refundStatus},
                #{refundAt}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Payment payment);

    @Select("""
            SELECT id, order_id, pay_no, pay_channel, pay_amount, pay_status,
                   transaction_id, paid_at, refund_no, refund_amount, refund_status,
                   refund_at, created_at, updated_at
            FROM payment
            WHERE order_id = #{orderId}
            ORDER BY id DESC
            LIMIT 1
            """)
    Payment selectByOrderId(@Param("orderId") Long orderId);

    @SelectProvider(type = PaymentSqlProvider.class, method = "buildSelectAdminListSql")
    List<Payment> selectAdminList(AdminPaymentQueryDTO queryDTO);

    @Update("""
            UPDATE payment
            SET pay_status = #{payStatus},
                transaction_id = #{transactionId},
                paid_at = #{paidAt},
                updated_at = #{updatedAt}
            WHERE order_id = #{orderId}
            """)
    int updatePaySuccessByOrderId(@Param("orderId") Long orderId,
                                  @Param("payStatus") String payStatus,
                                  @Param("transactionId") String transactionId,
                                  @Param("paidAt") LocalDateTime paidAt,
                                  @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE payment
            SET refund_no = #{refundNo},
                refund_amount = #{refundAmount},
                refund_status = #{refundStatus},
                refund_at = #{refundAt},
                updated_at = #{updatedAt}
            WHERE order_id = #{orderId}
            """)
    int updateRefundByOrderId(@Param("orderId") Long orderId,
                              @Param("refundNo") String refundNo,
                              @Param("refundAmount") BigDecimal refundAmount,
                              @Param("refundStatus") String refundStatus,
                              @Param("refundAt") LocalDateTime refundAt,
                              @Param("updatedAt") LocalDateTime updatedAt);

    class PaymentSqlProvider {

        public String buildSelectAdminListSql(final AdminPaymentQueryDTO queryDTO) {
            SQL sql = new SQL()
                    .SELECT("id, order_id, pay_no, pay_channel, pay_amount, pay_status")
                    .SELECT("transaction_id, paid_at, refund_no, refund_amount, refund_status")
                    .SELECT("refund_at, created_at, updated_at")
                    .FROM("payment");

            if (queryDTO != null) {
                if (queryDTO.getOrderId() != null) {
                    sql.WHERE("order_id = #{orderId}");
                }
                if (queryDTO.getPayStatus() != null && !queryDTO.getPayStatus().trim().isEmpty()) {
                    sql.WHERE("pay_status = #{payStatus}");
                }
                if (queryDTO.getRefundStatus() != null && !queryDTO.getRefundStatus().trim().isEmpty()) {
                    sql.WHERE("refund_status = #{refundStatus}");
                }
            }

            return sql.ORDER_BY("created_at DESC").toString();
        }
    }
}

