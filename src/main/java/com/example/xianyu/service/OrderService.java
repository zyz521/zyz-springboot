package com.example.xianyu.service;

import com.example.xianyu.entity.OrderInfo;
import com.example.xianyu.entity.Product;
import com.example.xianyu.repository.OrderInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderInfoRepository orderInfoRepository;

    public OrderService(OrderInfoRepository orderInfoRepository) {
        this.orderInfoRepository = orderInfoRepository;
    }

    public OrderInfo createOrder(Long buyerId, Product product) {
        OrderInfo order = new OrderInfo();
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId());
        order.setProductId(product.getId());
        order.setAmount(product.getPrice() == null ? BigDecimal.ZERO : product.getPrice());
        // status 默认为 0 待付款
        return orderInfoRepository.save(order);
    }

    public Optional<OrderInfo> findById(Long orderId) {
        return orderInfoRepository.findById(orderId);
    }

    public List<OrderInfo> listByBuyer(Long buyerId) {
        return orderInfoRepository.findByBuyerId(buyerId);
    }

    public List<OrderInfo> listBySeller(Long sellerId) {
        return orderInfoRepository.findBySellerId(sellerId);
    }

    /**
     * 支付订单
     */
    @Transactional
    public void payOrder(Long orderId) {
        OrderInfo order = orderInfoRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不正确，无法支付");
        }
        order.setStatus(1); // 已付款
        orderInfoRepository.save(order);
    }

    /**
     * 完成订单
     */
    @Transactional
    public void completeOrder(Long orderId, Long userId) {
        OrderInfo order = orderInfoRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态不正确，无法完成");
        }
        order.setStatus(2); // 已完成
        orderInfoRepository.save(order);
    }

    /**
     * 取消订单
     */
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        OrderInfo order = orderInfoRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("只能取消待付款的订单");
        }
        order.setStatus(3); // 已取消
        orderInfoRepository.save(order);
    }

    /**
     * 删除订单（只能删除已取消或已完成的订单）
     */
    @Transactional
    public void deleteOrder(Long orderId, Long userId) {
        OrderInfo order = orderInfoRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        // 只能删除已取消或已完成的订单
        if (order.getStatus() != 2 && order.getStatus() != 3) {
            throw new RuntimeException("只能删除已完成或已取消的订单");
        }
        orderInfoRepository.delete(order);
    }
}


