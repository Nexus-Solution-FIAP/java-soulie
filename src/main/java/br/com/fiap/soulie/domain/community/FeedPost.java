package br.com.fiap.soulie.domain.community;

import br.com.fiap.soulie.domain.garden.Garden;
import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "feed_posts")
public class FeedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garden_id")
    private Garden garden;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(name = "image_uri", length = 2000)
    private String imageUri;

    @Column(name = "mission_name")
    private String missionName;

    private Integer xp;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FeedPost() {
    }

    public static FeedPost create(User author, Garden garden, String text, String imageUri, String missionName, Integer xp) {
        var post = new FeedPost();
        post.author = author;
        post.garden = garden;
        post.text = text.trim();
        post.imageUri = imageUri;
        post.missionName = missionName;
        post.xp = xp;
        post.createdAt = LocalDateTime.now();
        return post;
    }
}
