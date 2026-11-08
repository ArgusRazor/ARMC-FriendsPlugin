package com.argusrazor.friends.model;

import java.util.*;

public class Party {

    private UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> invited = new HashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID newLeader) {
        if (members.contains(newLeader)) {
            this.leader = newLeader;
        }
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public int getSize() {
        return members.size();
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return leader != null && leader.equals(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
        invited.remove(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public void invite(UUID uuid) {
        invited.add(uuid);
    }

    public boolean isInvited(UUID uuid) {
        return invited.contains(uuid);
    }

    public void removeInvite(UUID uuid) {
        invited.remove(uuid);
    }
}
