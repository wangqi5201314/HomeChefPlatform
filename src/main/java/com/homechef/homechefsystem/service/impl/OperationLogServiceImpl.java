package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.entity.OperationLog;
import com.homechef.homechefsystem.mapper.OperationLogMapper;
import com.homechef.homechefsystem.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /**
     * 保存一条需要落库的业务记录。
     * 这个方法主要用于把日志、快照或处理结果写进数据库。
     * 它会先补齐必要字段，再调用 Mapper 完成保存。
     */
    @Override
    public void save(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }
}
