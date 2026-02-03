package com.example.xianyu.repository;

import com.example.xianyu.entity.OrderInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderInfoRepository extends JpaRepository<OrderInfo, Long> {

    List<OrderInfo> findByBuyerId(Long buyerId);

    List<OrderInfo> findBySellerId(Long sellerId);
}


