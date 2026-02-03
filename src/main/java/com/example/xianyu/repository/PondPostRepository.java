package com.example.xianyu.repository;

import com.example.xianyu.entity.PondPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PondPostRepository extends JpaRepository<PondPost, Long> {

    /**
     * 按创建时间倒序查询所有动态
     */
    List<PondPost> findAllByOrderByCreateTimeDesc();

    /**
     * 按分类查询动态
     */
    List<PondPost> findByCategoryOrderByCreateTimeDesc(String category);

    /**
     * 按城市查询动态
     */
    List<PondPost> findByCityOrderByCreateTimeDesc(String city);

    /**
     * 按分类和城市查询动态
     */
    List<PondPost> findByCategoryAndCityOrderByCreateTimeDesc(String category, String city);

    /**
     * 查询用户的动态
     */
    List<PondPost> findByUserIdOrderByCreateTimeDesc(Long userId);
}











