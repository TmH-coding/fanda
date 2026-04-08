<template>
  <view class="fd-page">
    <fd-nav-bar title="拼饭广场" />

    <!-- 发起拼饭按钮 -->
    <view class="create-btn" @tap="showCreate = true">
      <text class="create-btn__text">🍽️ 发起拼饭</text>
    </view>

    <!-- 拼饭列表 -->
    <view v-if="socialStore.openGroups.length === 0">
      <fd-empty icon="🤝" text="暂时没有拼饭局" btn-text="发起一个" @action="showCreate = true" />
    </view>

    <view v-for="group in socialStore.openGroups" :key="group.id" class="group-card fd-card">
      <view class="group-header">
        <text class="group-avatar">{{ group.avatar }}</text>
        <view class="group-creator">
          <text class="group-name">{{ group.creator }}</text>
          <text class="group-time">⏰ {{ group.time }}</text>
        </view>
        <view class="group-people">
          <text class="group-count">{{ group.currentPeople }}/{{ group.maxPeople }}</text>
          <text class="group-count-label">人</text>
        </view>
      </view>

      <text class="group-title">{{ group.title }}</text>
      <text class="group-location">📍 {{ group.location }}</text>

      <view class="group-tags">
        <text v-for="tag in group.tags" :key="tag" class="fd-tag--accent">{{ tag }}</text>
      </view>

      <!-- 投票 -->
      <view class="vote-section">
        <text class="vote-title">投票选餐厅</text>
        <view v-for="(candidate, idx) in group.candidates" :key="idx" class="vote-item" @tap="onVote(group.id, candidate)">
          <text class="vote-name">{{ candidate }}</text>
          <view class="vote-bar">
            <view class="vote-fill" :style="{ width: votePercent(group, candidate) + '%' }"></view>
          </view>
          <text class="vote-count">{{ group.votes[candidate] || 0 }}票</text>
        </view>
      </view>

      <view v-if="group.currentPeople < group.maxPeople" class="group-join fd-btn" @tap="onJoin(group.id)">
        <text>加入 🙋</text>
      </view>
      <view v-else class="group-full">
        <text class="fd-text-light">已满员</text>
      </view>
    </view>

    <!-- 发起拼饭弹窗 -->
    <fd-modal v-model:visible="showCreate" title="发起拼饭" @confirm="onCreate">
      <view class="form-item">
        <text class="form-label">标题</text>
        <input class="form-input" v-model="form.title" placeholder="想吃什么？一起来！" />
      </view>
      <view class="form-item">
        <text class="form-label">时间</text>
        <input class="form-input" v-model="form.time" placeholder="如 12:00" />
      </view>
      <view class="form-item">
        <text class="form-label">地点</text>
        <input class="form-input" v-model="form.location" placeholder="在哪吃" />
      </view>
      <view class="form-item">
        <text class="form-label">人数上限</text>
        <input class="form-input" type="number" v-model="form.maxPeople" placeholder="4" />
      </view>
    </fd-modal>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useSocialStore } from '@/stores/modules/social'
import { generateId } from '@/utils/format'
import FdNavBar from '@/components/common/fd-nav-bar.vue'
import FdEmpty from '@/components/common/fd-empty.vue'
import FdModal from '@/components/common/fd-modal.vue'

const socialStore = useSocialStore()
const showCreate = ref(false)

const form = ref({
  title: '',
  time: '',
  location: '',
  maxPeople: '4',
})

function votePercent(group, candidate) {
  const total = Object.values(group.votes).reduce((s, v) => s + v, 0)
  if (total === 0) return 0
  return Math.round(((group.votes[candidate] || 0) / total) * 100)
}

function onVote(groupId, candidate) {
  socialStore.vote(groupId, candidate)
}

function onJoin(groupId) {
  socialStore.join(groupId)
  uni.showToast({ title: '已加入 🎉', icon: 'none' })
}

function onCreate() {
  if (!form.value.title) {
    uni.showToast({ title: '请填写标题', icon: 'none' })
    return
  }
  const group = {
    id: generateId('g_'),
    creator: '我',
    avatar: '😎',
    title: form.value.title,
    time: form.value.time || '12:00',
    location: form.value.location || '待定',
    maxPeople: Number(form.value.maxPeople) || 4,
    currentPeople: 1,
    candidates: [],
    votes: {},
    status: 'open',
    tags: [],
  }
  socialStore.create(group)
  showCreate.value = false
  form.value = { title: '', time: '', location: '', maxPeople: '4' }
  uni.showToast({ title: '发起成功 🎉', icon: 'none' })
}

onMounted(async () => {
  await socialStore.load()
})
</script>

<style lang="scss" scoped>
.create-btn {
  margin: $fd-space-base $fd-space-md;
  @include fd-btn;
  @include fd-flex-center;
  padding: 24rpx;
  &__text { font-size: $fd-font-md; }
}

.group-card {
  margin-bottom: $fd-space-sm;
}
.group-header {
  display: flex;
  align-items: center;
  margin-bottom: $fd-space-sm;
}
.group-avatar {
  font-size: 60rpx;
  margin-right: $fd-space-sm;
}
.group-creator { flex: 1; }
.group-name {
  font-size: $fd-font-base;
  font-weight: 600;
  display: block;
}
.group-time {
  font-size: $fd-font-xs;
  color: $fd-text-light;
}
.group-people {
  @include fd-flex-column;
  align-items: center;
}
.group-count {
  font-size: $fd-font-lg;
  font-weight: 800;
  color: $fd-primary;
}
.group-count-label {
  font-size: $fd-font-xs;
  color: $fd-text-light;
}

.group-title {
  font-size: $fd-font-md;
  font-weight: 700;
  display: block;
  margin-bottom: 6rpx;
}
.group-location {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  display: block;
  margin-bottom: $fd-space-sm;
}
.group-tags {
  display: flex;
  gap: $fd-space-xs;
  margin-bottom: $fd-space-sm;
}

.vote-section {
  margin: $fd-space-sm 0;
}
.vote-title {
  font-size: $fd-font-sm;
  font-weight: 600;
  color: $fd-text-secondary;
  margin-bottom: $fd-space-xs;
  display: block;
}
.vote-item {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
  margin-bottom: 8rpx;
  &:active { opacity: 0.7; }
}
.vote-name {
  width: 140rpx;
  font-size: $fd-font-sm;
}
.vote-bar {
  flex: 1;
  height: 24rpx;
  background: #f0f0f0;
  border-radius: 12rpx;
  overflow: hidden;
}
.vote-fill {
  height: 100%;
  background: linear-gradient(90deg, $fd-primary, $fd-primary-light);
  border-radius: 12rpx;
  transition: width 0.3s ease;
}
.vote-count {
  width: 80rpx;
  font-size: $fd-font-xs;
  color: $fd-text-light;
  text-align: right;
}

.group-join {
  margin-top: $fd-space-sm;
  text-align: center;
}
.group-full {
  text-align: center;
  margin-top: $fd-space-sm;
}

/* 表单 */
.form-item {
  margin-bottom: $fd-space-base;
}
.form-label {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  margin-bottom: 8rpx;
  display: block;
}
.form-input {
  width: 100%;
  height: 72rpx;
  border: 2rpx solid $fd-border;
  border-radius: $fd-radius-sm;
  padding: 0 $fd-space-sm;
  font-size: $fd-font-base;
}
</style>
