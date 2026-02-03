package com.example.xianyu.controller;

import com.example.xianyu.entity.OrderInfo;
import com.example.xianyu.entity.Product;
import com.example.xianyu.service.OrderService;
import com.example.xianyu.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;

    public OrderController(OrderService orderService, ProductService productService) {
        this.orderService = orderService;
        this.productService = productService;
    }


    // 订单确认页面
    @GetMapping("/confirm/{productId}")
    public String confirm(@PathVariable Long productId, HttpSession session, Model model) {
        Long buyerId = (Long) session.getAttribute("userId");
        if (buyerId == null) {
            return "redirect:/auth/login";
        }
        Product product = productService.findById(productId)
                .orElse(null);
        if (product == null) {
            model.addAttribute("error", "商品不存在");
            return "redirect:/product/list";
        }
        // 检查商品是否在售
        if (product.getStatus() != 0) {
            model.addAttribute("error", "商品已下架或已售出");
            return "redirect:/product/detail?id=" + productId;
        }
        // 检查是否是自己发布的商品
        if (product.getUserId().equals(buyerId)) {
            model.addAttribute("error", "不能购买自己发布的商品");
            return "redirect:/product/detail?id=" + productId;
        }
        model.addAttribute("product", product);
        return "order/confirm";
    }

    // 创建订单（下单）
    @PostMapping("/create/{productId}")
    public String create(@PathVariable Long productId, HttpSession session, Model model) {
        Long buyerId = (Long) session.getAttribute("userId");
        if (buyerId == null) {
            return "redirect:/auth/login";
        }
        Product product = productService.findById(productId)
                .orElse(null);
        if (product == null) {
            model.addAttribute("error", "商品不存在");
            return "redirect:/product/list";
        }
        // 检查商品是否在售
        if (product.getStatus() != 0) {
            model.addAttribute("error", "商品已下架或已售出");
            return "redirect:/product/detail?id=" + productId;
        }
        // 检查是否是自己发布的商品
        if (product.getUserId().equals(buyerId)) {
            model.addAttribute("error", "不能购买自己发布的商品");
            return "redirect:/product/detail?id=" + productId;
        }
        // 创建订单记录（状态为待付款）
        OrderInfo order = orderService.createOrder(buyerId, product);
        return "redirect:/order/pay/" + order.getId();
    }

    // 支付页面
    @GetMapping("/pay/{orderId}")
    public String payPage(@PathVariable Long orderId, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        OrderInfo order = orderService.findById(orderId)
                .orElse(null);
        if (order == null) {
            model.addAttribute("error", "订单不存在");
            return "redirect:/order/my";
        }
        // 检查订单是否属于当前用户
        if (!order.getBuyerId().equals(userId)) {
            model.addAttribute("error", "无权访问此订单");
            return "redirect:/order/my";
        }
        // 检查订单状态
        if (order.getStatus() != 0) {
            model.addAttribute("error", "订单状态不正确");
            return "redirect:/order/my";
        }
        Product product = productService.findById(order.getProductId())
                .orElse(null);
        if (product == null) {
            model.addAttribute("error", "商品不存在");
            return "redirect:/order/my";
        }
        model.addAttribute("order", order);
        model.addAttribute("product", product);
        return "order/pay";
    }

    // 支付订单
    @PostMapping("/pay/{orderId}")
    public String pay(@PathVariable Long orderId, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        try {
            OrderInfo order = orderService.findById(orderId)
                    .orElse(null);
            if (order == null) {
                model.addAttribute("error", "订单不存在");
                return "redirect:/order/my";
            }
            // 检查订单是否属于当前用户
            if (!order.getBuyerId().equals(userId)) {
                model.addAttribute("error", "无权访问此订单");
                return "redirect:/order/my";
            }
            // 支付订单
            orderService.payOrder(orderId);
            // 标记商品为已售
            Product product = productService.findById(order.getProductId())
                    .orElse(null);
            if (product != null) {
                productService.markSold(product);
            }
            return "redirect:/order/detail/" + orderId;
        } catch (Exception e) {
            model.addAttribute("error", "支付失败：" + e.getMessage());
            return "redirect:/order/pay/" + orderId;
        }
    }

    // 订单详情
    @GetMapping("/detail/{orderId}")
    public String detail(@PathVariable Long orderId, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        OrderInfo order = orderService.findById(orderId)
                .orElse(null);
        if (order == null) {
            model.addAttribute("error", "订单不存在");
            return "redirect:/order/my";
        }
        // 检查订单是否属于当前用户
        if (!order.getBuyerId().equals(userId)) {
            model.addAttribute("error", "无权访问此订单");
            return "redirect:/order/my";
        }
        Product product = productService.findById(order.getProductId())
                .orElse(null);
        model.addAttribute("order", order);
        model.addAttribute("product", product);
        return "order/detail";
    }

    // 完成订单
    @PostMapping("/complete/{orderId}")
    public String complete(@PathVariable Long orderId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        try {
            orderService.completeOrder(orderId, userId);
            return "redirect:/order/detail/" + orderId;
        } catch (Exception e) {
            return "redirect:/order/my?error=" + e.getMessage();
        }
    }

    // 取消订单
    @PostMapping("/cancel/{orderId}")
    public String cancel(@PathVariable Long orderId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        try {
            orderService.cancelOrder(orderId, userId);
            return "redirect:/order/my";
        } catch (Exception e) {
            return "redirect:/order/my?error=" + e.getMessage();
        }
    }

    // 删除订单
    @PostMapping("/delete/{orderId}")
    public Object deleteOrder(@PathVariable Long orderId, 
                             HttpSession session,
                             jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            if (isAjaxRequest(request)) {
                return org.springframework.http.ResponseEntity.ok().body(java.util.Map.of("error", "未登录"));
            }
            return "redirect:/auth/login";
        }
        try {
            orderService.deleteOrder(orderId, userId);
            if (isAjaxRequest(request)) {
                return org.springframework.http.ResponseEntity.ok().body(java.util.Map.of("success", true));
            }
            return "redirect:/order/my";
        } catch (Exception e) {
            if (isAjaxRequest(request)) {
                return org.springframework.http.ResponseEntity.ok().body(java.util.Map.of("error", e.getMessage()));
            }
            return "redirect:/order/my?error=" + e.getMessage();
        }
    }

    private boolean isAjaxRequest(jakarta.servlet.http.HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith);
    }

    // 批量删除订单
    @PostMapping("/deleteBatch")
    public Object deleteBatch(@RequestParam(value = "ids", required = false) List<Long> ids,
                             HttpSession session,
                             jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            if (isAjaxRequest(request)) {
                return org.springframework.http.ResponseEntity.ok().body(java.util.Map.of("error", "未登录"));
            }
            return "redirect:/auth/login";
        }
        try {
            if (ids != null && !ids.isEmpty()) {
                for (Long orderId : ids) {
                    try {
                        orderService.deleteOrder(orderId, userId);
                    } catch (Exception e) {
                        // 继续删除其他订单，不中断
                        System.err.println("删除订单失败: " + orderId + ", " + e.getMessage());
                    }
                }
            }
            if (isAjaxRequest(request)) {
                return org.springframework.http.ResponseEntity.ok().body(java.util.Map.of("success", true));
            }
            return "redirect:/order/my";
        } catch (Exception e) {
            if (isAjaxRequest(request)) {
                return org.springframework.http.ResponseEntity.ok().body(java.util.Map.of("error", e.getMessage()));
            }
            return "redirect:/order/my?error=" + e.getMessage();
        }
    }

    // 我买到的
    @GetMapping("/my")
    public String myOrders(HttpSession session, Model model) {
        Long buyerId = (Long) session.getAttribute("userId");
        if (buyerId == null) {
            return "redirect:/auth/login";
        }
        List<OrderInfo> orders = orderService.listByBuyer(buyerId);
        // 为每个订单加载商品信息
        java.util.Map<Long, Product> productMap = new java.util.HashMap<>();
        for (OrderInfo order : orders) {
            productService.findById(order.getProductId()).ifPresent(product -> 
                productMap.put(order.getProductId(), product));
        }
        model.addAttribute("orders", orders);
        model.addAttribute("productMap", productMap);
        return "order/list";
    }
}


