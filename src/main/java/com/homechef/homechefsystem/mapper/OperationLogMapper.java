package com.homechef.homechefsystem.mapper;

import com.homechef.homechefsystem.entity.OperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface OperationLogMapper {

    @Insert("""
            INSERT INTO operation_log (
                operator_type, operator_id, module, operation, request_method, request_uri,
                request_params, response_data, ip, status, created_at
            ) VALUES (
                #{operatorType}, #{operatorId}, #{module}, #{operation}, #{requestMethod}, #{requestUri},
                #{requestParams}, #{responseData}, #{ip}, #{status}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog operationLog);
}
