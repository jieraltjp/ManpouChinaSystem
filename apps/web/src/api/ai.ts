import client from './client'

export interface TranslateRequest {
  sourceText: string
  sourceLang?: string
  targetLang?: string
}

export interface TranslateResponse {
  sourceText: string
  targetText: string
  nameJa?: string
  nameEn?: string
}

export const aiApi = {
  /**
   * 中译日翻译（商品名）。
   * 对应后端 POST /api/v1/ai/translate
   */
  translateZhToJa(data: TranslateRequest) {
    return client.post<TranslateResponse>('/ai/translate', {
      sourceText: data.sourceText,
      sourceLang: data.sourceLang ?? 'zh',
      targetLang: data.targetLang ?? 'ja',
    })
  },
}
