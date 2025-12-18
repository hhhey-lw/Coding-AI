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
        <el-descriptions :column="2" border>
          <el-descriptions-item label="向量数量">
            <el-tag type="info">{{ knowledgeBase.vectorCount }} 个</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ knowledgeBase.createTime }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
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
            添加向量
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
        <el-table-column prop="id" label="向量ID" width="300" show-overflow-tooltip class-name="ellipsis-cell hide-on-mobile" />
        <el-table-column prop="fileName" label="文件名" min-width="200" class-name="ellipsis-cell">
          <template #default="{ row }">
            <div class="file-cell">
              <el-icon><Document /></el-icon>
              <span>{{ row.fileName || '未命名' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="文件类型" width="120" class-name="ellipsis-cell hide-on-mobile">
          <template #default="{ row }">
            <el-tag v-if="row.fileType" size="small" effect="plain">{{ row.fileType }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容预览" min-width="300">
          <template #default="{ row }">
            <div class="content-preview">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" class-name="ellipsis-cell" />
        <el-table-column label="操作" width="260" class-name="operation-column">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleViewVector(row)"
            >
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button
              type="warning"
              size="small"
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
    <el-dialog 
      v-model="viewDialogVisible" 
      title="向量详情" 
      width="800px"
      class="vector-detail-dialog"
    >
      <div v-if="currentVector" class="vector-detail-container">
        <el-descriptions :column="2" border class="detail-info">
          <el-descriptions-item label="文件名">{{ currentVector.fileName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentVector.createTime }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="detail-section">
          <div class="section-title">元数据</div>
          <pre v-if="currentVector.metadata" class="metadata-view">{{ currentVector.metadata }}</pre>
          <div v-else class="empty-text">-</div>
        </div>
        
        <div class="detail-section">
          <div class="section-title">内容</div>
          <div class="content-detail-view">{{ currentVector.content }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- 相似性搜索对话框 -->
    <el-dialog 
      v-model="searchDialogVisible" 
      title="相似性搜索" 
      width="900px"
      class="similarity-search-dialog"
    >
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
  KnowledgeVectorAddRequest
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
      pagination.total = Number(response.data.total) || 0
    }
  } catch (error) {
    console.error('加载向量列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 返回知识库页面
const goBack = () => {
  router.push({ path: '/app', query: { tab: 'knowledge' } })
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
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
  overflow: hidden;
}

/* 头部卡片 */
.header-card {
  background: white;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 28px 40px;
  margin: 0 20px;
  margin-top: 6px;
  flex-shrink: 0;
  border-radius: 16px;

  :deep(.el-card__body) {
    padding: 0;
  }

  .header {
    .back-title {
      :deep(.el-button) {
        background: white;
        border: 1px solid #e4e7ed;
        color: #606266;
        font-weight: 500;
        transition: all 0.3s;
        padding: 8px 16px;
        border-radius: 8px;
        
        &:hover {
          border-color: #667eea;
          color: #667eea;
          background: #f8f9fc;
        }
      }

      .title {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-top: 12px;

        :deep(.el-icon) {
          color: #667eea !important;
        }

        h2 {
          margin: 0;
          font-size: 28px;
          font-weight: 700;
          color: #303133;
          letter-spacing: 0.5px;
        }

        :deep(.el-tag) {
          border-radius: 8px;
          font-weight: 500;
        }
      }
    }
  }

  .info {
    margin-top: 20px;
    
    :deep(.el-descriptions) {
      background: white;
      border-radius: 12px;
      overflow: hidden;
      border: 1px solid #e4e7ed;
      
      .el-descriptions__label {
        background: #f8f9fc;
        font-weight: 600;
        color: #606266;
      }
      
      .el-descriptions__content {
        color: #303133;
      }
    }
  }
}

/* 操作栏 */
.action-card {
  background: white;
  padding: 20px 40px;
  margin: 24px 20px 16px;
  flex-shrink: 0;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  }

  :deep(.el-card__body) {
    padding: 0;
  }

  .actions {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .left-actions {
      display: flex;
      gap: 12px;

      :deep(.el-button) {
        border-radius: 10px;
        padding: 10px 20px;
        font-weight: 500;
        transition: all 0.3s;

        &.el-button--primary {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border: none;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
          }
        }

        &:not(.el-button--primary) {
          border: 1.5px solid #e4e7ed;

          &:hover {
            border-color: #667eea;
            color: #667eea;
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
          }
        }
      }
    }

    .right-actions {
      :deep(.el-input) {
        border-radius: 10px;
        
        .el-input__wrapper {
          border-radius: 10px 0 0 10px;
          transition: all 0.3s;
          
          &:hover, &.is-focus {
            box-shadow: 0 0 0 1px #667eea inset;
          }
        }

        .el-input-group__append {
          border-radius: 0 10px 10px 0;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border: none;

          .el-button {
            color: white;
            
            &:hover {
              background: transparent;
            }
          }
        }
      }
    }
  }
}

/* 向量列表卡片 */
.vector-list-card {
  flex: 1;
  background: white;
  margin: 0 20px 20px 20px;
  padding: 24px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  min-height: 0;
  overflow: hidden;

  :deep(.el-card__body) {
    padding: 0;
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  :deep(.el-table) {
    flex: 1;
    border-radius: 8px;
    min-height: 0;

    .el-table__header-wrapper {
      th {
        background: linear-gradient(135deg, #f8f9fc 0%, #f0f2f8 100%);
        color: #606266;
        font-weight: 600;
        border: none;
      }
    }

    .el-table__body-wrapper {
      max-height: calc(100vh - 450px);
      overflow: auto;
      -webkit-overflow-scrolling: touch;

      tr {
        transition: all 0.3s;

        &:hover {
          background: #f8f9fc !important;
          transform: scale(1.001);
        }
      }

      td {
        border-bottom: 1px solid #f5f7fa;
      }
    }
  }

  :deep(.el-table .ellipsis-cell .cell) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .file-cell {
    display: flex;
    align-items: center;
    gap: 10px;
    
    :deep(.el-icon) {
      color: #667eea;
      font-size: 18px;
    }

    span {
      font-weight: 500;
      color: #303133;
    }
  }

  .content-preview {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
    color: #606266;
  }

  .pagination {
    margin-top: 24px;
    padding-top: 16px;
    border-top: 1px solid #f5f7fa;
    display: flex;
    justify-content: center;
    flex-shrink: 0;
    
    :deep(.el-pagination) {
      .el-pager li {
        border-radius: 8px;
        margin: 0 4px;
        transition: all 0.3s;

        &.is-active {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
        }

        &:hover:not(.is-active) {
          color: #667eea;
        }
      }

      button {
        border-radius: 8px;
        transition: all 0.3s;

        &:hover {
          color: #667eea;
        }
      }
    }
  }
}

/* 内容查看 */
.content-view {
  max-height: 400px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  padding: 16px;
  background: linear-gradient(135deg, #f8f9fc 0%, #f0f2f8 100%);
  border-radius: 12px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.8;
  border: 1px solid #e4e7ed;
}

/* 向量详情对话框 */
.vector-detail-dialog {
  :deep(.el-dialog) {
    max-height: 90vh;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    margin-top: 5vh !important;
  }

  :deep(.el-dialog__body) {
    max-height: calc(90vh - 120px);
    padding: 20px 24px;
    overflow: hidden !important;
  }
}

/* 相似性搜索对话框 */
.similarity-search-dialog {
  :deep(.el-dialog) {
    max-height: 90vh;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    margin-top: 5vh !important;
  }

  :deep(.el-dialog__body) {
    max-height: calc(90vh - 120px);
    padding: 20px 24px;
    overflow-y: auto;
    overflow-x: hidden;
  }
}

/* 向量详情容器 */
.vector-detail-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(90vh - 160px);
  overflow: hidden;

  .detail-info {
    flex-shrink: 0;
  }

  .detail-section {
    display: flex;
    flex-direction: column;
    gap: 10px;
    
    &:first-of-type {
      flex-shrink: 0;
    }

    &:last-child {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
    }

    .section-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
      padding-left: 4px;
      border-left: 3px solid #667eea;
      flex-shrink: 0;
    }

    .empty-text {
      color: #909399;
      font-size: 14px;
      padding: 12px;
      background: #f8f9fc;
      border-radius: 8px;
      text-align: center;
    }
  }
}

/* 向量详情内容查看 */
.content-detail-view {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  white-space: pre-wrap;
  word-break: break-all;
  padding: 16px;
  background: linear-gradient(135deg, #f8f9fc 0%, #f0f2f8 100%);
  border-radius: 8px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.8;
  border: 1px solid #e4e7ed;
  min-height: 200px;
  max-height: calc(90vh - 400px);
}

/* 元数据查看 */
.metadata-view {
  margin: 0;
  padding: 12px;
  background: #f8f9fc;
  border-radius: 8px;
  font-size: 13px;
  max-height: 120px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
}

/* 相似性搜索结果 */
.search-results {
  max-height: calc(90vh - 300px);
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 8px;

  h3 {
    margin: 0 0 24px 0;
    font-size: 20px;
    font-weight: 700;
    color: #303133;
    display: flex;
    align-items: center;
    gap: 8px;
    position: sticky;
    top: 0;
    background: white;
    padding: 12px 0;
    z-index: 1;

    &::before {
      content: '';
      width: 4px;
      height: 24px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 2px;
    }
  }

  .result-card {
    margin-bottom: 20px;
    border-radius: 12px;
    transition: all 0.3s;
    border: 1px solid #e4e7ed;
    overflow: hidden;

    &:hover {
      border-color: #667eea;
      box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
      transform: translateY(-2px);
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    .result-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 2px solid #f5f7fa;

      .result-index {
        font-weight: 700;
        font-size: 18px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      :deep(.el-tag) {
        border-radius: 8px;
        padding: 4px 12px;
        font-weight: 500;
      }

      .result-time {
        margin-left: auto;
        font-size: 13px;
        color: #909399;
        font-weight: 500;
      }
    }

    .result-content {
      color: #606266;
      line-height: 1.9;
      white-space: pre-wrap;
      word-break: break-all;
      padding: 16px;
      background: linear-gradient(135deg, #f8f9fc 0%, #f0f2f8 100%);
      border-radius: 10px;
      font-size: 14px;
      border: 1px solid #e8eef5;
      max-height: 300px;
      overflow-y: auto;
    }
  }
}

/* 搜索结果滚动条 */
.search-results::-webkit-scrollbar,
.search-results .result-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.search-results::-webkit-scrollbar-track,
.search-results .result-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.search-results::-webkit-scrollbar-thumb,
.search-results .result-content::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  transition: all 0.3s;
}

.search-results::-webkit-scrollbar-thumb:hover,
.search-results .result-content::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}

/* 上传组件 */
.upload-demo {
  width: 100%;

  :deep(.el-upload-dragger) {
    padding: 50px;
    border-radius: 12px;
    border: 2px dashed #d9d9d9;
    transition: all 0.3s;

    &:hover {
      border-color: #667eea;
      background: #f8f9fc;
    }

    .el-icon--upload {
      font-size: 72px;
      color: #667eea;
      margin-bottom: 16px;
    }

    .el-upload__text {
      font-size: 16px;
      color: #606266;
      
      em {
        color: #667eea;
        font-weight: 600;
      }
    }
  }

  :deep(.el-upload__tip) {
    margin-top: 12px;
    font-size: 13px;
    color: #909399;
  }
}

/* 对话框样式优化 */
:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;

  .el-dialog__header {
    background: white;
    padding: 20px 24px;
    margin: 0;
    border-bottom: 1px solid #e4e7ed;

    .el-dialog__title {
      color: #303133;
      font-size: 18px;
      font-weight: 600;
    }

    .el-dialog__headerbtn {
      top: 20px;
      right: 24px;

      .el-dialog__close {
        color: #909399;
        font-size: 20px;

        &:hover {
          color: #606266;
        }
      }
    }
  }

  .el-dialog__body {
    padding: 20px 24px;
    overflow: hidden;
  }

  .el-dialog__footer {
    padding: 16px 24px;
    background: #f8f9fc;
    border-top: 1px solid #e4e7ed;

    .el-button {
      border-radius: 10px;
      padding: 10px 24px;
      font-weight: 500;
      transition: all 0.3s;

      &.el-button--primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border: none;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
        }
      }
    }
  }
}

/* 表单样式优化 */
:deep(.el-form) {
  .el-form-item__label {
    font-weight: 600;
    color: #606266;
  }

  .el-input__wrapper {
    border-radius: 10px;
    transition: all 0.3s;

    &:hover, &.is-focus {
      box-shadow: 0 0 0 1px #667eea inset;
    }
  }

  .el-textarea__inner {
    border-radius: 10px;
    transition: all 0.3s;

    &:hover, &:focus {
      border-color: #667eea;
    }
  }
}

/* 表格操作按钮优化 */
:deep(.el-table) {
  .el-button--small {
    border-radius: 6px;
    font-weight: 500;
    transition: all 0.3s;

    &.el-button--primary {
      &:hover {
        transform: scale(1.05);
      }
    }

    &.el-button--warning {
      &:hover {
        transform: scale(1.05);
      }
    }

    &.el-button--danger {
      &:hover {
        transform: scale(1.05);
      }
    }
  }
}

/* 滚动条样式 */
.content-view::-webkit-scrollbar,
.content-detail-view::-webkit-scrollbar,
.metadata-view::-webkit-scrollbar,
:deep(.el-table__body-wrapper)::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.content-view::-webkit-scrollbar-track,
.content-detail-view::-webkit-scrollbar-track,
.metadata-view::-webkit-scrollbar-track,
:deep(.el-table__body-wrapper)::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.content-view::-webkit-scrollbar-thumb,
.content-detail-view::-webkit-scrollbar-thumb,
.metadata-view::-webkit-scrollbar-thumb,
:deep(.el-table__body-wrapper)::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  transition: all 0.3s;
}

.content-view::-webkit-scrollbar-thumb:hover,
.content-detail-view::-webkit-scrollbar-thumb:hover,
.metadata-view::-webkit-scrollbar-thumb:hover,
:deep(.el-table__body-wrapper)::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}

/* 动画效果 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.header-card,
.action-card,
.vector-list-card {
  animation: fadeInUp 0.6s ease-out;
}

.action-card {
  animation-delay: 0.1s;
}

.vector-list-card {
  animation-delay: 0.2s;
}

/* ========== 移动端响应式样式 ========== */
@media screen and (max-width: 768px) {
  /* 整体布局 */
  .knowledge-base-detail {
    background: #f5f7fa;
    height: auto;
    min-height: 100vh;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }

  /* 头部卡片 */
  .header-card {
    margin: 0 12px;
    margin-top: 6px;
    padding: 16px;
    border-radius: 12px;

    .header {
      .back-title {
        :deep(.el-button) {
          padding: 6px 12px;
          font-size: 13px;
        }

        .title {
          gap: 10px;
          margin-top: 10px;

          :deep(.el-icon) {
            font-size: 20px !important;
          }

          h2 {
            font-size: 18px;
          }

          :deep(.el-tag) {
            font-size: 12px;
            padding: 2px 8px;
          }
        }
      }
    }

    .info {
      margin-top: 14px;

      :deep(.el-descriptions) {
        .el-descriptions__label,
        .el-descriptions__content {
          padding: 8px 10px;
          font-size: 13px;
        }
      }
    }
  }

  /* 操作栏 */
  .action-card {
    margin: 12px;
    padding: 14px;
    border-radius: 12px;

    .actions {
      flex-direction: column;
      gap: 12px;

      .left-actions {
        width: 100%;
        flex-wrap: wrap;
        gap: 8px;

        :deep(.el-button + .el-button) {
          margin-left: 0;
        }

        :deep(.el-button) {
          flex: 1;
          min-width: calc(50% - 4px);
          padding: 10px 12px;
          font-size: 13px;
          justify-content: center;

          .el-icon {
            margin-right: 4px;
          }
        }
      }

      .right-actions {
        width: 100%;

        :deep(.el-input) {
          width: 100% !important;
        }
      }
    }
  }

  /* 向量列表卡片 */
  .vector-list-card {
    margin: 0 12px 12px;
    padding: 14px;
    border-radius: 12px;
    flex: none;
    overflow: visible;

    :deep(.el-card__body) {
      overflow: visible;
    }

    :deep(.el-table) {
      font-size: 13px;

      .el-table__header-wrapper th {
        padding: 10px 8px;
        font-size: 12px;
      }

      .el-table__body-wrapper {
        max-height: none;
        overflow: visible;

        td {
          padding: 10px 8px;
        }
      }

      /* 移动端隐藏部分列 */
      :deep(.hide-on-mobile) {
        display: none;
      }
    }

    .file-cell {
      gap: 6px;

      :deep(.el-icon) {
        font-size: 16px;
      }

      span {
        flex: 1;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 13px;
      }
    }

    .content-preview {
      font-size: 12px;
      max-width: 120px;
    }

    .pagination {
      margin-top: 16px;
      padding-top: 12px;
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;

      :deep(.el-pagination) {
        flex-wrap: nowrap;
        justify-content: flex-start;
        gap: 4px;
        min-width: max-content;

        .el-pagination__total,
        .el-pagination__jump {
          display: none;
        }

        .el-pagination__sizes {
          margin: 0;
        }

        .el-pager li {
          min-width: 28px;
          height: 28px;
          line-height: 28px;
          margin: 0 2px;
        }

        .btn-prev,
        .btn-next {
          min-width: 28px;
          height: 28px;
        }
      }
    }
  }

  /* 对话框移动端优化 */
  :deep(.el-dialog) {
    width: 95% !important;
    max-width: 95% !important;
    margin: 10px auto !important;
    border-radius: 12px;

    .el-dialog__header {
      padding: 14px 16px;

      .el-dialog__title {
        font-size: 16px;
      }

      .el-dialog__headerbtn {
        top: 14px;
        right: 16px;
      }
    }

    .el-dialog__body {
      padding: 14px 16px;
      max-height: 60vh;
      overflow-y: auto;
    }

    .el-dialog__footer {
      padding: 12px 16px;

      .el-button {
        padding: 8px 16px;
        font-size: 13px;
      }
    }
  }

  /* 上传组件 */
  .upload-demo {
    :deep(.el-upload-dragger) {
      padding: 30px 20px;

      .el-icon--upload {
        font-size: 48px;
        margin-bottom: 12px;
      }

      .el-upload__text {
        font-size: 14px;
      }
    }
  }

  /* 表单样式 */
  :deep(.el-form) {
    .el-form-item {
      margin-bottom: 16px;
    }

    .el-form-item__label {
      font-size: 13px;
      padding-bottom: 6px;
    }

    .el-textarea__inner {
      min-height: 100px !important;
    }
  }

  /* 向量详情对话框 */
  .vector-detail-container {
    max-height: 55vh;

    .detail-info {
      :deep(.el-descriptions__label),
      :deep(.el-descriptions__content) {
        padding: 8px 10px;
        font-size: 12px;
      }
    }

    .section-title {
      font-size: 13px;
    }
  }

  .content-detail-view {
    min-height: 150px;
    max-height: 200px;
    font-size: 12px;
    padding: 12px;
  }

  .metadata-view {
    font-size: 12px;
    padding: 10px;
    max-height: 80px;
  }

  /* 相似性搜索对话框 */
  .similarity-search-dialog {
    :deep(.el-form--inline) {
      .el-form-item {
        display: block;
        width: 100%;
        margin-right: 0;
        margin-bottom: 12px;

        .el-form-item__label {
          display: block;
          text-align: left;
          padding-bottom: 6px;
        }

        .el-form-item__content {
          width: 100%;

          .el-input {
            width: 100% !important;
          }
        }
      }
    }
  }

  .search-results {
    max-height: 45vh;
    padding-right: 4px;

    h3 {
      font-size: 16px;
      margin-bottom: 16px;
      padding: 8px 0;

      &::before {
        width: 3px;
        height: 18px;
      }
    }

    .result-card {
      margin-bottom: 12px;
      border-radius: 10px;

      :deep(.el-card__body) {
        padding: 14px;
      }

      .result-header {
        gap: 8px;
        margin-bottom: 12px;
        padding-bottom: 10px;
        flex-wrap: wrap;

        .result-index {
          font-size: 15px;
        }

        :deep(.el-tag) {
          font-size: 11px;
          padding: 2px 8px;
        }

        .result-time {
          font-size: 11px;
          width: 100%;
          margin-left: 0;
          margin-top: 6px;
        }
      }

      .result-content {
        padding: 12px;
        font-size: 13px;
        max-height: 150px;
        line-height: 1.7;
      }
    }
  }

  /* 表格操作按钮 */
  :deep(.el-table) {
    .el-button--small {
      padding: 4px 8px;
      font-size: 12px;

      .el-icon {
        margin-right: 2px;
      }
    }

    .operation-column .cell {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      align-items: center;
    }
  }

  /* 动画禁用（性能优化） */
  .header-card,
  .action-card,
  .vector-list-card {
    animation: none;
  }
}

/* 小屏手机额外优化 */
@media screen and (max-width: 480px) {
  .header-card {
    margin: 0 8px;
    margin-top: 4px;
    padding: 12px;

    .header .back-title .title {
      h2 {
        font-size: 16px;
      }
    }
  }

  .action-card {
    margin: 8px;
    padding: 12px;

    .actions .left-actions {
      :deep(.el-button) {
        min-width: 100%;
        font-size: 12px;
      }
    }
  }

  .vector-list-card {
    margin: 0 8px 8px;
    padding: 12px;

    :deep(.el-table) {
      .el-table__body-wrapper {
        max-height: calc(100vh - 360px);
      }
    }
  }

  :deep(.el-dialog) {
    width: 100% !important;
    max-width: 100% !important;
    margin: 0 !important;
    border-radius: 0;
    height: 100vh;

    .el-dialog__body {
      max-height: calc(100vh - 120px);
    }
  }
}
</style>

