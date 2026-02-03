package com.example.xianyu.controller;

import com.example.xianyu.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class HomeController {

    private final ProductService productService;

    @Value("${upload.dir:${user.dir}/uploads}")
    private String uploadDir;

    public HomeController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        // 首页推荐轮播：从用户已发布的在售商品中选择最新的几件
        model.addAttribute("recommendProducts", productService.listHomeRecommend());
        
        // 展示所有在售商品（按时间倒序）
        model.addAttribute("allProducts", productService.listOnSale(null, null));

        // 兜底方案：从 uploads/products 目录中读取图片，用于当数据库无商品时的轮播展示
        try {
            Path productsDir = Paths.get(uploadDir, "products").toAbsolutePath();
            if (Files.exists(productsDir)) {
                try (Stream<Path> stream = Files.list(productsDir)) {
                    List<String> recommendImages = stream
                            .filter(Files::isRegularFile)
                            .sorted((p1, p2) -> {
                                try {
                                    // 新文件排前面
                                    return Files.getLastModifiedTime(p2)
                                            .compareTo(Files.getLastModifiedTime(p1));
                                } catch (IOException e) {
                                    return 0;
                                }
                            })
                            .limit(8)
                            .map(path -> "/uploads/products/" + path.getFileName().toString())
                            .collect(Collectors.toList());
                    model.addAttribute("recommendImages", recommendImages);
                }
            }
        } catch (IOException ignored) {
            // 静默失败，不影响首页其他内容
        }

        return "index";
    }
}


