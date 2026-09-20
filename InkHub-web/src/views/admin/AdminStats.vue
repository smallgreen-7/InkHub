<template>
  <div class="stats">
    <div class="page-head">
      <h2 class="page-title">数据统计</h2>
    </div>

    <!-- 概览卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6"><div class="stat-card ink-card">
        <span class="stat-icon icon-article">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /></svg>
        </span>
        <div class="stat-info"><div class="num">{{ stats.articleCount ?? 0 }}</div><div class="label">文章总数</div></div>
      </div></el-col>
      <el-col :span="6"><div class="stat-card ink-card">
        <span class="stat-icon icon-user">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" /></svg>
        </span>
        <div class="stat-info"><div class="num">{{ stats.userCount ?? 0 }}</div><div class="label">用户总数</div></div>
      </div></el-col>
      <el-col :span="6"><div class="stat-card ink-card">
        <span class="stat-icon icon-comment">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
        </span>
        <div class="stat-info"><div class="num">{{ stats.commentCount ?? 0 }}</div><div class="label">评论总数</div></div>
      </div></el-col>
      <el-col :span="6"><div class="stat-card ink-card">
        <span class="stat-icon icon-view">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
        </span>
        <div class="stat-info"><div class="num">{{ stats.totalViews ?? 0 }}</div><div class="label">总浏览量</div></div>
      </div></el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="16">
      <el-col :span="12" class="chart-col">
        <div class="ink-card chart-card">
          <div class="chart-title">热门文章 Top10</div>
          <div ref="hotChart" class="chart"></div>
        </div>
      </el-col>
      <el-col :span="12" class="chart-col">
        <div class="ink-card chart-card">
          <div class="chart-title">分类分布</div>
          <div ref="catChart" class="chart"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getStats } from '@/api/admin'

const stats = ref({})
const hotChart = ref()
const catChart = ref()

onMounted(async () => {
  stats.value = await getStats()
  // 热门文章 Top10：横向柱状图（Map 字段是下划线：view_count）
  const hot = echarts.init(hotChart.value)
  hot.setOption({
    grid: { left: 130, right: 30, top: 10, bottom: 20 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f1f4' } }, axisLabel: { color: '#9ca3af' } },
    yAxis: {
      type: 'category',
      data: stats.value.hotArticles.map((h) => h.title.slice(0, 12)),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#374151', fontWeight: 500 },
    },
    series: [{
      type: 'bar',
      data: stats.value.hotArticles.map((h) => h.view_count),
      barWidth: 14,
      itemStyle: {
        borderRadius: [0, 7, 7, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#9bdbe8' },
          { offset: 1, color: '#1d9fc0' },
        ]),
      },
    }],
  })
  // 分类分布：环形饼图
  const cat = echarts.init(catChart.value)
  cat.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#6b7280' } },
    color: ['#1d9fc0', '#14304a', '#63c6da', '#9bdbe8', '#c9a87c', '#d09a3c', '#2f9e75', '#d4564f'],
    series: [{
      type: 'pie',
      radius: ['42%', '66%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 15, fontWeight: 700, color: '#111827' },
      },
      data: stats.value.categoryDistribution,
    }],
  })
})
</script>

<style scoped>
.page-head { margin-bottom: 18px; }
.page-title { margin: 0; font-size: 20px; font-weight: 800; color: var(--ink-ink); }

.stat-row { margin-bottom: 20px; }
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  border-radius: var(--ink-radius);
  margin-bottom: 16px;
}
.stat-icon {
  width: 46px; height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.icon-article { background: var(--ink-deep); box-shadow: 0 4px 12px rgba(12, 26, 40, 0.25); }
.icon-user { background: var(--ink-navy); box-shadow: 0 4px 12px rgba(20, 48, 74, 0.25); }
.icon-comment { background: var(--ink-accent); box-shadow: 0 4px 12px rgba(29, 159, 192, 0.28); }
.icon-view { background: #c9a87c; box-shadow: 0 4px 12px rgba(201, 168, 124, 0.28); }

.stat-info .num { font-size: 26px; font-weight: 800; color: var(--ink-ink); line-height: 1.2; }
.stat-info .label { font-size: 13px; color: var(--ink-faint); }

.chart-col { margin-bottom: 16px; }
.chart-card { padding: 20px; border-radius: var(--ink-radius); }
.chart-title { font-size: 15px; font-weight: 700; color: var(--ink-ink); margin-bottom: 12px; }
.chart { height: 340px; }
</style>
