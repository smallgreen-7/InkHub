-- ============================================================
-- InkHub 升级脚本 · 2026-11-09
-- 内容：user 表新增「通知偏好」三个开关（评论/点赞/收藏）
-- 说明：可重复执行会报 Duplicate column，属正常；已存在则跳过
-- 执行：mysql -uroot -p inkhub < upgrade_20261109.sql
-- ============================================================

ALTER TABLE `user`
  ADD COLUMN `notify_comment` tinyint NOT NULL DEFAULT 1 COMMENT '评论/回复通知：1开0关',
  ADD COLUMN `notify_like` tinyint NOT NULL DEFAULT 1 COMMENT '点赞通知：1开0关',
  ADD COLUMN `notify_favorite` tinyint NOT NULL DEFAULT 1 COMMENT '收藏通知：1开0关';

SELECT id, username, notify_comment, notify_like, notify_favorite FROM `user` LIMIT 5;
