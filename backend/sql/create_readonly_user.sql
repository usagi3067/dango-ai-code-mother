-- 创建 Grafana 专用只读账号
-- 使用前请将密码替换为实际值
CREATE USER IF NOT EXISTS 'grafana_reader'@'%'
  IDENTIFIED BY 'CHANGE_ME_TO_REAL_PASSWORD';

GRANT SELECT ON dango_ai_code_mother_prod.user TO 'grafana_reader'@'%';
GRANT SELECT ON dango_ai_code_mother_prod.app TO 'grafana_reader'@'%';
FLUSH PRIVILEGES;
