package com.situ.jifeng.upload.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 把上传目录暴露为静态资源，使上传后返回的 URL 能被访问到。
 *
 * <p><b>补的什么坑：</b>{@link com.situ.jifeng.upload.service.impl.UploadServiceImpl}
 * 把文件写到 {@code my.upload.location}（默认 {@code d:/shoplook2026/upload/}）并返回
 * {@code http://localhost:10020/upload/{type}/{雪花id}.{ext}}，但本服务此前<b>没有任何静态资源映射</b>，
 * 而该目录也不在 classpath 里。于是访问返回的 URL 时 DispatcherServlet 找不到 handler，
 * 抛异常后被 {@code GlobalExceptionHandler} 兜底成 {@code {"code":500,"msg":"系统繁忙"}}——
 * 表现为<b>上传接口 200、文件也真落盘了，但图片永远打不开</b>，排查时极易误判成上传失败。</p>
 *
 * <p>会员头像、品牌 Logo、商品主图/详情图都依赖这个映射，缺了它整个图片链路是断的。</p>
 */
@Configuration
public class UploadWebMvcConfig implements WebMvcConfigurer {

    @Value("${my.upload.location}")
    private String uploadLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // file: 前缀表示指向文件系统的物理路径（不是 classpath）；
        // 结尾必须是 "/"，否则 Spring 会把最后一段当成文件名而非目录
        String location = uploadLocation.endsWith("/") ? uploadLocation : uploadLocation + "/";
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + location);
    }
}
