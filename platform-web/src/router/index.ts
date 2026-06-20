import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/Index.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/home/Index.vue')
        },
        {
          path: '/knowledge',
          name: 'knowledge',
          component: () => import('@/views/knowledge/Index.vue')
        },
        {
          path: '/knowledge/:id',
          name: 'knowledge-detail',
          component: () => import('@/views/knowledge/Detail.vue')
        },
        {
          path: '/chat',
          name: 'chat',
          component: () => import('@/views/chat/Index.vue')
        },
        {
          path: '/chat/:id',
          name: 'assistant-detail',
          component: () => import('@/views/chat/AssistantDetail.vue')
        },
        {
          path: '/search',
          name: 'search',
          component: () => import('@/views/search/Index.vue')
        },
        {
          path: '/files',
          name: 'files',
          component: () => import('@/views/files/Index.vue')
        },
        {
          path: '/model',
          name: 'model',
          component: () => import('@/views/model/Index.vue')
        },
        {
          path: '/mcp',
          name: 'mcp',
          component: () => import('@/views/mcp/Index.vue')
        },
        {
          path: '/profile',
          name: 'profile',
          component: () => import('@/views/profile/Index.vue')
        },
        {
          path: '/team',
          name: 'team',
          component: () => import('@/views/team/Index.vue')
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
