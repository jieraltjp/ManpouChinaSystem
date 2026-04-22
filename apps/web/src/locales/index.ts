import { createI18n } from 'vue-i18n'
import zh from './zh.json'
import ja from './ja.json'

export type Locale = 'zh' | 'ja'

export const i18n = createI18n({
  legacy: false,
  locale: 'zh',
  fallbackLocale: 'zh',
  messages: { zh, ja },
})

export function setLocale(locale: Locale) {
  i18n.global.locale.value = locale
  localStorage.setItem('locale', locale)
}

export function getStoredLocale(): Locale {
  return (localStorage.getItem('locale') as Locale) ?? 'zh'
}
