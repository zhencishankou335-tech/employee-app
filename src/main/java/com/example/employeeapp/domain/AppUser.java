package com.example.employeeapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * システムにログインする利用者。
 *
 * 社員（{@link Employee}）とは別のテーブルにしている。
 * 社員名簿はシステムを使わない人も含むデータで、利用者はログインする人。
 * 同じものにすると「退職者を名簿から消せない」「派遣の人にログインさせられない」
 * といった要件で必ず行き詰まる。
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ログインID。重複禁止 */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * パスワード。
     *
     * 平文では絶対に保存しない。BCrypt でハッシュ化した文字列が入る。
     * BCrypt は同じパスワードでも毎回違う値になり（ソルトが混ざる）、
     * ハッシュから元のパスワードを復元することもできない。
     * だから「パスワードを忘れた」に対して再発行はできても、
     * 「今のパスワードを教える」はできない。
     */
    @Column(nullable = false, length = 100)
    private String password;

    /** 画面に出す名前。ログインIDをそのまま出すと誰か分かりにくい */
    @Column(nullable = false, length = 50)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * 有効かどうか。
     * 退職・休職のときは行を消さずにここを false にする。
     * 消してしまうと、過去の操作記録から誰だったのか辿れなくなる。
     */
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- getter / setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
