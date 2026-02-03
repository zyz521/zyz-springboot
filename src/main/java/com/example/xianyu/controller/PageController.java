package com.example.xianyu.controller;

import com.example.xianyu.entity.Product;
import com.example.xianyu.entity.User;
import com.example.xianyu.service.ProductService;
import com.example.xianyu.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Controller
public class PageController {
    private final UserService userService;
    private final ProductService productService;
    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public PageController(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
    }


    /**
     * 个人中心（整合个人资料和我的发布）
     */
    @GetMapping("/user/center")
    public String center(HttpSession session, Model model, @RequestParam(required = false) String success) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("username", session.getAttribute("username"));
        // 获取用户信息，包括头像
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("avatar", user.getAvatar());
        }
        // 获取用户发布的商品列表
        List<Product> products = productService.listByUser(userId);
        model.addAttribute("products", products);
        
        // 处理成功消息
        if (success != null) {
            model.addAttribute("success", success);
        }

        return "user/center";
    }

    /**
     * 个人资料页面（保留原有功能）
     */
    @GetMapping("/user/profile")
    public String profile(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("username", session.getAttribute("username"));
        // 获取用户信息，包括头像
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("avatar", user.getAvatar());
        }

        return "user/profile";
    }

/**
 * 上传头像
  */
    @PostMapping("/user/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file,
                               HttpSession session,
                               Model model) {
        Long userId = (Long) session.getAttribute("userId");
          if (userId == null) {
              return "redirect:/auth/login";
          }
          if (file.isEmpty()) {
              model.addAttribute("error", "请选择要上传的文件");
              return profile(session, model);
          }
          // 验证文件类型<br/>
          String originalFilename = file.getOriginalFilename();
          if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".jpg") &&
                  !originalFilename.toLowerCase().endsWith(".jpeg") &&
                  !originalFilename.toLowerCase().endsWith(".png") &&
                  !originalFilename.toLowerCase().endsWith(".gif"))) {
              model.addAttribute("error", "只支持 JPG、PNG、GIF 格式的图片");
                     return profile(session, model);
          }
          try {
              // 创建上传目录
              Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
              if (!Files.exists(uploadPath)) {
                  Files.createDirectories(uploadPath);
              }
              // 生成唯一文件名
              String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
              String newFilename = UUID.randomUUID().toString() + fileExtension;
              Path filePath = uploadPath.resolve(newFilename);
              // 保存文件
              file.transferTo(filePath.toFile());
              // 保存文件路径到数据库（相对路径，用于前端访问）
              String avatarPath = "/uploads/" + newFilename;
              userService.updateAvatar(userId, avatarPath);
              // 更新session中的用户信息
              Optional<User> userOpt = userService.findById(userId);
              if (userOpt.isPresent()) {
                  session.setAttribute("avatar", avatarPath);
              }
              return "redirect:/user/profile";
          } catch (IOException e) {
              model.addAttribute("error", "文件上传失败：" + e.getMessage());
              return profile(session, model);
          }
    }

    /**
     * 修改密码页面
     */
    @GetMapping("/user/change-password")
    public String changePasswordPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("username", session.getAttribute("username"));
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("avatar", user.getAvatar());
        }
        return "user/change-password";
    }

    /**
     * 处理修改密码请求
     */
    @PostMapping("/user/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 HttpSession session,
                                 Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        // 验证新密码和确认密码是否一致
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "新密码和确认密码不一致");
            return changePasswordPage(session, model);
        }

        // 验证新密码长度
        if (newPassword.length() < 6) {
            model.addAttribute("error", "新密码长度至少为6位");
            return changePasswordPage(session, model);
        }

        // 调用服务层修改密码
        boolean success = userService.changePassword(userId, oldPassword, newPassword);
        if (success) {
            model.addAttribute("success", "密码修改成功");
            return changePasswordPage(session, model);
        } else {
            model.addAttribute("error", "旧密码错误");
            return changePasswordPage(session, model);
        }
    }
}
