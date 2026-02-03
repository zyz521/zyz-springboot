package com.example.xianyu.repository;

import com.example.xianyu.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByUserIdOrderByCreateTimeDesc(Long userId);

    Long countByUserIdAndReadFlagFalse(Long userId);

    @org.springframework.data.jpa.repository.Query("""
        select m from Message m
        where m.productId = :productId
          and ((m.userId = :userA and m.senderId = :userB)
               or (m.userId = :userB and m.senderId = :userA))
        order by m.createTime asc
    """)
    List<Message> findThread(Long productId, Long userA, Long userB);

    @org.springframework.data.jpa.repository.Query("""
        select m from Message m
        where m.productId = :productId
          and m.userId = :me and m.senderId = :other
          and m.readFlag = false
    """)
    List<Message> findUnreadInThread(Long productId, Long me, Long other);

    /**
     * 查找某个帖子的未读消息（针对当前用户）
     */
    @org.springframework.data.jpa.repository.Query("""
        select m from Message m
        where m.postId = :postId
          and m.userId = :userId
          and m.readFlag = false
          and m.type = 'pond'
    """)
    List<Message> findUnreadByPostId(Long postId, Long userId);
}

