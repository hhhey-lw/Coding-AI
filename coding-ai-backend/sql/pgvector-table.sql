/*
 Navicat Premium Dump SQL

 Source Server         : Coding-AI-PG
 Source Server Type    : PostgreSQL
 Source Server Version : 160011 (160011)
 Source Host           : 121.4.116.162:25433
 Source Catalog        : ai-service
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 160011 (160011)
 File Encoding         : 65001

 Date: 18/12/2025 11:19:53
*/


-- ----------------------------
-- Table structure for knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS "public"."knowledge_base";
CREATE TABLE "public"."knowledge_base" (
  "id" int8 NOT NULL DEFAULT nextval('knowledge_base_id_seq'::regclass),
  "name" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "description" text COLLATE "pg_catalog"."default",
  "user_id" int8,
  "status" int2 DEFAULT 1,
  "vector_count" int8 DEFAULT 0,
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 DEFAULT 0
)
;
ALTER TABLE "public"."knowledge_base" OWNER TO "coding-ai";
COMMENT ON COLUMN "public"."knowledge_base"."id" IS '主键ID';
COMMENT ON COLUMN "public"."knowledge_base"."name" IS '知识库名称';
COMMENT ON COLUMN "public"."knowledge_base"."description" IS '知识库描述';
COMMENT ON COLUMN "public"."knowledge_base"."user_id" IS '创建用户ID';
COMMENT ON COLUMN "public"."knowledge_base"."status" IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN "public"."knowledge_base"."vector_count" IS '向量数量';
COMMENT ON COLUMN "public"."knowledge_base"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."knowledge_base"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."knowledge_base"."deleted" IS '是否删除：0-未删除，1-已删除';
COMMENT ON TABLE "public"."knowledge_base" IS '知识库表';

-- ----------------------------
-- Table structure for knowledge_vector
-- ----------------------------
DROP TABLE IF EXISTS "public"."knowledge_vector";
CREATE TABLE "public"."knowledge_vector" (
  "id" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "knowledge_base_id" int8 NOT NULL,
  "content" text COLLATE "pg_catalog"."default" NOT NULL,
  "metadata" text COLLATE "pg_catalog"."default",
  "embedding" "public"."vector",
  "file_name" varchar(500) COLLATE "pg_catalog"."default",
  "file_type" varchar(50) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 DEFAULT 0
)
;
ALTER TABLE "public"."knowledge_vector" OWNER TO "coding-ai";
COMMENT ON COLUMN "public"."knowledge_vector"."id" IS '主键ID（UUID）';
COMMENT ON COLUMN "public"."knowledge_vector"."knowledge_base_id" IS '所属知识库ID';
COMMENT ON COLUMN "public"."knowledge_vector"."content" IS '文档内容';
COMMENT ON COLUMN "public"."knowledge_vector"."metadata" IS '元数据（JSON格式）';
COMMENT ON COLUMN "public"."knowledge_vector"."embedding" IS '向量嵌入';
COMMENT ON COLUMN "public"."knowledge_vector"."file_name" IS '文件名';
COMMENT ON COLUMN "public"."knowledge_vector"."file_type" IS '文件类型';
COMMENT ON COLUMN "public"."knowledge_vector"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."knowledge_vector"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."knowledge_vector"."deleted" IS '是否删除：0-未删除，1-已删除';
COMMENT ON TABLE "public"."knowledge_vector" IS '知识库向量文档表';

-- ----------------------------
-- Indexes structure for table knowledge_base
-- ----------------------------
CREATE INDEX "idx_kb_create_time" ON "public"."knowledge_base" USING btree (
  "create_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_kb_status" ON "public"."knowledge_base" USING btree (
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST
);
CREATE INDEX "idx_kb_user_id" ON "public"."knowledge_base" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table knowledge_base
-- ----------------------------
ALTER TABLE "public"."knowledge_base" ADD CONSTRAINT "knowledge_base_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table knowledge_vector
-- ----------------------------
CREATE INDEX "idx_kv_create_time" ON "public"."knowledge_vector" USING btree (
  "create_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_kv_embedding_hnsw" ON "public"."knowledge_vector" (
  "embedding" "public"."vector_cosine_ops" ASC NULLS LAST
);
CREATE INDEX "idx_kv_file_name" ON "public"."knowledge_vector" USING btree (
  "file_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_kv_kb_id" ON "public"."knowledge_vector" USING btree (
  "knowledge_base_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table knowledge_vector
-- ----------------------------
ALTER TABLE "public"."knowledge_vector" ADD CONSTRAINT "knowledge_vector_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table knowledge_vector
-- ----------------------------
ALTER TABLE "public"."knowledge_vector" ADD CONSTRAINT "fk_kb_vector" FOREIGN KEY ("knowledge_base_id") REFERENCES "public"."knowledge_base" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
