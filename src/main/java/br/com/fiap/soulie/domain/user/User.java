package br.com.fiap.soulie.domain.user;

import br.com.fiap.soulie.domain.garden.Garden;
import br.com.fiap.soulie.domain.user.dto.UpdateUserData;
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

    @ManyToMany(mappedBy = "users")
    private Set<Garden> gardens = new HashSet<>();

    public User() {
    }

    public void updateData(@Valid UpdateUserData updateUserData) {
        if(updateUserData.name() != null ){
            this.name = updateUserData.name();
        }

        if(updateUserData.email() != null){
            this.email = updateUserData.email();
        }

        // Aqui precisa passar por uma validação,
        // o usuário não pode simplesmente mudar a senha quando ele quiser.
        if(updateUserData.password() != null){
            this.password = updateUserData.password();
        }
    }

    public void deleteUser() {
        this.active = false;
    }

    public void activateUser() {
        this.active = true;
    }

    public void assignHandle(String handle) {
        this.handle = handle;
    }

    public void updateSettings(Boolean notificationsEnabled, Boolean emailWeeklyDigest) {
        if (notificationsEnabled != null) {
            this.notificationsEnabled = notificationsEnabled;
        }
        if (emailWeeklyDigest != null) {
            this.emailWeeklyDigest = emailWeeklyDigest;
        }
    }

    public void addPoints(int xp) {
        if (xp > 0) {
            this.points += xp;
        }
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
        if (notificationsEnabled == null) {
            notificationsEnabled = true;
        }
        if (emailWeeklyDigest == null) {
            emailWeeklyDigest = false;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
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
