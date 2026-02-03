package com.example.xianyu.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 鱼塘动态实体
 */
@Entity
@Table(name = "pond_post")
@Data
public class PondPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 发布者用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 发布者用户名（冗余存储，方便展示）
     */
    @Column(nullable = false, length = 50)
    private String username;

    /**
     * 发布者头像（冗余存储）
     */
    @Column(length = 500)
    private String userAvatar;

    /**
     * 动态内容
     */
    @Lob
    private String content;

    /**
     * 图片URLs（多个用逗号分隔）
     */
    @Column(length = 2000)
    private String images;

    /**
     * 圈子分类：digital（数码）、photography（摄影）、acg（二次元）、figure（手办）、local（同城）、other（其他）
     */
    @Column(length = 50)
    private String category;

    /**
     * 城市（用于同城筛选）
     */
    @Column(length = 50)
    private String city;

    /**
     * 点赞数
     */
    @Column(nullable = false)
    private Integer likeCount;

    /**
     * 评论数
     */
    @Column(nullable = false)
    private Integer commentCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (likeCount == null) {
            likeCount = 0;
        }
        if (commentCount == null) {
            commentCount = 0;
        }
        if (category == null) {
            category = "other";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}











