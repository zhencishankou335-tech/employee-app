package com.example.employeeapp.security;

import com.example.employeeapp.domain.AppUser;
import com.example.employeeapp.domain.Role;
import com.example.employeeapp.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 本番で最初の管理者を1人だけ作る。
 *
 * 【なぜ必要か】
 * 開発用の利用者は db/dev のSQLで入れているが、本番ではそれを読まない。
 * そのままだと利用者が1人もいない＝誰もログインできない状態で起動してしまう。
 *
 * 【なぜSQLに直接書かないか】
 * マイグレーションのSQLに管理者を書くと、パスワードのハッシュがGitに残る。
 * 全社で同じ初期パスワードになり、変更しても履歴に残り続ける。
 * 環境変数で渡せば、Gitにもファイルにも残らない。
 *
 *   set ADMIN_USERNAME=admin
 *   set ADMIN_PASSWORD=（その場で決めた強いパスワード）
 *   java -jar employee-app.jar --spring.profiles.active=prod
 *
 * 環境変数が設定されていなければ、application-prod.properties の ${ADMIN_USERNAME} が
 * 解決できず、アプリは起動時にエラーで止まる。これは意図した動作。
 * 「管理者がいないまま起動してしまう」より「起動しない」ほうが安全なため。
 */
@Component
@Profile("prod")
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    /**
     * 表示名の既定値。
     *
     * .properties ファイルは ISO-8859-1 として読まれるため、
     * 設定ファイル側に日本語の既定値を書くと文字化けする（実測で確認）。
     * 日本語の固定値は、UTF-8でコンパイルされる Java のソース側に置く。
     */
    private static final String DEFAULT_DISPLAY_NAME = "システム管理者";

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;

    public AdminBootstrap(AppUserRepository repository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.username}") String username,
                          @Value("${app.admin.password}") String password,
                          @Value("${app.admin.display-name}") String displayName) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        // 1人でも利用者がいれば何もしない。
        // 毎回パスワードを上書きすると、運用中に変更した内容が起動のたびに戻ってしまう。
        if (repository.count() > 0) {
            return;
        }

        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "利用者が1人も登録されていません。"
                    + "環境変数 ADMIN_USERNAME / ADMIN_PASSWORD を設定して起動してください。");
        }

        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setDisplayName(displayName.isBlank() ? DEFAULT_DISPLAY_NAME : displayName);
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        repository.save(admin);

        // パスワードはログに出さない。ログは開発者以外も見ることがある。
        log.info("初期管理者を作成しました（ログインID: {}）。速やかにパスワードを変更してください。", username);
    }
}
