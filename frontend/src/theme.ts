export type ThemeId = 'light' | 'dark'

export const THEMES: readonly ThemeId[] = ['light', 'dark'] as const

const STORAGE_KEY = 'theme'

export function getStoredTheme(): ThemeId {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored === 'light' || stored === 'dark') return stored
  const prefersDark =
    typeof window !== 'undefined' &&
    window.matchMedia &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
  return prefersDark ? 'dark' : 'light'
}

export function applyTheme(theme: ThemeId): void {
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem(STORAGE_KEY, theme)
}

export function toggleTheme(): ThemeId {
  const next: ThemeId = getStoredTheme() === 'dark' ? 'light' : 'dark'
  applyTheme(next)
  return next
}
