<template>
  <el-drawer
    v-model="visible"
    title="节点配置"
    direction="rtl"
    size="400px"
    :before-close="handleClose"
  >
    <template #header>
      <div class="sidebar-header">
        <div class="node-info">
          <el-icon :size="32" class="node-icon">
            <component :is="getNodeIcon(currentNode?.data?.type || currentNode?.type)" />
          </el-icon>
          <div>
            <div class="node-name">{{ currentNode?.data?.name || '未命名节点' }}</div>
            <div class="node-type">{{ currentNode?.data?.desc || currentNode?.type }}</div>
          </div>
        </div>
      </div>
    </template>

    <div v-if="currentNode" class="sidebar-content">

      <!-- 输入参数 - End节点、Start节点、MCP节点和Script节点不显示（这些节点在配置区显示输入参数） -->
      <el-card v-if="inputParams.length > 0 && currentNode?.data?.type !== 'End' && currentNode?.data?.type !== 'Start' && currentNode?.data?.type !== 'MCP' && currentNode?.data?.type !== 'Script'" class="section-card" shadow="never">
        <template #header>
          <div class="section-header">
            <el-icon><Download /></el-icon>
            <span>输入参数</span>
          </div>
        </template>
        
        <div class="param-list">
          <div 
            v-for="(param, index) in inputParams" 
            :key="param.key"
            class="param-item"
            :class="{ 'has-divider': index < inputParams.length - 1 }"
          >
            <!-- 第一行：参数名和类型 -->
            <div class="param-header">
              <div class="param-name-wrapper">
                <span class="param-name">{{ param.key }}</span>
                <span v-if="param.required" class="required-mark">*</span>
              </div>
              <span class="param-type-badge">{{ param.type || 'string' }}</span>
            </div>
            
            <!-- 第二行：值来源和参数值 -->
            <div class="param-value-row">
              <el-select 
                v-model="param.value_from" 
                placeholder="来源"
                size="small"
                style="width: 100px"
                @change="handleParamChange"
              >
                <el-option label="直接输入" value="input" />
                <el-option label="引用变量" value="refer" />
              </el-select>
              
              <VariableInput
                v-if="param.value_from === 'input'"
                v-model="param.value"
                :placeholder="`输入${param.type || 'string'}类型的值，支持 $ 引用变量`"
                size="small"
                style="flex: 1"
                :available-variables="availableVariables"
                @change="handleParamChange"
              />
              <el-select 
                v-else
                v-model="param.value"
                placeholder="选择引用变量"
                filterable
                size="small"
                style="flex: 1"
                @change="handleParamChange"
              >
                <el-option 
                  v-for="ref in availableReferences"
                  :key="ref.value"
                  :label="ref.label"
                  :value="ref.value"
                />
              </el-select>
            </div>
            
            <!-- 提示信息 -->
            <div v-if="param.value_from === 'refer' && availableReferences.length === 0" class="no-reference-tip">
              <el-icon size="14"><Warning /></el-icon>
              <span>请先从前置节点连线到此节点</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 节点配置 - Start节点单独显示，不需要外层卡片 -->
      <!-- Start节点输入参数配置 -->
      <el-card v-if="currentNode?.data?.type === 'Start'" class="section-card" shadow="never">
        <template #header>
          <div class="section-header">
            <el-icon><Setting /></el-icon>
            <span>输入参数配置</span>
          </div>
        </template>
        
        <!-- Start节点特殊配置 -->
        <div class="start-node-config">
          <div class="start-params-header">
            <el-button 
              size="small" 
              type="primary" 
              @click="addStartParam"
              class="add-param-btn"
              style="margin-left: auto;"
            >
              <el-icon><Plus /></el-icon>
              添加参数
            </el-button>
          </div>
          
          <div v-if="!startInputParams || startInputParams.length === 0" class="empty-params">
            <p>暂无输入参数，点击"添加参数"开始配置</p>
          </div>
          
          <div v-else class="json-params-list">
            <div 
              v-for="(param, index) in startInputParams" 
              :key="index"
              class="json-param-item"
            >
              <div class="param-header">
                <span class="param-index">参数 {{ index + 1 }}</span>
                <el-button 
                  size="small" 
                  type="danger" 
                  link 
                  @click="removeStartParam(index)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              
              <el-form label-position="left" label-width="80px" size="small">
                <el-form-item label="参数名">
                  <el-input 
                    v-model="param.key"
                    placeholder="参数键名"
                    @change="handleStartParamsChange"
                  />
                </el-form-item>
                
                <el-form-item label="数据类型">
                  <el-select 
                    v-model="param.type"
                    placeholder="选择数据类型"
                    @change="handleStartParamsChange"
                  >
                    <el-option label="字符串" value="String" />
                    <el-option label="数字" value="Number" />
                    <el-option label="布尔值" value="Boolean" />
                    <el-option label="对象" value="Object" />
                    <el-option label="数组" value="Array" />
                    <el-option label="图片" value="Image" />
                  </el-select>
                </el-form-item>
                
                <el-form-item label="默认值">
                  <el-input 
                    v-model="param.value"
                    placeholder="输入默认值"
                    @change="handleStartParamsChange"
                  />
                </el-form-item>
              </el-form>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 其他节点配置 -->
      <el-card v-if="currentNode?.data?.type !== 'Start' && shouldShowNodeConfig" class="section-card" shadow="never">
        <template #header>
          <div class="section-header">
            <el-icon><Setting /></el-icon>
            <span>节点配置</span>
          </div>
        </template>
        
        <!-- Judge节点（条件/分支节点）特殊配置 -->
        <div v-if="currentNode?.data?.type === 'Judge'" class="judge-node-config">
          <div class="branches-container">
            <!-- IF分支（第一个分支） -->
            <div v-if="judgeBranches.length > 0" class="branch-item if-branch">
              <div class="branch-header">
                <span class="branch-label if-label">IF</span>
                <el-button size="small" type="danger" link @click="removeBranch(0)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              
              <el-form label-position="left" label-width="80px" size="small">
                <el-form-item label="条件变量">
                  <el-select 
                    v-model="judgeBranches[0].condition_var"
                    placeholder="选择引用变量"
                    filterable
                    @change="handleJudgeBranchesChange"
                  >
                    <el-option 
                      v-for="ref in availableReferences"
                      :key="ref.value"
                      :label="ref.label"
                      :value="ref.value"
                    />
                  </el-select>
                </el-form-item>
                
                <el-form-item label="操作符">
                  <el-select 
                    v-model="judgeBranches[0].operator"
                    placeholder="选择操作符"
                    @change="handleJudgeBranchesChange"
                  >
                    <el-option label="为空 (isNull)" value="isNull" />
                    <el-option label="不为空 (isNotNull)" value="isNotNull" />
                    <el-option label="等于 (equals)" value="equals" />
                    <el-option label="不等于 (notEquals)" value="notEquals" />
                  </el-select>
                </el-form-item>
                
                <el-form-item label="比较值" v-if="judgeBranches[0].operator === 'equals' || judgeBranches[0].operator === 'notEquals'">
                  <VariableInput
                    v-model="judgeBranches[0].compare_value"
                    placeholder="输入比较值，支持 $ 引用变量"
                    :available-variables="availableVariables"
                    @change="handleJudgeBranchesChange"
                  />
                </el-form-item>
              </el-form>
            </div>
            
            <!-- IF ELSE分支（中间的分支） -->
            <div 
              v-for="(branch, index) in judgeBranches.slice(1, hasElseBranch ? -1 : undefined)"
              :key="'else-if-' + index"
              class="branch-item else-if-branch"
            >
              <div class="branch-header">
                <span class="branch-label else-if-label">IF ELSE</span>
                <el-button size="small" type="danger" link @click="removeBranch(index + 1)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              
              <el-form label-position="left" label-width="80px" size="small">
                <el-form-item label="条件变量">
                  <el-select 
                    v-model="branch.condition_var"
                    placeholder="选择引用变量"
                    filterable
                    @change="handleJudgeBranchesChange"
                  >
                    <el-option 
                      v-for="ref in availableReferences"
                      :key="ref.value"
                      :label="ref.label"
                      :value="ref.value"
                    />
                  </el-select>
                </el-form-item>
                
                <el-form-item label="操作符">
                  <el-select 
                    v-model="branch.operator"
                    placeholder="选择操作符"
                    @change="handleJudgeBranchesChange"
                  >
                    <el-option label="为空 (isNull)" value="isNull" />
                    <el-option label="不为空 (isNotNull)" value="isNotNull" />
                    <el-option label="等于 (equals)" value="equals" />
                    <el-option label="不等于 (notEquals)" value="notEquals" />
                  </el-select>
                </el-form-item>
                
                <el-form-item label="比较值" v-if="branch.operator === 'equals' || branch.operator === 'notEquals'">
                  <VariableInput
                    v-model="branch.compare_value"
                    placeholder="输入比较值，支持 $ 引用变量"
                    :available-variables="availableVariables"
                    @change="handleJudgeBranchesChange"
                  />
                </el-form-item>
              </el-form>
            </div>
            
            <!-- ELSE分支（最后一个分支） -->
            <div v-if="hasElseBranch" class="branch-item else-branch">
              <div class="branch-header">
                <span class="branch-label else-label">ELSE</span>
                <el-button size="small" type="danger" link @click="removeBranch(judgeBranches.length - 1)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <div class="else-tip">默认分支，无需条件</div>
            </div>
            
            <!-- 添加IF ELSE按钮 -->
            <div class="add-branch-btn-container">
              <el-button 
                size="small" 
                type="primary" 
                plain
                @click="addElseIfBranch"
                v-if="judgeBranches.length >= 1"
              >
                <el-icon><Plus /></el-icon>
                添加 IF ELSE 分支
              </el-button>
              
              <el-button 
                size="small" 
                type="success" 
                plain
                @click="addElseBranch"
                v-if="judgeBranches.length >= 1 && !hasElseBranch"
              >
                <el-icon><Plus /></el-icon>
                添加 ELSE 分支
              </el-button>
            </div>
          </div>
        </div>
        
        <!-- ImgGen节点（图像生成节点）特殊配置 -->
        <div v-else-if="currentNode?.data?.type === 'ImgGen'" class="imggen-node-config">
          <el-form label-position="left" label-width="80px" size="small">
            <el-form-item label="提示词">
              <VariableInput
                v-model="imgGenConfig.prompt"
                type="textarea"
                :rows="4"
                placeholder="请输入生成提示词，支持 $ 触发变量列表"
                :available-variables="availableVariables"
                @change="handleImgGenConfigChange"
              />
            </el-form-item>
            
            <el-divider content-position="left">模型配置</el-divider>
            
            <el-form-item label="供应商">
              <el-select 
                v-model="imgGenConfig.provider"
                placeholder="选择供应商"
                filterable
                @change="handleImgGenProviderChange"
              >
                <el-option 
                  v-for="provider in imgGenProviders"
                  :key="provider.provider"
                  :label="provider.providerName"
                  :value="provider.provider"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="模型" v-if="imgGenConfig.provider">
              <el-select 
                v-model="imgGenConfig.model"
                placeholder="选择模型"
                filterable
                @change="handleImgGenConfigChange"
              >
                <el-option 
                  v-for="model in imgGenAvailableModels"
                  :key="model.modelId"
                  :label="model.modelId"
                  :value="model.modelId"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="图片尺寸">
              <el-select 
                v-model="imgGenConfig.imgSize"
                placeholder="请选择图片尺寸"
                @change="handleImgGenConfigChange"
                style="width: 100%"
              >
                <el-option label="1K (1920×1080)" value="1920x1080" />
                <el-option label="2K (2560×1440)" value="2560x1440" />
                <el-option label="4K (4096×2160)" value="4096x2160" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="最大数量">
              <el-input-number 
                v-model="imgGenConfig.maxImages"
                :min="1"
                :max="6"
                @change="handleImgGenConfigChange"
                style="width: 100%"
              />
            </el-form-item>
            
            <el-form-item label="启用水印">
              <el-switch 
                v-model="imgGenConfig.watermark"
                @change="handleImgGenConfigChange"
              />
            </el-form-item>
            
            <el-divider content-position="left">参考图</el-divider>
            
            <!-- 参考图模式切换 -->
            <el-form-item label="参考图模式">
              <el-radio-group v-model="imgGenConfig.referenceMode" @change="handleImgGenReferenceModeChange">
                <el-radio-button label="upload">上传图片</el-radio-button>
                <el-radio-button label="variable">引用变量</el-radio-button>
              </el-radio-group>
            </el-form-item>
            
            <!-- 上传模式 -->
            <div v-show="imgGenConfig.referenceMode === 'upload'" class="reference-images">
              <div class="images-container">
                <div 
                  v-for="(url, index) in imgGenConfig.imageUrls" 
                  :key="index"
                  class="image-item"
                >
                  <el-image 
                    :src="url" 
                    fit="cover"
                    class="preview-image"
                  >
                    <template #error>
                      <div class="image-error">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <el-button 
                    class="remove-btn"
                    size="small"
                    type="danger"
                    circle
                    @click="removeReferenceImage(index)"
                  >
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
                
                <el-upload
                  v-if="imgGenConfig.imageUrls.length < 3"
                  class="upload-item"
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleImageUpload"
                  accept="image/*"
                >
                  <div class="upload-placeholder">
                    <el-icon :size="32"><Plus /></el-icon>
                    <div class="upload-text">添加参考图</div>
                  </div>
                </el-upload>
              </div>
              <div class="reference-tip">最多上传3张参考图片</div>
            </div>
            
            <!-- 变量引用模式 -->
            <div v-show="imgGenConfig.referenceMode === 'variable'" class="reference-variables">
              <!-- 空状态提示 -->
              <div v-if="imgGenConfig.referenceVariables.length === 0" class="empty-variables">
                <el-icon :size="32" color="#909399"><Picture /></el-icon>
                <p>未使用参考图，点击下方按钮添加变量引用</p>
              </div>
              
              <div class="variables-list">
                <div 
                  v-for="(variable, index) in imgGenConfig.referenceVariables" 
                  :key="index"
                  class="variable-item"
                >
                  <el-select 
                    v-model="imgGenConfig.referenceVariables[index]"
                    placeholder="选择图片变量（可选）"
                    filterable
                    clearable
                    style="flex: 1"
                  @change="handleImgGenConfigChange"
                  >
                    <el-option 
                      v-for="ref in availableReferences"
                      :key="ref.value"
                      :label="ref.label"
                      :value="ref.value"
                    />
                  </el-select>
                  <el-button 
                    type="danger"
                    size="small"
                    circle
                    @click="removeImgGenVariable(index)"
                  >
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
                
                <el-button
                  v-if="imgGenConfig.referenceVariables.length < 3"
                  type="primary"
                  size="small"
                  plain
                  @click="addImgGenVariable"
                  class="add-variable-btn"
                >
                  <el-icon><Plus /></el-icon>
                  添加变量引用
                </el-button>
              </div>
              <div class="variable-tip">最多引用3个变量，可不使用参考图</div>
            </div>
          </el-form>
        </div>
        
        <!-- VideoGen节点（视频生成节点）特殊配置 -->
        <div v-else-if="currentNode?.data?.type === 'VideoGen'" class="videogen-node-config">
          <el-form label-position="left" label-width="80px" size="small">
            <el-form-item label="提示词">
              <VariableInput
                v-model="videoGenConfig.input_prompt"
                type="textarea"
                :rows="4"
                placeholder="请输入生成提示词，支持 $ 触发变量列表"
                :available-variables="availableVariables"
                @change="handleVideoGenConfigChange"
              />
            </el-form-item>
            
            <el-divider content-position="left">模型配置</el-divider>
            
            <el-form-item label="供应商">
              <el-select 
                v-model="videoGenConfig.provider"
                placeholder="选择供应商"
                filterable
                @change="handleVideoGenProviderChange"
              >
                <el-option 
                  v-for="provider in videoGenProviders"
                  :key="provider.provider"
                  :label="provider.providerName"
                  :value="provider.provider"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="模型" v-if="videoGenConfig.provider">
              <el-select 
                v-model="videoGenConfig.model_id"
                placeholder="选择模型"
                filterable
                @change="handleVideoGenConfigChange"
              >
                <el-option 
                  v-for="model in videoGenAvailableModels"
                  :key="model.modelId"
                  :label="model.modelId"
                  :value="model.modelId"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="分辨率">
              <el-select 
                v-model="videoGenConfig.resolution"
                placeholder="请选择分辨率"
                @change="handleVideoGenConfigChange"
                style="width: 100%"
              >
                <el-option label="480P" value="480P" />
                <el-option label="720P" value="720P" />
                <el-option label="1080P" value="1080P" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="时长(秒)">
              <el-input-number 
                v-model="videoGenConfig.duration"
                :min="5"
                :max="10"
                @change="handleVideoGenConfigChange"
                style="width: 100%"
              />
            </el-form-item>
            
            <el-divider content-position="left">帧设置</el-divider>
            
            <div class="frame-images">
              <!-- 首帧图片 -->
              <el-form-item label="首帧图片">
                <!-- 首帧模式切换 -->
                <el-radio-group v-model="videoGenConfig.firstFrameMode" @change="handleVideoGenFrameModeChange" style="margin-bottom: 12px;">
                  <el-radio-button label="upload">上传图片</el-radio-button>
                  <el-radio-button label="variable">引用变量</el-radio-button>
                </el-radio-group>
                
                <!-- 上传模式 -->
                <div v-show="videoGenConfig.firstFrameMode === 'upload'" class="frame-upload-container">
                  <div v-if="videoGenConfig.first_frame_image" class="frame-image-item">
                    <el-image 
                      :src="videoGenConfig.first_frame_image" 
                      fit="cover"
                      class="frame-preview-image"
                    >
                      <template #error>
                        <div class="image-error">
                          <el-icon><Picture /></el-icon>
                        </div>
                      </template>
                    </el-image>
                    <el-button 
                      class="remove-btn"
                      size="small"
                      type="danger"
                      circle
                      @click="removeFirstFrame"
                    >
                      <el-icon><Close /></el-icon>
                    </el-button>
                  </div>
                  
                  <el-upload
                    v-else
                    class="frame-upload-item"
                    action="#"
                    :auto-upload="false"
                    :show-file-list="false"
                    :on-change="handleFirstFrameUpload"
                    accept="image/*"
                  >
                    <div class="upload-placeholder">
                      <el-icon :size="24"><Plus /></el-icon>
                      <div class="upload-text">上传首帧</div>
                    </div>
                  </el-upload>
                </div>
                
                <!-- 变量引用模式 -->
                <div v-show="videoGenConfig.firstFrameMode === 'variable'" class="frame-variable-container">
                  <el-select 
                    v-model="videoGenConfig.first_frame_variable"
                    placeholder="选择首帧图片变量（可选）"
                    filterable
                    clearable
                    style="width: 100%"
                    @change="handleVideoGenConfigChange"
                  >
                    <el-option 
                      v-for="ref in availableReferences"
                      :key="ref.value"
                      :label="ref.label"
                      :value="ref.value"
                    />
                  </el-select>
                </div>
              </el-form-item>
              
              <!-- 尾帧图片 -->
              <el-form-item label="尾帧图片">
                <!-- 尾帧模式切换 -->
                <el-radio-group v-model="videoGenConfig.tailFrameMode" @change="handleVideoGenFrameModeChange" style="margin-bottom: 12px;">
                  <el-radio-button label="upload">上传图片</el-radio-button>
                  <el-radio-button label="variable">引用变量</el-radio-button>
                </el-radio-group>
                
                <!-- 上传模式 -->
                <div v-show="videoGenConfig.tailFrameMode === 'upload'" class="frame-upload-container">
                  <div v-if="videoGenConfig.tail_frame_image" class="frame-image-item">
                    <el-image 
                      :src="videoGenConfig.tail_frame_image" 
                      fit="cover"
                      class="frame-preview-image"
                    >
                      <template #error>
                        <div class="image-error">
                          <el-icon><Picture /></el-icon>
                        </div>
                      </template>
                    </el-image>
                    <el-button 
                      class="remove-btn"
                      size="small"
                      type="danger"
                      circle
                      @click="removeTailFrame"
                    >
                      <el-icon><Close /></el-icon>
                    </el-button>
                  </div>
                  
                  <el-upload
                    v-else
                    class="frame-upload-item"
                    action="#"
                    :auto-upload="false"
                    :show-file-list="false"
                    :on-change="handleTailFrameUpload"
                    accept="image/*"
                  >
                    <div class="upload-placeholder">
                      <el-icon :size="24"><Plus /></el-icon>
                      <div class="upload-text">上传尾帧</div>
                    </div>
                  </el-upload>
                </div>
                
                <!-- 变量引用模式 -->
                <div v-show="videoGenConfig.tailFrameMode === 'variable'" class="frame-variable-container">
                  <el-select 
                    v-model="videoGenConfig.tail_frame_variable"
                    placeholder="选择尾帧图片变量（可选）"
                    filterable
                    clearable
                    style="width: 100%"
                    @change="handleVideoGenConfigChange"
                  >
                    <el-option 
                      v-for="ref in availableReferences"
                      :key="ref.value"
                      :label="ref.label"
                      :value="ref.value"
                    />
                  </el-select>
                </div>
              </el-form-item>
            </div>
          </el-form>
        </div>
        
        <!-- MusicGen节点（音乐生成节点）特殊配置 -->
        <div v-else-if="currentNode?.data?.type === 'MusicGen'" class="musicgen-node-config">
          <el-form label-position="left" label-width="80px" size="small">
            <el-form-item label="提示词">
              <VariableInput
                v-model="musicGenConfig.prompt"
                type="textarea"
                :rows="4"
                placeholder="请输入音乐风格提示词，支持 $ 触发变量列表"
                :available-variables="availableVariables"
                @change="handleMusicGenConfigChange"
              />
            </el-form-item>
            
            <el-form-item label="歌词">
              <VariableInput
                v-model="musicGenConfig.lyrics"
                type="textarea"
                :rows="6"
                placeholder="请输入歌词内容，支持 $ 触发变量列表"
                :available-variables="availableVariables"
                @change="handleMusicGenConfigChange"
              />
            </el-form-item>
            
            <el-divider content-position="left">模型配置</el-divider>
            
            <el-form-item label="供应商">
              <el-select 
                v-model="musicGenConfig.provider"
                placeholder="选择供应商"
                filterable
                @change="handleMusicGenProviderChange"
              >
                <el-option 
                  v-for="provider in musicGenProviders"
                  :key="provider.provider"
                  :label="provider.providerName"
                  :value="provider.provider"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="模型" v-if="musicGenConfig.provider">
              <el-select 
                v-model="musicGenConfig.model"
                placeholder="选择模型"
                filterable
                @change="handleMusicGenConfigChange"
              >
                <el-option 
                  v-for="model in musicGenAvailableModels"
                  :key="model.modelId"
                  :label="model.modelId"
                  :value="model.modelId"
                />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- Script节点（脚本节点）特殊配置 -->
        <div v-else-if="currentNode?.data?.type === 'Script'" class="script-node-config">
          <el-form label-position="left" label-width="80px" size="small">
            <el-form-item label="脚本类型">
              <el-select 
                v-model="scriptConfig.scriptType"
                placeholder="请选择脚本类型"
                @change="handleScriptConfigChange"
                style="width: 100%"
              >
                <el-option label="JavaScript" value="javascript" />
                <el-option label="Python" value="python" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="脚本内容">
              <VariableInput
                v-model="scriptConfig.scriptContent"
                type="textarea"
                :rows="12"
                placeholder="请输入脚本内容，支持 $ 触发变量列表"
                :available-variables="availableVariables"
                @change="handleScriptConfigChange"
              />
            </el-form-item>
          </el-form>
          
          <el-divider content-position="left">输入参数配置</el-divider>
          
          <div class="script-input-params">
            <div class="input-params-header">
              <el-button 
                size="small" 
                type="primary" 
                @click="addScriptInputParam"
                class="add-param-btn"
              >
                <el-icon><Plus /></el-icon>
                添加输入参数
              </el-button>
            </div>
            
            <div v-if="!scriptInputParams || scriptInputParams.length === 0" class="empty-params">
              <p>暂无输入参数，点击"添加输入参数"开始配置</p>
            </div>
            
            <div v-else class="json-params-list">
              <div 
                v-for="(param, index) in scriptInputParams" 
                :key="index"
                class="json-param-item"
              >
                <div class="param-header">
                  <span class="param-index">输入 {{ index + 1 }}</span>
                  <el-button 
                    size="small" 
                    type="danger" 
                    link 
                    @click="removeScriptInputParam(index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                
                <el-form label-position="left" label-width="80px" size="small">
                  <el-form-item label="参数名">
                    <el-input 
                      v-model="param.key"
                      placeholder="参数键名"
                      @change="handleScriptInputParamsChange"
                    />
                  </el-form-item>
                  
                  <el-form-item label="数据类型">
                    <el-select 
                      v-model="param.type"
                      placeholder="选择数据类型"
                      @change="handleScriptInputParamsChange"
                      style="width: 100%"
                    >
                      <el-option label="字符串" value="String" />
                      <el-option label="数字" value="Number" />
                      <el-option label="布尔值" value="Boolean" />
                      <el-option label="对象" value="Object" />
                      <el-option label="数组" value="Array" />
                    </el-select>
                  </el-form-item>
                  
                  <el-form-item label="参数值">
                    <VariableInput
                      v-model="param.value"
                      placeholder="输入参数值，支持 $ 触发变量列表"
                      :available-variables="availableVariables"
                      @change="handleScriptInputParamsChange"
                    />
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </div>
          
          <el-divider content-position="left">输出参数配置</el-divider>
          
          <div class="script-output-params">
            <div class="output-params-header">
              <el-button 
                size="small" 
                type="primary" 
                @click="addScriptOutputParam"
                class="add-param-btn"
              >
                <el-icon><Plus /></el-icon>
                添加输出参数
              </el-button>
            </div>
            
            <div v-if="!scriptOutputParams || scriptOutputParams.length === 0" class="empty-params">
              <p>暂无输出参数，点击"添加输出参数"开始配置</p>
            </div>
            
            <div v-else class="json-params-list">
              <div 
                v-for="(param, index) in scriptOutputParams" 
                :key="index"
                class="json-param-item"
              >
                <div class="param-header">
                  <span class="param-index">输出 {{ index + 1 }}</span>
                  <el-button 
                    size="small" 
                    type="danger" 
                    link 
                    @click="removeScriptOutputParam(index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                
                <el-form label-position="left" label-width="80px" size="small">
                  <el-form-item label="参数名">
                    <el-input 
                      v-model="param.key"
                      placeholder="输出键名"
                      @change="handleScriptOutputParamsChange"
                    />
                  </el-form-item>
                  
                  <el-form-item label="数据类型">
                    <el-select 
                      v-model="param.type"
                      placeholder="选择数据类型"
                      @change="handleScriptOutputParamsChange"
                      style="width: 100%"
                    >
                      <el-option label="字符串" value="string" />
                      <el-option label="数字" value="number" />
                      <el-option label="布尔值" value="boolean" />
                      <el-option label="对象" value="object" />
                      <el-option label="数组" value="array" />
                    </el-select>
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Email节点（邮件发送节点）特殊配置 -->
        <div v-else-if="currentNode?.data?.type === 'Email'" class="email-node-config">
          <el-form label-position="left" label-width="100px" size="small">
            <el-form-item label="收件人">
              <VariableInput
                v-model="emailConfig.to"
                placeholder="请输入收件人邮箱，支持 $ 触发变量列表"
                :available-variables="availableVariables"
                @change="handleEmailConfigChange"
              />
            </el-form-item>
            
            <el-form-item label="发件人">
              <el-input 
                v-model="emailConfig.from"
                placeholder="请输入发件人邮箱"
                @change="handleEmailConfigChange"
              />
            </el-form-item>
            
            <el-form-item label="主题">
              <VariableInput
                v-model="emailConfig.subject"
                placeholder="请输入邮件主题，支持 $ 触发变量列表"
                :available-variables="availableVariables"
                @change="handleEmailConfigChange"
              />
            </el-form-item>
            
            <el-form-item label="内容">
              <VariableInput
                v-model="emailConfig.content"
                type="textarea"
                :rows="8"
                placeholder="请输入邮件内容，支持 $ 触发变量列表"
                :available-variables="availableVariables"
                @change="handleEmailConfigChange"
              />
            </el-form-item>
            
            <el-divider content-position="left">邮件配置</el-divider>
            
            <el-form-item label="HTML格式">
              <el-switch 
                v-model="emailConfig.html"
                @change="handleEmailConfigChange"
              />
            </el-form-item>
            
            <el-form-item label="授权密码">
              <el-input 
                v-model="emailConfig.authorization"
                placeholder="请输入发件人授权密码"
                type="password"
                show-password
                @change="handleEmailConfigChange"
              />
            </el-form-item>
          </el-form>
        </div>
        
        <!-- LLM节点特殊配置 -->
        <div v-else-if="currentNode?.data?.type === 'TextGen'" class="llm-node-config">
          <el-form label-position="left" label-width="100px" size="small">
            <el-form-item label="系统提示词">
              <VariableInput
                v-model="llmConfig.sys_prompt_content"
                type="textarea"
                :rows="3"
                placeholder="输入系统提示词，支持 $ 触发变量列表"
                :available-variables="availableVariables"
                @change="handleLLMConfigChange"
              />
            </el-form-item>
            
            <el-form-item label="用户提示词">
              <VariableInput
                v-model="llmConfig.prompt_content"
                type="textarea"
                :rows="4"
                placeholder="输入提示词，支持 $ 触发变量列表，例如：${Start_1.input}"
                :available-variables="availableVariables"
                @change="handleLLMConfigChange"
              />
            </el-form-item>
            
            <el-divider content-position="left">模型配置</el-divider>
            
            <el-form-item label="供应商">
              <el-select 
                v-model="llmConfig.model_config.provider"
                placeholder="选择供应商"
                filterable
                @change="handleLlmProviderChange"
              >
                <el-option 
                  v-for="provider in llmProviders"
                  :key="provider.provider"
                  :label="provider.providerName"
                  :value="provider.provider"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="模型" v-if="llmConfig.model_config.provider">
              <el-select 
                v-model="llmConfig.model_config.model_id"
                placeholder="选择模型"
                filterable
                @change="handleLLMConfigChange"
              >
                <el-option 
                  v-for="model in llmAvailableModels"
                  :key="model.modelId"
                  :label="model.modelId"
                  :value="model.modelId"
                />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- MCP节点特殊配置 -->
        <div v-else-if="currentNode?.data?.type === 'MCP'" class="mcp-node-config">
          <el-form label-position="left" label-width="100px" size="small">
            <el-form-item label="MCP服务">
              <el-select 
                v-model="mcpConfig.server_code"
                placeholder="选择MCP服务"
                filterable
                @change="handleMcpServerChange"
              >
                <el-option 
                  v-for="server in aggregatedMcpServers"
                  :key="server.server_code"
                  :label="server.server_name"
                  :value="server.server_code"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="工具名称" v-if="mcpConfig.server_code">
              <el-select 
                v-model="mcpConfig.tool_name"
                placeholder="选择工具"
                filterable
                @change="handleMcpToolChange"
              >
                <el-option 
                  v-for="tool in availableTools"
                  :key="tool.tool_name"
                  :label="tool.tool_name"
                  :value="tool.tool_name"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <!-- 工具参数 -->
          <div v-if="mcpInputParams.length > 0" class="mcp-input-params">
            <el-divider content-position="left">工具参数</el-divider>
            <div class="param-list">
              <div 
                v-for="(param, index) in mcpInputParams" 
                :key="param.key"
                class="param-item"
                :class="{ 'has-divider': index < mcpInputParams.length - 1 }"
              >
                <!-- 第一行：参数名和类型 -->
                <div class="param-header">
                  <div class="param-name-wrapper">
                    <span class="param-name">{{ param.key }}</span>
                    <span class="required-mark">*</span>
                    <el-tooltip v-if="param.desc" :content="param.desc" placement="top">
                      <el-icon class="param-desc-icon" size="14">
                        <QuestionFilled />
                      </el-icon>
                    </el-tooltip>
                  </div>
                  <span class="param-type-badge">{{ param.type || 'string' }}</span>
                </div>
                
                <!-- 第二行：参数值输入框 -->
                <div class="param-value-row">
                  <VariableInput
                    v-model="param.value"
                    :placeholder="`输入${param.type || 'string'}类型的值，支持 $ 引用变量`"
                    size="small"
                    style="width: 100%"
                    :available-variables="availableVariables"
                    @change="handleMcpParamChange"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- End节点特殊配置 -->
        <div v-else-if="currentNode?.data?.type === 'End'" class="end-node-config">
          <!-- 输出类型切换 -->
          <el-form label-position="left" label-width="80px" size="small">
            <el-form-item label="输出类型">
              <el-radio-group 
                v-model="nodeConfig.output_type" 
                @change="handleOutputTypeChange"
                class="output-type-radio"
              >
                <el-radio-button label="text">文本输出</el-radio-button>
                <el-radio-button label="json">JSON输出</el-radio-button>
              </el-radio-group>
            </el-form-item>
            
            <!-- 文本模板配置 -->
            <div v-if="nodeConfig.output_type === 'text'" class="text-config">
              <el-form-item label="文本模板">
                <VariableInput
                  v-model="nodeConfig.text_template"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入文本模板，支持 $ 触发变量列表，例如：${Start_1.input}"
                  :available-variables="availableVariables"
                  @change="handleConfigChange(nodeConfig)"
                />
              </el-form-item>
            </div>
            
            <!-- JSON参数配置 -->
            <div v-if="nodeConfig.output_type === 'json'" class="json-config">
              <div class="json-params-header">
                <label class="json-params-label">JSON参数配置</label>
                <el-button 
                  size="small" 
                  type="primary" 
                  @click="addJsonParam"
                  class="add-param-btn"
                >
                  <el-icon><Plus /></el-icon>
                  添加参数
                </el-button>
              </div>
              
              <div v-if="!nodeConfig.json_params || nodeConfig.json_params.length === 0" class="empty-params">
                <p>暂无JSON参数，点击"添加参数"开始配置</p>
              </div>
              
              <div v-else class="json-params-list">
                <div 
                  v-for="(param, index) in nodeConfig.json_params" 
                  :key="index"
                  class="json-param-item"
                >
                  <div class="param-header">
                    <span class="param-index">参数 {{ index + 1 }}</span>
                    <el-button 
                      size="small" 
                      type="danger" 
                      link 
                      @click="removeJsonParam(index)"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                  
                  <el-form-item label="键名">
                    <el-input 
                      v-model="param.key"
                      placeholder="输出字段名"
                      @change="handleConfigChange(nodeConfig)"
                    />
                  </el-form-item>
                  
                  <el-form-item label="数据类型">
                    <el-select 
                      v-model="param.type"
                      placeholder="选择数据类型"
                      @change="handleConfigChange(nodeConfig)"
                    >
                      <el-option label="字符串" value="string" />
                      <el-option label="数字" value="number" />
                      <el-option label="布尔值" value="boolean" />
                      <el-option label="对象" value="object" />
                      <el-option label="数组" value="array" />
                    </el-select>
                  </el-form-item>
                  
                  <el-form-item label="值来源">
                    <el-select 
                      v-model="param.value_from"
                      placeholder="选择值来源"
                      @change="handleConfigChange(nodeConfig)"
                    >
                      <el-option label="直接输入" value="input" />
                      <el-option label="引用变量" value="refer" />
                    </el-select>
                  </el-form-item>
                  
                  <el-form-item label="参数值">
                    <VariableInput
                      v-if="param.value_from === 'input'"
                      v-model="param.value"
                      :placeholder="`输入${param.type || 'string'}类型的值，支持 $ 引用变量`"
                      :available-variables="availableVariables"
                      @change="handleConfigChange(nodeConfig)"
                    />
                    <el-select 
                      v-else
                      v-model="param.value"
                      placeholder="选择引用变量"
                      filterable
                      @change="handleConfigChange(nodeConfig)"
                    >
                      <el-option 
                        v-for="ref in availableReferences"
                        :key="ref.value"
                        :label="ref.label"
                        :value="ref.value"
                      />
                    </el-select>
                  </el-form-item>
                </div>
              </div>
            </div>
          </el-form>
        </div>
        
        <!-- 通用节点配置 -->
        <div v-else>
          <div v-if="Object.keys(nodeConfig).length === 0" class="empty-config">
            <p>暂无配置项</p>
          </div>
          
          <el-form v-else :model="nodeConfig" label-position="left" label-width="80px" size="small">
            <el-form-item 
              v-for="(value, key) in nodeConfig" 
              :key="key"
              :label="String(key)"
            >
              <!-- 字符串类型 -->
              <el-input 
                v-if="typeof value === 'string'"
                v-model="nodeConfig[key]"
                :placeholder="`请输入${key}`"
                @change="handleConfigChange(nodeConfig)"
              />
              
              <!-- 数字类型 -->
              <el-input-number 
                v-else-if="typeof value === 'number'"
                v-model="nodeConfig[key]"
                :placeholder="`请输入${key}`"
                @change="handleConfigChange(nodeConfig)"
                style="width: 100%"
              />
              
              <!-- 布尔类型 -->
              <el-switch 
                v-else-if="typeof value === 'boolean'"
                v-model="nodeConfig[key]"
                @change="handleConfigChange(nodeConfig)"
              />
              
              <!-- 数组类型 -->
              <el-input 
                v-else-if="Array.isArray(value)"
                :model-value="JSON.stringify(value, null, 2)"
                type="textarea"
                :rows="3"
                placeholder="JSON数组格式"
                @input="(val: string) => { 
                  try { 
                    nodeConfig[key] = JSON.parse(val) 
                    handleConfigChange(nodeConfig)
                  } catch(e) { 
                    console.error('Invalid JSON:', e) 
                  }
                }"
              />
              
              <!-- 对象类型 -->
              <el-input 
                v-else-if="typeof value === 'object' && value !== null"
                :model-value="JSON.stringify(value, null, 2)"
                type="textarea"
                :rows="4"
                placeholder="JSON对象格式"
                @input="(val: string) => { 
                  try { 
                    nodeConfig[key] = JSON.parse(val) 
                    handleConfigChange(nodeConfig)
                  } catch(e) { 
                    console.error('Invalid JSON:', e) 
                  }
                }"
              />
              
              <!-- 其他类型 -->
              <el-input 
                v-else
                :model-value="String(value)"
                @input="(val: string) => {
                  nodeConfig[key] = val
                  handleConfigChange(nodeConfig)
                }"
              />
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <!-- 输出参数 - Start节点不显示 -->
      <el-card v-if="outputParams.length > 0 && currentNode?.data?.type !== 'Start'" class="section-card" shadow="never">
        <template #header>
          <div class="section-header">
            <el-icon><Upload /></el-icon>
            <span>输出参数</span>
          </div>
        </template>
        
        <div class="param-list">
          <div 
            v-for="(param, index) in outputParams" 
            :key="param.key"
            class="param-item param-readonly"
            :class="{ 'has-divider': index < outputParams.length - 1 }"
          >
            <div class="param-header">
              <div class="param-name-wrapper">
                <span class="param-name">{{ param.key }}</span>
                <!-- MCP节点的输出参数显示特殊提示 -->
                <el-tooltip 
                  v-if="isMCPNode && param.key === 'result' && selectedTool?.return_description" 
                  placement="top"
                  effect="dark"
                  raw-content
                >
                  <template #content>
                    <div style="white-space: pre-wrap; max-width: 400px;">{{ selectedTool.return_description }}</div>
                  </template>
                  <el-icon class="param-desc-icon mcp-desc-icon" size="16" style="color: #409eff; cursor: help;">
                    <QuestionFilled />
                  </el-icon>
                </el-tooltip>
                <!-- 其他节点的常规描述提示 -->
                <el-tooltip v-else-if="param.desc" :content="param.desc" placement="top">
                  <el-icon class="param-desc-icon" size="14">
                    <QuestionFilled />
                  </el-icon>
                </el-tooltip>
              </div>
              <span class="param-type-badge">{{ param.type || 'string' }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <template #footer>
      <div class="sidebar-footer">
        <el-button type="primary" @click="handleSave" class="save-button">
          保存配置
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { WorkflowNode } from '@/types/workflow'
import VariableInput from './VariableInput.vue'
import WorkflowAPI, { type McpServer, type ModelInfo, type FileUploadResponse } from '@/api/workflow'

// Props
interface Props {
  modelValue: boolean
  node: WorkflowNode | null
  availableNodes?: WorkflowNode[]
  edges?: any[] // 工作流的边
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'node-save': [node: WorkflowNode]
  'node-delete': [nodeId: string]
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const currentNode = ref<WorkflowNode | null>(null)
const nodeForm = ref({
  name: '',
  desc: ''
})

const nodeConfig = ref<Record<string, any>>({})

// 监听节点变化
watch(() => props.node, (newNode) => {
  if (newNode) {
    currentNode.value = { ...newNode }
    nodeForm.value = {
      name: newNode.data.name || '',
      desc: newNode.data.desc || ''
    }
    
    // End节点特殊处理
    const isEndNode = newNode.type === 'End' || newNode.data?.type === 'End'
    if (isEndNode) {
      const endConfig = newNode.data.config?.node_param || {}
      nodeConfig.value = {
        output_type: endConfig.output_type || 'text',
        text_template: endConfig.text_template || '',
        json_params: endConfig.json_params || []
      }
    } else {
      nodeConfig.value = { ...(newNode.data.config?.node_param || {}) }
    }
  } else {
    currentNode.value = null
  }
}, { immediate: true, deep: true })

// 计算属性
const inputParams = computed(() => {
  return currentNode.value?.data.config?.input_params || []
})

const outputParams = computed(() => {
  return currentNode.value?.data.config?.output_params || []
})

// 判断是否为End节点
const isEndNode = computed(() => {
  return currentNode.value?.type === 'End' || currentNode.value?.data?.type === 'End'
})

// 判断是否为Start节点
const isStartNode = computed(() => {
  return currentNode.value?.type === 'Start' || currentNode.value?.data?.type === 'Start'
})

// 判断是否应该显示节点配置卡片
const shouldShowNodeConfig = computed(() => {
  const nodeType = currentNode.value?.data?.type
  
  // 特殊节点（有自己的定制化UI）总是显示
  if (nodeType === 'Judge' || nodeType === 'TextGen' || nodeType === 'MCP' || nodeType === 'End' || nodeType === 'ImgGen' || nodeType === 'VideoGen' || nodeType === 'MusicGen' || nodeType === 'Script' || nodeType === 'Email') {
    return true
  }
  
  // 通用节点：只有当nodeConfig不为空时才显示
  return Object.keys(nodeConfig.value).length > 0
})

// Start节点的输入参数
const startInputParams = ref<any[]>([])

// 监听Start节点的输入参数变化
watch(() => props.node, (newNode) => {
  if (newNode && newNode.data?.type === 'Start') {
    const params = newNode.data.config?.input_params || []
    // 如果没有参数，添加默认的input参数
    startInputParams.value = params.length > 0 
      ? [...params] 
      : [{
          key: 'input',
          type: 'String',
          value: '',
          value_from: 'input'
        }]
  }
}, { immediate: true, deep: true })

// 判断是否为Judge节点
const isJudgeNode = computed(() => {
  return currentNode.value?.type === 'Judge' || currentNode.value?.data?.type === 'Judge'
})

// Judge节点的分支配置
const judgeBranches = ref<any[]>([])

// 判断是否有ELSE分支
const hasElseBranch = computed(() => {
  return judgeBranches.value.length > 0 && judgeBranches.value[judgeBranches.value.length - 1].is_else === true
})

// 监听Judge节点的分支变化
watch(() => props.node, (newNode) => {
  if (newNode && newNode.data?.type === 'Judge') {
    const branches = newNode.data.config?.node_param?.branches || []
    
    if (branches.length > 0) {
      // 将后端格式转换为前端UI格式
      judgeBranches.value = branches.map((branch, index) => {
        // 检查是否为ELSE分支：
        // 1. label为"ELSE" 或
        // 2. 没有条件且是最后一个分支
        const isElse = branch.label === 'ELSE' || 
                      ((!branch.conditions || branch.conditions.length === 0) && index === branches.length - 1)
        
        if (isElse) {
          return {
            id: branch.id || '',
            condition_var: '',
            operator: '',
            compare_value: '',
            is_else: true
          }
        } else {
          // 从conditions中提取第一个条件
          const condition = branch.conditions?.[0]
          return {
            id: branch.id || '',
            condition_var: condition?.left_key?.value || '',
            operator: condition?.operator || 'equals',
            compare_value: condition?.right_value || '',
            is_else: false
          }
        }
      })
    } else {
      // 初始化时如果没有分支，添加一个默认IF分支
      judgeBranches.value = [{
        id: '',
        condition_var: '',
        operator: 'equals',
        compare_value: '',
        is_else: false
      }]
    }
  }
}, { immediate: true, deep: true })

// 判断是否为ImgGen节点
const isImgGenNode = computed(() => {
  return currentNode.value?.type === 'ImgGen' || currentNode.value?.data?.type === 'ImgGen'
})

// ImgGen节点配置
const imgGenConfig = ref<any>({
  provider: '',
  model: '',
  prompt: '',
  imageUrls: [],
  maxImages: 1,
  imgSize: '1920x1080',
  watermark: false,
  // UI控制字段（不传给后端）
  referenceMode: 'upload', // 'upload' 或 'variable'
  referenceVariables: [''] // 引用变量模式下的变量数组，至少有一个空元素
})

// ImgGen模型列表
const imgGenModels = ref<ModelInfo[]>([])
const imgGenProviders = computed(() => {
  console.log('📊 imgGenProviders computed 执行, imgGenModels数量:', imgGenModels.value.length)
  
  const providerMap = new Map<string, { provider: string, providerName: string, models: ModelInfo[] }>()
  
  imgGenModels.value.forEach(model => {
    console.log('处理模型:', model)
    if (!providerMap.has(model.provider)) {
      providerMap.set(model.provider, {
        provider: model.provider,
        providerName: model.providerName,
        models: []
      })
    }
    providerMap.get(model.provider)!.models.push(model)
  })
  
  const result = Array.from(providerMap.values())
  console.log('📊 imgGenProviders结果:', result)
  return result
})

const selectedImgGenProvider = computed(() => {
  return imgGenProviders.value.find(p => p.provider === imgGenConfig.value.provider)
})

const imgGenAvailableModels = computed(() => {
  return selectedImgGenProvider.value?.models || []
})

// 判断是否为VideoGen节点
const isVideoGenNode = computed(() => {
  return currentNode.value?.type === 'VideoGen' || currentNode.value?.data?.type === 'VideoGen'
})

// VideoGen节点配置
const videoGenConfig = ref<any>({
  provider: '',
  model_id: '',
  input_prompt: '',
  first_frame_image: '',
  tail_frame_image: '',
  resolution: '720P',
  duration: 5,
  // UI控制字段（不传给后端）
  firstFrameMode: 'upload', // 'upload' 或 'variable'
  tailFrameMode: 'upload', // 'upload' 或 'variable'
  first_frame_variable: '', // 首帧变量引用
  tail_frame_variable: '' // 尾帧变量引用
})

// VideoGen模型列表
const videoGenModels = ref<ModelInfo[]>([])
const videoGenProviders = computed(() => {
  const providerMap = new Map<string, { provider: string, providerName: string, models: ModelInfo[] }>()
  
  videoGenModels.value.forEach(model => {
    if (!providerMap.has(model.provider)) {
      providerMap.set(model.provider, {
        provider: model.provider,
        providerName: model.providerName,
        models: []
      })
    }
    providerMap.get(model.provider)!.models.push(model)
  })
  
  return Array.from(providerMap.values())
})

const selectedVideoGenProvider = computed(() => {
  return videoGenProviders.value.find(p => p.provider === videoGenConfig.value.provider)
})

const videoGenAvailableModels = computed(() => {
  return selectedVideoGenProvider.value?.models || []
})

// 判断是否为MusicGen节点
const isMusicGenNode = computed(() => {
  return currentNode.value?.type === 'MusicGen' || currentNode.value?.data?.type === 'MusicGen'
})

// MusicGen节点配置
const musicGenConfig = ref<any>({
  provider: '',
  model: '',
  prompt: '',
  lyrics: ''
})

// MusicGen模型列表
const musicGenModels = ref<ModelInfo[]>([])
const musicGenProviders = computed(() => {
  const providerMap = new Map<string, { provider: string, providerName: string, models: ModelInfo[] }>()
  
  musicGenModels.value.forEach(model => {
    if (!providerMap.has(model.provider)) {
      providerMap.set(model.provider, {
        provider: model.provider,
        providerName: model.providerName,
        models: []
      })
    }
    providerMap.get(model.provider)!.models.push(model)
  })
  
  return Array.from(providerMap.values())
})

const selectedMusicGenProvider = computed(() => {
  return musicGenProviders.value.find(p => p.provider === musicGenConfig.value.provider)
})

const musicGenAvailableModels = computed(() => {
  return selectedMusicGenProvider.value?.models || []
})

// 判断是否为Script节点
const isScriptNode = computed(() => {
  return currentNode.value?.type === 'Script' || currentNode.value?.data?.type === 'Script'
})

// Script节点配置
const scriptConfig = ref<any>({
  scriptType: 'javascript',
  scriptContent: ''
})

// Script节点的输入参数
const scriptInputParams = ref<any[]>([])

// Script节点的输出参数
const scriptOutputParams = ref<any[]>([])

// 判断是否为Email节点
const isEmailNode = computed(() => {
  return currentNode.value?.type === 'Email' || currentNode.value?.data?.type === 'Email'
})

// Email节点配置
const emailConfig = ref<any>({
  to: '',
  from: '',
  subject: '',
  content: '',
  html: false,
  authorization: ''
})

// 判断是否为LLM节点
const isLLMNode = computed(() => {
  return currentNode.value?.type === 'TextGen' || currentNode.value?.data?.type === 'TextGen'
})

// LLM节点配置
const llmConfig = ref<any>({
  sys_prompt_content: '',
  prompt_content: '',
  model_config: {
    provider: '',
    model_id: '',
    model_name: '',
    params: []
  }
})

// LLM模型列表
const llmModels = ref<ModelInfo[]>([])
const llmProviders = computed(() => {
  const providerMap = new Map<string, { provider: string, providerName: string, models: ModelInfo[] }>()
  
  llmModels.value.forEach(model => {
    if (!providerMap.has(model.provider)) {
      providerMap.set(model.provider, {
        provider: model.provider,
        providerName: model.providerName,
        models: []
      })
    }
    providerMap.get(model.provider)!.models.push(model)
  })
  
  return Array.from(providerMap.values())
})

const selectedLlmProvider = computed(() => {
  return llmProviders.value.find(p => p.provider === llmConfig.value.model_config.provider)
})

const llmAvailableModels = computed(() => {
  return selectedLlmProvider.value?.models || []
})

// 判断是否为MCP节点
const isMCPNode = computed(() => {
  return currentNode.value?.type === 'MCP' || currentNode.value?.data?.type === 'MCP'
})

// MCP相关数据
const mcpServers = ref<McpServer[]>([])
const mcpConfig = ref<any>({
  server_code: '',
  server_name: '',
  tool_name: ''
})

// 聚合后的MCP服务列表（按server_code分组）
const aggregatedMcpServers = computed(() => {
  const serverMap = new Map<string, {
    server_code: string
    server_name: string
    tools: McpServer[]
  }>()
  
  mcpServers.value.forEach(server => {
    if (!serverMap.has(server.server_code)) {
      serverMap.set(server.server_code, {
        server_code: server.server_code,
        server_name: server.server_name,
        tools: []
      })
    }
    serverMap.get(server.server_code)!.tools.push(server)
  })
  
  return Array.from(serverMap.values())
})

// 当前选中的服务（聚合后）
const selectedAggregatedServer = computed(() => {
  return aggregatedMcpServers.value.find(server => server.server_code === mcpConfig.value.server_code)
})

// 当前选中服务的工具列表
const availableTools = computed(() => {
  return selectedAggregatedServer.value?.tools || []
})

// 当前选中的工具
const selectedTool = computed(() => {
  return availableTools.value.find(tool => tool.tool_name === mcpConfig.value.tool_name)
})

const mcpInputParams = ref<any[]>([])

// 获取MCP服务列表
const loadMcpServers = async () => {
  try {
    const response = await WorkflowAPI.getMcpServers()
    if (response.code === 1 && response.data) {
      mcpServers.value = response.data
    }
  } catch (error) {
    console.error('Failed to load MCP servers:', error)
  }
}

// 加载模型列表
const loadModels = async () => {
  try {
    // 加载LLM模型
    const llmResponse = await WorkflowAPI.getModelList('TextGen')
    if (llmResponse.code === 1 && llmResponse.data) {
      llmModels.value = llmResponse.data
    }
    
    // 加载图片生成模型
    const imgGenResponse = await WorkflowAPI.getModelList('ImageGen')
    if (imgGenResponse.code === 1 && imgGenResponse.data) {
      imgGenModels.value = imgGenResponse.data
    }
    
    // 加载视频生成模型
    const videoGenResponse = await WorkflowAPI.getModelList('VideoGen')
    if (videoGenResponse.code === 1 && videoGenResponse.data) {
      videoGenModels.value = videoGenResponse.data
    }
    
    // 加载音乐生成模型
    const musicGenResponse = await WorkflowAPI.getModelList('MusicGen')
    if (musicGenResponse.code === 1 && musicGenResponse.data) {
      musicGenModels.value = musicGenResponse.data
    }
  } catch (error) {
    console.error('Failed to load models:', error)
  }
}

// 监听ImgGen节点的配置变化（只在节点ID变化时触发，不要deep watch）
watch(() => props.node?.id, (newNodeId, oldNodeId) => {
  // 只有当节点ID变化时才重新加载配置
  if (newNodeId !== oldNodeId && props.node && props.node.data?.type === 'ImgGen') {
    const config = props.node.data.config?.node_param || {}
    
    // 判断imageUrls是变量引用还是上传的URL
    const imageUrls = config.imageUrls || []
    const hasVariableReference = imageUrls.length > 0 && imageUrls.some((url: string) => url.includes('${'))
    
    imgGenConfig.value = {
      provider: config.provider || 'volcengine',
      model: config.model || 'doubao-seedream-4-0',
      prompt: config.prompt || '',
      // 同时保留两份数据，不丢失
      imageUrls: hasVariableReference ? [] : imageUrls,
      referenceVariables: hasVariableReference ? [...imageUrls] : [''],
      maxImages: config.maxImages || 1,
      imgSize: config.imgSize || '1920x1080',
      watermark: config.watermark || false,
      // UI控制字段：根据数据内容判断当前应该使用哪种模式
      referenceMode: hasVariableReference ? 'variable' : 'upload'
    }
    console.log('加载ImgGen节点配置:', imgGenConfig.value)
  }
}, { immediate: true })

// 监听VideoGen节点的配置变化（只在节点ID变化时触发）
watch(() => props.node?.id, (newNodeId, oldNodeId) => {
  if (newNodeId !== oldNodeId && props.node && props.node.data?.type === 'VideoGen') {
    const config = props.node.data.config?.node_param || {}
    
    // 判断是变量引用还是上传的URL
    const firstFrameIsVariable = config.first_frame_image && config.first_frame_image.includes('${')
    const tailFrameIsVariable = config.tail_frame_image && config.tail_frame_image.includes('${')
    
    videoGenConfig.value = {
      provider: config.provider || 'volcengine',
      model_id: config.model_id || 'doubao-video-generation',
      input_prompt: config.input_prompt || '',
      // 同时保留两份数据，不丢失
      first_frame_image: firstFrameIsVariable ? '' : (config.first_frame_image || ''),
      first_frame_variable: firstFrameIsVariable ? config.first_frame_image : '',
      tail_frame_image: tailFrameIsVariable ? '' : (config.tail_frame_image || ''),
      tail_frame_variable: tailFrameIsVariable ? config.tail_frame_image : '',
      resolution: config.resolution || '720P',
      duration: config.duration || 5,
      // UI控制字段：根据数据内容判断当前应该使用哪种模式
      firstFrameMode: firstFrameIsVariable ? 'variable' : 'upload',
      tailFrameMode: tailFrameIsVariable ? 'variable' : 'upload'
    }
    console.log('加载VideoGen节点配置:', videoGenConfig.value)
  }
}, { immediate: true })

// 监听MusicGen节点的配置变化
watch(() => props.node, (newNode) => {
  if (newNode && newNode.data?.type === 'MusicGen') {
    const config = newNode.data.config?.node_param || {}
    musicGenConfig.value = {
      provider: config.provider || 'volcengine',
      model: config.model || 'doubao-music-generation',
      prompt: config.prompt || '',
      lyrics: config.lyrics || ''
    }
  }
}, { immediate: true, deep: true })

// 监听Script节点的配置变化
watch(() => props.node, (newNode) => {
  if (newNode && newNode.data?.type === 'Script') {
    const config = newNode.data.config?.node_param || {}
    scriptConfig.value = {
      scriptType: config.scriptType || 'javascript',
      scriptContent: config.scriptContent || ''
    }
    
    // 加载输入参数
    const inputParams = newNode.data.config?.input_params || []
    scriptInputParams.value = inputParams.length > 0 
      ? [...inputParams] 
      : []
    
    // 加载输出参数
    const outputParams = newNode.data.config?.output_params || []
    scriptOutputParams.value = outputParams.length > 0 
      ? [...outputParams] 
      : []
  }
}, { immediate: true, deep: true })

// 监听Email节点的配置变化
watch(() => props.node, (newNode) => {
  if (newNode && newNode.data?.type === 'Email') {
    const config = newNode.data.config?.node_param || {}
    emailConfig.value = {
      to: config.to || '',
      from: config.from || '',
      subject: config.subject || '',
      content: config.content || '',
      html: config.html || false,
      authorization: config.authorization || ''
    }
  }
}, { immediate: true, deep: true })

// 监听LLM节点的配置变化
watch(() => props.node, (newNode) => {
  if (newNode && newNode.data?.type === 'TextGen') {
    const config = newNode.data.config?.node_param || {}
    llmConfig.value = {
      sys_prompt_content: config.sys_prompt_content || '',
      prompt_content: config.prompt_content || '',
      model_config: {
        provider: config.model_config?.provider || '',
        model_id: config.model_config?.model_id || '',
        model_name: config.model_config?.model_name || '',
        params: config.model_config?.params || []
      }
    }
  }
}, { immediate: true, deep: true })

// 监听MCP节点的配置变化
watch(() => props.node, (newNode) => {
  if (newNode && newNode.data?.type === 'MCP') {
    const config = newNode.data.config?.node_param || {}
    mcpConfig.value = {
      server_code: config.server_code || '',
      server_name: config.server_name || '',
      tool_name: config.tool_name || ''
    }
    
    // 加载输入参数（保留已有的配置值）
    const inputParams = newNode.data.config?.input_params || []
    if (inputParams.length > 0) {
      mcpInputParams.value = [...inputParams]
    } else {
      mcpInputParams.value = []
    }
  }
}, { immediate: true, deep: true })

// 可用的变量引用（用于下拉选择）
const availableReferences = computed(() => {
  if (!props.availableNodes || !currentNode.value) return []
  
  // 寻找当前节点的所有可达前驱节点（广度优先搜索）
  const findAllReachablePredecessors = (targetNodeId: string): Set<string> => {
    const reachablePredecessors = new Set<string>()
    const queue: string[] = [targetNodeId]
    const visited = new Set<string>()
    
    while (queue.length > 0) {
      const currentNodeId = queue.shift()!
      if (visited.has(currentNodeId)) continue
      visited.add(currentNodeId)
      
      // 查找指向当前节点的所有边
      const incomingEdges = (props.edges || []).filter(edge => edge.target === currentNodeId)
      
      for (const edge of incomingEdges) {
        const sourceNodeId = edge.source
        
        // 如果这个源节点不是目标节点本身，则加入可达前驱集合
        if (sourceNodeId !== targetNodeId) {
          reachablePredecessors.add(sourceNodeId)
          
          // 继续向上搜索这个源节点的前驱
          if (!visited.has(sourceNodeId)) {
            queue.push(sourceNodeId)
          }
        }
      }
    }
    
    return reachablePredecessors
  }
  
  const allReachablePredecessorIds = findAllReachablePredecessors(currentNode.value.id)
  
  // 获取所有可达的前驱节点
  const reachableNodes = props.availableNodes.filter(node => allReachablePredecessorIds.has(node.id))
  
  // 收集所有可达前驱节点的输出参数
  const references = reachableNodes.flatMap(node => {
    const outputs = node.data.config?.output_params || []
    return outputs.map(output => ({
      label: `${node.data.name || node.id}.${output.key}`,
      value: `\${${node.id}.${output.key}}`,
      nodeId: node.id,
      nodeName: node.data.name
    }))
  })
  
  return references
})

// 可用变量（用于 VariableInput 组件）
const availableVariables = computed(() => {
  return availableReferences.value.map(ref => ({
    label: ref.label,
    value: ref.value,
    nodeId: ref.nodeId,
    nodeName: ref.nodeName,
    type: 'string' // 可以根据实际类型设置
  }))
})

// 工具方法
const getNodeIcon = (nodeType?: string) => {
  const iconMap: Record<string, string> = {
    'Start': 'VideoPlay',
    'End': 'VideoPause',
    'TextGen': 'ChatDotRound',
    'MCP': 'Connection',
    'ImgGen': 'Picture',
    'VideoGen': 'VideoCamera',
    'MusicGen': 'Headphone',
    'Script': 'DocumentCopy',
    'Email': 'Message',
    'Judge': 'Switch',
    'Output': 'Upload'
  }
  return iconMap[nodeType || ''] || 'Box'
}

const getParamTypeColor = (type: string) => {
  const colorMap: Record<string, string> = {
    'string': '',
    'number': 'success',
    'boolean': 'warning',
    'object': 'info',
    'array': 'danger'
  }
  return colorMap[type] || ''
}


// 事件处理
const handleClose = () => {
  visible.value = false
}

const handleFormChange = () => {
  if (currentNode.value) {
    currentNode.value.data.name = nodeForm.value.name
    currentNode.value.data.desc = nodeForm.value.desc
  }
}

const handleParamChange = () => {
  // 参数变化处理 - 输入参数已经通过 v-model 直接绑定到 inputParams，会自动更新到 currentNode
  // 确保 input_params 的变化被同步到 currentNode.value.data.config
  if (currentNode.value && inputParams.value.length > 0) {
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      input_params: [...inputParams.value]
    }
  }
}

const handleConfigChange = (newConfig: Record<string, any>) => {
  nodeConfig.value = { ...newConfig }
  if (currentNode.value) {
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      node_param: newConfig
    }
  }
}

// Start节点特殊方法
const addStartParam = () => {
  startInputParams.value.push({
    key: '',
    type: 'String',
    value: '',
    value_from: 'input'
  })
  handleStartParamsChange()
}

const removeStartParam = (index: number) => {
  if (startInputParams.value.length > index) {
    startInputParams.value.splice(index, 1)
    handleStartParamsChange()
  }
}

const handleStartParamsChange = () => {
  if (currentNode.value) {
    // 输出参数与输入参数相同
    const outputParams = startInputParams.value.map(param => ({
      key: param.key,
      type: param.type,
      desc: `输出参数: ${param.key}`
    }))
    
    // 更新配置
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      input_params: startInputParams.value,
      output_params: outputParams,
      node_param: {}
    }
  }
}

// ImgGen节点特殊方法
const handleImgGenProviderChange = () => {
  // 切换供应商时清空模型选择
  imgGenConfig.value.model = ''
  handleImgGenConfigChange()
}

const handleImgGenConfigChange = () => {
  if (currentNode.value) {
    // 根据referenceMode动态决定使用哪个数据源
    let imageUrls = []
    
    if (imgGenConfig.value.referenceMode === 'variable') {
      // 变量引用模式：过滤掉空值
      imageUrls = imgGenConfig.value.referenceVariables.filter((v: string) => v && v.trim().length > 0)
    } else {
      // 上传模式：使用imageUrls数组
      imageUrls = imgGenConfig.value.imageUrls || []
    }
    
    const backendConfig = {
      provider: imgGenConfig.value.provider,
      model: imgGenConfig.value.model,
      prompt: imgGenConfig.value.prompt,
      imgSize: imgGenConfig.value.imgSize,
      maxImages: imgGenConfig.value.maxImages,
      watermark: imgGenConfig.value.watermark,
      imageUrls: imageUrls
    }
    
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      node_param: backendConfig
    }
  }
}

// 切换参考图模式
const handleImgGenReferenceModeChange = () => {
  console.log('=== 切换参考图模式 ===')
  console.log('当前模式:', imgGenConfig.value.referenceMode)
  console.log('referenceVariables:', JSON.stringify(imgGenConfig.value.referenceVariables))
  console.log('imageUrls:', JSON.stringify(imgGenConfig.value.imageUrls))
  
  // 切换到变量引用模式时，如果没有任何变量，初始化一个空元素方便用户添加
  // 但允许用户删除所有变量（即不使用参考图）
  if (imgGenConfig.value.referenceMode === 'variable') {
    if (!imgGenConfig.value.referenceVariables || imgGenConfig.value.referenceVariables.length === 0) {
      console.log('初始化 referenceVariables 为 [""]')
      imgGenConfig.value.referenceVariables = ['']
    }
  }
  
  console.log('切换后 referenceVariables:', JSON.stringify(imgGenConfig.value.referenceVariables))
  handleImgGenConfigChange()
}

// 添加图片变量引用
const addImgGenVariable = () => {
  if (imgGenConfig.value.referenceVariables.length < 3) {
    imgGenConfig.value.referenceVariables.push('')
  }
}

// 移除图片变量引用（允许删除所有变量，即不使用参考图）
const removeImgGenVariable = (index: number) => {
  imgGenConfig.value.referenceVariables.splice(index, 1)
  handleImgGenConfigChange()
}

const handleImageUpload = async (file: any) => {
  try {
    console.log('开始上传图片:', file.name)
    
    // 调用后端接口上传图片
    const response = await WorkflowAPI.uploadImage(file.raw)
    
    if (response.code === 1 && response.data) {
      if (imgGenConfig.value.imageUrls.length < 3) {
      // 将返回的URL添加到配置中
      imgGenConfig.value.imageUrls.push(response.data.fileUrl)
      handleImgGenConfigChange()
      
      console.log('图片上传成功，URL:', response.data.fileUrl)
      ElMessage.success('图片上传成功')
      } else {
        ElMessage.warning('最多只能上传3张参考图')
      }
    } else {
      console.error('图片上传失败:', response.message)
      ElMessage.error(`图片上传失败: ${response.message || '未知错误'}`)
    }
  } catch (error) {
    console.error('图片上传异常:', error)
    ElMessage.error('图片上传失败')
  }
}

const removeReferenceImage = (index: number) => {
  if (imgGenConfig.value.imageUrls.length > index) {
    imgGenConfig.value.imageUrls.splice(index, 1)
    handleImgGenConfigChange()
  }
}

// VideoGen节点特殊方法
const handleVideoGenProviderChange = () => {
  // 切换供应商时清空模型选择
  videoGenConfig.value.model_id = ''
  handleVideoGenConfigChange()
}

// 切换视频帧模式
const handleVideoGenFrameModeChange = () => {
  console.log('=== 切换视频帧模式 ===')
  console.log('首帧模式:', videoGenConfig.value.firstFrameMode)
  console.log('尾帧模式:', videoGenConfig.value.tailFrameMode)
  handleVideoGenConfigChange()
}

const handleVideoGenConfigChange = () => {
  if (currentNode.value) {
    // 构建传给后端的配置，移除UI控制字段
    const backendConfig = {
      provider: videoGenConfig.value.provider,
      model_id: videoGenConfig.value.model_id,
      input_prompt: videoGenConfig.value.input_prompt,
      resolution: videoGenConfig.value.resolution,
      duration: videoGenConfig.value.duration,
      // 根据模式决定first_frame_image的值
      first_frame_image: videoGenConfig.value.firstFrameMode === 'variable' 
        ? videoGenConfig.value.first_frame_variable 
        : videoGenConfig.value.first_frame_image,
      // 根据模式决定tail_frame_image的值
      tail_frame_image: videoGenConfig.value.tailFrameMode === 'variable' 
        ? videoGenConfig.value.tail_frame_variable 
        : videoGenConfig.value.tail_frame_image
    }
    
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      node_param: backendConfig
    }
  }
}

const handleFirstFrameUpload = async (file: any) => {
  try {
    console.log('开始上传首帧图片:', file.name)
    
    // 调用后端接口上传图片
    const response = await WorkflowAPI.uploadImage(file.raw)
    
    if (response.code === 1 && response.data) {
      // 将返回的URL保存到配置中
      videoGenConfig.value.first_frame_image = response.data.fileUrl
      handleVideoGenConfigChange()
      
      console.log('首帧图片上传成功，URL:', response.data.fileUrl)
      ElMessage.success('首帧图片上传成功')
    } else {
      console.error('首帧图片上传失败:', response.message)
      ElMessage.error(`首帧图片上传失败: ${response.message || '未知错误'}`)
    }
  } catch (error) {
    console.error('首帧图片上传异常:', error)
    ElMessage.error('首帧图片上传失败')
  }
}

const handleTailFrameUpload = async (file: any) => {
  try {
    console.log('开始上传尾帧图片:', file.name)
    
    // 调用后端接口上传图片
    const response = await WorkflowAPI.uploadImage(file.raw)
    
    if (response.code === 1 && response.data) {
      // 将返回的URL保存到配置中
      videoGenConfig.value.tail_frame_image = response.data.fileUrl
      handleVideoGenConfigChange()
      
      console.log('尾帧图片上传成功，URL:', response.data.fileUrl)
      ElMessage.success('尾帧图片上传成功')
    } else {
      console.error('尾帧图片上传失败:', response.message)
      ElMessage.error(`尾帧图片上传失败: ${response.message || '未知错误'}`)
    }
  } catch (error) {
    console.error('尾帧图片上传异常:', error)
    ElMessage.error('尾帧图片上传失败')
  }
}

const removeFirstFrame = () => {
  videoGenConfig.value.first_frame_image = ''
  handleVideoGenConfigChange()
}

const removeTailFrame = () => {
  videoGenConfig.value.tail_frame_image = ''
  handleVideoGenConfigChange()
}

// MusicGen节点特殊方法
const handleMusicGenProviderChange = () => {
  // 切换供应商时清空模型选择
  musicGenConfig.value.model = ''
  handleMusicGenConfigChange()
}

const handleMusicGenConfigChange = () => {
  if (currentNode.value) {
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      node_param: musicGenConfig.value
    }
  }
}

// Script节点特殊方法
const handleScriptConfigChange = () => {
  if (currentNode.value) {
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      node_param: scriptConfig.value
    }
  }
}

// 脚本节点输入参数方法
const addScriptInputParam = () => {
  scriptInputParams.value.push({
    key: '',
    type: 'String',
    value: ''
  })
  handleScriptInputParamsChange()
}

const removeScriptInputParam = (index: number) => {
  if (scriptInputParams.value.length > index) {
    scriptInputParams.value.splice(index, 1)
    handleScriptInputParamsChange()
  }
}

const handleScriptInputParamsChange = () => {
  if (currentNode.value) {
    // 为每个输入参数添加value_from字段
    const processedInputParams = scriptInputParams.value.map(param => {
      // 检查value是否包含变量引用 ${xxx.xxx}
      const hasVariableReference = param.value && typeof param.value === 'string' && /\$\{[^}]+\}/.test(param.value)
      
      return {
        key: param.key,
        type: param.type,
        value: param.value,
        value_from: hasVariableReference ? 'refer' : 'input'
      }
    })
    
    // 更新配置
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      input_params: processedInputParams
    }
  }
}

// 脚本节点输出参数方法
const addScriptOutputParam = () => {
  scriptOutputParams.value.push({
    key: '',
    type: 'string',
    desc: ''
  })
  handleScriptOutputParamsChange()
}

const removeScriptOutputParam = (index: number) => {
  if (scriptOutputParams.value.length > index) {
    scriptOutputParams.value.splice(index, 1)
    handleScriptOutputParamsChange()
  }
}

const handleScriptOutputParamsChange = () => {
  if (currentNode.value) {
    // 更新输出参数，添加描述信息
    const outputParams = scriptOutputParams.value.map(param => ({
      key: param.key,
      type: param.type,
      desc: `脚本输出: ${param.key}`
    }))
    
    // 更新配置
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      output_params: outputParams
    }
  }
}

// Email节点特殊方法
const handleEmailConfigChange = () => {
  if (currentNode.value) {
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      node_param: emailConfig.value
    }
  }
}

// LLM节点特殊方法
const addLLMParam = () => {
  llmConfig.value.model_config.params.push({
    key: '',
    value: '',
    enable: true
  })
  handleLLMConfigChange()
}

const removeLLMParam = (index: number) => {
  if (llmConfig.value.model_config.params.length > index) {
    llmConfig.value.model_config.params.splice(index, 1)
    handleLLMConfigChange()
  }
}

const handleLlmProviderChange = () => {
  // 切换供应商时清空模型选择
  llmConfig.value.model_config.model_id = ''
  llmConfig.value.model_config.model_name = ''
  handleLLMConfigChange()
}

const handleLLMConfigChange = () => {
  if (currentNode.value) {
    // 当选择了模型时，自动填充model_name
    if (llmConfig.value.model_config.model_id) {
      const selectedModel = llmAvailableModels.value.find(m => m.modelId === llmConfig.value.model_config.model_id)
      if (selectedModel) {
        llmConfig.value.model_config.model_name = selectedModel.modelId
      }
    }
    
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      node_param: llmConfig.value
    }
  }
}

// MCP节点特殊方法
const handleMcpServerChange = () => {
  if (currentNode.value && selectedAggregatedServer.value) {
    // 更新server_name
    mcpConfig.value.server_name = selectedAggregatedServer.value.server_name
    // 清空工具选择和参数
    mcpConfig.value.tool_name = ''
    mcpInputParams.value = []
    
    // 更新配置
    handleMcpConfigChange()
  }
}

const handleMcpToolChange = () => {
  if (currentNode.value && selectedTool.value) {
    // 根据选中的工具初始化输入参数
    const toolParams = selectedTool.value.tool_params || []
    const existingParams = mcpInputParams.value
    
    // 保留已有的参数值（如果key匹配）
    mcpInputParams.value = toolParams.map(param => {
      const existing = existingParams.find(p => p.key === param.key)
      return {
        key: param.key,
        type: param.type,
        desc: param.desc,
        value: existing?.value || '',
        required: true
      }
    })
    
    // 更新配置
    handleMcpConfigChange()
  }
}

const handleMcpConfigChange = () => {
  if (currentNode.value) {
    // 为每个input_params添加valueFrom字段
    const processedInputParams = mcpInputParams.value.map(param => {
      const hasVariableReference = param.value && typeof param.value === 'string' && /\$\{[^}]+\}/.test(param.value)
      return {
        ...param,
        value_from: hasVariableReference ? 'refer' : 'input'
      }
    })
    
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      input_params: processedInputParams,
      node_param: mcpConfig.value,
      output_params: [
        { 
          key: 'result', 
          type: 'string', 
          desc: selectedTool.value?.return_description || 'MCP执行结果' 
        }
      ]
    }
  }
}

const handleMcpParamChange = () => {
  // 参数变化时更新配置
  if (currentNode.value) {
    // 为每个input_params添加valueFrom字段
    const processedInputParams = mcpInputParams.value.map(param => {
      const hasVariableReference = param.value && typeof param.value === 'string' && /\$\{[^}]+\}/.test(param.value)
      return {
        ...param,
        value_from: hasVariableReference ? 'refer' : 'input'
      }
    })
    
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      input_params: processedInputParams
    }
  }
}

// Judge节点特殊方法
const addElseIfBranch = () => {
  // 如果最后一个是ELSE分支，插入到倒数第二个位置
  // 如果没有ELSE分支，直接添加到末尾





  const insertIndex = hasElseBranch.value ? judgeBranches.value.length - 1 : judgeBranches.value.length
  
  judgeBranches.value.splice(insertIndex, 0, {
    id: '',
    condition_var: '',
    operator: 'equals',
    compare_value: '',
    is_else: false  // 明确设置为false，表示这是IF ELSE分支，不是ELSE分支
  })
  
  handleJudgeBranchesChange()
}

const addElseBranch = () => {
  judgeBranches.value.push({
    id: '',
    condition_var: '',
    operator: '',
    compare_value: '',
    is_else: true
  })
  handleJudgeBranchesChange()
}

const removeBranch = (index: number) => {
  if (judgeBranches.value.length > index) {
    judgeBranches.value.splice(index, 1)
    handleJudgeBranchesChange()
  }
}

const handleJudgeBranchesChange = () => {
  if (currentNode.value) {
    // 从边信息中获取每个分支的目标节点ID
    const getTargetNodeIdForBranch = (branchIndex: number): string => {
      if (!props.edges || !currentNode.value) {
        console.log('edges或currentNode不存在')
        return ''
      }
      
      // Judge节点的sourceHandle格式为: "${nodeId}-output-${index}"
      const sourceHandleId = `${currentNode.value.id}-output-${branchIndex}`
      
      console.log(`查找分支${branchIndex}的边，sourceHandle: ${sourceHandleId}`)
      console.log('所有边:', props.edges)
      
      // 查找从当前节点出发、sourceHandle匹配的边
      const edge = props.edges.find(e => {
        const match = e.source === currentNode.value!.id && e.sourceHandle === sourceHandleId
        if (match) {
          console.log(`找到匹配的边:`, e)
        }
        return match
      })
      
      return edge ? edge.target : ''
    }
    
    // 生成分支标签
    const getBranchLabel = (index: number, isElse: boolean): string => {
      if (isElse) return 'ELSE'
      if (index === 0) return 'IF'
      return 'ELSE IF'
    }
    
    // 将简化的分支配置转换为后端格式
    const branches = judgeBranches.value.map((branch, index) => {
      // 所有分支都从边信息中获取目标节点ID
      const targetId = getTargetNodeIdForBranch(index)
      console.log(`分支${index}的目标节点ID:`, targetId)
      
      if (branch.is_else) {
        // ELSE分支：id为目标节点ID，label为"ELSE"，无条件
        return {
          id: targetId,
          label: 'ELSE',
          conditions: []
        }
      } else {
        // IF/ELSE IF分支：id为目标节点ID，有条件
        return {
          id: targetId,
          label: getBranchLabel(index, false),
          conditions: [{
            left_key: {
              key: '',
              type: 'String',
              value: branch.condition_var || ''
            },
            operator: branch.operator || 'equals',
            right_value: branch.compare_value || ''
          }]
        }
      }
    })
    
    currentNode.value.data.config = {
      ...currentNode.value.data.config,
      node_param: {
        branches
      }
    }
    
    console.log('Judge节点分支配置:', JSON.stringify(branches, null, 2))
  }
}

// End节点特殊方法
const handleOutputTypeChange = () => {
  // 切换输出类型时清理对应字段
  if (nodeConfig.value.output_type === 'text') {
    delete nodeConfig.value.json_params
    if (!nodeConfig.value.text_template) {
      nodeConfig.value.text_template = ''
    }
  } else if (nodeConfig.value.output_type === 'json') {
    delete nodeConfig.value.text_template
    if (!nodeConfig.value.json_params) {
      nodeConfig.value.json_params = []
    }
  }
  handleConfigChange(nodeConfig.value)
}

const addJsonParam = () => {
  if (!nodeConfig.value.json_params) {
    nodeConfig.value.json_params = []
  }
  nodeConfig.value.json_params.push({
    key: '',
    value: '',
    value_from: 'input',
    type: 'string'
  })
  handleConfigChange(nodeConfig.value)
}

const removeJsonParam = (index: number) => {
  if (nodeConfig.value.json_params && nodeConfig.value.json_params.length > index) {
    nodeConfig.value.json_params.splice(index, 1)
    handleConfigChange(nodeConfig.value)
  }
}

const handleSave = () => {
  if (currentNode.value) {
    console.log('=== 开始保存节点 ===')
    console.log('节点类型:', currentNode.value.type, currentNode.value.data?.type)
    console.log('isJudgeNode:', isJudgeNode.value)
    
    // Start节点特殊处理 - 已经在handleStartParamsChange中更新了
    if (isStartNode.value) {
      // Start节点的配置已经在handleStartParamsChange中实时更新
      // 这里只需要发出保存事件
      console.log('保存Start节点:', currentNode.value)
      emit('node-save', currentNode.value)
    } 
    // ImgGen节点特殊处理
    else if (isImgGenNode.value) {
      // 构建传给后端的配置，移除UI控制字段
      let imageUrls = []
      
      // 根据referenceMode决定imageUrls的内容
      if (imgGenConfig.value.referenceMode === 'variable') {
        // 变量引用模式：过滤掉空值
        imageUrls = imgGenConfig.value.referenceVariables.filter((v: string) => v && v.trim().length > 0)
      } else {
        // 上传模式：使用imageUrls数组
        imageUrls = imgGenConfig.value.imageUrls || []
      }
      
      const backendConfig = {
        provider: imgGenConfig.value.provider,
        model: imgGenConfig.value.model,
        prompt: imgGenConfig.value.prompt,
        imgSize: imgGenConfig.value.imgSize,
        maxImages: imgGenConfig.value.maxImages,
        watermark: imgGenConfig.value.watermark,
        imageUrls: imageUrls
      }
      
      currentNode.value.data.config = {
        ...currentNode.value.data.config,
        input_params: [...inputParams.value],
        node_param: backendConfig
      }
      console.log('保存ImgGen节点:', {
        id: currentNode.value.id,
        config: currentNode.value.data.config
      })
      emit('node-save', currentNode.value)
    }
    // VideoGen节点特殊处理
    else if (isVideoGenNode.value) {
      // 构建传给后端的配置，移除UI控制字段
      const backendConfig = {
        provider: videoGenConfig.value.provider,
        model_id: videoGenConfig.value.model_id,
        input_prompt: videoGenConfig.value.input_prompt,
        resolution: videoGenConfig.value.resolution,
        duration: videoGenConfig.value.duration,
        // 根据模式决定first_frame_image的值
        first_frame_image: videoGenConfig.value.firstFrameMode === 'variable' 
          ? videoGenConfig.value.first_frame_variable 
          : videoGenConfig.value.first_frame_image,
        // 根据模式决定tail_frame_image的值
        tail_frame_image: videoGenConfig.value.tailFrameMode === 'variable' 
          ? videoGenConfig.value.tail_frame_variable 
          : videoGenConfig.value.tail_frame_image
      }
      
      currentNode.value.data.config = {
        ...currentNode.value.data.config,
        input_params: [...inputParams.value],
        node_param: backendConfig
      }
      console.log('保存VideoGen节点:', {
        id: currentNode.value.id,
        config: currentNode.value.data.config
      })
      emit('node-save', currentNode.value)
    }
    // MusicGen节点特殊处理
    else if (isMusicGenNode.value) {
      // MusicGen节点的配置已经在handleMusicGenConfigChange中实时更新
      // 但是需要确保所有数据都是最新的
      currentNode.value.data.config = {
        ...currentNode.value.data.config,
        input_params: [...inputParams.value],
        node_param: musicGenConfig.value
      }
      console.log('保存MusicGen节点:', {
        id: currentNode.value.id,
        config: currentNode.value.data.config
      })
      emit('node-save', currentNode.value)
    }
    // Script节点特殊处理
    else if (isScriptNode.value) {
      // 为每个输入参数添加value_from字段
      const processedInputParams = scriptInputParams.value.map(param => {
        // 检查value是否包含变量引用 ${xxx.xxx}
        const hasVariableReference = param.value && typeof param.value === 'string' && /\$\{[^}]+\}/.test(param.value)
        
        return {
          key: param.key,
          type: param.type,
          value: param.value,
          value_from: hasVariableReference ? 'refer' : 'input'
        }
      })
      
      // 更新输出参数
      const outputParams = scriptOutputParams.value.map(param => ({
        key: param.key,
        type: param.type,
        desc: `脚本输出: ${param.key}`
      }))
      
      currentNode.value.data.config = {
        ...currentNode.value.data.config,
        input_params: processedInputParams,
        output_params: outputParams,
        node_param: scriptConfig.value
      }
      console.log('保存Script节点:', {
        id: currentNode.value.id,
        config: currentNode.value.data.config,
        input_params: processedInputParams
      })
      emit('node-save', currentNode.value)
    }
    // Email节点特殊处理
    else if (isEmailNode.value) {
      // Email节点的配置已经在handleEmailConfigChange中实时更新
      // 但是需要确保所有数据都是最新的
      currentNode.value.data.config = {
        ...currentNode.value.data.config,
        input_params: [...inputParams.value],
        output_params: [
          { key: 'output', type: 'string', desc: '发送成功or发送失败' }
        ],
        node_param: emailConfig.value
      }
      console.log('保存Email节点:', {
        id: currentNode.value.id,
        config: currentNode.value.data.config
      })
      emit('node-save', currentNode.value)
    }
    // LLM节点特殊处理 - 已经在handleLLMConfigChange中更新了
    else if (isLLMNode.value) {
      // LLM节点的配置已经在handleLLMConfigChange中实时更新
      // 但是需要确保所有数据都是最新的
      currentNode.value.data.config = {
        ...currentNode.value.data.config,
        input_params: [...inputParams.value],
        node_param: llmConfig.value
      }
      console.log('保存LLM节点:', {
        id: currentNode.value.id,
        config: currentNode.value.data.config
      })
      emit('node-save', currentNode.value)
    }
    // MCP节点特殊处理
    else if (isMCPNode.value) {
      // 为每个input_params添加valueFrom字段
      const processedInputParams = mcpInputParams.value.map(param => {
        // 检查value是否包含变量引用 ${xxx.xxx}
        const hasVariableReference = param.value && typeof param.value === 'string' && /\$\{[^}]+\}/.test(param.value)
        
        return {
          ...param,
          value_from: hasVariableReference ? 'refer' : 'input'
        }
      })
      
      currentNode.value.data.config = {
        ...currentNode.value.data.config,
        input_params: processedInputParams,
        node_param: mcpConfig.value,
        output_params: [
          { 
            key: 'result', 
            type: 'string', 
            desc: selectedTool.value?.return_description || 'MCP执行结果' 
          }
        ]
      }
      console.log('保存MCP节点:', {
        id: currentNode.value.id,
        config: currentNode.value.data.config,
        input_params: processedInputParams
      })
      emit('node-save', currentNode.value)
    }
    // Judge节点特殊处理
    else if (isJudgeNode.value) {
      // 保存时重新生成branches配置，确保ID是最新的
      const getTargetNodeIdForBranch = (branchIndex: number): string => {
        if (!props.edges || !currentNode.value) return ''
        
        const sourceHandleId = `${currentNode.value.id}-output-${branchIndex}`
        const edge = props.edges.find(e => 
          e.source === currentNode.value!.id && 
          e.sourceHandle === sourceHandleId
        )
        
        return edge ? edge.target : ''
      }
      
      // 生成分支标签
      const getBranchLabel = (index: number, isElse: boolean): string => {
        if (isElse) return 'ELSE'
        if (index === 0) return 'IF'
        return 'ELSE IF'
      }
      
      const branches = judgeBranches.value.map((branch, index) => {
        // 所有分支都从边信息中获取目标节点ID
        const targetId = getTargetNodeIdForBranch(index)
        
        if (branch.is_else) {
          // ELSE分支：id为目标节点ID，label为"ELSE"，无条件
          return {
            id: targetId,
            label: 'ELSE',
            conditions: []
          }
        } else {
          // IF/ELSE IF分支：id为目标节点ID，有条件
          return {
            id: targetId,
            label: getBranchLabel(index, false),
            conditions: [{
              left_key: {
                key: '',
                type: 'String',
                value: branch.condition_var || ''
              },
              operator: branch.operator || 'equals',
              right_value: branch.compare_value || ''
            }]
          }
        }
      })
      
      currentNode.value.data.config = {
        ...currentNode.value.data.config,
        node_param: {
          branches
        }
      }
      
      console.log('保存Judge节点，branches配置:', JSON.stringify(branches, null, 2))
      emit('node-save', currentNode.value)
    }
    // End节点和其他节点
    else {
      // 更新节点配置
      currentNode.value.data.config = {
        ...currentNode.value.data.config,
        input_params: [...inputParams.value],
        node_param: nodeConfig.value
      }
      
      console.log('保存其他节点:', {
        id: currentNode.value.id,
        type: currentNode.value.type,
        config: currentNode.value.data.config
      })
      emit('node-save', currentNode.value)
    }
    
    visible.value = false
  }
}

// 在组件挂载时加载MCP服务列表和模型列表
onMounted(() => {
  loadMcpServers()
  loadModels()
})

</script>

<style scoped>
.sidebar-header {
  display: flex;
  align-items: center;
  width: 100%;
}

.node-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.node-icon {
  color: #409eff;
}

.node-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.node-type {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.sidebar-content {
  height: 100%;
  overflow-y: auto;
  padding-top: 4px;
  padding-bottom: 4px;
  border-radius: 12px;
  margin: 4px;
}

.section-card {
  width: 100%;
  margin-bottom: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.section-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.section-card:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 13px;
  color: #1d1d1f;
  letter-spacing: -0.01em;
}

.param-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.param-item {
  padding: 12px 0;
  background: transparent;
  transition: all 0.2s ease;
}

.param-item.has-divider {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.param-item.param-readonly {
  /* 只读参数样式，保持与可编辑参数一致 */
}

.param-desc-icon {
  color: #909399;
  cursor: help;
  transition: color 0.2s ease;
}

.param-desc-icon:hover {
  color: #409eff;
}

.param-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.param-name-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
}

.param-name {
  font-weight: 500;
  font-size: 14px;
  color: #303133;
}

.required-mark {
  color: #f56c6c;
  font-size: 14px;
  font-weight: bold;
  line-height: 1;
}

.param-type-badge {
  font-size: 11px;
  color: #86868b;
  background: rgba(0, 0, 0, 0.04);
  padding: 4px 10px;
  border-radius: 6px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'Segoe UI', sans-serif;
  font-weight: 500;
  letter-spacing: 0.01em;
}

.param-value-row {
  display: flex;
  gap: 8px;
  align-items: center;
}


.param-config {
  margin-top: 8px;
}

.config-form {
  min-height: 100px;
}

.sidebar-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px 0;
  border-top: 1px solid #e4e7ed;
}

.save-button {
  width: 80%;
  height: 40px;
  border-radius: 20px;
  font-size: 15px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
}

.save-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

.save-button:active {
  transform: translateY(0);
}

:deep(.el-card) {
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

:deep(.el-card__header) {
  padding: 14px 18px;
  background: linear-gradient(180deg, #fafbfc 0%, #f5f6f8 100%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

:deep(.el-card__body) {
  padding: 18px;
  background: #ffffff;
}

:deep(.el-form-item) {
  margin-bottom: 12px;
}

:deep(.el-form-item__label) {
  font-size: 12px;
  color: #606266;
}

/* 滚动条样式 */
.sidebar-content::-webkit-scrollbar {
  width: 6px;
}

.sidebar-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.sidebar-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.sidebar-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.empty-config {
  text-align: center;
  padding: 20px;
  color: #909399;
}

.empty-config p {
  margin: 0;
  font-size: 14px;
}

.no-reference-tip {
  margin-top: 10px;
  padding: 8px 10px;
  background: #fef9e7;
  border-radius: 6px;
  border: 1px solid #fdeaa5;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #e6a23c;
}

/* End节点特殊样式 */
.end-node-config {
  padding: 8px 0;
}

.output-type-radio {
  width: 100%;
}

.output-type-radio :deep(.el-radio-button__inner) {
  width: 90px;
  text-align: center;
}

.text-config {
  margin-top: 16px;
}

.json-config {
  margin-top: 16px;
}

.json-params-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.json-params-label {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.add-param-btn {
  height: 28px;
  font-size: 12px;
}

.empty-params {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 13px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px dashed #e4e7ed;
}

.json-params-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.json-param-item {
  background: #f8f9fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  position: relative;
}

.param-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.param-index {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

.json-param-item :deep(.el-form-item) {
  margin-bottom: 8px;
}

.json-param-item :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

/* Judge节点（条件/分支节点）样式 */
.judge-node-config {
  padding: 8px 0;
}

.branches-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.branch-item {
  background: #f8f9fa;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  position: relative;
}

.if-branch {
  border-color: #409eff;
  background: #ecf5ff;
}

.else-if-branch {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.else-branch {
  border-color: #67c23a;
  background: #f0f9ff;
}

.branch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.branch-label {
  font-size: 14px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 4px;
  color: white;
}

.if-label {
  background: #409eff;
}

.else-if-label {
  background: #e6a23c;
}

.else-label {
  background: #67c23a;
}

.else-tip {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 12px;
}

.add-branch-btn-container {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 8px;
}

.branch-item :deep(.el-form-item) {
  margin-bottom: 8px;
}

.branch-item :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

/* Start节点样式 */
.start-node-config {
  padding: 8px 0;
}

.start-params-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 12px;
}

/* ImgGen节点样式 */
.imggen-node-config {
  padding: 8px 0;
}

.reference-images {
  margin-top: 8px;
}

.images-container {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e4e7ed;
  transition: all 0.3s ease;
}

.image-item:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.preview-image {
  width: 100%;
  height: 100%;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 32px;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  padding: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.image-item:hover .remove-btn {
  opacity: 1;
}

.upload-item {
  width: 100px;
  height: 100px;
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fafbfc;
}

.upload-placeholder:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.upload-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.reference-tip {
  font-size: 12px;
  color: #909399;
  text-align: center;
  margin-top: 4px;
}

.variable-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  text-align: center;
}

.variables-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.variable-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-variable-btn {
  width: 100%;
  margin-top: 4px;
}

.empty-variables {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #fafbfc;
  border: 1px dashed #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
}

.empty-variables p {
  margin: 8px 0 0 0;
  font-size: 13px;
  color: #909399;
  text-align: center;
}

.frame-variable-container {
  margin-top: 8px;
}

/* VideoGen节点样式 */
.videogen-node-config {
  padding: 8px 0;
}

.frame-images {
  margin-top: 8px;
}

.frame-upload-container {
  width: 100%;
}

.frame-image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e4e7ed;
  transition: all 0.3s ease;
}

.frame-image-item:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.frame-image-item:hover .remove-btn {
  opacity: 1;
}

.frame-preview-image {
  width: 100%;
  height: 100%;
}

.frame-upload-item {
  width: 120px;
  height: 120px;
}

/* MusicGen节点样式 */
.musicgen-node-config {
  padding: 8px 0;
}

/* Script节点样式 */
.script-node-config {
  padding: 8px 0;
}

.script-input-params {
  margin-top: 8px;
}

.script-output-params {
  margin-top: 8px;
}

.input-params-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 12px;
}

.output-params-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 12px;
}

/* Email节点样式 */
.email-node-config {
  padding: 8px 0;
}

/* LLM节点样式 */
.llm-node-config {
  padding: 8px 0;
}

.model-params-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.model-param-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 6px;
}

.model-param-item :deep(.el-checkbox) {
  flex-shrink: 0;
  min-width: 120px;
}

.model-param-item :deep(.el-input) {
  flex: 1;
}
</style>
