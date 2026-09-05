# 社員名簿（業務系Webアプリ練習）

Java + Spring Boot で作った、業務系Webアプリケーションの最小構成。
一覧・検索・登録・編集・削除・CSV出力という、社内システムの骨格そのものを1本にまとめてある。

## 動かし方

Java 21 が入っていれば、これだけで動く。

```
cd employee-app
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run          # Mac / Linux
```

ブラウザで http://localhost:8080 を開く。初回起動時に社員データが12件入る。

止めるときはコンソールで Ctrl + C。

### テストの実行

```
mvnw.cmd test
```

### データベースの中身を見る

http://localhost:8080/h2-console

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
| DBアクセス | Spring Data JPA / Hibernate |
| データベース | H2（ファイルモード） |
| 入力チェック | Jakarta Bean Validation |
| ビルド | Maven（mvnw 同梱のため Maven のインストール不要） |
| テスト | JUnit 5 / MockMvc / AssertJ |

## 画面と機能

| 画面 | URL | できること |
|---|---|---|
| 社員一覧 | `/employees` | 検索（氏名・フリガナ・社員番号の部分一致／部署／在籍区分）、ページング、件数表示 |
| 新規登録 | `/employees/new` | 入力チェック付きの登録 |
| 編集 | `/employees/{id}/edit` | 既存値を出して更新 |
| 退職処理 | `POST /employees/{id}/retire` | 行を残したまま在籍区分だけ変える（論理削除） |
| 削除 | `POST /employees/{id}/delete` | 行ごと消す（物理削除） |
| CSV出力 | `/employees/csv` | 画面の検索条件をそのまま反映してCSVを落とす |

## ファイル構成

```
src/main/java/com/example/employeeapp/
├── EmployeeAppApplication.java   起動クラス
├── DataInitializer.java          初期データ投入（学習用）
├── domain/                       データそのもの
│   ├── Employee.java             DBの1行に対応するクラス
│   ├── Department.java           部署（選択肢を固定するenum）
│   └── EmploymentStatus.java     在籍区分
├── repository/
│   └── EmployeeRepository.java   DBアクセス。検索SQLはここ
├── service/
│   ├── EmployeeService.java      業務ルール（重複判定・論理削除など）
│   └── DuplicateEmployeeNumberException.java
└── web/
    ├── EmployeeController.java   URLの受け口。画面に渡す値を組み立てる
    ├── EmployeeForm.java         画面入力の入れ物＋入力チェックのルール
    └── HomeController.java

src/main/resources/
├── application.properties        DB接続やポートなどの設定
├── templates/employees/
│   ├── list.html                 一覧画面
│   └── form.html                 登録・編集画面
└── static/css/app.css            画面のスタイル

src/test/java/com/example/employeeapp/
├── repository/EmployeeRepositoryTest.java   検索条件の組み合わせを検証
└── web/EmployeeControllerTest.java          入力チェックと画面遷移を検証
```

## 意識して入れた「業務系らしさ」

面談で聞かれたときに答えられるように、なぜそうしたかを書いておく。

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
