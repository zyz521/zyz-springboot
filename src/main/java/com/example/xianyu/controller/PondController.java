package com.example.xianyu.controller;

import com.example.xianyu.entity.PondComment;
import com.example.xianyu.entity.PondPost;
import com.example.xianyu.service.MessageService;
import com.example.xianyu.service.PondService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pond")
public class PondController {

    private final PondService pondService;
    private final MessageService messageService;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public PondController(PondService pondService, MessageService messageService) {
        this.pondService = pondService;
        this.messageService = messageService;
    }

    /**
     * 鱼塘首页 - 动态列表
     */
    @GetMapping
    public String index(@RequestParam(required = false) String category,
                       @RequestParam(required = false) String city,
                       Model model,
                       HttpSession session) {
        List<PondPost> posts = pondService.listPosts(category, city);
        model.addAttribute("posts", posts);
        model.addAttribute("category", category);
        model.addAttribute("city", city);
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("userId", session.getAttribute("userId"));
        return "pond/index";
    }

    /**
     * 发布动态页面
     */
    @GetMapping("/publish")
    public String publishPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("username", session.getAttribute("username"));
        return "pond/publish";
    }

    /**
     * 发布动态
     */
    @PostMapping("/publish")
    public String publish(@RequestParam String content,
                         @RequestParam(required = false) String category,
                         @RequestParam(required = false) String city,
                         @RequestParam(required = false) MultipartFile[] images,
                         HttpSession session,
                         Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            // 处理图片上传
            String imageUrls = "";
            if (images != null && images.length > 0) {
                List<String> urls = java.util.Arrays.stream(images)
                    .filter(file -> !file.isEmpty())
                    .map(file -> {
                        try {
                            String originalFilename = file.getOriginalFilename();
                            String extension = originalFilename != null && originalFilename.contains(".")
                                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                                : "";
                            String filename = UUID.randomUUID().toString() + extension;
                            Path uploadPath = Paths.get(uploadDir, "pond", filename);
                            Files.createDirectories(uploadPath.getParent());
                            Files.write(uploadPath, file.getBytes());
                            return "/uploads/pond/" + filename;
                        } catch (IOException e) {
                            throw new RuntimeException("图片上传失败", e);
                        }
                    })
                    .collect(Collectors.toList());
                imageUrls = String.join(",", urls);
            }

            pondService.createPost(userId, content, imageUrls, category, city);
            return "redirect:/pond";
        } catch (Exception e) {
            model.addAttribute("error", "发布失败：" + e.getMessage());
            model.addAttribute("username", session.getAttribute("username"));
            return "pond/publish";
        }
    }

    /**
     * 动态详情页
     */
    @GetMapping("/post/{id}")
    public String postDetail(@PathVariable Long id, Model model, HttpSession session) {
        Optional<PondPost> postOpt = pondService.getPost(id);
        if (postOpt.isEmpty()) {
            return "redirect:/pond";
        }

        PondPost post = postOpt.get();
        List<PondComment> comments = pondService.getComments(id);

        // 如果用户已登录，标记该帖子的所有未读消息为已读
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            messageService.markPostMessagesRead(id, userId);
        }

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("userId", userId);
        return "pond/detail";
    }

    /**
     * 添加评论
     */
    @PostMapping("/comment")
    public String addComment(@RequestParam Long postId,
                           @RequestParam String content,
                           @RequestParam(required = false) Long parentId,
                           HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            pondService.addComment(postId, userId, content, parentId);
            return "redirect:/pond/post/" + postId;
        } catch (Exception e) {
            return "redirect:/pond/post/" + postId + "?error=" + e.getMessage();
        }
    }

    /**
     * 获取评论列表API（JSON格式）
     */
    @GetMapping("/post/{id}/comments/api")
    @ResponseBody
    public java.util.Map<String, Object> getCommentsApi(@PathVariable Long id) {
        List<PondComment> comments = pondService.getComments(id);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("comments", comments);
        return result;
    }

    /**
     * 删除评论
     */
    @PostMapping("/comment/delete/{id}")
    public String deleteComment(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            // 获取评论信息以获取postId
            Optional<PondComment> commentOpt = pondService.getComment(id);
            if (commentOpt.isEmpty()) {
                return "redirect:/pond?error=评论不存在";
            }
            
            Long postId = commentOpt.get().getPostId();
            pondService.deleteComment(id, userId);
            return "redirect:/pond/post/" + postId;
        } catch (Exception e) {
            // 尝试获取postId用于重定向
            try {
                Optional<PondComment> commentOpt = pondService.getComment(id);
                if (commentOpt.isPresent()) {
                    return "redirect:/pond/post/" + commentOpt.get().getPostId() + "?error=" + e.getMessage();
                }
            } catch (Exception ignored) {
            }
            return "redirect:/pond?error=" + e.getMessage();
        }
    }

    /**
     * 删除动态
     */
    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            pondService.deletePost(id, userId);
            return "redirect:/pond";
        } catch (Exception e) {
            return "redirect:/pond?error=" + e.getMessage();
        }
    }
}

