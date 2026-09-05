package com.example.employeeapp.security;

import com.example.employeeapp.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ログインIDから利用者を引いてくる。
 *
 * Spring Security は「IDを渡すから、その人の情報とパスワードを返して」としか言ってこない。
 * 入力されたパスワードが合っているかの照合は Spring Security 側がやるので、
 * ここでパスワードを比較してはいけない（比較を自作すると事故のもとになる）。
 *
 * このクラスを @Service として置いておくだけで、Spring Boot が自動で使う。
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    public DatabaseUserDetailsService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByUsername(username)
                .map(AppUserDetails::new)
                // メッセージに「そのIDは存在しない」と書かない。
                // 存在するIDだけ反応が変わると、IDの総当たりで有効なIDを特定されてしまう。
                .orElseThrow(() -> new UsernameNotFoundException("認証に失敗しました"));
    }
}
