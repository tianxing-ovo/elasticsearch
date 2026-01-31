package com.ltx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 *
 * @author tianxing
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 添加视图控制器
     *
     * @param registry 视图控制器注册对象
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/html/index.html");
        registry.addViewController("/admin").setViewName("forward:/html/admin.html");
    }
}
