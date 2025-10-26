<template>
  <div class="rich-text-content">
    <template v-for="(item, index) in parsedContent" :key="index">
      <!-- 图片 -->
      <div v-if="item.type === 'image'" class="content-image">
        <el-image 
          :src="item.url" 
          :alt="item.alt"
          fit="contain"
          :preview-src-list="[item.url]"
          class="content-image-element"
        >
          <template #error>
            <div class="image-error">
              <el-icon><Picture /></el-icon>
              <span>图片加载失败</span>
            </div>
          </template>
        </el-image>
        <p v-if="item.alt" class="image-caption">{{ item.alt }}</p>
      </div>

      <!-- 音频 -->
      <div v-else-if="item.type === 'audio'" class="content-audio">
        <div class="audio-player">
          <div class="audio-icon-wrapper">
            <el-icon class="audio-icon" :size="20"><Headset /></el-icon>
          </div>
          <div class="audio-control">
            <audio :src="item.url" controls class="audio-element"></audio>
          </div>
          <a :href="item.url" target="_blank" download class="audio-download">
            <el-button size="small" type="primary" plain round>
              <el-icon><Download /></el-icon>
              下载
            </el-button>
          </a>
        </div>
        <p v-if="item.text && item.text !== '音频文件'" class="audio-caption">{{ item.text }}</p>
      </div>

      <!-- 视频 -->
      <div v-else-if="item.type === 'video'" class="content-video">
        <video 
          :src="item.url" 
          controls 
          class="content-video-element"
        >
          您的浏览器不支持视频播放
        </video>
        <p v-if="item.text" class="video-caption">{{ item.text }}</p>
      </div>

      <!-- 普通文本（使用Markdown渲染） -->
      <MarkdownRenderer v-else-if="item.type === 'text'" :content="item.text || ''" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Picture, Headset, Download } from '@element-plus/icons-vue'
import MarkdownRenderer from './MarkdownRenderer.vue'

interface ContentItem {
  type: 'text' | 'image' | 'audio' | 'video'
  text?: string
  url?: string
  alt?: string
}

const props = defineProps<{
  content: string
}>()

// 解析内容
const parsedContent = computed<ContentItem[]>(() => {
  if (!props.content) return []
  
  const items: ContentItem[] = []
  let remainingText = props.content
  
  // 正则表达式
  const imageRegex = /!\[(.*?)\]\((.*?)\)/g  // 匹配 ![alt](url)
  const audioLinkRegex = /\[([^\]]*?(?:music|音乐|audio|音频)[^\]]*?)\]\((.*?\.(?:mp3|wav|ogg|m4a))\)/gi  // 匹配 [music](url.mp3)
  const videoLinkRegex = /\[([^\]]*?(?:video|视频)[^\]]*?)\]\((.*?\.(?:mp4|webm|ogg))\)/gi  // 匹配 [video](url.mp4)
  const audioUrlRegex = /(https?:\/\/[^\s]+\.(?:mp3|wav|ogg|m4a))/gi  // 匹配纯音频URL
  const videoUrlRegex = /(https?:\/\/[^\s]+\.(?:mp4|webm|ogg))/gi  // 匹配纯视频URL
  
  // 收集所有匹配项
  const matches: Array<{ index: number; length: number; item: ContentItem }> = []
  
  // 匹配图片（需要检查URL是否为音频/视频文件）
  let match: RegExpExecArray | null
  while ((match = imageRegex.exec(remainingText)) !== null) {
    const url = match[2]
    const alt = match[1]
    
    // 检查是否是音频文件
    if (/\.(mp3|wav|ogg|m4a|flac|aac)$/i.test(url)) {
      matches.push({
        index: match.index,
        length: match[0].length,
        item: { type: 'audio', url: url, text: alt || '音频文件' }
      })
    }
    // 检查是否是视频文件
    else if (/\.(mp4|webm|ogg|avi|mov)$/i.test(url)) {
      matches.push({
        index: match.index,
        length: match[0].length,
        item: { type: 'video', url: url, text: alt || '视频文件' }
      })
    }
    // 否则作为图片处理
    else {
      matches.push({
        index: match.index,
        length: match[0].length,
        item: { type: 'image', url: url, alt: alt }
      })
    }
  }
  
  // 匹配音频链接（带文本）
  while ((match = audioLinkRegex.exec(remainingText)) !== null) {
    matches.push({
      index: match.index,
      length: match[0].length,
      item: { type: 'audio', url: match[2], text: match[1] }
    })
  }
  
  // 匹配视频链接（带文本）
  while ((match = videoLinkRegex.exec(remainingText)) !== null) {
    matches.push({
      index: match.index,
      length: match[0].length,
      item: { type: 'video', url: match[2], text: match[1] }
    })
  }
  
  // 匹配纯音频URL（不在[]()中）
  const textWithoutMarkdown = remainingText.replace(/\[.*?\]\(.*?\)/g, '')
  let tempText = remainingText
  while ((match = audioUrlRegex.exec(textWithoutMarkdown)) !== null) {
    const realIndex = tempText.indexOf(match[0])
    if (realIndex !== -1 && !isInsideMarkdownLink(remainingText, realIndex)) {
      matches.push({
        index: realIndex,
        length: match[0].length,
        item: { type: 'audio', url: match[0] }
      })
    }
  }
  
  // 匹配纯视频URL（不在[]()中）
  while ((match = videoUrlRegex.exec(textWithoutMarkdown)) !== null) {
    const realIndex = tempText.indexOf(match[0])
    if (realIndex !== -1 && !isInsideMarkdownLink(remainingText, realIndex)) {
      matches.push({
        index: realIndex,
        length: match[0].length,
        item: { type: 'video', url: match[0] }
      })
    }
  }
  
  // 按位置排序
  matches.sort((a, b) => a.index - b.index)
  
  // 构建结果数组
  let lastIndex = 0
  for (const match of matches) {
    // 添加前面的文本
    if (match.index > lastIndex) {
      const text = remainingText.substring(lastIndex, match.index).trim()
      if (text) {
        items.push({ type: 'text', text })
      }
    }
    
    // 添加匹配项
    items.push(match.item)
    lastIndex = match.index + match.length
  }
  
  // 添加剩余文本
  if (lastIndex < remainingText.length) {
    const text = remainingText.substring(lastIndex).trim()
    if (text) {
      items.push({ type: 'text', text })
    }
  }
  
  // 如果没有任何匹配，返回纯文本
  if (items.length === 0 && remainingText.trim()) {
    items.push({ type: 'text', text: remainingText.trim() })
  }
  
  return items
})

// 检查URL是否在markdown链接中
const isInsideMarkdownLink = (text: string, urlIndex: number): boolean => {
  // 简单检查：向前查找最近的 ]( 和向后查找最近的 )
  const before = text.substring(0, urlIndex)
  const after = text.substring(urlIndex)
  const lastLinkStart = before.lastIndexOf('](')
  const nextLinkEnd = after.indexOf(')')
  
  if (lastLinkStart === -1) return false
  const nextCloseBracket = before.substring(lastLinkStart).indexOf(')')
  return nextCloseBracket === -1 && nextLinkEnd !== -1
}
</script>

<style scoped>
.rich-text-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.content-image {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.content-image-element {
  max-width: 400px;
  width: 100%;
  height: auto;
  border-radius: 8px;
  cursor: pointer;
}

.content-image-element :deep(img) {
  border-radius: 8px;
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #999;
  background: #f5f5f5;
  border-radius: 8px;
  min-height: 200px;
  max-width: 400px;
}

.image-caption {
  font-size: 14px;
  color: #666;
  text-align: center;
  margin: 0;
  font-style: italic;
}

.content-audio {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 8px 0;
}

.audio-player {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.audio-player:hover {
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
  transform: translateY(-2px);
}

.audio-icon-wrapper {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  backdrop-filter: blur(10px);
}

.audio-icon {
  color: white;
}

.audio-control {
  flex: 1;
  min-width: 0;
}

.audio-element {
  width: 100%;
  height: 40px;
  border-radius: 8px;
}

/* 自定义音频控件样式 */
.audio-element::-webkit-media-controls-panel {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
}

.audio-download {
  flex-shrink: 0;
  text-decoration: none;
}

.audio-download :deep(.el-button) {
  background: rgba(255, 255, 255, 0.9);
  border-color: transparent;
  color: #667eea;
  font-weight: 500;
}

.audio-download :deep(.el-button:hover) {
  background: white;
  color: #764ba2;
  border-color: transparent;
}

.audio-caption {
  font-size: 13px;
  color: #909399;
  margin: 0;
  padding-left: 16px;
  font-style: italic;
}

.content-video {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.content-video-element {
  max-width: 500px;
  width: 100%;
  height: auto;
  border-radius: 8px;
}

.video-caption {
  font-size: 14px;
  color: #666;
  text-align: center;
  margin: 0;
  font-style: italic;
}
</style>

