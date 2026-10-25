<template>
  <div class="app-shell">
    <header class="topbar"><div class="topbar-inner">
      <div class="brand" @click="$router.push('/home')"><div class="brand-mark">C</div><div><strong>CampusHub</strong><small>校园生活服务平台</small></div></div>
      <el-menu :default-active="$route.path" mode="horizontal" router :ellipsis="false" class="nav-menu">
        <el-menu-item index="/home">首页</el-menu-item><el-menu-item index="/secondhand">二手交易</el-menu-item><el-menu-item index="/activities">校园活动</el-menu-item><el-menu-item index="/community">问答社区</el-menu-item>
      </el-menu>
      <div class="user-area"><el-button text @click="$router.push('/cart')"><el-icon><ShoppingCart /></el-icon>购物车</el-button><el-button type="primary" plain round @click="$router.push('/login')">登录</el-button></div>
    </div></header>
    <main class="page-wrap"><router-view /></main>
  </div>
</template>
