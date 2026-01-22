-- 应用标签系统迁移脚本
-- 为 app 表添加 tag 字段

-- 切换库
use dango_ai_code_mother;

-- 添加 tag 字段（存储英文标识）
ALTER TABLE app ADD COLUMN tag VARCHAR(32) DEFAULT 'website' COMMENT '应用标签';

-- 添加索引以提高查询性能
CREATE INDEX idx_tag ON app(tag);
