import { createRouter, createWebHistory } from 'vue-router'
import { isTokenValid } from '../utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/company-select',
    name: 'CompanySelect',
    component: () => import('../views/CompanySelect.vue')
  },
  {
    path: '/wage-print/:id',
    name: 'WagePrint',
    component: () => import('../views/WagePrint.vue'),
    meta: { title: '打印工资条' }
  },
  // ===== PC端路由 =====
  {
    path: '/',
    component: () => import('../views/layout/Layout.vue'),
    redirect: '/dashboard',
    meta: { layout: 'pc' },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'dashboard-board',
        name: 'DashboardBoard',
        component: () => import('../views/DashboardBoard.vue'),
        meta: { title: '工作看板' }
      },
      {
        path: 'history-board',
        name: 'HistoryBoard',
        component: () => import('../views/HistoryBoard.vue'),
        meta: { title: '历史工作' }
      },
      {
        path: 'employee',
        name: 'Employee',
        component: () => import('../views/Employee.vue'),
        meta: { title: '员工管理', allowedRoles: ['boss', 'supervisor', 'superadmin'] }
      },
      {
        path: 'company-manage',
        name: 'CompanyManage',
        component: () => import('../views/CompanyManage.vue'),
        meta: { title: '公司管理', allowedRoles: ['superadmin'] }
      },
      {
        path: 'process-template',
        name: 'ProcessTemplate',
        component: () => import('../views/ProcessTemplate.vue'),
        meta: { title: '工序模板' }
      },
      {
        path: 'style',
        name: 'Style',
        component: () => import('../views/Bundle.vue'),
        meta: { title: '生产管理' }
      },
      {
        path: 'quick-entry',
        name: 'QuickEntry',
        component: () => import('../views/QuickEntry.vue'),
        meta: { title: '快速录入' }
      },
      {
        path: 'bundle',
        redirect: (to) => ({ path: '/style', query: to.query })
      },
      {
        path: 'bundle/print',
        name: 'QrPrint',
        component: () => import('../views/QrPrint.vue'),
        meta: { title: '二维码打印' }
      },
      {
        path: 'work-records',
        name: 'WorkRecords',
        component: () => import('../views/WorkRecords.vue'),
        meta: { title: '工作记录' }
      },
      {
        path: 'work-review',
        name: 'WorkReview',
        component: () => import('../views/WorkReview.vue'),
        meta: { title: '报工审核', allowedRoles: ['boss', 'supervisor', 'superadmin'] }
      },
      {
        path: 'employee-work',
        name: 'EmployeeWork',
        component: () => import('../views/EmployeeWork.vue'),
        meta: { title: '员工工作', allowedRoles: ['boss', 'supervisor', 'superadmin'] }
      },
      {
        path: 'shipment-daily',
        name: 'ShipmentDaily',
        component: () => import('../views/ShipmentDaily.vue'),
        meta: { title: '每日出货', allowedRoles: ['boss', 'superadmin'] }
      },
      {
        path: 'unshipped',
        name: 'UnshippedList',
        component: () => import('../views/UnshippedList.vue'),
        meta: { title: '未出货清单', allowedRoles: ['boss', 'superadmin'] }
      },
      {
        path: 'renew-logs',
        name: 'RenewLogs',
        component: () => import('../views/RenewLog.vue'),
        meta: { title: '续费记录', allowedRoles: ['superadmin'] }
      },
      {
        path: 'wage-adjustment',
        name: 'WageAdjustment',
        component: () => import('../views/WageAdjustment.vue'),
        meta: { title: '工资调整', allowedRoles: ['boss', 'supervisor', 'superadmin'] }
      },
      {
        path: 'wage-settlement',
        name: 'WageSettlement',
        component: () => import('../views/WageSettlement.vue'),
        meta: { title: '工资结算', allowedRoles: ['boss', 'supervisor', 'superadmin'] }
      },
      {
        path: 'analysis-api',
        name: 'AnalysisApi',
        component: () => import('../views/AnalysisApi.vue'),
        meta: { title: '数据分析', allowedRoles: ['boss', 'superadmin'] }
      },
      {
        path: 'notifications',
        name: 'SupervisorNotifications',
        component: () => import('../views/SupervisorNotifications.vue'),
        meta: { title: '通知' }
      },
      {
        path: 'boss-notifications',
        name: 'BossNotifications',
        component: () => import('../views/BossNotifications.vue'),
        meta: { title: '通知' }
      },
      {
        path: 'notification-manage',
        name: 'NotificationManage',
        component: () => import('../views/NotificationManage.vue'),
        meta: { title: '通知管理', allowedRoles: ['boss', 'supervisor', 'superadmin'] }
      }
    ]
  },
  // ===== 手机端路由（员工端） =====
  {
    path: '/mobile',
    component: () => import('../views/layout/MobileLayout.vue'),
    redirect: '/mobile/home',
    meta: { layout: 'mobile' },
    children: [
      {
        path: 'home',
        name: 'MobileHome',
        component: () => import('../views/mobile/MobileHome.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'scan',
        name: 'MobileScan',
        component: () => import('../views/mobile/MobileScan.vue'),
        meta: { title: '扫码报工' }
      },
      {
        path: 'records',
        name: 'MobileRecords',
        component: () => import('../views/mobile/MobileRecords.vue'),
        meta: { title: '报工记录' }
      },
      {
        path: 'profile',
        name: 'MobileProfile',
        component: () => import('../views/mobile/MobileProfile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'notifications',
        name: 'MobileNotifications',
        component: () => import('../views/mobile/MobileNotifications.vue'),
        meta: { title: '通知' }
      },
      {
        path: 'wage-sign',
        name: 'MobileWageSign',
        component: () => import('../views/mobile/MobileWageSign.vue'),
        meta: { title: '工资签名' }
      },
      {
        path: 'wage-records',
        name: 'MobileWageRecords',
        component: () => import('../views/mobile/MobileWageRecords.vue'),
        meta: { title: '工资调整' }
      }
    ]
  },
  // ===== 手机端路由（主管端） =====
  {
    path: '/mobile-supervisor',
    component: () => import('../views/layout/MobileSupervisorLayout.vue'),
    redirect: '/mobile-supervisor/home',
    meta: { layout: 'mobile-supervisor' },
    children: [
      {
        path: 'home',
        name: 'MobileSupervisorHome',
        component: () => import('../views/mobile/MobileSupervisorHome.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'scan',
        name: 'MobileSupervisorScan',
        component: () => import('../views/mobile/MobileScan.vue'),
        meta: { title: '扫码报工' }
      },
      {
        path: 'review',
        name: 'MobileSupervisorReview',
        component: () => import('../views/mobile/MobileSupervisorReview.vue'),
        meta: { title: '报工审核' }
      },
      {
        path: 'records',
        name: 'MobileSupervisorRecords',
        component: () => import('../views/mobile/MobileRecords.vue'),
        meta: { title: '报工记录' }
      },
      {
        path: 'wage-records',
        name: 'MobileSupervisorWageRecords',
        component: () => import('../views/mobile/MobileWageRecords.vue'),
        meta: { title: '工资调整' }
      },
      {
        path: 'wage-sign',
        name: 'MobileSupervisorWageSign',
        component: () => import('../views/mobile/MobileWageSign.vue'),
        meta: { title: '工资签名' }
      },
      {
        path: 'notifications',
        name: 'MobileSupervisorNotifications',
        component: () => import('../views/mobile/MobileSupervisorNotifications.vue'),
        meta: { title: '通知' }
      },
      {
        path: 'scan-bundle',
        name: 'MobileSupervisorScanBundle',
        component: () => import('../views/mobile/MobileSupervisorScanBundle.vue'),
        meta: { title: '扫码查扎' }
      },
      {
        path: 'ship',
        name: 'MobileSupervisorShip',
        component: () => import('../views/mobile/MobileSupervisorShip.vue'),
        meta: { title: '扫码出货' }
      },
      {
        path: 'board',
        name: 'MobileSupervisorBoard',
        component: () => import('../views/mobile/MobileSupervisorBoard.vue'),
        meta: { title: '工作看板' }
      },
      {
        path: 'profile',
        name: 'MobileSupervisorProfile',
        component: () => import('../views/mobile/MobileSupervisorProfile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  if (to.meta.public) {
    next()
    return
  }

  // 检查 token 是否存在且未过期
  if (!isTokenValid()) {
    // 记录原始路径，登录后自动返回（只接受站内相对路径，避免开放重定向）
    const redirect = to.fullPath.startsWith('/') ? encodeURIComponent(to.fullPath) : ''
    next(redirect ? `/login?redirect=${redirect}` : '/login')
    return
  }

  // 获取用户角色（从 localStorage）
  const currentRole = localStorage.getItem('current_role') || ''
  const isSuperAdmin = localStorage.getItem('user_type') === '0'

  // 角色白名单校验：如果路由配置了 allowedRoles，检查当前角色是否在允许列表中
  if (to.meta.allowedRoles) {
    const effectiveRole = isSuperAdmin ? 'superadmin' : currentRole
    if (!to.meta.allowedRoles.includes(effectiveRole)) {
      // 无权限，跳回首页
      if (currentRole === 'worker') {
        next('/mobile')
      } else {
        next('/dashboard')
      }
      return
    }
  }

  // 工人角色 && 访问PC端首页 → 自动跳转到手机端
  if (currentRole === 'worker' && to.path === '/') {
    next('/mobile')
    return
  }

  // 工人角色 && 访问PC端路由 → 自动跳转到手机端对应页面
  if (currentRole === 'worker' && to.meta.layout === 'pc') {
    const mobileMap = {
      '/dashboard': '/mobile/home',
      '/work-review': '/mobile/records',
      '/work-records': '/mobile/records',
      '/wage-settlement': '/mobile/wage-sign',
      '/wage-adjustment': '/mobile/wage-records'
    }
    const target = mobileMap[to.path]
    if (target) {
      next(target)
      return
    }
    // 其他PC页面工人无法访问，跳回手机首页
    next('/mobile')
    return
  }

  // 判断是否为小屏设备
  const isMobileScreen = window.innerWidth <= 768

  // 主管角色 && 小屏 && 访问PC端首页 → 跳转到主管手机端
  if (currentRole === 'supervisor' && isMobileScreen && to.path === '/') {
    next('/mobile-supervisor')
    return
  }

  // 主管角色 && 小屏 && 访问PC端路由 → 跳转到主管手机端对应页面
  if (currentRole === 'supervisor' && isMobileScreen && to.meta.layout === 'pc') {
    const mobileMap = {
      '/dashboard': '/mobile-supervisor/home',
      '/dashboard-board': '/mobile-supervisor/board',
      '/work-review': '/mobile-supervisor/review',
      '/work-records': '/mobile-supervisor/review',
      '/bundle': '/mobile-supervisor/scan',
      '/notifications': '/mobile-supervisor/notifications'
    }
    const target = mobileMap[to.path]
    if (target) {
      next(target)
      return
    }
    next('/mobile-supervisor')
    return
  }

  // 主管角色 && 大屏 && 访问手机端路由 → 跳转到PC端
  if (currentRole === 'supervisor' && !isMobileScreen && to.meta.layout === 'mobile-supervisor') {
    next('/dashboard')
    return
  }

  // 主管角色 && 小屏 && 访问工人手机端路由 → 跳转到主管手机端
  if (currentRole === 'supervisor' && isMobileScreen && to.meta.layout === 'mobile') {
    next('/mobile-supervisor')
    return
  }

  // 工人角色 && 访问主管手机端路由 → 跳转到工人手机端
  if (currentRole === 'worker' && to.meta.layout === 'mobile-supervisor') {
    next('/mobile')
    return
  }

  next()
})

export default router
