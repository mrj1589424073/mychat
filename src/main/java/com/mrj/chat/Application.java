package com.mrj.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import tk.mybatis.spring.annotation.MapperScan;

import javax.swing.*;

@SpringBootApplication
@MapperScan(basePackages = "com.mrj.chat.mapper")
public class Application {
    //通过这种方式来管理对象
    @Bean
    public SpringUtil getSpringUtil(){
        return new SpringUtil();
    }
    public static void main(String[] args) {
        //有网才能获取ip
        SpringApplication.run(Application.class,args);
    }
}
