package com.example.xianyu.repository;

import com.example.xianyu.entity.PondComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PondCommentRepository extends JpaRepository<PondComment, Long> {

    /**
     * 查询动态的所有评论（按时间正序）
     */
    List<PondComment> findByPostIdOrderByCreateTimeAsc(Long postId);

    /**
     * 查询动态的顶级评论（无父评论）
     */
    List<PondComment> findByPostIdAndParentIdIsNullOrderByCreateTimeAsc(Long postId);

    /**
     * 查询评论的回复
     */
    List<PondComment> findByParentIdOrderByCreateTimeAsc(Long parentId);

    /**
     * 统计动态的评论数
     */
    long countByPostId(Long postId);
}











