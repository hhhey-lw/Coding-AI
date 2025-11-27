<template>
  <div class="rich-text-content">
    <template v-for="(item, index) in parsedContent" :key="index">
      <!-- 图片 -->
      <div v-if="item.type === 'image'" class="content-image">
        <div class="image-wrapper">
          <el-image 
            :src="item.url" 
            :alt="item.alt"
            fit="contain"
            :preview-src-list="[item.url]"
            :initial-index="0"
            class="content-image-element"
          >
            <template #error>
              <div class="image-error">
                <el-icon><Picture /></el-icon>
                <span>图片加载失败</span>
              </div>
            </template>
          </el-image>
          <div class="image-tag">
            <el-icon><ZoomIn /></el-icon>
            <span>预览</span>
          </div>
        </div>
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
      <div v-else-if="item.type === 'text'" class="content-text-bubble">
        <MarkdownRenderer :content="item.text || ''" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Picture, Headset, Download, ZoomIn, Link } from '@element-plus/icons-vue'
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
    
    matches.push({
      index: match.index,
      length: match[0].length,
      item: { type: 'image', url: url, alt: alt }
    })
  }
  
  // 暂时不提取Markdown格式的音频和视频链接，保留在文本中显示，后续通过扫描URL来生成播放器
  /*
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
  */
  
  // 按位置排序
  matches.sort((a, b) => a.index - b.index)
  
  // 构建初步结果数组
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

  // 后处理：扫描文本块中的媒体链接，在文本块后追加预览
  const finalItems: ContentItem[] = []
  const plainImageUrlRegex = /(https?:\/\/[^\s]+\.(?:jpg|jpeg|png|gif|webp|svg|bmp))/gi
  const plainAudioUrlRegex = /(https?:\/\/[^\s]+\.(?:mp3|wav|ogg|m4a))/gi
  const plainVideoUrlRegex = /(https?:\/\/[^\s]+\.(?:mp4|webm|ogg))/gi

  for (const item of items) {
    finalItems.push(item)
    
    if (item.type === 'text' && item.text) {
      // 1. 图片预览
      plainImageUrlRegex.lastIndex = 0
      let imgMatch: RegExpExecArray | null
      const foundUrls = new Set<string>()

      while ((imgMatch = plainImageUrlRegex.exec(item.text)) !== null) {
        const url = imgMatch[0]
        if (!foundUrls.has(url)) {
          foundUrls.add(url)
          finalItems.push({
            type: 'image',
            url: url,
            alt: '图片预览'
          })
        }
      }
      
      // 2. 音频预览
      plainAudioUrlRegex.lastIndex = 0
      let audioMatch: RegExpExecArray | null
      const foundAudioUrls = new Set<string>()
      
      while ((audioMatch = plainAudioUrlRegex.exec(item.text)) !== null) {
        const url = audioMatch[0]
        if (!foundAudioUrls.has(url)) {
          foundAudioUrls.add(url)
          finalItems.push({
            type: 'audio',
            url: url,
            text: '音频预览'
          })
        }
      }
      
      // 3. 视频预览
      plainVideoUrlRegex.lastIndex = 0
      let videoMatch: RegExpExecArray | null
      const foundVideoUrls = new Set<string>()
      
      while ((videoMatch = plainVideoUrlRegex.exec(item.text)) !== null) {
        const url = videoMatch[0]
        if (!foundVideoUrls.has(url)) {
          foundVideoUrls.add(url)
          finalItems.push({
            type: 'video',
            url: url,
            text: '视频预览'
          })
        }
      }
    }
  }
  
  return finalItems
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

.content-text-bubble {
  background: #f7f8fa;
  border-radius: 12px;
  padding: 12px 16px;
}

.content-image {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 8px 0; /* 增加上下间距，与文本块分隔 */
}

.image-wrapper {
  position: relative;
  display: inline-block;
  max-width: 400px;
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.content-image-element {
  width: 100%;
  height: auto;
  border-radius: 8px;
  cursor: pointer;
  display: block;
}

.image-tag {
  position: absolute;
  top: 0;
  right: 12px;
  background: #409eff;
  color: white;
  padding: 4px 8px;
  border-radius: 0 0 4px 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transform: translateY(-100%);
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.4);
  z-index: 10;
  pointer-events: none;
}

.image-wrapper:hover .image-tag {
  opacity: 1;
  transform: translateY(0);
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

.audio-source-link {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 8px;
  width: fit-content;
  margin-bottom: 4px;
}

.audio-source-link a {
  color: #409eff;
  text-decoration: none;
  font-size: 14px;
  word-break: break-all;
}

.audio-source-link a:hover {
  text-decoration: underline;
}

.audio-player {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.audio-player:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  border-color: #c0c4cc;
}

.audio-icon-wrapper {
  width: 48px;
  height: 48px;
  background: #f2f6fc;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.audio-icon {
  color: #409eff;
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

