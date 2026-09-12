import request from '@/utils/request'

/**
 * 上传图片，返回可访问 URL。
 *
 * <p>后端限制：扩展名 jpg/jpeg/png/gif/webp/bmp，大小 ≤ 10MB。</p>
 * <p><b>两条隐藏规则：</b></p>
 * <ol>
 *   <li>返回 URL 里的 host:port 是后端写死的（http://localhost:10020/...），换机器访问会失效。</li>
 *   <li>上传后 5 分钟内没把这个 URL 存进业务数据，文件会被定时任务删掉——所以拿到 URL 要尽快提交表单。</li>
 * </ol>
 *
 * @param {File} file
 * @param {string} type 子目录：good / brand / member / common
 */
export function uploadFile(file, type = 'common') {
  const form = new FormData()
  form.append('file', file)
  form.append('type', type)
  return request.post('/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
