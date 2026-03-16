package com.homechef.homechefsystem.controller.order;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.OrderCancelDTO;
import com.homechef.homechefsystem.dto.OrderCreateDTO;
import com.homechef.homechefsystem.dto.OrderQueryDTO;
import com.homechef.homechefsystem.service.OrderService;
import com.homechef.homechefsystem.vo.OrderDetailVO;
import com.homechef.homechefsystem.vo.OrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public Result<OrderDetailVO> createOrder(@RequestBody OrderCreateDTO orderCreateDTO) {
        OrderDetailVO orderDetailVO = orderService.createOrder(orderCreateDTO);
        if (orderDetailVO == null) {
            return Result.error(500, "create order failed");
        }
        return Result.success(orderDetailVO);
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> getById(@PathVariable Long id) {
        OrderDetailVO orderDetailVO = orderService.getById(id);
        if (orderDetailVO == null) {
            return Result.error(404, "order not found");
        }
        return Result.success(orderDetailVO);
    }

    @GetMapping("/list")
    public Result<List<OrderListVO>> getOrderList(OrderQueryDTO queryDTO) {
        return Result.success(orderService.getOrderList(queryDTO));
    }

    @PostMapping("/{id}/cancel")
    public Result<OrderDetailVO> cancelById(@PathVariable Long id, @RequestBody OrderCancelDTO orderCancelDTO) {
        OrderDetailVO orderDetailVO = orderService.cancelById(id, orderCancelDTO);
        if (orderDetailVO == null) {
            return Result.error(404, "order not found");
        }
        return Result.success(orderDetailVO);
    }
}
