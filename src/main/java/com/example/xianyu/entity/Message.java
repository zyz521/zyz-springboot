package com.example.xianyu.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "message")
@Data
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 收件人用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 发送人用户ID（系统消息为空）
     */
    private Long senderId;

    /**
     * 发送人昵称快照（避免关联查）
     */
    @Column(length = 50)
    private String senderName;

    /**
     * 消息标题
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * 消息正文
     */
    @Lob
    private String content;

    /**
     * 关联商品（用于买家联系卖家）
     */
    private Long productId;

    /**
     * 关联鱼塘帖子（用于帖子评论通知）
     */
    private Long postId;

    /**
     * 消息类型：system / order / chat / pond
     */
    @Column(length = 20)
    private String type;

    /**
     * 是否已读
     */
    @Column(nullable = false)
    private Boolean readFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (readFlag == null) {
            readFlag = Boolean.FALSE;
        }
        if (type == null) {
            type = "system";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}

