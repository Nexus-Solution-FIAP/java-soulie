package br.com.fiap.soulie.domain.community;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "feed_comments")
public class FeedComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private FeedPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User author;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FeedComment() {
    }

    public static FeedComment create(FeedPost post, User author, String text) {
        var comment = new FeedComment();
        comment.post = post;
        comment.author = author;
        comment.text = text.trim();
        comment.createdAt = LocalDateTime.now();
        return comment;
    }
}
