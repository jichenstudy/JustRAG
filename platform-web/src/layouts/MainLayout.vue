<template>
  <div
    class="main-layout"
    :style="{
      backgroundColor: themeStore.theme.colors.background,
      color: themeStore.theme.colors.text
    }"
  >
    <!-- 侧边导航栏 -->
    <aside
      class="sidebar"
      :style="{
        backgroundColor: themeStore.theme.colors.background,
        borderRight: `1px solid ${themeStore.theme.colors.border}`
      }"
    >
      <div class="logo">
        JustRAG
      </div>

      <nav class="nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          :style="{
            color: themeStore.theme.colors.text
          }"
        >
          <n-icon :component="item.icon" :size="20" />
          <span>{{ item.name }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <n-dropdown
          trigger="click"
          :options="userMenuOptions"
          @select="handleUserMenuSelect"
        >
          <div class="user-info" :style="{ borderColor: themeStore.theme.colors.border }">
            <n-avatar :size="32" round :src="authStore.user?.avatar || undefined">
              <template v-if="!authStore.user?.avatar">
                {{ authStore.user?.nickname?.charAt(0) || authStore.user?.username?.charAt(0) || '?' }}
              </template>
            </n-avatar>
            <span class="username">{{ authStore.user?.nickname || authStore.user?.username || '用户' }}</span>
          </div>
        </n-dropdown>
        <n-button text @click="themeStore.toggleTheme" class="theme-toggle">
          <n-icon :component="themeStore.currentTheme === 'dark' ? SunnyOutline : MoonOutline" :size="20" />
        </n-button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NIcon, NButton, NDropdown, NAvatar } from 'naive-ui'
import {
  HomeOutline,
  LibraryOutline,
  ChatbubbleEllipsesOutline,
  SearchOutline,
  FolderOpenOutline,
  MoonOutline,
  SunnyOutline,
  SettingsOutline,
  ExtensionPuzzleOutline,
  PersonOutline,
  LogOutOutline,
  PeopleOutline
} from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const authStore = useAuthStore()

const navItems = [
  { name: '主页', path: '/', icon: HomeOutline },
  { name: '聊天', path: '/chat', icon: ChatbubbleEllipsesOutline },
  { name: '搜索', path: '/search', icon: SearchOutline },
  { name: '知识库', path: '/knowledge', icon: LibraryOutline },
  { name: '文件管理', path: '/files', icon: FolderOpenOutline },
  { name: '模型配置', path: '/model', icon: SettingsOutline },
  { name: 'MCP配置', path: '/mcp', icon: ExtensionPuzzleOutline },
  { name: '团队管理', path: '/team', icon: PeopleOutline }
]

const userMenuOptions = [
  {
    label: '个人信息',
    key: 'profile',
    icon: () => h(NIcon, null, { default: () => h(PersonOutline) })
  },
  {
    label: '退出登录',
    key: 'logout',
    icon: () => h(NIcon, null, { default: () => h(LogOutOutline) })
  }
]

// 判断菜单项是否激活
const isActive = (path: string) => {
  if (path === '/') {
    // 主页只有在路径完全匹配时才激活
    return route.path === '/'
  }
  // 其他页面使用开头匹配
  return route.path.startsWith(path)
}

function handleUserMenuSelect(key: string) {
  if (key === 'profile') {
    router.push('/profile')
  } else if (key === 'logout') {
    authStore.logout()
  }
}
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  width: 100%;
  transition: background-color 0.3s ease;
}

.sidebar {
  width: 220px;
  display: flex;
  flex-direction: column;
  padding: 24px 0;
  transition: all 0.3s ease;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 24px;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 32px;
}

.logo img {
  width: 50px;
  height: 50px;
  flex-shrink: 0;
  border-radius: 8px;
}

.nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
}

.nav-item:hover {
  background-color: v-bind('themeStore.theme.colors.hover');
}

.nav-item.active {
  background-color: v-bind('themeStore.theme.colors.active');
  font-weight: 500;
}

.sidebar-footer {
  padding: 0 12px;
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.user-info:hover {
  background-color: v-bind('themeStore.theme.colors.hover');
}

.username {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.theme-toggle {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 12px;
  color: v-bind('themeStore.theme.colors.text');
}

.main-content {
  flex: 1;
  overflow-y: auto;
  position: relative;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
