<template>
  <div class="settings-page">
    <h2 class="page-title">个人设置</h2>

    <!-- 基本信息区 -->
    <a-card title="基本信息" class="settings-card">
      <a-form :model="profileForm" layout="vertical" @finish="handleProfileSubmit">
        <a-form-item label="头像">
          <div class="avatar-upload">
            <a-upload
              :show-upload-list="false"
              :before-upload="handleAvatarUpload"
              accept="image/jpeg,image/png,image/gif,image/webp"
            >
              <div class="avatar-wrapper">
                <UserAvatar
                  :src="profileForm.userAvatar"
                  :name="profileForm.userName"
                  :size="80"
                />
                <div class="avatar-overlay">
                  <CameraOutlined />
                </div>
              </div>
            </a-upload>
            <a-spin v-if="avatarUploading" class="avatar-spin" />
          </div>
        </a-form-item>

        <a-form-item
          label="昵称"
          name="userName"
          :rules="[
            { required: true, message: '请输入昵称' },
            { min: 2, max: 20, message: '昵称长度需在 2-20 字符之间' }
          ]"
        >
          <a-input v-model:value="profileForm.userName" placeholder="请输入昵称" />
        </a-form-item>

        <a-form-item
          label="简介"
          name="userProfile"
          :rules="[{ max: 200, message: '简介不能超过 200 字符' }]"
        >
          <a-textarea
            v-model:value="profileForm.userProfile"
            placeholder="介绍一下自己吧"
            :rows="3"
            :maxlength="200"
            show-count
          />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="profileLoading">
            保存修改
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 修改密码区 -->
    <a-card title="修改密码" class="settings-card">
      <a-form
        :model="passwordForm"
        layout="vertical"
        @finish="handlePasswordSubmit"
        ref="passwordFormRef"
      >
        <a-form-item
          label="旧密码"
          name="oldPassword"
          :rules="[{ required: true, message: '请输入旧密码' }]"
        >
          <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入旧密码" />
        </a-form-item>

        <a-form-item
          label="新密码"
          name="newPassword"
          :rules="[
            { required: true, message: '请输入新密码' },
            { min: 8, message: '密码至少 8 位' }
          ]"
        >
          <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码" />
        </a-form-item>

        <a-form-item
          label="确认密码"
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认新密码' },
            { validator: validateCheckPassword }
          ]"
        >
          <a-input-password
            v-model:value="passwordForm.checkPassword"
            placeholder="请再次输入新密码"
          />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="passwordLoading">
            修改密码
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { CameraOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { updateMyProfile, uploadAvatar, changePassword } from '@/api/user/userController'
import UserAvatar from '@/components/UserAvatar.vue'

const loginUserStore = useLoginUserStore()
const passwordFormRef = ref()

// 基本信息表单
const profileForm = reactive({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  checkPassword: '',
})

const profileLoading = ref(false)
const passwordLoading = ref(false)
const avatarUploading = ref(false)

// 初始化表单数据
onMounted(() => {
  const user = loginUserStore.loginUser
  profileForm.userName = user.userName || ''
  profileForm.userAvatar = user.userAvatar || ''
  profileForm.userProfile = user.userProfile || ''
})

// 头像上传
const handleAvatarUpload = async (file: File) => {
  if (file.size > 2 * 1024 * 1024) {
    message.error('头像文件不能超过 2MB')
    return false
  }
  avatarUploading.value = true
  try {
    const res = await uploadAvatar(file)
    if (res.data.code === 0 && res.data.data) {
      profileForm.userAvatar = res.data.data
      message.success('头像上传成功')
    } else {
      message.error('头像上传失败：' + (res.data.message || '未知错误'))
    }
  } catch (e) {
    message.error('头像上传失败')
  } finally {
    avatarUploading.value = false
  }
  return false // 阻止 antd 默认上传
}

// 保存基本信息
const handleProfileSubmit = async () => {
  profileLoading.value = true
  try {
    const res = await updateMyProfile({
      userName: profileForm.userName,
      userAvatar: profileForm.userAvatar,
      userProfile: profileForm.userProfile,
    })
    if (res.data.code === 0) {
      message.success('资料更新成功')
      await loginUserStore.fetchLoginUser()
    } else {
      message.error('更新失败：' + (res.data.message || '未知错误'))
    }
  } catch (e) {
    message.error('更新失败')
  } finally {
    profileLoading.value = false
  }
}

// 确认密码校验
const validateCheckPassword = (_rule: any, value: string) => {
  if (value && value !== passwordForm.newPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

// 修改密码
const handlePasswordSubmit = async () => {
  passwordLoading.value = true
  try {
    const res = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      checkPassword: passwordForm.checkPassword,
    })
    if (res.data.code === 0) {
      message.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.checkPassword = ''
      passwordFormRef.value?.resetFields()
    } else {
      message.error('修改失败：' + (res.data.message || '未知错误'))
    }
  } catch (e) {
    message.error('修改失败')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.settings-page {
  max-width: 600px;
  margin: 24px auto;
  padding: 0 16px;
}

.page-title {
  margin-bottom: 24px;
  font-size: 24px;
  font-weight: 600;
}

.settings-card {
  margin-bottom: 24px;
}

.avatar-upload {
  display: inline-block;
  position: relative;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 50%;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-spin {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
</style>
