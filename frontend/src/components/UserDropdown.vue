<!--
  UserDropdown.vue - 用户下拉菜单组件

  【功能】
  显示用户头像和用户名，悬浮时显示下拉菜单（退出登录）
  未登录时显示登录按钮
-->
<template>
  <template v-if="loginUserStore.loginUser.id">
    <a-dropdown>
      <a-space class="user-info">
        <UserAvatar
          :src="loginUserStore.loginUser.userAvatar"
          :name="loginUserStore.loginUser.userName"
          :size="size"
        />
        <span v-if="showName" class="user-name">
          {{ loginUserStore.loginUser.userName ?? '无名' }}
        </span>
      </a-space>
      <template #overlay>
        <a-menu>
          <a-menu-item @click="doLogout">
            <LogoutOutlined />
            退出登录
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </template>
  <a-button v-else type="primary" @click="router.push('/user/login')">
    登录
  </a-button>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LogoutOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/user/userController'
import UserAvatar from '@/components/UserAvatar.vue'

/**
 * Props
 */
withDefaults(defineProps<{
  size?: number       // 头像大小
  showName?: boolean  // 是否显示用户名
}>(), {
  size: 32,
  showName: true
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

/**
 * 退出登录
 */
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.user-info {
  cursor: pointer;
}

.user-name {
  color: #333;
  font-size: 14px;
}
</style>
