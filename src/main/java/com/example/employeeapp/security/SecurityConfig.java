package com.example.employeeapp.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ログインと権限の設定。
 *
 * 【誰が何をできるか】
 *   ログインしていない : ログイン画面だけ
 *   VIEWER（閲覧のみ） : 一覧・検索・集計・CSV出力
 *   ADMIN（管理者）     : 上記に加えて 登録・編集・退職・削除
 *
 * 【画面のボタンを隠すだけでは駄目な理由】
 * 一覧画面では VIEWER に編集ボタンを表示しないようにしているが、
 * それは「見た目の親切」でしかない。URLを直接打てば画面は開けてしまう。
 * 実際に止めているのはここ（サーバ側）。
 * 画面側の制御とサーバ側の制御は、必ず両方書く。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * パスワードのハッシュ化方式。
     *
     * BCrypt はわざと計算が遅くなるように作られている。
     * 総当たりで試す攻撃に時間をかけさせるため。
     * MD5 や SHA-256 は速すぎるので、パスワード保存には使わない。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 開発時だけ、H2コンソール（DBの中身を見る画面）を通す設定。
     *
     * 条件を2つとも満たすときだけ、このBeanが作られる。
     *   @Profile("dev")           … 本番プロファイルでは作らない
     *   @ConditionalOnProperty    … H2コンソール自体が有効なときだけ作る
     *
     * 片方だけにしない理由：
     *   プロファイルだけ  → コンソールが無効な環境でも、この設定を読み込もうとして起動に失敗する
     *   プロパティだけ    → 本番でうっかり spring.h2.console.enabled=true にすると開いてしまう
     * 両方を条件にすると、間違えたときに「開いてしまう」のではなく「作られない」側に倒れる。
     *
     * H2コンソールは frame の中で動き、独自のPOSTを投げるため、
     * このパスに限って CSRF対策と frame禁止 を外す必要がある。
     * アプリ本体の設定には手を付けない。
     */
    @Bean
    @Order(1)
    @Profile("dev")
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(PathRequest.toH2Console())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .build();
    }

    /**
     * アプリ本体の設定。
     *
     * CSRF対策は既定で有効。無効化していない。
     * Thymeleaf の th:action を使ったフォームには、隠しトークンが自動で埋め込まれる。
     */
    @Bean
    @Order(2)
    public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // ログイン画面とエラー画面は、ログインしていなくても開けないと困る
                        .requestMatchers("/login", "/error").permitAll()
                        // CSSなどの静的ファイル
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        // データを変える操作（登録・更新・退職・削除）はすべてPOST。
                        // まとめて管理者だけに限定する。
                        .requestMatchers(HttpMethod.POST, "/employees/**").hasRole("ADMIN")
                        // 入力画面を開くところ（GET）も管理者だけ
                        .requestMatchers("/employees/new", "/employees/*/edit").hasRole("ADMIN")

                        // それ以外（一覧・検索・集計・CSV）はログインしていれば見られる
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/login")
                        // ログイン成功後は必ず一覧へ。第2引数 true は
                        // 「元々どこを開こうとしていたかに関わらず一覧に飛ばす」の意味。
                        .defaultSuccessUrl("/employees", true)
                        .failureUrl("/login?error")
                        .permitAll())

                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())

                // 権限が足りないときに真っ白な403を返さず、説明のある画面を出す
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))

                .build();
    }
}
