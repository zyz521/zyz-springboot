package com.example.xianyu.service;

import com.example.xianyu.entity.PondComment;
import com.example.xianyu.entity.PondPost;
import com.example.xianyu.entity.User;
import com.example.xianyu.repository.PondCommentRepository;
import com.example.xianyu.repository.PondPostRepository;
import com.example.xianyu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class PondService {

    private final PondPostRepository postRepository;
    private final PondCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public PondService(PondPostRepository postRepository,
                      PondCommentRepository commentRepository,
                      UserRepository userRepository,
                      MessageService messageService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    /**
     * 发布动态
     */
    @Transactional
    public PondPost createPost(Long userId, String content, String images, String category, String city) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOpt.get();
        PondPost post = new PondPost();
        post.setUserId(userId);
        post.setUsername(user.getUsername());
        post.setUserAvatar(user.getAvatar());
        post.setContent(content);
        post.setImages(images);
        post.setCategory(category != null ? category : "other");
        post.setCity(city);

        return postRepository.save(post);
    }

    /**
     * 获取动态列表
     */
    public List<PondPost> listPosts(String category, String city) {
        if (category != null && !category.isEmpty() && city != null && !city.isEmpty()) {
            return postRepository.findByCategoryAndCityOrderByCreateTimeDesc(category, city);
        } else if (category != null && !category.isEmpty()) {
            return postRepository.findByCategoryOrderByCreateTimeDesc(category);
        } else if (city != null && !city.isEmpty()) {
            return postRepository.findByCityOrderByCreateTimeDesc(city);
        } else {
            return postRepository.findAllByOrderByCreateTimeDesc();
        }
    }

    /**
     * 获取动态详情
     */
    public Optional<PondPost> getPost(Long postId) {
        return postRepository.findById(postId);
    }

    /**
     * 获取用户的动态
     */
    public List<PondPost> getUserPosts(Long userId) {
        return postRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 添加评论
     */
    @Transactional
    public PondComment addComment(Long postId, Long userId, String content, Long parentId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        Optional<PondPost> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new RuntimeException("动态不存在");
        }

        User user = userOpt.get();
        PondPost post = postOpt.get();
        
        PondComment comment = new PondComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setUsername(user.getUsername());
        comment.setUserAvatar(user.getAvatar());
        comment.setContent(content);
        comment.setParentId(parentId);

        PondComment saved = commentRepository.save(comment);

        // 更新动态的评论数
        post.setCommentCount((int) commentRepository.countByPostId(postId));
        postRepository.save(post);

        // 如果评论的不是自己的帖子，给帖子发布者发送消息通知
        if (!post.getUserId().equals(userId)) {
            messageService.sendPondCommentMessage(
                userId,
                user.getUsername(),
                post.getUserId(),
                postId,
                post.getContent(),
                content
            );
        }

        return saved;
    }

    /**
     * 获取动态的评论列表
     */
    public List<PondComment> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreateTimeAsc(postId);
    }

    /**
     * 获取评论详情
     */
    public Optional<PondComment> getComment(Long commentId) {
        return commentRepository.findById(commentId);
    }

    /**
     * 删除评论
     * 只有帖子发布者和评论者本人可以删除
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Optional<PondComment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new RuntimeException("评论不存在");
        }

        PondComment comment = commentOpt.get();
        
        // 获取帖子信息
        Optional<PondPost> postOpt = postRepository.findById(comment.getPostId());
        if (postOpt.isEmpty()) {
            throw new RuntimeException("动态不存在");
        }

        PondPost post = postOpt.get();
        
        // 检查权限：只有帖子发布者或评论者本人可以删除
        boolean isPostOwner = post.getUserId().equals(userId);
        boolean isCommentOwner = comment.getUserId().equals(userId);
        
        if (!isPostOwner && !isCommentOwner) {
            throw new RuntimeException("无权删除此评论");
        }

        // 删除评论
        commentRepository.delete(comment);

        // 更新动态的评论数
        post.setCommentCount((int) commentRepository.countByPostId(comment.getPostId()));
        postRepository.save(post);
    }

    /**
     * 删除动态
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Optional<PondPost> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new RuntimeException("动态不存在");
        }

        PondPost post = postOpt.get();
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此动态");
        }

        // 优先删除对应的图片文件（如果有）
        String images = post.getImages();
        if (images != null && !images.isEmpty()) {
            String[] urls = images.split(",");
            for (String url : urls) {
                String trimmed = url == null ? null : url.trim();
                if (trimmed == null || trimmed.isEmpty()) {
                    continue;
                }
                // URL 形式类似于 /uploads/pond/xxx.png，这里只取文件名并使用与上传时相同的目录规则
                int lastSlash = trimmed.lastIndexOf('/');
                String filename = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
                Path filePath = Paths.get(uploadDir, "pond", filename);
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    // 文件删除失败不阻塞业务，仅打印到错误输出，避免影响数据库删除
                    System.err.println("删除鱼塘图片失败: " + filePath + " - " + e.getMessage());
                }
            }
        }

        // 删除所有评论
        commentRepository.deleteAll(commentRepository.findByPostIdOrderByCreateTimeAsc(postId));
        // 删除动态
        postRepository.delete(post);
    }
}

