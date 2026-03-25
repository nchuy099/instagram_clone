package com.nchuy099.mini_instagram.post.repository;

import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Post> findByUserInOrderByCreatedAtDesc(List<User> users, Pageable pageable);

    @Query("""
            select p
            from Post p
            where lower(coalesce(p.caption, '')) like lower(concat('%', :query, '%'))
            order by p.createdAt desc
            """)
    List<Post> searchPostsByCaption(@Param("query") String query);

    @Query("""
            select distinct h.name
            from PostHashtag ph
            join ph.hashtag h
            where lower(h.name) like lower(concat('%', :query, '%'))
            order by h.name asc
            """)
    List<String> searchHashtags(@Param("query") String query);

    @Query("""
            select distinct p
            from PostHashtag ph
            join ph.post p
            join ph.hashtag h
            where lower(h.name) = lower(:hashtag)
            order by p.createdAt desc
            """)
    List<Post> searchPostsByHashtag(@Param("hashtag") String hashtag);
}
