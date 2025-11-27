<template>
  <div class="markdown-body" v-html="renderedContent"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps<{
  content: string
}>()

// 配置 markdown-it
const md: MarkdownIt = new MarkdownIt({
  html: true,        // 允许HTML标签
  linkify: true,     // 自动转换URL为链接
  typographer: true, // 智能标点
  breaks: true,      // 转换换行符为<br>
  highlight: function (str: string, lang: string): string {
    // 代码高亮
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
               hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
               '</code></pre>'
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

// 自定义链接渲染，使其在新标签页打开
const defaultLinkRender = md.renderer.rules.link_open || function(tokens: any, idx: any, options: any, _env: any, self: any) {
  return self.renderToken(tokens, idx, options)
}

md.renderer.rules.link_open = function (tokens: any, idx: any, options: any, _env: any, self: any) {
  const aIndex = tokens[idx].attrIndex('target')
  if (aIndex < 0) {
    tokens[idx].attrPush(['target', '_blank'])
  } else {
    tokens[idx].attrs![aIndex][1] = '_blank'
  }
  
  // 添加 rel="noopener noreferrer" 提升安全性
  const relIndex = tokens[idx].attrIndex('rel')
  if (relIndex < 0) {
    tokens[idx].attrPush(['rel', 'noopener noreferrer'])
  } else {
    tokens[idx].attrs![relIndex][1] = 'noopener noreferrer'
  }
  
  return defaultLinkRender(tokens, idx, options, _env, self)
}

// 自定义图片渲染
const defaultImageRender = md.renderer.rules.image || function(tokens: any, idx: any, options: any, _env: any, self: any) {
  return self.renderToken(tokens, idx, options)
}

md.renderer.rules.image = function (tokens: any, idx: any, options: any, _env: any, self: any) {
  return defaultImageRender(tokens, idx, options, _env, self)
}

// 渲染内容
const renderedContent = computed(() => {
  if (!props.content) return ''
  return md.render(props.content)
})
</script>

<style scoped>
/* GitHub风格的Markdown样式 */
.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif;
  font-size: 15px;
  line-height: 1.6;
  word-wrap: break-word;
  color: #24292f;
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
  color: #1f2328;
}

.markdown-body :deep(h1) {
  font-size: 1.8em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #d0d7de;
}

.markdown-body :deep(h2) {
  font-size: 1.5em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #d0d7de;
}

.markdown-body :deep(h3) {
  font-size: 1.25em;
}

.markdown-body :deep(h4) {
  font-size: 1em;
}

.markdown-body :deep(h5) {
  font-size: 0.875em;
}

.markdown-body :deep(h6) {
  font-size: 0.85em;
  color: #656d76;
}

.markdown-body :deep(p) {
  margin-top: 0;
  margin-bottom: 16px;
}

.markdown-body :deep(blockquote) {
  margin: 0 0 16px 0;
  padding: 0 1em;
  color: #656d76;
  border-left: 0.25em solid #d0d7de;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin-top: 0;
  margin-bottom: 16px;
  padding-left: 2em;
}

.markdown-body :deep(li) {
  margin-bottom: 4px;
}

.markdown-body :deep(li + li) {
  margin-top: 0.25em;
}

.markdown-body :deep(code) {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(175, 184, 193, 0.2);
  border-radius: 6px;
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace;
}

.markdown-body :deep(pre) {
  margin-top: 0;
  margin-bottom: 16px;
  padding: 16px;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
  background-color: #f6f8fa;
  border-radius: 6px;
}

.markdown-body :deep(pre code) {
  display: inline;
  padding: 0;
  margin: 0;
  overflow: visible;
  line-height: inherit;
  background-color: transparent;
  border: 0;
  font-size: 100%;
}

.markdown-body :deep(a) {
  color: #0969da;
  text-decoration: none;
  transition: color 0.2s;
}

.markdown-body :deep(a:hover) {
  color: #0550ae;
  text-decoration: underline;
}

.markdown-body :deep(strong) {
  font-weight: 600;
}

.markdown-body :deep(em) {
  font-style: italic;
}

.markdown-body :deep(hr) {
  height: 0.25em;
  padding: 0;
  margin: 24px 0;
  background-color: #d0d7de;
  border: 0;
}

.markdown-body :deep(table) {
  border-spacing: 0;
  border-collapse: collapse;
  margin-top: 0;
  margin-bottom: 16px;
  width: 100%;
  overflow: auto;
}

.markdown-body :deep(table th) {
  font-weight: 600;
  padding: 6px 13px;
  border: 1px solid #d0d7de;
  background-color: #f6f8fa;
}

.markdown-body :deep(table td) {
  padding: 6px 13px;
  border: 1px solid #d0d7de;
}

.markdown-body :deep(table tr) {
  background-color: #ffffff;
  border-top: 1px solid #d0d7de;
}

.markdown-body :deep(table tr:nth-child(2n)) {
  background-color: #f6f8fa;
}

.markdown-body :deep(img) {
  max-width: 100%;
  box-sizing: content-box;
  border-radius: 8px;
}

.markdown-body :deep(.hljs) {
  background: #f6f8fa;
  color: #24292f;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}

/* 任务列表样式 */
.markdown-body :deep(input[type="checkbox"]) {
  margin: 0 0.5em 0 0;
  vertical-align: middle;
}

/* 音频播放器样式 */
.markdown-body :deep(.markdown-audio) {
  margin: 16px 0;
}

.markdown-body :deep(.audio-player-wrapper) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fefefe;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;
}

.markdown-body :deep(.audio-icon) {
  color: #6b7280;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
}

.markdown-body :deep(.audio-player-wrapper audio) {
  flex: 1;
  height: 40px;
  outline: none;
}

.markdown-body :deep(.media-download) {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #ffffff;
  color: #6b7280;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
  flex-shrink: 0;
  border: 1px solid #d1d5db;
}

.markdown-body :deep(.media-download:hover) {
  background: #f9fafb;
  color: #374151;
}

.markdown-body :deep(.media-download svg) {
  width: 16px;
  height: 16px;
}

.markdown-body :deep(.media-caption) {
  font-size: 14px;
  color: #656d76;
  margin: 8px 0 0 0;
  padding-left: 12px;
  font-style: italic;
}

/* 视频播放器样式 */
.markdown-body :deep(.markdown-video) {
  margin: 16px 0;
}

.markdown-body :deep(.video-player) {
  max-width: 500px;
  width: 100%;
  height: auto;
  border-radius: 8px;
  display: block;
}

/* 图片样式优化 */
.markdown-body :deep(img) {
  max-width: 400px;
  width: 100%;
  height: auto;
  box-sizing: content-box;
  border-radius: 8px;
  cursor: pointer;
}
</style>

