package com.jingdong.backend.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/static/category")
public class StaticAssetController {
  // 本地静态占位资源接口，给分类图标等演示资源返回简单 SVG。

  @GetMapping(value = "/{categoryId}.png", produces = "image/svg+xml")
  public ResponseEntity<byte[]> categoryIcon(@PathVariable String categoryId) {
    String label = switch (categoryId) {
      case "supermarket" -> "超";
      case "market" -> "菜";
      case "fruit" -> "果";
      case "flower" -> "花";
      case "health" -> "药";
      case "home" -> "家";
      case "cake" -> "糕";
      case "checkin" -> "签";
      case "brand" -> "牌";
      case "coupon" -> "券";
      default -> "京";
    };
    String svg = """
        <svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96">
          <rect width="96" height="96" rx="24" fill="#fff0eb"/>
          <circle cx="48" cy="48" r="30" fill="#e1251b"/>
          <text x="48" y="57" font-size="28" text-anchor="middle" fill="#ffffff" font-family="Arial, sans-serif">%s</text>
        </svg>
        """.formatted(label);

    return ResponseEntity.ok()
        .contentType(MediaType.valueOf("image/svg+xml"))
        .body(svg.getBytes(StandardCharsets.UTF_8));
  }
}
