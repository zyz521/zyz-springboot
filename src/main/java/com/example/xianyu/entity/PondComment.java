package com.example.xianyu.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 鱼塘动态评论实体
 */
@Entity
@Table(name = "pond_comment")
@Data
public class PondComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属动态ID
     */
    @Column(nullable = false)
    private Long postId;

    /**
     * 评论用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 评论用户名（冗余存储）
     */
    @Column(nullable = false, length = 50)
    private String username;

    /**
     * 评论用户头像（冗余存储）
     */
    @Column(length = 500)
    private String userAvatar;

    /**
     * 评论内容
     */
    @Lob
    private String content;

    /**
     * 父评论ID（用于回复功能）
     */
    private Long parentId;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
    }
}











