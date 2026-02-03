package com.example.xianyu.repository;

import com.example.xianyu.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatus(Integer status);

    List<Product> findByTitleContainingAndStatus(String keyword, Integer status);

    List<Product> findByCategoryAndStatus(String category, Integer status);
    /**
     * 查询某个用户发布的所有商品（不区分状态，用于个人管理页）
     */
    List<Product> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 查询某个用户在售的商品，供后续扩展使用
     */
    List<Product> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, Integer status);
    /**
     * 查询最新上架的在售商品（带主图），用于首页轮播推荐
     */
    List<Product> findTop8ByStatusAndImageUrlIsNotNullOrderByCreateTimeDesc(Integer status);
    
    /**
     * 查询所有在售且有图片的商品，用于推荐算法
     */
    List<Product> findByStatusAndImageUrlIsNotNull(Integer status);

}




