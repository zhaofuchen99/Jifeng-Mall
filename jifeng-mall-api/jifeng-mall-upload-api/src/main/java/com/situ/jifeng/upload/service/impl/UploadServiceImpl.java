package com.situ.jifeng.upload.service.impl;

import cn.hutool.core.util.IdUtil;
import com.situ.jifeng.spi.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class UploadServiceImpl implements UploadService {
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

    //文件上传本地路径
    @Value("${my.upload.location}")
    private String uploadLocation;

    //访问前缀
    @Value("${my.upload.url-prefix}")
    private String urlPrefix;
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String upload(MultipartFile file, String type) {
        File dir = new File(uploadLocation + File.separator + type);
        if (!dir.exists()) {
            boolean b = dir.mkdirs();
            if (!b) {
                throw new RuntimeException("创建目录失败");
            }
        }

        String fileName = file.getOriginalFilename();//上传文件名
        assert fileName != null;
        int idx = fileName.lastIndexOf(".");

        //文件扩展名，包括点
        String ext = fileName.substring(idx);
        //完整文件名
        fileName = IdUtil.getSnowflakeNextIdStr() + ext;

        try {
            File target = new File(dir.getCanonicalPath() + File.separator + fileName);
            file.transferTo(target);//传输文件

            if (urlPrefix.endsWith("/")) {
                urlPrefix = urlPrefix.substring(0, urlPrefix.length() - 1);
            }

            String url = urlPrefix + "/" + type + "/" + fileName;

            //定时删除任务，用于删除已上传，但未使用到的图片
            scheduledExecutorService.schedule(() -> {
                if (!redisTemplate.hasKey(url)) {
                    var _ = target.delete();//删除文件
                }
            }, 5, TimeUnit.MINUTES);

            return url;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
