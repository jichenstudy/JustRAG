# Just RAG Platform - 前端项目

基于 Vue 3 + TypeScript + Vite + Naive UI 构建的现代化知识库管理与对话平台。

## ✨ 特性

- 🎨 **现代化设计** - 简洁大方的界面设计,非传统后台管理风格
- 🌓 **主题切换** - 支持亮色/暗色主题,使用纯黑(#161618)和纯白(#ffffff)配色
- 📚 **知识库管理** - 完整的知识库CRUD,文档管理,配置管理
- 💬 **智能对话** - 支持自由对话和知识库对话
- 🔍 **智能搜索** - 语义搜索功能(需要后端支持)
- 📁 **文件管理** - 文件上传、查询、管理
- 🚀 **响应式布局** - 适配不同屏幕尺寸

## 🛠️ 技术栈

- **框架**: Vue 3.4+ (Composition API)
- **构建工具**: Vite 5.0+
- **语言**: TypeScript 5.3+
- **状态管理**: Pinia 2.1+
- **路由**: Vue Router 4.2+
- **UI组件库**: Naive UI 2.38+
- **图标**: @vicons/ionicons5
- **HTTP客户端**: Axios 1.6+
- **工具库**: @vueuse/core

## 📦 项目结构

```
platform-web/
├── src/
│   ├── api/                  # API接口
│   │   ├── knowledgeBase.ts  # 知识库API
│   │   ├── document.ts       # 文档API
│   │   └── chat.ts           # 聊天API
│   ├── assets/               # 静态资源
│   │   └── styles/           # 全局样式
│   ├── config/               # 配置文件
│   │   └── theme.ts          # 主题配置
│   ├── layouts/              # 布局组件
│   │   └── MainLayout.vue    # 主布局
│   ├── router/               # 路由配置
│   │   └── index.ts
│   ├── stores/               # 状态管理
│   │   └── theme.ts          # 主题Store
│   ├── types/                # TypeScript类型定义
│   │   └── index.ts
│   ├── utils/                # 工具函数
│   │   └── request.ts        # HTTP请求封装
│   ├── views/                # 页面组件
│   │   ├── home/             # 主页
│   │   ├── knowledge/        # 知识库模块
│   │   ├── chat/             # 聊天模块
│   │   ├── search/           # 搜索模块
│   │   └── files/            # 文件管理模块
│   ├── App.vue               # 根组件
│   └── main.ts               # 入口文件
├── index.html                # HTML模板
├── package.json              # 依赖配置
├── tsconfig.json             # TypeScript配置
├── vite.config.ts            # Vite配置
└── README.md                 # 项目说明
```

## 🚀 快速开始

### 安装依赖

```bash
cd platform-web
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:3000

### 生产构建

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 🎯 功能模块

### 1. 主页 (/)
- 展示前5个知识库
- 展示前5个聊天会话
- 快速导航到各个功能

### 2. 知识库管理 (/knowledge)
- **列表页**: 创建、查看、编辑、删除知识库
- **详情页**:
  - 查看知识库文档列表
  - 上传文档
  - 查看知识库配置信息

### 3. 聊天 (/chat)
- **列表页**: 管理所有对话(增删查)
- **详情页**:
  - 实时对话界面
  - 支持自由对话和知识库对话
  - 消息历史记录

### 4. 搜索 (/search)
- 语义搜索界面
- 支持选择知识库范围
- 调整Top K参数
- 展示搜索结果

### 5. 文件管理 (/files)
- 文件列表展示
- 文件上传
- 文件搜索
- 文件删除

## 🎨 主题系统

项目支持亮色/暗色两种主题:

### 亮色主题
- 背景: #ffffff
- 文字: #161618
- 表面: #f5f5f5

### 暗色主题
- 背景: #161618
- 文字: #ffffff
- 表面: #1f1f21

主题切换按钮位于侧边栏底部。

## 🔗 API接口

### 后端接口地址
默认代理到 `http://localhost:8080`

可在 `vite.config.ts` 中修改:

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 主要接口

- **知识库**: `/api/knowledge-base`
- **文档**: `/api/document`
- **聊天**: `/api/chat`

详见 `src/api/` 目录下的接口定义。

## 📝 开发规范

### 代码风格
- 使用 Composition API
- 使用 TypeScript 严格模式
- 组件使用 `<script setup>` 语法
- 样式使用 scoped CSS

### 命名规范
- 组件文件: PascalCase (e.g., `MainLayout.vue`)
- 组件实例: camelCase
- 常量: UPPER_CASE
- API文件: camelCase

### Git提交规范
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 样式调整
- refactor: 重构
- test: 测试
- chore: 构建/工具链

## 📄 License

MIT License

## 👥 贡献

欢迎提交 Issue 和 Pull Request!
