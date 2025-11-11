-- 启用pgvector扩展（如果尚未启用）
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    user_id BIGINT,
    status SMALLINT DEFAULT 1,
    vector_count BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- 创建索引
CREATE INDEX idx_kb_user_id ON knowledge_base(user_id);
CREATE INDEX idx_kb_status ON knowledge_base(status);
CREATE INDEX idx_kb_create_time ON knowledge_base(create_time);

-- 为知识库表添加注释
COMMENT ON TABLE knowledge_base IS '知识库表';
COMMENT ON COLUMN knowledge_base.id IS '主键ID';
COMMENT ON COLUMN knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN knowledge_base.description IS '知识库描述';
COMMENT ON COLUMN knowledge_base.user_id IS '创建用户ID';
COMMENT ON COLUMN knowledge_base.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN knowledge_base.vector_count IS '向量数量';
COMMENT ON COLUMN knowledge_base.create_time IS '创建时间';
COMMENT ON COLUMN knowledge_base.update_time IS '更新时间';
COMMENT ON COLUMN knowledge_base.deleted IS '是否删除：0-未删除，1-已删除';

-- 创建向量文档表
CREATE TABLE IF NOT EXISTS knowledge_vector (
    id VARCHAR(100) PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    metadata TEXT,
    embedding vector(1024),
    file_name VARCHAR(500),
    file_type VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0,
    CONSTRAINT fk_kb_vector FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_kv_kb_id ON knowledge_vector(knowledge_base_id);
CREATE INDEX idx_kv_file_name ON knowledge_vector(file_name);
CREATE INDEX idx_kv_create_time ON knowledge_vector(create_time);

-- 为向量表添加注释
COMMENT ON TABLE knowledge_vector IS '知识库向量文档表';
COMMENT ON COLUMN knowledge_vector.id IS '主键ID（UUID）';
COMMENT ON COLUMN knowledge_vector.knowledge_base_id IS '所属知识库ID';
COMMENT ON COLUMN knowledge_vector.content IS '文档内容';
COMMENT ON COLUMN knowledge_vector.metadata IS '元数据（JSON格式）';
COMMENT ON COLUMN knowledge_vector.embedding IS '向量嵌入';
COMMENT ON COLUMN knowledge_vector.file_name IS '文件名';
COMMENT ON COLUMN knowledge_vector.file_type IS '文件类型';
COMMENT ON COLUMN knowledge_vector.create_time IS '创建时间';
COMMENT ON COLUMN knowledge_vector.update_time IS '更新时间';
COMMENT ON COLUMN knowledge_vector.deleted IS '是否删除：0-未删除，1-已删除';

-- 创建HNSW向量索引（用于快速相似性搜索）
-- 使用余弦距离操作符
CREATE INDEX IF NOT EXISTS idx_kv_embedding_hnsw ON knowledge_vector 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- 或者使用IVFFlat索引（备选方案，适用于较小数据集）
-- CREATE INDEX IF NOT EXISTS idx_kv_embedding_ivfflat ON knowledge_vector 
-- USING ivfflat (embedding vector_cosine_ops)
-- WITH (lists = 100);

