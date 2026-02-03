package com.example.xianyu.service;

import com.example.xianyu.entity.Comment;
import com.example.xianyu.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public Comment addComment(Long productId,
                              Long userId,
                              String username,
                              String content) {
        Comment c = new Comment();
        c.setProductId(productId);
        c.setUserId(userId);
        c.setUsername(username);
        c.setContent(content);
        return commentRepository.save(c);
    }

    public List<Comment> listByProduct(Long productId) {
        return commentRepository.findByProductIdOrderByCreateTimeDesc(productId);
    }
}



