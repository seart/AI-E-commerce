package com.jingdong.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.jingdong.backend.mapper")
public class JingdongBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(JingdongBackendApplication.class, args);
  }
}
