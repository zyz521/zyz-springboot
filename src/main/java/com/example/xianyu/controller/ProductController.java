package com.example.xianyu.controller;

import com.example.xianyu.entity.Product;
import com.example.xianyu.service.CommentService;
import com.example.xianyu.service.FavoriteService;
import com.example.xianyu.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
@Controller
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;
    private final CommentService commentService;
    private final FavoriteService favoriteService;
    @Value("${upload.dir:${user.dir}/uploads}")
    private String uploadDir;

    public ProductController(ProductService productService,
                             CommentService commentService,
                             FavoriteService favoriteService) {
        this.productService = productService;
        this.commentService = commentService;
        this.favoriteService = favoriteService;
    }

    // 商品列表：支持关键字和分类筛选
    @GetMapping("/list")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String category,
                       Model model) {
        List<Product> products = productService.listOnSale(keyword, category);
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        return "product/list";
    }

    // 商品详情
    @GetMapping("/detail")
    public String detail(@RequestParam("id") Long id, Model model) {
        return productService.findById(id)
                .map(product -> {
                    model.addAttribute("product", product);
                    model.addAttribute("comments", commentService.listByProduct(id));
                    return "product/detail";
                })
                .orElse("redirect:/product/list");
    }
    // 我的发布（当前用户的所有商品）
    @GetMapping("/my")
    public String myProducts(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        List<Product> products = productService.listByUser(userId);
        model.addAttribute("products", products);
        return "product/my-list";
    }
    // 发布页面
    @GetMapping("/publish")
    public String publishPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/auth/login";
        }
        // 清除之前的错误信息（如果有）
        if (!model.containsAttribute("error")) {
            model.addAttribute("error", null);
        }
        return "product/publish";
    }


    // 提交发布
    @PostMapping("/publish")
    public String publish(@RequestParam String title,
                          @RequestParam String description,
                          @RequestParam BigDecimal price,
                          @RequestParam(required = false) String category,
                          @RequestParam(value = "images", required = false) MultipartFile[] images,
                          HttpSession session,
                          Model model) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        if (userId == null || username == null) {
            return "redirect:/auth/login";
        }
        // 验证必须上传至少一张图片
        if (images == null || images.length == 0) {
            model.addAttribute("error", "请至少上传一张商品图片");
            return "product/publish";
        }
        
        // 验证图片数量
        if (images.length > 9) {
            model.addAttribute("error", "最多只能上传9张图片");
            return "product/publish";
        }
        
        try {
            StringBuilder imageUrls = new StringBuilder();
            // 图片保存到 products/用户名/ 目录
            Path uploadPath = Paths.get(uploadDir, "products", username).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            int validImageCount = 0;
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String original = image.getOriginalFilename();
                    if (original != null) {// 验证文件格式
                        String lower = original.toLowerCase();
                        if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif"))) {
                            model.addAttribute("error", "图片格式仅支持 JPG/PNG/GIF");
                            return "product/publish";
                        }

                        // 验证文件大小（5MB）
                        if (image.getSize() > 5 * 1024 * 1024) {
                            model.addAttribute("error", "图片大小不能超过5MB");
                            return "product/publish";
                        }

                        // 保存文件
                        String ext = original.substring(original.lastIndexOf("."));
                        String newName = UUID.randomUUID().toString() + ext;
                        Path filePath = uploadPath.resolve(newName);
                        image.transferTo(filePath.toFile());

                        // 拼接URL（包含用户名路径）
                        if (imageUrls.length() > 0) {
                            imageUrls.append(",");
                        }
                        imageUrls.append("/uploads/products/").append(username).append("/").append(newName);
                        validImageCount++;
                    }
                }
            }
            
            // 再次验证：确保至少有一张有效图片
            if (validImageCount == 0) {
                model.addAttribute("error", "请至少上传一张有效的商品图片");
                return "product/publish";
            }

            String imageUrlStr = imageUrls.toString();
            productService.publish(userId, title, description, price, category, imageUrlStr);
            return "redirect:/product/list";
        } catch (IOException e) {
            model.addAttribute("error", "图片上传失败：" + e.getMessage());
            return "product/publish";
        } catch (Exception e) {
            model.addAttribute("error", "发布失败：" + e.getMessage());
            return "product/publish";
        }
    }
    // 下架商品
    @PostMapping("/off")
    public String off(@RequestParam Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        Optional<Product> optional = productService.findById(id);
        if (optional.isPresent() && optional.get().getUserId().equals(userId)) {
            Product product = optional.get();
            // 先删除图片文件
            deleteProductImages(product);
            // 清空图片URL，避免数据不一致
            product.setImageUrl(null);
            // 再下架商品
            productService.offShelf(product);
        }
        return "redirect:/product/my";
    }

    /**
     * 删除商品图片文件
     */
    private void deleteProductImages(Product product) {
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                // 图片URL格式：/uploads/products/用户名/文件名 或 /uploads/products/文件名（旧格式）
                String[] imageUrls = product.getImageUrl().split(",");
                for (String imageUrl : imageUrls) {
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        // 移除开头的 /uploads，转换为文件系统路径
                        String relativePath = imageUrl.startsWith("/uploads/") 
                            ? imageUrl.substring("/uploads/".length()) 
                            : imageUrl;
                        Path imagePath = Paths.get(uploadDir, relativePath).toAbsolutePath();
                        if (Files.exists(imagePath)) {
                            Files.delete(imagePath);
                        }
                    }
                }
            } catch (IOException e) {
                // 记录日志，但不影响删除流程
                System.err.println("删除商品图片失败: " + e.getMessage());
            }
        }
    }

    // 删除商品
    @PostMapping("/delete")
    public String delete(@RequestParam Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        Optional<Product> optional = productService.findById(id);
        if (optional.isPresent() && optional.get().getUserId().equals(userId)) {
            Product product = optional.get();
            // 先删除图片文件
            deleteProductImages(product);
            // 再删除数据库记录
            productService.delete(product);
        }
        return "redirect:/product/my";
    }
    // 批量删除商品
    @PostMapping("/deleteBatch")
    public String deleteBatch(@RequestParam(value = "ids", required = false) List<Long> ids,
                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        if (ids != null) {
            for (Long id : ids) {
                Optional<Product> optional = productService.findById(id);
                if (optional.isPresent() && optional.get().getUserId().equals(userId)) {
                    Product product = optional.get();
                    // 先删除图片文件
                    deleteProductImages(product);
                    // 再删除数据库记录
                    productService.delete(product);
                }
            }
        }
        return "redirect:/product/my";
    }
    // 商品评论
    @PostMapping("/comment")
    public String comment(@RequestParam Long productId,
                          @RequestParam String content,
                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        if (userId == null || username == null) {
            return "redirect:/auth/login";
        }

        commentService.addComment(productId, userId, username, content);
        return "redirect:/product/detail?id=" + productId;
    }

    // 商品收藏
    @PostMapping("/favorite")
    public String favorite(@RequestParam Long productId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        favoriteService.addFavorite(userId, productId);
        return "redirect:/product/detail?id=" + productId;
    }

    // 编辑页面
    @GetMapping("/edit")
    public String editPage(@RequestParam Long id, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        Optional<Product> optional = productService.findById(id);
        if (optional.isEmpty()) {
            return "redirect:/product/my";
        }
        Product product = optional.get();
        // 验证商品是否属于当前用户
        if (!product.getUserId().equals(userId)) {
            return "redirect:/product/my";
        }
        model.addAttribute("product", product);
        if (!model.containsAttribute("error")) {
            model.addAttribute("error", null);
        }
        return "product/edit";
    }

    // 提交编辑
    @PostMapping("/update")
    public String update(@RequestParam Long id,
                        @RequestParam String title,
                        @RequestParam String description,
                        @RequestParam BigDecimal price,
                        @RequestParam(required = false) String category,
                        @RequestParam(value = "images", required = false) MultipartFile[] images,
                        @RequestParam(value = "keepImages", required = false) String keepImages,
                        HttpSession session,
                        Model model) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        if (userId == null || username == null) {
            return "redirect:/auth/login";
        }
        
        Optional<Product> optional = productService.findById(id);
        if (optional.isEmpty()) {
            return "redirect:/product/my";
        }
        Product product = optional.get();
        // 验证商品是否属于当前用户
        if (!product.getUserId().equals(userId)) {
            return "redirect:/product/my";
        }

        try {
            String imageUrlStr = null;
            
            // 处理图片：如果有新上传的图片，使用新图片；否则保留原有图片
            if (images != null && images.length > 0) {
                // 验证图片数量
                int validImageCount = 0;
                for (MultipartFile image : images) {
                    if (image != null && !image.isEmpty()) {
                        validImageCount++;
                    }
                }
                
                if (validImageCount > 9) {
                    model.addAttribute("error", "最多只能上传9张图片");
                    model.addAttribute("product", product);
                    return "product/edit";
                }
                
                if (validImageCount > 0) {
                    // 删除旧图片（如果用户上传了新图片）
                    deleteProductImages(product);
                    
                    // 保存新图片
                    StringBuilder imageUrls = new StringBuilder();
                    Path uploadPath = Paths.get(uploadDir, "products", username).toAbsolutePath();
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    for (MultipartFile image : images) {
                        if (image != null && !image.isEmpty()) {
                            String original = image.getOriginalFilename();
                            if (original != null) {
                                // 验证文件格式
                                String lower = original.toLowerCase();
                                if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif"))) {
                                    model.addAttribute("error", "图片格式仅支持 JPG/PNG/GIF");
                                    model.addAttribute("product", product);
                                    return "product/edit";
                                }

                                // 验证文件大小（5MB）
                                if (image.getSize() > 5 * 1024 * 1024) {
                                    model.addAttribute("error", "图片大小不能超过5MB");
                                    model.addAttribute("product", product);
                                    return "product/edit";
                                }

                                // 保存文件
                                String ext = original.substring(original.lastIndexOf("."));
                                String newName = UUID.randomUUID().toString() + ext;
                                Path filePath = uploadPath.resolve(newName);
                                image.transferTo(filePath.toFile());

                                // 拼接URL
                                if (imageUrls.length() > 0) {
                                    imageUrls.append(",");
                                }
                                imageUrls.append("/uploads/products/").append(username).append("/").append(newName);
                            }
                        }
                    }
                    imageUrlStr = imageUrls.toString();
                }
            }
            
            // 如果没有上传新图片，使用保留的图片URL
            if (imageUrlStr == null || imageUrlStr.isEmpty()) {
                if (keepImages != null && !keepImages.isEmpty()) {
                    imageUrlStr = keepImages;
                } else {
                    // 如果没有新图片也没有保留的图片，保留原有图片
                    imageUrlStr = product.getImageUrl();
                }
            }
            
            // 验证至少有一张图片
            if (imageUrlStr == null || imageUrlStr.isEmpty()) {
                model.addAttribute("error", "请至少保留或上传一张商品图片");
                model.addAttribute("product", product);
                return "product/edit";
            }

            productService.update(product, title, description, price, category, imageUrlStr);
            return "redirect:/product/my";
        } catch (IOException e) {
            model.addAttribute("error", "图片上传失败：" + e.getMessage());
            model.addAttribute("product", product);
            return "product/edit";
        } catch (Exception e) {
            model.addAttribute("error", "更新失败：" + e.getMessage());
            model.addAttribute("product", product);
            return "product/edit";
        }
    }
}