package com.example.xianyu.repository;

import com.example.xianyu.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByProductIdOrderByCreateTimeDesc(Long productId);
}



