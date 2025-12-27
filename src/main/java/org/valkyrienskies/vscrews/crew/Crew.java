package org.valkyrienskies.vscrews.crew;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Crew {
    String name;
    UUID owner;
    Set<UUID> members = new HashSet<>();

    public Crew(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members.add(owner);
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    public void addMember(UUID player) {
        members.add(player);
    }

    public void removeMember(UUID player) {
        members.remove(player);
    }

    public Set<UUID> getMembers() {
        return members;
    }
}
