package br.com.fiap.soulie.domain.user;

import br.com.fiap.soulie.domain.garden.Garden;
import br.com.fiap.soulie.infra.exception.BusinessException;
import br.com.fiap.soulie.domain.user.dto.UpdateUserData;
import br.com.fiap.soulie.domain.user.enums.UserRole;
import br.com.fiap.soulie.domain.user.dto.UserRequest;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Entity(name = "User")
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;
    private int points;
    private Boolean active;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    private String handle;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "streak_days")
    private int streakDays;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "co2_saved_kg")
    private double co2SavedKg;

    @Column(name = "water_saved_liters")
    private double waterSavedLiters;

    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled;

    @Column(name = "email_weekly_digest")
    private Boolean emailWeeklyDigest;

    @Column(name = "expo_push_token")
    private String expoPushToken;

    @Column(name = "allow_nudges")
    private Boolean allowNudges;

    @Column(name = "show_full_name")
    private Boolean showFullName;

    @Builder.Default
    @ManyToMany(mappedBy = "users")
    private Set<Garden> gardens = new HashSet<>();


    /**
     * Atualiza nome e e-mail. **Não mexe em senha de propósito.**
     *
     * <p>A versão anterior atribuía {@code updateUserData.password()} direto no
     * campo, sem passar pelo encoder — ou seja, {@code PUT /user} gravava a senha
     * em texto puro no banco, ao contrário do cadastro. Além de expor a
     * credencial num eventual dump, travava o usuário fora da conta, porque o
     * BCrypt nunca casa com um valor não criptografado.
     *
     * <p>Troca de senha agora é operação própria: {@link #changePassword}.
     */
    public void updateData(@Valid UpdateUserData updateUserData) {
        if (updateUserData.name() != null && !updateUserData.name().isBlank()) {
            this.name = updateUserData.name();
        }

        if (updateUserData.email() != null && !updateUserData.email().isBlank()) {
            this.email = updateUserData.email();
        }
    }

    /**
     * Troca a senha, sempre criptografada.
     *
     * <p>Pendência conhecida, registrada em {@code docs/07-seguranca.md}: o fluxo
     * ideal exigiria a senha atual antes de aceitar a nova. Hoje basta estar
     * autenticado, então um token roubado permite trocar a senha e tomar a conta.
     */
    public void changePassword(String rawPassword, PasswordEncoder passwordEncoder) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return;
        }
        this.password = passwordEncoder.encode(rawPassword);
    }

    public void deleteUser() {
        this.active = false;
        this.expoPushToken = null;
    }

    public void activateUser() {
        this.active = true;
    }

    public void assignHandle(String handle) {
        this.handle = handle;
    }

    public void updateSettings(Boolean notificationsEnabled, Boolean emailWeeklyDigest,
                               Boolean allowNudges, Boolean showFullName) {
        if (notificationsEnabled != null) {
            this.notificationsEnabled = notificationsEnabled;
        }
        if (emailWeeklyDigest != null) {
            this.emailWeeklyDigest = emailWeeklyDigest;
        }
        if (allowNudges != null) {
            this.allowNudges = allowNudges;
        }
        if (showFullName != null) {
            this.showFullName = showFullName;
        }
    }

    public boolean allowsNudges() {
        return allowNudges == null || allowNudges;
    }

    public String publicName() {
        if (Boolean.FALSE.equals(showFullName) && handle != null && !handle.isBlank()) {
            return handle;
        }
        return name;
    }

    public void addPoints(int xp) {
        if (xp > 0) {
            this.points += xp;
        }
    }

    /**
     * Gasta pontos no resgate de uma recompensa.
     *
     * <p><b>Esta guarda NÃO resolve corrida.</b> Ela impede saldo negativo
     * dentro de uma transação, mas duas transações simultâneas leem o mesmo
     * saldo, as duas passam por aqui e as duas gravam — gastando o mesmo saldo
     * duas vezes. Quem resolve isso é o débito atômico em
     * {@code UserRepository.debitPoints}, que o {@code RewardService} usa, mais
     * o {@code CHECK (points >= 0)} da migration V21.
     *
     * <p>Mantida como última linha de defesa para quem manipular a entidade
     * fora daquele caminho.
     */
    public void spendPoints(int amount) {
        if (amount <= 0) {
            throw new BusinessException("A quantidade de pontos a gastar deve ser positiva");
        }
        if (amount > this.points) {
            throw BusinessException.conflito("Saldo de pontos insuficiente");
        }
        this.points -= amount;
    }

    public void addImpact(double co2, double water) {
        if (co2 > 0) {
            this.co2SavedKg += co2;
        }
        if (water > 0) {
            this.waterSavedLiters += water;
        }
    }

    public void registerActivityDay(LocalDate today) {
        if (today.equals(lastActivityDate)) {
            return;
        }
        if (lastActivityDate != null && lastActivityDate.equals(today.minusDays(1))) {
            streakDays += 1;
        } else {
            streakDays = 1;
        }
        lastActivityDate = today;
    }

    public int currentStreak(LocalDate today) {
        if (lastActivityDate == null || streakDays <= 0) {
            return 0;
        }
        if (lastActivityDate.equals(today) || lastActivityDate.equals(today.minusDays(1))) {
            return streakDays;
        }
        return 0;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (role == null) {
            role = UserRole.USER;
        }
        if (notificationsEnabled == null) {
            notificationsEnabled = true;
        }
        if (emailWeeklyDigest == null) {
            emailWeeklyDigest = false;
        }
        if (allowNudges == null) {
            allowNudges = true;
        }
        if (showFullName == null) {
            showFullName = true;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Antes isto era ROLE_USER fixo, o que tornava inútil o
        // @EnableMethodSecurity e deixava as operações de catálogo abertas a
        // qualquer autenticado.
        UserRole papel = role == null ? UserRole.USER : role;
        return List.of(new SimpleGrantedAuthority(papel.authority()));
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    @Override
    public String getPassword(){
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active == null || active;
    }

    public void encryptPassword(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }
}
