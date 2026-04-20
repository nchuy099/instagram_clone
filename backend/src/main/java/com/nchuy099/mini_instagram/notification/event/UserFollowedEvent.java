package com.nchuy099.mini_instagram.notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserFollowedEvent {
    private final UUID actorId;
    private final String actorUsername;
    private final String actorAvatarUrl;
    private final UUID recipientId;
    private final String recipientPrincipal;
}
