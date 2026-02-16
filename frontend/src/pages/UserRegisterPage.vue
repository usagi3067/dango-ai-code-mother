<template>
  <div class="auth-container">
    <AuthBrandPanel />
    <div class="auth-right">
      <div class="mobile-logo">
        <img src="@/assets/logo.svg" alt="Logo" class="mobile-logo-img" />
        <span class="mobile-logo-text">Dango AI Code</span>
      </div>
      <div class="form-wrapper">
        <h2 class="form-title">创建账号</h2>
        <p class="form-subtitle">注册后即可开始使用</p>
        <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" placeholder="请输入账号" size="large">
              <template #prefix><UserOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
            </a-input>
          </a-form-item>
          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码不能少于 8 位' }
            ]"
          >
            <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" size="large">
              <template #prefix><LockOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item
            name="checkPassword"
            :rules="[
              { required: true, message: '请再次输入密码' },
              { validator: validatePassword }
            ]"
          >
            <a-input-password v-model:value="formState.checkPassword" placeholder="请再次输入密码" size="large">
              <template #prefix><LockOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
            </a-input-password>
          </a-form-item>
          <div class="form-links">
            已有账号？<RouterLink to="/user/login">去登录</RouterLink>
          </div>
          <a-form-item>
            <a-button type="primary" html-type="submit" size="large" block>注册</a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import AuthBrandPanel from '@/components/AuthBrandPanel.vue'
import { userRegister } from '@/api/user/userController'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validatePassword = (_rule: any, value: string) => {
  if (value !== formState.userPassword) {
    return Promise.reject(new Error('两次密码不一致'))
  }
  return Promise.resolve()
}

const handleSubmit = async (values: any) => {
  const res = await userRegister(values)
  if (res.data.code === 0 && res.data.data) {
    message.success('注册成功，请登录')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  min-height: 100vh;
}

.auth-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #fff;
}

.mobile-logo { display: none; }
.mobile-logo-img { width: 32px; height: 32px; }
.mobile-logo-text { font-size: 18px; font-weight: 600; color: #1a1a2e; }

.form-wrapper { width: 100%; max-width: 380px; }
.form-title { font-size: 28px; font-weight: 600; color: #1a1a2e; margin-bottom: 8px; }
.form-subtitle { font-size: 14px; color: #999; margin-bottom: 32px; }
.form-links { text-align: center; margin-bottom: 16px; color: #666; font-size: 14px; }
.form-links a { color: #667eea; text-decoration: none; }
.form-links a:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .auth-right { flex: none; width: 100%; min-height: 100vh; }
  .mobile-logo { display: flex; align-items: center; gap: 10px; margin-bottom: 40px; }
}
</style>