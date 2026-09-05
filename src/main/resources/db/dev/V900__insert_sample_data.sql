-- 動作確認用のサンプルデータ。
--
-- 【このファイルが db/migration ではなく db/dev にある理由】
-- 本番のDBに練習用の社員やパスワードが入ってしまうと事故になる。
-- Flyway が読むフォルダをプロファイルごとに変えてあり、
--   dev  : classpath:db/migration, classpath:db/dev  ← このファイルも読む
--   prod : classpath:db/migration                     ← 読まない
-- テーブル定義（本番にも必要）と、確認用データ（開発だけ）を物理的に分けている。
--
-- 番号を V900 と大きく飛ばしてあるのは、あとから V4, V5 とテーブル定義を足したときに
-- 番号がぶつからないようにするため。

-- ---------------------------------------------------------------
-- ログイン用アカウント（開発用。本番では使わない）
-- ---------------------------------------------------------------
-- パスワードは BCrypt でハッシュ化した値。元のパスワードは
--   admin  / admin12345
--   viewer / viewer12345
-- ハッシュ値は BCryptPasswordEncoder で実際に生成したもの。
-- BCrypt は同じパスワードでも毎回違うハッシュになる（ソルトが混ざる）ので、
-- この文字列をコピーして使い回すことはできない。
insert into app_user (username, password, display_name, role, enabled, created_at) values
('admin',  '$2a$10$1FGx906IOsJYqNuIdDuqL.AbBeSM5g3Ve5p9O44N9JneADFzUnVse', '管理者 太郎', 'ADMIN',  true, current_timestamp),
('viewer', '$2a$10$LuCx8/peG7S2PZHRF8dvs.scTEhmj71fNV4R2dhTtMDGKEb6QQT8O', '閲覧 花子',   'VIEWER', true, current_timestamp);

-- ---------------------------------------------------------------
-- 社員データ
-- ---------------------------------------------------------------
insert into employee
  (employee_number, name, name_kana, department, email, hire_date, status, note, created_at, updated_at, version) values
('E0001', '山田 太郎', 'ヤマダ タロウ',     'SALES',          'yamada@example.com',     date '2018-04-01', 'ACTIVE',  '',                current_timestamp, current_timestamp, 0),
('E0002', '佐藤 花子', 'サトウ ハナコ',     'DEVELOPMENT',    'sato@example.com',       date '2020-10-01', 'ACTIVE',  '',                current_timestamp, current_timestamp, 0),
('E0003', '鈴木 一郎', 'スズキ イチロウ',   'ACCOUNTING',     'suzuki@example.com',     date '2015-04-01', 'ACTIVE',  '',                current_timestamp, current_timestamp, 0),
('E0004', '田中 美咲', 'タナカ ミサキ',     'HR',             'tanaka@example.com',     date '2022-04-01', 'LEAVE',   '育児休業中',       current_timestamp, current_timestamp, 0),
('E0005', '高橋 健',   'タカハシ ケン',     'DEVELOPMENT',    'takahashi@example.com',  date '2019-07-16', 'ACTIVE',  '',                current_timestamp, current_timestamp, 0),
('E0006', '伊藤 由美', 'イトウ ユミ',       'GENERAL_AFFAIRS','ito@example.com',        date '2012-04-02', 'ACTIVE',  '',                current_timestamp, current_timestamp, 0),
('E0007', '渡辺 修',   'ワタナベ オサム',   'SALES',          'watanabe@example.com',   date '2016-09-01', 'RETIRED', '2024年3月末退職',  current_timestamp, current_timestamp, 0),
('E0008', '小林 彩',   'コバヤシ アヤ',     'DEVELOPMENT',    'kobayashi@example.com',  date '2023-04-03', 'ACTIVE',  '',                current_timestamp, current_timestamp, 0),
('E0009', '加藤 大輔', 'カトウ ダイスケ',   'ACCOUNTING',     'kato@example.com',       date '2021-01-12', 'ACTIVE',  '',                current_timestamp, current_timestamp, 0),
('E0010', '吉田 千夏', 'ヨシダ チナツ',     'SALES',          'yoshida@example.com',    date '2024-04-01', 'ACTIVE',  '',                current_timestamp, current_timestamp, 0),
('E0011', '山本 翔',   'ヤマモト ショウ',   'DEVELOPMENT',    'yamamoto@example.com',   date '2017-04-01', 'ACTIVE',  '',                current_timestamp, current_timestamp, 0),
('E0012', '中村 恵',   'ナカムラ メグミ',   'HR',             'nakamura@example.com',   date '2014-11-04', 'ACTIVE',  '',                current_timestamp, current_timestamp, 0);
