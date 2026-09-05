package com.example.employeeapp.security;

import com.example.employeeapp.domain.AppUser;
import com.example.employeeapp.domain.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security にログイン中の人を渡すための入れ物。
 *
 * Spring Security は {@link UserDetails} という決まった形しか受け取らない。
 * こちらのDBのクラス（{@link AppUser}）をそのまま渡すことはできないので、
 * ここで包んで渡す。
 *
 * 画面に「〇〇さん」と出したいので、Spring Security 標準の User クラスではなく
 * 自前のクラスにして displayName を持たせている。
 */
public class AppUserDetails implements UserDetails {

    private final AppUser user;

    public AppUserDetails(AppUser user) {
        this.user = user;
    }

    /** 権限。ここでは1人1つだけ持たせている */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().getAuthority()));
    }

    /** BCryptでハッシュ化されたパスワード。照合は Spring Security 側がやる */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /** false の人はログインできない（退職・休職でアカウントを止めた場合） */
    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    // --- ここから下はこのアプリ独自。画面やコントローラから使う ---

    public String getDisplayName() {
        return user.getDisplayName();
    }

    public Role getRole() {
        return user.getRole();
    }
}
