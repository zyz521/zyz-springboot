package com.example.xianyu.controller;

import com.example.xianyu.entity.Product;
import com.example.xianyu.entity.User;
import com.example.xianyu.service.MessageService;
import com.example.xianyu.service.PondService;
import com.example.xianyu.service.ProductService;
import com.example.xianyu.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class MessageController {

    private final MessageService messageService;
    private final ProductService productService;
    private final UserService userService;
    private final PondService pondService;

    public MessageController(MessageService messageService, ProductService productService, UserService userService, PondService pondService) {
        this.messageService = messageService;
        this.productService = productService;
        this.userService = userService;
        this.pondService = pondService;
    }

    /**
     * 消息中心
     */
    @GetMapping("/messages")
    public String messages(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        List<com.example.xianyu.entity.Message> all = messageService.listByUser(userId);
        model.addAttribute("unreadCount", messageService.countUnread(userId));
        model.addAttribute("currentUserId", userId);
        model.addAttribute("threads", buildThreads(all, userId));
        return "message/index";
    }

    /**
     * 将消息聚合为线程，避免同一对话多条卡片
     */
    private List<ThreadView> buildThreads(List<com.example.xianyu.entity.Message> list, Long currentUserId) {
        // 按创建时间降序
        list.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        Map<String, ThreadView> map = new LinkedHashMap<>();
        for (com.example.xianyu.entity.Message m : list) {
            // 系统消息单独按ID区分
            if (m.getSenderId() == null) {
                String key = "system-" + m.getId();
                map.putIfAbsent(key, ThreadView.fromMessage(m, currentUserId, 0));
                continue;
            }
            Long otherId = Objects.equals(m.getSenderId(), currentUserId) ? m.getUserId() : m.getSenderId();
            // 鱼塘消息按 postId 分组，商品消息按 productId 分组
            String key;
            if (m.getPostId() != null) {
                key = "pond-" + m.getPostId() + "-" + otherId;
            } else {
                key = "product-" + (m.getProductId() != null ? m.getProductId() : 0L) + "-" + otherId;
            }
            ThreadView tv = map.get(key);
            if (tv == null) {
                tv = ThreadView.fromMessage(m, currentUserId, 0);
                tv.setOtherUserId(otherId);
                map.put(key, tv);
            }
            // 未读数累加（仅计算当前用户收到的未读）
            if (m.getUserId().equals(currentUserId) && Boolean.FALSE.equals(m.getReadFlag())) {
                tv.setUnread(tv.getUnread() + 1);
            }
        }
        // 填充商品标题快照和帖子标题快照
        map.values().forEach(tv -> {
            if (tv.getProductId() != null && tv.getProductTitle() == null) {
                productService.findById(tv.getProductId()).ifPresent(p -> tv.setProductTitle(p.getTitle()));
            }
            if (tv.getPostId() != null && tv.getPostTitle() == null) {
                pondService.getPost(tv.getPostId()).ifPresent(p -> {
                    String content = p.getContent();
                    tv.setPostTitle(content != null && content.length() > 30 ? content.substring(0, 30) + "..." : content);
                });
            }
            if (tv.getOtherName() == null && tv.getOtherUserId() != null) {
                userService.findById(tv.getOtherUserId()).ifPresent(u -> tv.setOtherName(u.getUsername()));
            }
        });
        return new ArrayList<>(map.values());
    }

    private static class ThreadView {
        private Long lastId;
        private Long productId;
        private String productTitle;
        private Long postId;
        private String postTitle;
        private Long otherUserId;
        private String otherName;
        private String lastContent;
        private String lastType;
        private LocalDateTime lastTime;
        private int unread;

        static ThreadView fromMessage(com.example.xianyu.entity.Message m, Long currentUserId, int unread) {
            ThreadView tv = new ThreadView();
            tv.lastId = m.getId();
            tv.productId = m.getProductId();
            tv.postId = m.getPostId();
            tv.productTitle = m.getTitle();
            tv.postTitle = m.getTitle();
            tv.lastContent = m.getContent();
            tv.lastType = m.getType();
            tv.lastTime = m.getCreateTime();
            tv.unread = unread;
            Long otherId = Objects.equals(m.getSenderId(), currentUserId) ? m.getUserId() : m.getSenderId();
            tv.otherUserId = otherId;
            tv.otherName = m.getSenderName();
            return tv;
        }

        public Long getLastId() { return lastId; }
        public Long getProductId() { return productId; }
        public String getProductTitle() { return productTitle; }
        public Long getPostId() { return postId; }
        public String getPostTitle() { return postTitle; }
        public Long getOtherUserId() { return otherUserId; }
        public String getOtherName() { return otherName; }
        public String getLastContent() { return lastContent; }
        public String getLastType() { return lastType; }
        public LocalDateTime getLastTime() { return lastTime; }
        public int getUnread() { return unread; }

        public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
        public void setPostTitle(String postTitle) { this.postTitle = postTitle; }
        public void setOtherUserId(Long otherUserId) { this.otherUserId = otherUserId; }
        public void setOtherName(String otherName) { this.otherName = otherName; }
        public void setUnread(int unread) { this.unread = unread; }
    }

    /**
     * 标记单条已读
     */
    @PostMapping("/messages/read")
    public String markRead(@RequestParam Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        messageService.markRead(id, userId);
        return "redirect:/messages";
    }

    /**
     * 全部已读
     */
    @PostMapping("/messages/readAll")
    public Object markAllRead(HttpSession session,
                             jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok().body(java.util.Map.of("error", "未登录"));
            }
            return "redirect:/auth/login";
        }
        messageService.markAllRead(userId);
        if (isAjaxRequest(request)) {
            return ResponseEntity.ok().body(java.util.Map.of("success", true));
        }
        return "redirect:/messages";
    }

    /**
     * 从商品详情联系卖家
     */
    @PostMapping("/messages/send")
    public String sendMessage(@RequestParam Long productId,
                              @RequestParam String content,
                              HttpSession session,
                              Model model,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        if (userId == null || username == null) {
            return "redirect:/auth/login";
        }
        Product product = productService.findById(productId).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "商品不存在");
            return "redirect:/product/detail?id=" + productId;
        }
        // 不允许给自己发送
        if (product.getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "无法给自己发送消息");
            return "redirect:/product/detail?id=" + productId;
        }
        // 内容简单校验
        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "消息内容不能为空");
            return "redirect:/product/detail?id=" + productId;
        }
        messageService.sendChatMessage(userId, username, product.getUserId(),
                product.getId(), product.getTitle(), content.trim());
        return "redirect:/messages";
    }

    /**
     * 会话详情（双向聊天）
     */
    @GetMapping("/messages/thread")
    public String thread(@RequestParam Long productId,
                         @RequestParam("with") Long otherUserId,
                         HttpSession session,
                         Model model) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        if (userId == null || username == null) {
            return "redirect:/auth/login";
        }
        if (otherUserId.equals(userId)) {
            model.addAttribute("error", "无法与自己会话");
            return "redirect:/messages";
        }
        Product product = productService.findById(productId).orElse(null);
        if (product == null) {
            model.addAttribute("error", "商品不存在");
            return "redirect:/messages";
        }
        User other = userService.findById(otherUserId).orElse(null);
        if (other == null) {
            model.addAttribute("error", "用户不存在");
            return "redirect:/messages";
        }
        // 标记我收到的未读为已读
        messageService.markThreadRead(productId, userId, otherUserId);
        model.addAttribute("messages", messageService.listThread(productId, userId, otherUserId));
        model.addAttribute("product", product);
        model.addAttribute("otherUser", other);
        model.addAttribute("currentUserId", userId);
        return "message/thread";
    }

    /**
     * 会话内发送
     */
    @PostMapping("/messages/thread/send")
    public Object sendInThread(@RequestParam Long productId,
                               @RequestParam("with") Long otherUserId,
                               @RequestParam String content,
                               HttpSession session,
                               jakarta.servlet.http.HttpServletRequest request,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        boolean isAjax = isAjaxRequest(request);
        
        if (userId == null || username == null) {
            if (isAjax) {
                return ResponseEntity.ok().body(java.util.Map.of("error", "未登录"));
            }
            return "redirect:/auth/login";
        }
        if (otherUserId.equals(userId)) {
            if (isAjax) {
                return ResponseEntity.ok().body(java.util.Map.of("error", "无法与自己会话"));
            }
            redirectAttributes.addFlashAttribute("error", "无法与自己会话");
            return "redirect:/messages";
        }
        Product product = productService.findById(productId).orElse(null);
        if (product == null) {
            if (isAjax) {
                return ResponseEntity.ok().body(java.util.Map.of("error", "商品不存在"));
            }
            redirectAttributes.addFlashAttribute("error", "商品不存在");
            return "redirect:/messages";
        }
        User other = userService.findById(otherUserId).orElse(null);
        if (other == null) {
            if (isAjax) {
                return ResponseEntity.ok().body(java.util.Map.of("error", "用户不存在"));
            }
            redirectAttributes.addFlashAttribute("error", "用户不存在");
            return "redirect:/messages";
        }
        if (content == null || content.trim().isEmpty()) {
            if (isAjax) {
                return ResponseEntity.ok().body(java.util.Map.of("error", "消息内容不能为空"));
            }
            redirectAttributes.addFlashAttribute("error", "消息内容不能为空");
            return "redirect:/messages/thread?productId=" + productId + "&with=" + otherUserId;
        }
        messageService.sendChatMessage(userId, username, otherUserId, productId, product.getTitle(), content.trim());
        if (isAjax) {
            return ResponseEntity.ok().body(java.util.Map.of("success", true));
        }
        return "redirect:/messages/thread?productId=" + productId + "&with=" + otherUserId;
    }

    private boolean isAjaxRequest(jakarta.servlet.http.HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith);
    }

    /**
     * 获取对话消息（JSON API，用于轮询刷新）
     */
    @GetMapping("/messages/thread/api")
    @ResponseBody
    public java.util.Map<String, Object> threadApi(@RequestParam Long productId,
                                                    @RequestParam("with") Long otherUserId,
                                                    HttpSession session) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            result.put("error", "未登录");
            return result;
        }
        if (otherUserId.equals(userId)) {
            result.put("error", "无法与自己会话");
            return result;
        }
        // 标记我收到的未读为已读
        messageService.markThreadRead(productId, userId, otherUserId);
        java.util.List<com.example.xianyu.entity.Message> messages = messageService.listThread(productId, userId, otherUserId);
        // 转换为简单格式
        java.util.List<java.util.Map<String, Object>> msgList = new java.util.ArrayList<>();
        for (com.example.xianyu.entity.Message m : messages) {
            java.util.Map<String, Object> msg = new java.util.HashMap<>();
            msg.put("id", m.getId());
            msg.put("senderId", m.getSenderId());
            msg.put("senderName", m.getSenderName() != null ? m.getSenderName() : "系统");
            msg.put("content", m.getContent());
            msg.put("createTime", m.getCreateTime());
            msgList.add(msg);
        }
        result.put("messages", msgList);
        result.put("currentUserId", userId);
        return result;
    }

    /**
     * 获取消息中心数据（JSON API，用于轮询刷新）
     */
    @GetMapping("/messages/api")
    @ResponseBody
    public java.util.Map<String, Object> messagesApi(HttpSession session) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            result.put("error", "未登录");
            return result;
        }
        List<com.example.xianyu.entity.Message> all = messageService.listByUser(userId);
        long unreadCount = messageService.countUnread(userId);
        List<ThreadView> threads = buildThreads(all, userId);
        
        // 转换为JSON格式
        java.util.List<java.util.Map<String, Object>> threadList = new java.util.ArrayList<>();
        for (ThreadView tv : threads) {
            java.util.Map<String, Object> thread = new java.util.HashMap<>();
            thread.put("lastId", tv.getLastId());
            thread.put("productId", tv.getProductId());
            thread.put("productTitle", tv.getProductTitle());
            thread.put("postId", tv.getPostId());
            thread.put("postTitle", tv.getPostTitle());
            thread.put("otherUserId", tv.getOtherUserId());
            thread.put("otherName", tv.getOtherName());
            thread.put("lastContent", tv.getLastContent());
            thread.put("lastType", tv.getLastType());
            thread.put("lastTime", tv.getLastTime());
            thread.put("unread", tv.getUnread());
            threadList.add(thread);
        }
        
        result.put("unreadCount", unreadCount);
        result.put("threads", threadList);
        result.put("currentUserId", userId);
        return result;
    }

    /**
     * 获取未读消息数量（轻量级API，用于全局红点提示）
     */
    @GetMapping("/messages/unread-count")
    @ResponseBody
    public java.util.Map<String, Object> unreadCountApi(HttpSession session) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            result.put("unreadCount", 0);
            return result;
        }
        long unreadCount = messageService.countUnread(userId);
        result.put("unreadCount", unreadCount);
        return result;
    }

    /**
     * 删除消息线程
     */
    @PostMapping("/messages/delete")
    public Object deleteThread(@RequestParam(required = false) Long productId,
                              @RequestParam(required = false) Long postId,
                              @RequestParam(required = false) Long otherUserId,
                              HttpSession session,
                              jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok().body(java.util.Map.of("error", "未登录"));
            }
            return "redirect:/auth/login";
        }
        try {
            messageService.deleteThread(productId, postId, userId, otherUserId);
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok().body(java.util.Map.of("success", true));
            }
            return "redirect:/messages";
        } catch (Exception e) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok().body(java.util.Map.of("error", e.getMessage()));
            }
            return "redirect:/messages?error=" + e.getMessage();
        }
    }
}

