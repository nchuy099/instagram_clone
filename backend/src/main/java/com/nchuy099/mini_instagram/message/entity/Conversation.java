package com.nchuy099.mini_instagram.message.entity;

import com.nchuy099.mini_instagram.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Conversation extends BaseEntity {
}
