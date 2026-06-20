import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { darkTheme as naiveDarkTheme, lightTheme as naiveLightTheme, GlobalThemeOverrides } from 'naive-ui'
import { lightTheme, darkTheme, type Theme } from '@/config/theme'

export const useThemeStore = defineStore('theme', () => {
  const currentTheme = ref<'light' | 'dark'>('light')

  const theme = computed<Theme>(() => {
    return currentTheme.value === 'dark' ? darkTheme : lightTheme
  })

  const naiveTheme = computed(() => {
    return currentTheme.value === 'dark' ? naiveDarkTheme : naiveLightTheme
  })

  const naiveThemeOverrides = computed<GlobalThemeOverrides>(() => {
    const colors = theme.value.colors
    return {
      common: {
        primaryColor: colors.text,
        primaryColorHover: colors.textSecondary,
        primaryColorPressed: colors.text,
        textColorBase: colors.text,
        textColor1: colors.text,
        textColor2: colors.textSecondary,
        borderColor: colors.border,
        bodyColor: colors.background,
        cardColor: colors.surface,
        modalColor: colors.surface,
        popoverColor: colors.surface,
        tableColor: colors.surface,
        inputColor: colors.surface,
        codeColor: colors.surface,
        tagColor: colors.surface,
        actionColor: colors.hover
      },
      Card: {
        borderColor: colors.border,
        color: colors.surface
      },
      Input: {
        border: '1px solid ' + colors.border,
        borderHover: '1px solid ' + colors.text,
        borderFocus: '1px solid ' + colors.text,
        color: colors.surface,
        colorFocus: colors.surface
      },
      Button: {
        border: '1px solid ' + colors.border,
        textColor: colors.text,
        textColorHover: colors.text,
        textColorPressed: colors.text,
        textColorFocus: colors.text,
        color: 'transparent',
        colorHover: colors.hover,
        colorPressed: colors.active,
        colorFocus: colors.hover
      }
    }
  })

  function toggleTheme() {
    currentTheme.value = currentTheme.value === 'dark' ? 'light' : 'dark'
    localStorage.setItem('theme', currentTheme.value)
  }

  function initTheme() {
    const savedTheme = localStorage.getItem('theme') as 'light' | 'dark' | null
    if (savedTheme) {
      currentTheme.value = savedTheme
    }
  }

  return {
    currentTheme,
    theme,
    naiveTheme,
    naiveThemeOverrides,
    toggleTheme,
    initTheme
  }
})
