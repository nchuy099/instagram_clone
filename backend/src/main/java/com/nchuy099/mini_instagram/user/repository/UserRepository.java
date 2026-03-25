package com.nchuy099.mini_instagram.user.repository;

import com.nchuy099.mini_instagram.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByUsernameOrEmailOrPhoneNumber(String username, String email, String phoneNumber);

    @Query("""
            select u
            from User u
            where lower(u.username) like lower(concat('%', :query, '%'))
               or lower(coalesce(u.fullName, '')) like lower(concat('%', :query, '%'))
            order by u.username asc
            """)
    List<User> searchUsers(@Param("query") String query);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
