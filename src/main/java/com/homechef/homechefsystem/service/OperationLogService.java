package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.entity.OperationLog;

public interface OperationLogService {

    /**
     * 保存一条日志记录。
     */
    void save(OperationLog operationLog);
}
