package com.example.employeeapp.repository;

import com.example.employeeapp.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 利用者テーブルへのアクセス。
 *
 * ログイン時に「入力されたIDの人がいるか」を引くのが主な用途。
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
