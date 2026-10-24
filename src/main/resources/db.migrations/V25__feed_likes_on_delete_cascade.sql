-- Curtidas precisam cair com o post. Comentários já tinham ON DELETE CASCADE.

ALTER TABLE feed_likes DROP CONSTRAINT fk_feed_like_post;
ALTER TABLE feed_likes
    ADD CONSTRAINT fk_feed_like_post FOREIGN KEY (post_id) REFERENCES feed_posts (id) ON DELETE CASCADE;
