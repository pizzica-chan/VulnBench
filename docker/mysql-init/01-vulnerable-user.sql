-- =========================================================================
-- 脆弱版（/vulnerable）専用の DB ユーザーを作る。
--   * MySQL コンテナの初回起動時（データディレクトリが空のとき）に自動で実行される。
--   * 脆弱版の SQL インジェクションで実行される SQL は、このユーザーの権限で動く。
--     vuln_* テーブルにしか権限がないため、対策版の sec_* テーブルは読めない。
--   * テーブル自体はアプリ起動時に Flyway が作る。まだ無いテーブルにテーブル単位で GRANT するには
--     CREATE 権限を含める必要がある（MySQL の仕様）。DROP や ALTER は与えない。
-- =========================================================================
CREATE USER 'secapp_vuln'@'%' IDENTIFIED BY 'secapp_vuln';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE ON secapp.vuln_users    TO 'secapp_vuln'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE ON secapp.vuln_posts    TO 'secapp_vuln'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE ON secapp.vuln_comments TO 'secapp_vuln'@'%';
