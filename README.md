# 社員名簿（業務系Webアプリ練習）

Java + Spring Boot で作った、業務系Webアプリケーションの最小構成。
一覧・検索・登録・編集・削除・CSV出力に加えて、
**ログイン / DBマイグレーション管理 / 排他制御 / 本番向け設定の分離** まで入れてある。

## 動かし方

Java 21 が入っていれば、これだけで動く。

```
cd employee-app
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run          # Mac / Linux
```

ブラウザで http://localhost:8080 を開くとログイン画面が出る。

| ログインID | パスワード | 権限 | できること |
|---|---|---|---|
| `admin` | `meibo-admin-2026!` | 管理者 | すべて |
| `viewer` | `meibo-viewer-2026!` | 閲覧のみ | 一覧・検索・集計・CSV出力 |

**この2つのアカウントは開発（dev）プロファイルのときだけ作られる。** 本番では作られない。
ログイン画面にこの情報が出るのも dev のときだけ（後述）。

止めるときはコンソールで Ctrl + C。

### テストの実行

```
mvnw.cmd test
```

39件。内訳は下の「テスト」を参照。

### データベースの中身を見る

http://localhost:8080/h2-console （**開発プロファイルのときだけ開く**）

| 項目 | 値 |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/employeedb` |
| ユーザー名 | `sa` |
| パスワード | （空欄） |

## 使っている技術

| | |
|---|---|
| 言語 | Java 21（Eclipse Temurin） |
| フレームワーク | Spring Boot 4.1.1 |
| 画面 | Thymeleaf |
| 認証・認可 | Spring Security 7.1.1 |
| DBアクセス | Spring Data JPA / Hibernate |
| DBマイグレーション | Flyway 12.4.0 |
| データベース | H2（ファイルモード） |
| 入力チェック | Jakarta Bean Validation |
| ビルド | Maven（mvnw 同梱のため Maven のインストール不要） |
| テスト | JUnit 5 / MockMvc / AssertJ / Mockito / Spring Security Test |

## 画面と機能

| 画面 | URL | 必要な権限 |
|---|---|---|
| ログイン | `/login` | 不要 |
| 社員一覧 | `/employees` | ログイン |
| 部署別人数（集計） | `/employees/summary` | ログイン |
| CSV出力 | `/employees/csv` | ログイン |
| 新規登録 | `/employees/new` → `POST /employees` | 管理者 |
| 編集 | `/employees/{id}/edit` → `POST /employees/{id}` | 管理者 |
| 退職処理（論理削除） | `POST /employees/{id}/retire` | 管理者 |
| 削除（物理削除） | `POST /employees/{id}/delete` | 管理者 |
| ログアウト | `POST /logout` | ログイン |

閲覧のみの利用者には、一覧画面から「新規登録」ボタンと「操作」列が消える。
ただし**それは見た目の親切でしかない**。URLを直接打っても
サーバ側（`SecurityConfig`）で 403 になることをテストで確かめてある。

## ファイル構成

```
src/main/java/com/example/employeeapp/
├── EmployeeAppApplication.java   起動クラス
├── domain/                       データそのもの
│   ├── Employee.java             DBの1行に対応するクラス（version = 排他制御用）
│   ├── Department.java           部署（選択肢を固定するenum）
│   ├── EmploymentStatus.java     在籍区分
│   ├── AppUser.java              ログインする利用者
│   └── Role.java                 権限（ADMIN / VIEWER）
├── repository/
│   ├── EmployeeRepository.java   DBアクセス。検索SQLと集計SQLはここ
│   └── AppUserRepository.java
├── security/
│   ├── SecurityConfig.java       誰が何をできるか
│   ├── DatabaseUserDetailsService.java  ログインIDから利用者を引く
│   ├── AppUserDetails.java       Spring Security に渡す入れ物
│   └── AdminBootstrap.java       本番で最初の管理者を1人だけ作る
├── service/
│   ├── EmployeeService.java      業務ルール（重複判定・論理削除・集計・排他判定）
│   ├── DepartmentStatusCount.java / DepartmentSummary.java  集計用
│   ├── DuplicateEmployeeNumberException.java
│   └── StaleEmployeeException.java      他の人が先に更新していた
└── web/
    ├── EmployeeController.java   URLの受け口
    ├── EmployeeForm.java         画面入力の入れ物＋入力チェックのルール
    ├── LoginController.java      ログイン画面・権限不足の画面
    ├── CurrentUserAdvice.java    全画面にログイン中の人の名前を渡す
    ├── GlobalExceptionHandler.java
    └── HomeController.java

src/main/resources/
├── application.properties        全プロファイル共通
├── application-dev.properties    開発用（既定）
├── application-prod.properties   本番用
├── db/migration/                 テーブル定義（本番にも適用する）
│   ├── V1__create_employee.sql
│   ├── V2__create_app_user.sql
│   └── V3__add_version_to_employee.sql
├── db/dev/                       確認用データ（開発でしか読まない）
│   ├── V900__insert_sample_data.sql
│   └── V901__change_dev_passwords.sql
├── templates/
│   ├── fragments/layout.html     共通ヘッダ
│   ├── login.html
│   ├── employees/{list,form,summary}.html
│   └── error/message.html
└── static/css/app.css

src/test/java/com/example/employeeapp/
├── repository/EmployeeRepositoryTest.java      検索条件の組み合わせ（8件）
├── service/EmployeeSummaryTest.java            集計（7件）
├── service/EmployeeOptimisticLockTest.java     排他制御（4件）
├── web/EmployeeControllerTest.java             入力チェックと画面遷移（8件）
├── web/SecurityAccessTest.java                 誰が何をできるか（7件）
├── web/LoginPageTest.java                      本番で開発用情報を出さない（3件）
└── web/LoginPageDevProfileTest.java            開発では出す（1件）
```

## 意識して入れた「業務系らしさ」

面談で聞かれたときに答えられるように、なぜそうしたかを書いておく。

### 基本の作り

**画面の入力をエンティティに直接流し込んでいない**
`EmployeeForm` を挟んでいる。画面から来る値は検証前で信用できないため、
DBのクラスに直結すると意図しない項目まで書き換えられる事故につながる。

**業務ルールをコントローラに書いていない**
社員番号の重複判定などは `EmployeeService` に置いた。
画面が増えたときに同じ判定をコピーすると、必ずどこかで食い違う。

**削除の既定を論理削除にしている**
退職者のデータは過去の伝票や履歴から参照される。
行ごと消すと整合性が壊れるため、在籍区分を変えるだけにしてある。

**登録後にリダイレクトしている（PRGパターン）**
そのまま画面を返すと、ブラウザの再読み込みで二重登録される。

**社員番号の重複をDBの unique 制約でも止めている**
アプリ側のチェックだけだと、2人が同時に登録したときにすり抜ける。

**CSVの各項目をダブルクォートで囲んでいる**
備考にカンマが1つ入った瞬間に列がずれるため。
先頭にBOMを付けているのは、付けないとExcelで日本語が文字化けするから。

**一覧に件数を出している**
業務系では「何件ヒットしたか」を必ず聞かれる。

**集計で0人の部署も表に出している**
SQLの `group by` は該当が1件も無い組み合わせを返さない。
そのままだと表から部署が消え、「0人」なのか「その部署が無い」のか読み手に区別できない。

### ログイン（認証・認可）

**利用者テーブルを社員テーブルと分けている**
社員名簿はシステムを使わない人も含むデータ。利用者はログインする人。
同じにすると「退職者を名簿から消せない」「派遣の人にログインさせられない」で行き詰まる。

**パスワードはBCryptでハッシュ化して保存**
平文で持たない。BCryptはわざと計算が遅く作られていて、総当たりに時間をかけさせる。
MD5やSHA-256は速すぎるのでパスワード保存には使わない。

**アカウントは削除せず `enabled` で止める**
消すと「誰が操作したか」の記録から辿れなくなる。

**ログイン失敗のメッセージで「そのIDは存在しない」と言わない**
存在するIDだけ反応が変わると、総当たりで有効なIDを特定されてしまう。

**CSRF対策を有効のままにしている**
Thymeleaf の `th:action` を使ったフォームには隠しトークンが自動で入る。
`csrf().disable()` と書けばテストは楽になるが、それは対策を消しているだけ。
「トークンが無いPOSTは管理者でも拒否される」ことをテストで確かめてある。

**ログアウトをPOSTにしている**
GETにすると、外部サイトの画像タグ1つで勝手にログアウトさせられる。

**画面のボタンを隠すのとサーバ側の制限を、両方書いている**
ボタンを隠すのは親切のため。実際に止めているのはサーバ側。
どちらか片方だけでは足りない。

### DBマイグレーション管理（Flyway）

**`ddl-auto` を `update` から `validate` に変えた**
`update` は「エンティティに合わせてテーブルを勝手に変える」設定。
何がいつ変わったか記録が残らず、本番で使うと事故になる。
`validate` は「定義とテーブルが食い違っていたら起動時に止める」だけで、変更はしない。

**テーブル定義の変更を SQL ファイルとして残している**
`db/migration/V1__...sql` から順に適用され、適用済みの記録が
`flyway_schema_history` テーブルに残る。
**一度適用したファイルは編集しない**（チェックサムが合わずに起動が止まる）。
直したいときは新しい V番号 のファイルを足す。

**確認用データを別フォルダ（`db/dev`）に置いている**
テーブル定義は本番にも必要だが、練習用の社員12人とパスワードは本番に入ってはいけない。
Flywayが読むフォルダをプロファイルごとに変えて、物理的に分けている。

**テストでもマイグレーションを実行している**
テスト用にテーブルを自動生成すると、本番に流れるSQLを誰も確かめないまま進むことになる。
実際の `db/migration` を流した上でテストしているので、SQLを間違えればテストが落ちる。

### 排他制御（楽観ロック）

防ぎたいのはこれ。

```
10:00 Aさんが社員E0001の編集画面を開く（部署=営業部）
10:01 Bさんが同じ社員の編集画面を開く
10:02 Bさんが部署を「開発部」にして保存
10:03 Aさんが備考だけ直して保存
→ Aさんの画面は10:00時点の情報を持っているので、Bさんの変更が消える
```

**この事故は、起きても誰も気づかない**のが最悪の点。
「先月直したはずの部署がまた戻っている」という形で、何か月も後に発覚する。

**`@Version` の列を足し、編集画面と版数を往復させている**
保存時に「画面を開いたときの版数」と「今DBにある版数」を突き合わせ、
食い違っていれば保存せずに止める。最新の内容を読み直して画面に出す。
どちらを採用するかをシステムが勝手に決めない。

**アプリ側の比較とDB側の `@Version` を両方使っている**
DB側の仕組みだけだと「まったく同時刻に処理が重なったとき」しか捕まえられない。
編集画面を開いたまま10分後に保存、という業務でよくある操作は、
アプリ側の明示的な比較でないと検知できない。
社員番号の重複を「アプリ側チェック＋DBのunique制約」で二重にしているのと同じ考え方。

### 本番向け設定の分離

環境で変わる設定を共通ファイルに書かない。

| | dev（既定） | prod |
|---|---|---|
| DB接続先 | ファイルに直書き（使い捨てDB） | **環境変数から**。既定値を持たない |
| 確認用データ | 読む | 読まない |
| SQLのコンソール表示 | する | しない |
| H2コンソール | 開く | 無効 |
| テンプレートのキャッシュ | しない | する |
| エラー画面の詳細 | 出す | 出さない |
| セッションCookie | 既定 | HttpOnly / Secure / SameSite |
| ログ | INFO | WARN＋ファイル出力 |
| ログイン画面の開発用アカウント表示 | する | **しない** |

**接続情報に既定値を書いていない**
`spring.datasource.url=${DB_URL}` と書き、環境変数が無ければ**起動を失敗させる**。
「うっかり開発用のDBに本番でつないでいた」より「起動しない」ほうが安全。

**初期管理者を環境変数から作る**
マイグレーションSQLに管理者を書くと、パスワードのハッシュがGitに残り、
全社で同じ初期パスワードになる。環境変数で渡せばファイルにもGitにも残らない。

**「本番前に手で消す」を作らない**
ログイン画面の開発用アカウント表示は、当初HTMLに直接書いていた。
それだと本番プロファイルで起動しても表示されてしまう（実測で発見した）。
プロファイルで自動的に消える形に直し、テストで固定した。

## 本番プロファイルで動かす

```
mvnw.cmd -DskipTests package

set DB_URL=jdbc:h2:file:C:/opt/employee/db/employeedb
set DB_USERNAME=sa
set DB_PASSWORD=（DBのパスワード）
set ADMIN_USERNAME=admin
set ADMIN_PASSWORD=（その場で決めた強いパスワード）

java -jar target\employee-app-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

利用者が1人もいなければ、この情報で管理者が1人だけ作られる。
2回目以降の起動では何もしない（運用中に変更したパスワードが戻らないように）。

**確認済みのこと**（2026-09-05 実測）

- 環境変数が無いと起動しない
- 起動後、`employee` テーブルは0件（確認用データを読んでいない）
- `app_user` は1件（作られた管理者のみ）
- `flyway_schema_history` に V1 / V2 / V3 のみ（V900は入っていない）
- H2コンソールの画面が出ない
- ログイン画面に開発用アカウントが出ない

**確認していないこと**

- PostgreSQL など H2 以外のDBでの動作。
  移すには JDBCドライバの追加と、`db/migration` のSQLをそのDBの文法に直す作業が要る
- HTTPS環境での動作。`session.cookie.secure=true` にしてあるため、
  HTTPで動かすとログインが維持できない（それが正しい動作）
- 実際のサーバへのデプロイ

## 入れていないもの

面談で「足りないものは？」と聞かれたときの答え。

| | なぜ必要か |
|---|---|
| 操作ログ（誰がいつ何を変えたか） | 業務系では監査で必ず求められる。今は登録日時・更新日時だけで、更新者を持っていない |
| パスワードの変更画面・有効期限 | 初期パスワードのまま使い続けられてしまう |
| ログイン失敗回数によるアカウントロック | 総当たり攻撃を止められない |
| 権限のもっと細かい制御（部署ごとの参照範囲など） | 今は「全部見える」か「見えない」の2択 |
| 悲観ロック（編集中に他の人を入れない） | 楽観ロックは「保存時に気づく」方式。長時間の入力では取り直しが発生する |
| 一括登録（CSV取り込み） | 出力はあるが取り込みがない |
| 帳票（PDF）出力 | |
| 本番相当のDB（PostgreSQL等）での検証 | |

## 環境について（このPC固有）

このPCではウイルス対策ソフト（Norton）がHTTPS通信を検査していて、
証明書が `Norton Web/Mail Shield Root` に差し替わる。
ブラウザやcurlはWindowsの証明書ストアを見るので通るが、
Javaは自前の信頼リスト（cacerts）を使うため、ライブラリのダウンロードに失敗する。

対策として、Nortonのルート証明書をJDKの信頼リストに取り込んである。
プロジェクト側には何も設定を置いていないので、このコードは別のPCでもそのまま動く。

- 取り込み用スクリプト: `..\_norton-cert\import-norton-cert.ps1`（管理者権限で実行）
- 元に戻す: `..\_norton-cert\uninstall-norton-cert.ps1`
- **JDKを入れ直したら、取り込みスクリプトをもう一度実行する**

同じ理由で npm / pip / Git も詰まることがある。そのときは:

| ツール | 対処 |
|---|---|
| Git | `git config --global http.sslBackend schannel` |
| Node.js | 環境変数 `NODE_EXTRA_CA_CERTS` に `norton-root.cer` を指定 |
| Python | `pip install pip-system-certs` |

## フォルダ名を英字にしている理由

**Windows では、パスに日本語が含まれると `mvnw spring-boot:run` が起動に失敗する。**

```
java.lang.ClassNotFoundException: com.example.employeeapp.EmployeeAppApplication
```

Spring Boot 側の既知の不具合（非ASCIIパスでメインクラスを見つけられない）。
2026-09-05 に 4.1.1 で再現を確認した。
https://github.com/spring-projects/spring-boot/issues/43051

親フォルダを1つでも日本語のままにすると再現するため、
`crowdsourcing` / `03_java-web-app` のどちらも英字にしてある。
**このフォルダ名は日本語に戻さないこと。**

（`java -jar target\*.jar` なら日本語パスでも起動する。実測確認済み）

## 実装中に踏んだ落とし穴

同じところで詰まる人のために残しておく。

**Spring Boot 4 では starter を使わないと自動設定が入らない**
`org.flywaydb:flyway-core` だけを依存に足しても、Flywayは動かない。
起動しても**ログに1行も出ない**ため気づきにくい。
`spring-boot-starter-flyway` を使う。テスト用は `spring-boot-starter-flyway-test`。

**`.properties` ファイルは ISO-8859-1 として読まれる**
値に日本語を書くと文字化けする。
既定値に「システム管理者」と書いたところ、DBには `U+00E3 U+0082 U+00B7 …`
（UTF-8のバイト列が1文字ずつバラバラになった状態）で保存された。
日本語の固定値は Java のソース側に置く。

**権限エラーの画面を `@GetMapping` にすると 405 になる**
Spring Security は権限不足のときフォワードで飛ばすので、
元がPOSTならその画面にもPOSTで届く。`@RequestMapping` にする。
URL直打ち（GET）では正しく403が出るので、見落としやすい。

**開発用パスワードにChromeが警告を出した**
`admin12345` / `viewer12345` は実際に流出したパスワードの一覧に載っており、
ログインのたびに Chrome が「データ侵害で検出されました」と警告した。
手元だけで動くアプリなので実害は無いが、画面を見せる場面で邪魔になる。
**V900を書き換えず、V901を足して更新した**（適用済みのマイグレーションは編集しない）。

**内容が同じ更新では版数が上がらない**
Hibernateは変更が無ければUPDATE文を出さないので `@Version` も増えない。
排他制御の動作確認をするときは、毎回違う値で更新すること。
