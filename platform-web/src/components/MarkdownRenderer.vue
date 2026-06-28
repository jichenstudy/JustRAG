<template>
  <div class="markdown-body" v-html="renderedContent"></div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import katex from 'katex'
import 'highlight.js/styles/github.css'
import 'katex/dist/katex.min.css'

const props = defineProps<{
  content: string
}>()

// 节流渲染：使用 requestAnimationFrame 避免频密 markdown 解析
const debouncedContent = ref(props.content)
let rafId: number | null = null

watch(
  () => props.content,
  (newVal) => {
    if (rafId !== null) {
      cancelAnimationFrame(rafId)
    }
    rafId = requestAnimationFrame(() => {
      debouncedContent.value = newVal
      rafId = null
    })
  },
  { immediate: true }
)

// KaTeX 数学公式扩展
const mathExtension = {
  extensions: [
    // 行内公式: $...$
    {
      name: 'inlineMath',
      level: 'inline',
      start(src: string) {
        return src.indexOf('$')
      },
      tokenizer(this: any, src: string) {
        const match = src.match(/^\$([^$\n]+?)\$/)
        if (match) {
          return {
            type: 'inlineMath',
            raw: match[0],
            text: match[1].trim()
          }
        }
      },
      renderer(this: any, token: any) {
        try {
          return katex.renderToString(token.text, {
            throwOnError: false,
            output: 'html'
          })
        } catch {
          return token.raw
        }
      }
    },
    // 块级公式: $$...$$
    {
      name: 'blockMath',
      level: 'block',
      start(src: string) {
        return src.indexOf('$$')
      },
      tokenizer(this: any, src: string) {
        const match = src.match(/^\$\$([\s\S]+?)\$\$/)
        if (match) {
          return {
            type: 'blockMath',
            raw: match[0],
            text: match[1].trim()
          }
        }
      },
      renderer(this: any, token: any) {
        try {
          return katex.renderToString(token.text, {
            throwOnError: false,
            displayMode: true,
            output: 'html'
          })
        } catch {
          return token.raw
        }
      }
    }
  ]
}

// 配置 marked
marked.use(mathExtension)

// 配置 marked（代码高亮 + 换行 + GFM 表格等）
// highlight 在 marked v17+ 的 TS 类型中已移除，但运行时仍生效
marked.setOptions({
  highlight: function (code: string, lang: string) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch {
        return code
      }
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
} as any)

const renderedContent = computed(() => {
  if (!debouncedContent.value) return ''
  try {
    const fixedContent = fixMarkdownTable(debouncedContent.value)
    return marked(fixedContent) as string
  } catch (e) {
    console.error('Markdown 渲染错误:', e)
    return debouncedContent.value
  }
})

/**
 * 修复 Markdown 表格中非法的对齐分隔符
 * 标准语法: --- / :--- / :---: / ---:
 * AI 模型可能生成 ::---、---:-- 等非法变体
 */
function fixMarkdownTable(content: string): string {
  return content.replace(
    /^\|[\s\S]*?\|\n\|([\s\S]*?)\|/gm,
    (match, separator) => {
      // 修复每个分隔单元格
      const fixed = separator.replace(
        /:?-{3,}:?/g,
        (cell: string) => {
          // 去掉多余的冒号，只保留标准格式
          const hasLeft = cell.startsWith(':')
          const hasRight = cell.endsWith(':')
          if (hasLeft && hasRight) return ':---:'
          if (hasLeft) return ':---'
          if (hasRight) return '---:'
          return '---'
        }
      )
      return match.replace(separator, fixed)
    }
  )
}
</script>

<style scoped>
.markdown-body {
  font-size: 15px;
  line-height: 1.7;
  word-wrap: break-word;
}

.markdown-body :deep(pre) {
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
}

.markdown-body :deep(code) {
  background-color: rgba(175, 184, 193, 0.2);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 85%;
}

.markdown-body :deep(pre code) {
  background-color: transparent;
  padding: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 2em;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #dfe2e5;
  padding-left: 16px;
  color: #6a737d;
  margin: 0;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #dfe2e5;
  padding: 8px 12px;
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #dfe2e5;
  margin: 16px 0;
}

.markdown-body :deep(a) {
  color: #0366d6;
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-body :deep(p) {
  margin-top: 0;
  margin-bottom: 16px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}
</style>
