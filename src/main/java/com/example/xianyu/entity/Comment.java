package com.example.xianyu.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment")
@Data
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属商品
     */
    @Column(nullable = false)
    private Long productId;

    /**
     * 评论用户
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 冗余存一份用户名，方便展示
     */
    @Column(nullable = false, length = 50)
    private String username;

    /**
     * 评论内容
     */
    @Lob
    private String content;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
    }
}



