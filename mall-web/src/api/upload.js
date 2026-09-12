import request from '@/utils/request'

/**
 * 上传图片，返回可访问的 URL。
 *
 * <p>限制（后端校验）：扩展名 jpg/jpeg/png/gif/webp/bmp，大小 ≤ 10MB。</p>
 * <p><b>注意两条隐藏规则：</b></p>
 * <ol>
 *   <li>返回的 URL 里带的 host:port 是后端配置写死的（http://localhost:10020/...），
 *       换机器访问会失效。开发环境本机没问题。</li>
 *   <li>上传后若 5 分钟内没把这个 URL 存进业务数据（Redis 里查不到该 URL 的键），
 *       文件会被定时任务删掉。所以拿到 URL 要尽快提交表单。</li>
 * </ol>
 *
 * @param {File} file
 * @param {string} type 子目录，如 good / brand / member / common
 */
export function uploadFile(file, type = 'common') {
  const form = new FormData()
  form.append('file', file)
  form.append('type', type)
  return request.post('/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
