/**
 * 头像图片压缩工具。
 * 使用 Canvas 将图片压缩为 200×200 JPEG，输出裸 base64 字符串。
 */

const MAX_SIZE = 5 * 1024 * 1024 // 5MB
const OUTPUT_SIZE = 200
const OUTPUT_QUALITY = 0.75
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp']

export interface CompressResult {
  base64: string
  originalSize: number
  compressedSize: number
}

export async function compressAvatar(file: File): Promise<CompressResult> {
  if (!ALLOWED_TYPES.includes(file.type)) {
    throw new Error('INVALID_TYPE')
  }
  if (file.size > MAX_SIZE) {
    throw new Error('FILE_TOO_LARGE')
  }

  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = OUTPUT_SIZE
      canvas.height = OUTPUT_SIZE
      const ctx = canvas.getContext('2d')!
      // 居中裁切：取短边为基准，等比缩放后居中
      const srcSize = Math.min(img.width, img.height)
      const sx = (img.width - srcSize) / 2
      const sy = (img.height - srcSize) / 2
      ctx.drawImage(img, sx, sy, srcSize, srcSize, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE)
      const dataUrl = canvas.toDataURL('image/jpeg', OUTPUT_QUALITY)
      const base64 = dataUrl.substring('data:image/jpeg;base64,'.length)
      URL.revokeObjectURL(img.src)
      resolve({
        base64,
        originalSize: file.size,
        compressedSize: Math.ceil(base64.length * 0.75), // 粗估原始大小
      })
    }
    img.onerror = () => {
      URL.revokeObjectURL(img.src)
      reject(new Error('LOAD_ERROR'))
    }
    img.src = URL.createObjectURL(file)
  })
}
