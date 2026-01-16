package com.nchuy099.mini_instagram.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass // Indicate that this is a base class for JPA entities
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @CreationTimestamp
    @Column(name = "\"createdAt\"")
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "\"updatedAt\"")
    Instant updatedAt;

}
