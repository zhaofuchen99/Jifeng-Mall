package com.situ.jifeng.upload.api;

import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.spi.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口（依据设计文档 8.2/8.3：POST /upload，multipart）。
 *
 * <p>上传后返回可访问 URL；用于品牌 Logo、商品主图/详情图、会员头像等。</p>
 */
@RestController
public class UploadApi {

    /** 允许的图片扩展名 */
    private static final String[] ALLOWED_EXT = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"};

    private UploadService uploadService;

    @Autowired
    public void setUploadService(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * 文件上传。
     *
     * @param file 上传文件
     * @param type 业务类型（品牌/商品/会员等，用于分目录存储）
     * @return 可访问 URL
     */
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonResp upload(@RequestParam("file") MultipartFile file,
                           @RequestParam(name = "type", defaultValue = "common") String type) {
        // 校验空文件
        if (file == null || file.isEmpty()) {
            return JsonResp.fail(400, "请上传文件");
        }
        // 校验类型（图片）
        String original = file.getOriginalFilename();
        boolean allowed = original != null && java.util.Arrays.stream(ALLOWED_EXT)
                .anyMatch(original.toLowerCase()::endsWith);
        if (!allowed) {
            return JsonResp.fail(400, "仅支持图片文件（jpg/png/gif/webp/bmp）");
        }
        // 校验大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return JsonResp.fail(400, "文件大小不能超过 10MB");
        }
        try {
            String url = uploadService.upload(file, type);
            return JsonResp.success(url);
        } catch (Exception e) {
            return JsonResp.fail(500, "上传失败：" + e.getMessage());
        }
    }
}
