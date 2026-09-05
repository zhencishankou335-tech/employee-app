-- 開発用アカウントのパスワードを変更する。
--
-- 【なぜ変えるか】
-- 当初の admin12345 / viewer12345 は、実際に流出したパスワードの一覧に載っている文字列だった。
-- そのため Chrome でログインするたびに
-- 「パスワードを変更してください：たった今使用したパスワードがデータ侵害で検出されました」
-- という警告が出ていた（2026-09-05 実機で確認）。
--
-- 手元だけで動く練習用アプリなので実害は無いが、
-- 画面を人に見せる場面で毎回この警告が出るのは邪魔なので変更する。
--
-- 【なぜ V900 を書き換えないか】
-- V900 は既に適用済み。適用済みのファイルを編集すると、
-- Flyway が記録しているチェックサムと合わなくなり、次回起動時にエラーで止まる。
-- 「一度適用したファイルは直さない。新しい番号のファイルを足す」がマイグレーション管理の原則。
-- 本番のDBに対しては、そもそも過去に戻って直すことができないので、この原則は必須になる。
--
-- 【変更後】
--   admin  / meibo-admin-2026!
--   viewer / meibo-viewer-2026!
-- ハッシュは BCryptPasswordEncoder で実際に生成した値。
-- BCrypt はソルトが混ざるため、同じパスワードでも毎回違う文字列になる。

update app_user
   set password = '$2a$10$tuk/0gRn5op7GOrghefGGOf7LJyKizg4UZV95ZHofRh5Kj/tWbgw2'
 where username = 'admin';

update app_user
   set password = '$2a$10$T5NuxgNiRFdSTkE1fXkE7uNiCit4dGK/T/VLBGRXAcObOe4DqReJ2'
 where username = 'viewer';
