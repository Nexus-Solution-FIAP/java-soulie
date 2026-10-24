package br.com.fiap.soulie.domain.community;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notifications")
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserNotification() {
    }

    public static UserNotification nudge(User receiver, String senderName) {
        var notification = new UserNotification();
        notification.user = receiver;
        notification.title = "Cutucada no jardim";
        String who = senderName == null || senderName.isBlank() ? "Um amigo" : senderName.trim();
        notification.body = who + " te cutucou. Continue as atividades para não perder o ritmo!";
        notification.createdAt = LocalDateTime.now();
        return notification;
    }

    /**
     * Notificação gerada pelo próprio sistema, sem outro usuário envolvido.
     *
     * <p>É por aqui que o resultado da validação de imagem chega ao usuário: a
     * análise acontece depois da requisição, então não há como avisar na
     * resposta.
     */
    public static UserNotification system(User receiver, String title, String body) {
        var notification = new UserNotification();
        notification.user = receiver;
        notification.title = title;
        notification.body = body;
        notification.createdAt = LocalDateTime.now();
        return notification;
    }

    public void markRead(LocalDateTime when) {
        if (readAt == null) {
            readAt = when;
        }
    }
}
