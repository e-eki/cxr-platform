package io.github.eeki.cxr.userservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Пользователь системы.
 *
 * <p>Реализует {@link UserDetails}, чтобы Spring Security мог использовать
 * эту сущность напрямую в процессе аутентификации. Пароль хранится в виде
 * BCrypt-хэша.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity implements UserDetails {

    /** Уникальный идентификатор пользователя. Генерируется приложением. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Логин пользователя. Уникален в системе. */
    @Column(unique = true, nullable = false, length = 64)
    private String username;

    /** Email пользователя. Уникален в системе. */
    @Column(unique = true, nullable = false, length = 128)
    private String email;

    /** BCrypt-хэш пароля. Никогда не возвращается клиенту. */
    @Column(nullable = false)
    private String password;

    /** Роль пользователя в системе. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    /** Возвращает authorities в формате, ожидаемом Spring Security. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}