package com.example.xianyu.service;

import com.example.xianyu.entity.Product;
import com.example.xianyu.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product publish(Long userId, String title, String description,
                           BigDecimal price, String category, String imageUrl) {
        Product p = new Product();
        p.setUserId(userId);
        p.setTitle(title);
        p.setDescription(description);
        p.setPrice(price);
        p.setCategory(category);
        p.setImageUrl(imageUrl);
        return productRepository.save(p);
    }

    public List<Product> listOnSale(String keyword, String category) {
        // 优先按分类过滤，其次才是关键字搜索
        if (category != null && !category.isEmpty()) {
            return productRepository.findByCategoryAndStatus(category, 0);
        }
        if (keyword != null && !keyword.isEmpty()) {
            return productRepository.findByTitleContainingAndStatus(keyword, 0);
        }
        return productRepository.findByStatus(0);
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Product markSold(Product product) {
        product.setStatus(1);
        return productRepository.save(product);
    }
    /**
     * 当前登录用户发布的所有商品（按时间倒序）
     */
    /**
     * 首页推荐商品 - 综合推荐算法
     * 推荐策略：
     * 1. 优先选择最新发布的在售商品（时间权重：50%）
     * 2. 必须有图片（必须条件）
     * 3. 价格在合理范围内（价格权重：20%）
     * 4. 商品描述完整性（描述权重：15%）
     * 5. 多图商品优先（图片数量权重：10%）
     * 6. 加入随机性，避免总是显示相同商品（随机权重：5%）
     * 7. 最多返回8个商品用于轮播
     */
    public List<Product> listHomeRecommend() {
        // 获取所有在售且有图片的商品
        List<Product> candidates = productRepository.findByStatusAndImageUrlIsNotNull(0);
        
        if (candidates.isEmpty()) {
            return candidates;
        }
        
        // 严格过滤：只保留有有效图片的商品
        // 1. imageUrl 不为 null
        // 2. imageUrl 去除空格后不为空
        // 3. imageUrl 必须包含有效的图片路径（包含 /uploads/ 或图片文件扩展名）
        candidates = candidates.stream()
                .filter(p -> {
                    String imageUrl = p.getImageUrl();
                    if (imageUrl == null || imageUrl.trim().isEmpty()) {
                        return false;
                    }
                    String trimmed = imageUrl.trim();
                    // 检查是否包含有效的图片路径标识
                    // 必须包含 /uploads/ 路径，或者包含图片文件扩展名
                    boolean hasValidPath = trimmed.contains("/uploads/");
                    boolean hasImageExtension = trimmed.matches(".*\\.(jpg|jpeg|png|gif|webp)(,.*)?");
                    return hasValidPath || hasImageExtension;
                })
                .collect(java.util.stream.Collectors.toList());
        
        if (candidates.isEmpty()) {
            return candidates;
        }
        
        // 计算每个商品的推荐分数
        long currentTime = System.currentTimeMillis();
        double maxPrice = candidates.stream()
                .mapToDouble(p -> p.getPrice() != null ? p.getPrice().doubleValue() : 0)
                .max()
                .orElse(10000);
        double minPrice = candidates.stream()
                .mapToDouble(p -> p.getPrice() != null ? p.getPrice().doubleValue() : 0)
                .min()
                .orElse(0);
        double priceRange = maxPrice - minPrice;
        
        // 按综合评分排序并限制数量
        return candidates.stream()
                .sorted((p1, p2) -> {
                    // 重新计算评分用于排序
                    double score1 = calculateRecommendScore(p1, currentTime, minPrice, maxPrice, priceRange);
                    double score2 = calculateRecommendScore(p2, currentTime, minPrice, maxPrice, priceRange);
                    return Double.compare(score2, score1); // 降序
                })
                .limit(8)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 计算商品推荐分数
     * 评分维度：
     * - 时间新鲜度（50%）：越新发布的商品分数越高，使用指数衰减
     * - 价格合理性（20%）：价格在中等偏下区间得分更高
     * - 描述完整性（15%）：有详细描述的商品优先
     * - 图片数量（10%）：多图商品优先展示
     * - 随机性（5%）：增加多样性，避免总是相同商品
     */
    private double calculateRecommendScore(Product product, long currentTime, 
                                          double minPrice, double maxPrice, double priceRange) {
        double score = 0.0;
        
        // 1. 时间新鲜度评分（50%权重）- 越新分数越高，使用指数衰减
        if (product.getCreateTime() != null) {
            long timeDiff = currentTime - product.getCreateTime()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();
            // 3天内的商品给高分，超过3天按指数衰减
            long threeDays = 3L * 24 * 60 * 60 * 1000;
            double timeScore = Math.max(0, Math.exp(-(double) timeDiff / (threeDays * 2)));
            score += timeScore * 0.5;
        }
        
        // 2. 价格合理性评分（20%权重）- 价格在中等偏下区间得分更高
        if (product.getPrice() != null && priceRange > 0) {
            double price = product.getPrice().doubleValue();
            double priceRatio = (price - minPrice) / priceRange;
            // 价格在0.2-0.6区间得分最高（中等偏下价格）
            double priceScore = 1.0 - Math.abs(priceRatio - 0.4) * 2;
            priceScore = Math.max(0, Math.min(1, priceScore));
            score += priceScore * 0.2;
        }
        
        // 3. 描述完整性评分（15%权重）- 有详细描述的商品优先
        if (product.getDescription() != null && !product.getDescription().trim().isEmpty()) {
            int descLength = product.getDescription().length();
            // 描述长度在50-200字之间得分最高
            double descScore = 1.0;
            if (descLength < 50) {
                descScore = descLength / 50.0;
            } else if (descLength > 200) {
                descScore = Math.max(0.8, 1.0 - (descLength - 200) / 500.0);
            }
            score += descScore * 0.15;
        }
        
        // 4. 图片数量评分（10%权重）- 多图商品优先展示
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            int imageCount = product.getImageUrl().split(",").length;
            // 图片数量越多分数越高，最多3张图给满分
            double imageScore = Math.min(1.0, imageCount / 3.0);
            score += imageScore * 0.1;
        }
        
        // 5. 随机性评分（5%权重）- 增加多样性
        score += Math.random() * 0.05;
        
        return score;
    }

    public List<Product> listByUser(Long userId) {
        return productRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 将商品标记为下架（仅本人可操作）
     */
    public Product offShelf(Product product) {
        product.setStatus(2);
        return productRepository.save(product);
    }

    /**
     * 删除商品（仅本人可操作）
     */
    public void delete(Product product) {
        productRepository.delete(product);
    }

    /**
     * 更新商品信息（仅本人可操作）
     */
    public Product update(Product product, String title, String description,
                         BigDecimal price, String category, String imageUrl) {
        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        // 如果提供了新图片，则更新图片URL
        if (imageUrl != null && !imageUrl.isEmpty()) {
            product.setImageUrl(imageUrl);
        }
        return productRepository.save(product);
    }
}


