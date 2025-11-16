<template>
  <div class="knowledge-base-detail">
    <!-- 头部 -->
    <el-card class="header-card">
      <div class="header">
        <div class="back-title">
          <el-button link @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <div class="title">
            <el-icon size="24" color="#409eff"><Collection /></el-icon>
            <h2>{{ knowledgeBase?.name || '加载中...' }}</h2>
            <el-tag v-if="knowledgeBase" :type="knowledgeBase.status === 1 ? 'success' : 'danger'" effect="plain">
              {{ knowledgeBase.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>
        </div>
      </div>
      <div v-if="knowledgeBase" class="info">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="知识库ID">{{ knowledgeBase.id }}</el-descriptions-item>
          <el-descriptions-item label="向量数量">
            <el-tag type="info">{{ knowledgeBase.vectorCount }} 个</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ knowledgeBase.createTime }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="3">
            {{ knowledgeBase.description || '无描述' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="action-card">
      <div class="actions">
        <div class="left-actions">
          <el-button type="primary" @click="showUploadDialog">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
          <el-button @click="showAddVectorDialog">
            <el-icon><Plus /></el-icon>
            手动添加向量
          </el-button>
          <el-button @click="showSearchDialog">
            <el-icon><Search /></el-icon>
            相似性搜索
          </el-button>
        </div>
        <div class="right-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文件名"
            clearable
            style="width: 300px"
            @clear="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </el-card>

    <!-- 向量列表 -->
    <el-card class="vector-list-card">
      <el-table
        v-loading="loading"
        :data="vectorList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="向量ID" width="300" show-overflow-tooltip />
        <el-table-column prop="fileName" label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-cell">
              <el-icon><Document /></el-icon>
              <span>{{ row.fileName || '未命名' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="文件类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.fileType" size="small" effect="plain">{{ row.fileType }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容预览" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              link
              @click="handleViewVector(row)"
            >
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button
              type="warning"
              size="small"
              link
              @click="handleEditVector(row)"
            >
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-popconfirm
              title="确定要删除这个向量吗？"
              @confirm="handleDeleteVector(row.id)"
            >
              <template #reference>
                <el-button
                  type="danger"
                  size="small"
                  link
                >
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 上传文件对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件到知识库" width="600px">
      <el-upload
        ref="uploadRef"
        class="upload-demo"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept=".pdf,.doc,.docx,.txt,.md"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、TXT、Markdown 等文件格式
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleUpload"
          :loading="uploadLoading"
          :disabled="!selectedFile"
        >
          确定上传
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑向量对话框 -->
    <el-dialog
      v-model="vectorDialogVisible"
      :title="vectorDialogTitle"
      width="700px"
      @close="handleVectorDialogClose"
    >
      <el-form
        ref="vectorFormRef"
        :model="vectorForm"
        :rules="vectorRules"
        label-width="100px"
      >
        <el-form-item label="文件名" prop="fileName">
          <el-input v-model="vectorForm.fileName" placeholder="请输入文件名" />
        </el-form-item>
        <el-form-item label="文件类型" prop="fileType">
          <el-input v-model="vectorForm.fileType" placeholder="例如：txt, pdf" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="vectorForm.content"
            type="textarea"
            :rows="10"
            placeholder="请输入文档内容"
            maxlength="10000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="元数据">
          <el-input
            v-model="vectorForm.metadata"
            type="textarea"
            :rows="3"
            placeholder='可选，JSON格式，例如：{"source": "manual"}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vectorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleVectorSubmit" :loading="vectorSubmitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看向量详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="向量详情" width="800px">
      <el-descriptions v-if="currentVector" :column="1" border>
        <el-descriptions-item label="向量ID">{{ currentVector.id }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ currentVector.fileName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ currentVector.fileType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentVector.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentVector.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="元数据">
          <pre v-if="currentVector.metadata" style="margin: 0">{{ currentVector.metadata }}</pre>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="内容">
          <div class="content-view">{{ currentVector.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 相似性搜索对话框 -->
    <el-dialog v-model="searchDialogVisible" title="相似性搜索" width="900px">
      <el-form :inline="true">
        <el-form-item label="查询内容">
          <el-input
            v-model="searchQuery"
            placeholder="请输入要搜索的内容"
            style="width: 400px"
          />
        </el-form-item>
        <el-form-item label="返回数量">
          <el-input-number v-model="searchTopK" :min="1" :max="20" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSimilaritySearch" :loading="searchLoading">
            搜索
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <div v-if="searchResults.length > 0" class="search-results">
        <h3>搜索结果（{{ searchResults.length }} 条）</h3>
        <el-card
          v-for="(result, index) in searchResults"
          :key="result.id"
          class="result-card"
          shadow="hover"
        >
          <div class="result-header">
            <span class="result-index">#{{ index + 1 }}</span>
            <el-tag v-if="result.fileName" size="small">{{ result.fileName }}</el-tag>
            <span class="result-time">{{ result.createTime }}</span>
          </div>
          <div class="result-content">
            {{ result.content }}
          </div>
        </el-card>
      </div>
      <el-empty v-else description="暂无搜索结果" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type UploadFile } from 'element-plus'
import {
  ArrowLeft,
  Collection,
  Upload,
  Plus,
  Search,
  Document,
  View,
  Edit,
  Delete,
  UploadFilled
} from '@element-plus/icons-vue'
import { KnowledgeBaseAPI, KnowledgeVectorAPI } from '@/api/knowledge'
import type {
  KnowledgeBase,
  KnowledgeVector,
  KnowledgeVectorAddRequest,
  KnowledgeVectorUpdateRequest
} from '@/types/knowledge'

const router = useRouter()
const route = useRoute()

// 知识库ID
const knowledgeBaseId = computed(() => Number(route.params.id))

// 知识库信息
const knowledgeBase = ref<KnowledgeBase>()

// 向量列表
const vectorList = ref<KnowledgeVector[]>([])
const loading = ref(false)
const searchKeyword = ref('')

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 上传对话框
const uploadDialogVisible = ref(false)
const uploadRef = ref()
const selectedFile = ref<File>()
const uploadLoading = ref(false)

// 向量对话框
const vectorDialogVisible = ref(false)
const vectorDialogTitle = ref('添加向量')
const vectorFormRef = ref<FormInstance>()
const vectorSubmitLoading = ref(false)

const vectorForm = reactive<KnowledgeVectorAddRequest & { id?: string }>({
  knowledgeBaseId: knowledgeBaseId.value,
  content: '',
  fileName: '',
  fileType: '',
  metadata: ''
})

const vectorRules = {
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' }
  ]
}

// 查看向量详情
const viewDialogVisible = ref(false)
const currentVector = ref<KnowledgeVector>()

// 相似性搜索
const searchDialogVisible = ref(false)
const searchQuery = ref('')
const searchTopK = ref(5)
const searchLoading = ref(false)
const searchResults = ref<KnowledgeVector[]>([])

// 选中的行
const selectedRows = ref<KnowledgeVector[]>([])

// 加载知识库信息
const loadKnowledgeBase = async () => {
  try {
    const response = await KnowledgeBaseAPI.get(knowledgeBaseId.value)
    if (response.code === 1 && response.data) {
      knowledgeBase.value = response.data
    }
  } catch (error) {
    console.error('加载知识库信息失败:', error)
  }
}

// 加载向量列表
const loadVectorList = async () => {
  loading.value = true
  try {
    const response = await KnowledgeVectorAPI.page({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      knowledgeBaseId: knowledgeBaseId.value,
      fileName: searchKeyword.value || undefined
    })

    if (response.code === 1 && response.data) {
      vectorList.value = response.data.records
      pagination.total = response.data.total
    }
  } catch (error) {
    console.error('加载向量列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 返回
const goBack = () => {
  router.back()
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadVectorList()
}

// 分页变化
const handleSizeChange = () => {
  loadVectorList()
}

const handleCurrentChange = () => {
  loadVectorList()
}

// 显示上传对话框
const showUploadDialog = () => {
  selectedFile.value = undefined
  uploadDialogVisible.value = true
}

// 文件选择
const handleFileChange = (file: UploadFile) => {
  selectedFile.value = file.raw
}

// 上传文件
const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploadLoading.value = true
  try {
    const response = await KnowledgeVectorAPI.uploadFile(knowledgeBaseId.value, selectedFile.value)
    if (response.code === 1) {
      ElMessage.success('上传成功')
      uploadDialogVisible.value = false
      loadVectorList()
      loadKnowledgeBase() // 刷新向量数量
    } else {
      ElMessage.error(response.message || '上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
  } finally {
    uploadLoading.value = false
  }
}

// 显示添加向量对话框
const showAddVectorDialog = () => {
  vectorDialogTitle.value = '添加向量'
  Object.assign(vectorForm, {
    knowledgeBaseId: knowledgeBaseId.value,
    content: '',
    fileName: '',
    fileType: '',
    metadata: ''
  })
  delete vectorForm.id
  vectorDialogVisible.value = true
}

// 编辑向量
const handleEditVector = (row: KnowledgeVector) => {
  vectorDialogTitle.value = '编辑向量'
  Object.assign(vectorForm, {
    id: row.id,
    knowledgeBaseId: row.knowledgeBaseId,
    content: row.content,
    fileName: row.fileName,
    fileType: row.fileType,
    metadata: row.metadata
  })
  vectorDialogVisible.value = true
}

// 提交向量表单
const handleVectorSubmit = async () => {
  if (!vectorFormRef.value) return

  await vectorFormRef.value.validate(async (valid) => {
    if (valid) {
      vectorSubmitLoading.value = true
      try {
        let response
        if (vectorForm.id) {
          // 编辑：只传递UpdateRequest需要的字段
          response = await KnowledgeVectorAPI.update({
            id: vectorForm.id,
            content: vectorForm.content,
            fileName: vectorForm.fileName,
            fileType: vectorForm.fileType,
            metadata: vectorForm.metadata
          })
        } else {
          // 新增：传递完整字段
          response = await KnowledgeVectorAPI.add({
            knowledgeBaseId: vectorForm.knowledgeBaseId,
            content: vectorForm.content,
            fileName: vectorForm.fileName,
            fileType: vectorForm.fileType,
            metadata: vectorForm.metadata
          })
        }

        if (response.code === 1) {
          ElMessage.success(vectorForm.id ? '更新成功' : '添加成功')
          vectorDialogVisible.value = false
          loadVectorList()
          loadKnowledgeBase()
        } else {
          ElMessage.error(response.message || '操作失败')
        }
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('操作失败')
      } finally {
        vectorSubmitLoading.value = false
      }
    }
  })
}

// 向量对话框关闭
const handleVectorDialogClose = () => {
  vectorFormRef.value?.resetFields()
}

// 查看向量详情
const handleViewVector = async (row: KnowledgeVector) => {
  currentVector.value = row
  viewDialogVisible.value = true
}

// 删除向量
const handleDeleteVector = async (id: string) => {
  try {
    const response = await KnowledgeVectorAPI.delete(id)
    if (response.code === 1) {
      ElMessage.success('删除成功')
      loadVectorList()
      loadKnowledgeBase()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    console.error('删除向量失败:', error)
    ElMessage.error('删除失败')
  }
}

// 显示搜索对话框
const showSearchDialog = () => {
  searchQuery.value = ''
  searchResults.value = []
  searchDialogVisible.value = true
}

// 相似性搜索
const handleSimilaritySearch = async () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入搜索内容')
    return
  }

  searchLoading.value = true
  try {
    const response = await KnowledgeVectorAPI.similaritySearch(
      knowledgeBaseId.value,
      searchQuery.value,
      searchTopK.value
    )

    if (response.code === 1 && response.data) {
      searchResults.value = response.data
      if (response.data.length === 0) {
        ElMessage.info('未找到相关结果')
      }
    } else {
      ElMessage.error(response.message || '搜索失败')
    }
  } catch (error) {
    console.error('相似性搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    searchLoading.value = false
  }
}

// 选择变化
const handleSelectionChange = (selection: KnowledgeVector[]) => {
  selectedRows.value = selection
}

// 初始化
onMounted(() => {
  loadKnowledgeBase()
  loadVectorList()
})
</script>

<style scoped lang="scss">
.knowledge-base-detail {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: hidden;
}

/* 头部卡片 */
.header-card {
  background: white;
  border-bottom: 1px solid #e4e7ed;
  padding: 20px 32px;
  flex-shrink: 0;

  .header {
    .back-title {
      .title {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-top: 8px;

        h2 {
          margin: 0;
          font-size: 24px;
          font-weight: 600;
          color: #303133;
        }
      }
    }
  }

  .info {
    margin-top: 16px;
  }
}

/* 操作栏 */
.action-card {
  background: white;
  padding: 16px 32px;
  margin: 16px 0;
  flex-shrink: 0;

  .actions {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .left-actions {
      display: flex;
      gap: 10px;
    }
  }
}

/* 向量列表卡片 */
.vector-list-card {
  flex: 1;
  background: white;
  margin: 0 32px 80px 32px;
  padding: 20px;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  :deep(.el-table) {
    flex: 1;
  }

  .file-cell {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}

/* 内容查看 */
.content-view {
  max-height: 400px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 8px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

/* 相似性搜索结果 */
.search-results {
  h3 {
    margin: 0 0 20px 0;
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .result-card {
    margin-bottom: 16px;
    border-radius: 8px;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
    }

    .result-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      .result-index {
        font-weight: bold;
        font-size: 16px;
        color: #409eff;
      }

      .result-time {
        margin-left: auto;
        font-size: 12px;
        color: #909399;
      }
    }

    .result-content {
      color: #606266;
      line-height: 1.8;
      white-space: pre-wrap;
      word-break: break-all;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 6px;
      font-size: 14px;
    }
  }
}

/* 上传组件 */
.upload-demo {
  width: 100%;

  :deep(.el-upload-dragger) {
    padding: 40px;
  }
}

/* 滚动条样式 */
.content-view::-webkit-scrollbar {
  width: 6px;
}

.content-view::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.content-view::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.content-view::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>

