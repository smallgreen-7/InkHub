<template>
  <div class="admin-category">
    <div class="page-head">
      <h2 class="page-title">分类标签</h2>
    </div>

    <el-row :gutter="20">
      <!-- 分类管理 -->
      <el-col :span="12" class="col">
        <div class="ink-card panel">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">分类管理</h3>
              <p class="panel-sub">组织文章内容结构</p>
            </div>
            <el-button type="primary" class="ink-gradient-btn" @click="openCatDialog()">+ 新增分类</el-button>
          </div>
          <el-table :data="categories" stripe>
            <el-table-column prop="name" label="分类名" min-width="120" />
            <el-table-column prop="articleCount" label="文章数" width="90" align="center">
              <template #default="{ row }">
                <span class="count-badge">{{ row.articleCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openCatDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="delCat(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <!-- 标签管理 -->
      <el-col :span="12" class="col">
        <div class="ink-card panel">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">标签管理</h3>
              <p class="panel-sub">为文章添加精细标记</p>
            </div>
            <el-button type="primary" class="ink-gradient-btn" @click="openTagDialog()">+ 新增标签</el-button>
          </div>
          <el-table :data="tags" stripe>
            <el-table-column prop="name" label="标签名" min-width="120">
              <template #default="{ row }">
                <span class="tag-chip"># {{ row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="articleCount" label="文章数" width="90" align="center">
              <template #default="{ row }">
                <span class="count-badge">{{ row.articleCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="right">
              <template #default="{ row }">
                <el-button link type="danger" @click="delTag(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- 分类新增/编辑弹窗 -->
    <el-dialog v-model="catDialog.show" :title="catDialog.form.id ? '编辑分类' : '新增分类'" width="380px" align-center>
      <el-form label-width="70px">
        <el-form-item label="分类名">
          <el-input v-model="catDialog.form.name" maxlength="50" placeholder="请输入分类名" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="catDialog.form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catDialog.show = false">取消</el-button>
        <el-button type="primary" class="ink-gradient-btn" @click="saveCat">保存</el-button>
      </template>
    </el-dialog>

    <!-- 标签新增弹窗 -->
    <el-dialog v-model="tagDialog.show" title="新增标签" width="380px" align-center>
      <el-form label-width="70px">
        <el-form-item label="标签名">
          <el-input v-model="tagDialog.name" maxlength="50" placeholder="请输入标签名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialog.show = false">取消</el-button>
        <el-button type="primary" class="ink-gradient-btn" @click="saveTag">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategories, getTags } from '@/api/category'
import { addCategory, updateCategory, deleteCategory, addTag, deleteTag } from '@/api/admin'

const categories = ref([])
const tags = ref([])

// 分类弹窗：show 开关 + 表单（id 有值 = 编辑）
const catDialog = reactive({ show: false, form: { id: null, name: '', sort: 0 } })
// 标签弹窗
const tagDialog = reactive({ show: false, name: '' })

async function load() {
  categories.value = await getCategories()
  tags.value = await getTags()
}
function openCatDialog(row = null) {
  catDialog.form = row ? { id: row.id, name: row.name, sort: row.sort ?? 0 } : { id: null, name: '', sort: 0 }
  catDialog.show = true
}
async function saveCat() {
  if (!catDialog.form.name.trim()) return ElMessage.warning('分类名不能为空')
  if (catDialog.form.id) await updateCategory(catDialog.form.id, catDialog.form)
  else await addCategory(catDialog.form)
  ElMessage.success('保存成功')
  catDialog.show = false
  load()
}
async function delCat(row) {
  await ElMessageBox.confirm(`删除分类「${row.name}」？（文章会变成无分类）`, '提示', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  load()
}
function openTagDialog() {
  tagDialog.name = ''
  tagDialog.show = true
}
async function saveTag() {
  if (!tagDialog.name.trim()) return ElMessage.warning('标签名不能为空')
  await addTag({ name: tagDialog.name.trim() })
  ElMessage.success('保存成功')
  tagDialog.show = false
  load()
}
async function delTag(row) {
  await ElMessageBox.confirm(`删除标签「${row.name}」？`, '提示', { type: 'warning' })
  await deleteTag(row.id)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

<style scoped>
.page-head { margin-bottom: 18px; }
.page-title { margin: 0; font-size: 20px; font-weight: 800; color: var(--ink-ink); }

.col { margin-bottom: 20px; }
.panel { padding: 20px 20px 10px; border-radius: var(--ink-radius); }
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}
.panel-title { margin: 0; font-size: 16px; font-weight: 700; color: var(--ink-ink); }
.panel-sub { margin: 4px 0 0; font-size: 12.5px; color: var(--ink-faint); }

.count-badge {
  background: var(--ink-primary-50);
  color: var(--ink-primary);
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
  padding: 1px 10px;
}
.tag-chip {
  color: var(--ink-primary);
  background: var(--ink-primary-50);
  border-radius: 999px;
  padding: 2px 12px;
  font-size: 12.5px;
}
</style>
