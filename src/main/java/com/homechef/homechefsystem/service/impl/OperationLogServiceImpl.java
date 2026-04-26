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

    @Override
    /**
     * 保存一条操作日志记录。
     */
    public void save(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }
}
