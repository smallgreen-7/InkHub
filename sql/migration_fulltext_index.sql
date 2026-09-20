-- ========================================================
-- InkHub AI 优化迁移：给 article 表加 FULLTEXT 索引（BM25 检索用）
-- 执行前提：MySQL 8.0+，数据库 inkhub
-- ========================================================

-- 1. 检查并添加 ngram 分词器的全文索引
-- ngram 适合中文分词，token_size=2 默认即可
ALTER TABLE article
    ADD FULLTEXT INDEX ft_title_content (title, content_md)
    WITH PARSER ngram;

-- 验证索引创建成功
SHOW INDEX FROM article WHERE Index_type = 'FULLTEXT';

-- 2. 测试 BM25 检索是否工作（可选）
-- SELECT id, title, MATCH(title, content_md) AGAINST('Spring AI' IN NATURAL LANGUAGE MODE) AS score
-- FROM article
-- WHERE status = 1
--   AND MATCH(title, content_md) AGAINST('Spring AI' IN NATURAL LANGUAGE MODE)
-- ORDER BY score DESC
-- LIMIT 10;
