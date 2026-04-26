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
     * 方法说明：在 操作日志服务实现 中处理 save 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public void save(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }
}
