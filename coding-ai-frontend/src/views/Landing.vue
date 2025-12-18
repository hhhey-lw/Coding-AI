<template>
  <div class="landing">
    <div class="spotlight" ref="spotlightEl"></div>
    <div class="spotlight-glow" ref="spotlightGlowEl"></div>

    <nav class="navbar">
      <div class="logo">Coding AI</div>
      <ul class="nav-links">
        <li><a href="#">首页</a></li>
      </ul>
      <div class="nav-buttons">
        <router-link to="/login" class="btn btn-outline">登陆</router-link>
      </div>
    </nav>

    <section class="hero">
      <div class="hero-videos-bg">
        <div class="hero-video-item" ref="videoCardEl">
          <video
            ref="videoEl"
            src="https://longcoding-ai-service.oss-cn-hangzhou.aliyuncs.com/files/34ce2fcd4c064ddd92a5c3140a4f41d3.mp4"
            muted
            playsinline
          ></video>
          <div class="video-overlay"></div>
        </div>
      </div>

      <div class="glow-orb glow-orb-1"></div>
      <div class="glow-orb glow-orb-2"></div>

      <div class="hero-content">
        <h1><span>创造</span>你的专属智能体和工作流</h1>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const spotlightEl = ref<HTMLDivElement | null>(null)
const spotlightGlowEl = ref<HTMLDivElement | null>(null)
const videoCardEl = ref<HTMLDivElement | null>(null)
const videoEl = ref<HTMLVideoElement | null>(null)

let animationFrame: number | null = null
let isReversing = false

const onMouseMove = (e: MouseEvent) => {
  const x = `${e.clientX}px`
  const y = `${e.clientY}px`

  spotlightEl.value?.style.setProperty('--mouse-x', x)
  spotlightEl.value?.style.setProperty('--mouse-y', y)
  spotlightGlowEl.value?.style.setProperty('--mouse-x', x)
  spotlightGlowEl.value?.style.setProperty('--mouse-y', y)
}

const reversePlay = () => {
  const video = videoEl.value
  if (!video || !isReversing) return

  if (video.currentTime > 0) {
    video.currentTime = Math.max(0, video.currentTime - 0.05)
    animationFrame = requestAnimationFrame(reversePlay)
  } else {
    isReversing = false
  }
}

const onVideoMouseEnter = async () => {
  const video = videoEl.value
  if (!video) return

  isReversing = false
  if (animationFrame) cancelAnimationFrame(animationFrame)

  try {
    await video.play()
  } catch {
    // ignore autoplay restriction
  }
}

const onVideoMouseLeave = () => {
  const video = videoEl.value
  if (!video) return

  isReversing = true
  video.pause()
  animationFrame = requestAnimationFrame(reversePlay)
}

onMounted(() => {
  document.addEventListener('mousemove', onMouseMove)

  if (videoCardEl.value) {
    videoCardEl.value.addEventListener('mouseenter', onVideoMouseEnter)
    videoCardEl.value.addEventListener('mouseleave', onVideoMouseLeave)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onMouseMove)

  if (videoCardEl.value) {
    videoCardEl.value.removeEventListener('mouseenter', onVideoMouseEnter)
    videoCardEl.value.removeEventListener('mouseleave', onVideoMouseLeave)
  }

  if (animationFrame) cancelAnimationFrame(animationFrame)
})
</script>

<style scoped>
.landing {
  --primary-color: #6366f1;
  --primary-hover: #4f46e5;
  --secondary-color: #ec4899;
  --bg-dark: #000000;
  --bg-card: #12121a;
  --text-primary: #ffffff;
  --text-secondary: #a1a1aa;
  --border-color: rgba(255, 255, 255, 0.1);
  --gradient-1: linear-gradient(135deg, #6366f1 0%, #ec4899 100%);
  --gradient-2: linear-gradient(135deg, #06b6d4 0%, #6366f1 100%);

  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: var(--bg-dark);
  color: var(--text-primary);
  line-height: 1.6;
  overflow-x: hidden;
  min-height: 100vh;
}

.landing * {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  padding: 16px 48px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-color);
}

.logo {
  font-size: 24px;
  font-weight: 800;
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-links {
  display: flex;
  gap: 32px;
  list-style: none;
}

.nav-links a {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.3s;
}

.nav-links a:hover {
  color: var(--text-primary);
}

.nav-buttons {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.btn-outline:hover {
  background: rgba(255, 255, 255, 0.1);
}

.hero {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 120px 24px 80px;
  position: relative;
  overflow: hidden;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 980px;
  margin-top: 32px;
}

.hero h1 {
  font-size: clamp(28px, 4.2vw, 52px);
  font-weight: 800;
  line-height: 1.15;
  margin-bottom: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #a1a1aa 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero h1 span {
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.spotlight {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  pointer-events: none;
  z-index: 2;
  mask-image: radial-gradient(circle 180px at var(--mouse-x, -1000px) var(--mouse-y, -1000px), transparent 0%, transparent 70%, black 100%);
  -webkit-mask-image: radial-gradient(circle 180px at var(--mouse-x, -1000px) var(--mouse-y, -1000px), transparent 0%, transparent 70%, black 100%);
}

.spotlight-glow {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 3;
  background: radial-gradient(circle 260px at var(--mouse-x, -1000px) var(--mouse-y, -1000px), rgba(255, 255, 255, 0.14) 0%, rgba(255, 255, 255, 0.06) 25%, rgba(255, 255, 255, 0.0) 60%);
  mix-blend-mode: screen;
  opacity: 0.9;
}

.glow-orb {
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: float 8s ease-in-out infinite;
}

.glow-orb-1 {
  top: 20%;
  left: 10%;
  background: var(--primary-color);
}

.glow-orb-2 {
  bottom: 20%;
  right: 10%;
  background: var(--secondary-color);
  animation-delay: -4s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-30px) scale(1.1);
  }
}

.hero-videos-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  padding-bottom: 100px;
  z-index: 0;
}

.hero-video-item {
  position: relative;
  width: 320px;
  height: auto;
  overflow: hidden;
  cursor: pointer;
  background: transparent;
  border: none;
}

.hero-video-item video {
  width: 100%;
  height: auto;
  object-fit: contain;
  display: block;
}

.hero-video-item .video-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.hero-video-item:hover .video-overlay {
  opacity: 0;
}

@media (max-width: 768px) {
  .navbar {
    padding: 16px 24px;
  }

  .nav-links {
    display: none;
  }

  .hero {
    padding: 100px 24px 60px;
  }

  .hero-video-item {
    width: 280px;
  }
}
</style>
