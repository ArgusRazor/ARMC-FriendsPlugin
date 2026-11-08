package com.argusrazor.friends.model;

import java.util.UUID;

public class FriendRequest {

    private final UUID sender;
    private final UUID receiver;
    private final long createdAt;

    public FriendRequest(UUID sender, UUID receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getReceiver() {
        return receiver;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(long timeoutMillis) {
        return (System.currentTimeMillis() - createdAt) > timeoutMillis;
    }
}
