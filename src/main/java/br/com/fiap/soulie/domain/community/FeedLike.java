package br.com.fiap.soulie.domain.community;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "feed_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"})
)
public class FeedLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private FeedPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    public FeedLike() {
    }

    public static FeedLike of(FeedPost post, User user) {
        var like = new FeedLike();
        like.post = post;
        like.user = user;
        return like;
    }
}
