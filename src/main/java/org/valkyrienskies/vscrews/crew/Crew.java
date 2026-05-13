package org.valkyrienskies.vscrews.crew;

import org.valkyrienskies.core.api.Ship;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Crew {

    private final String name;
    private final UUID owner;

    private final Set<UUID> members = new HashSet<>();
    private final Set<Ship> ownedShips = new HashSet<>();

    public Crew(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members.add(owner);
    }

    /* =========================
     * BASIC INFO
     * ========================= */

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    /* =========================
     * MEMBERS
     * ========================= */

    public boolean isMember(UUID player) {
        return player != null && members.contains(player);
    }

    public void addMember(UUID player) {
        if (player != null) {
            members.add(player);
        }
    }

    public void removeMember(UUID player) {
        if (player != null) {
            members.remove(player);
        }
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    /* =========================
     * SHIPS
     * ========================= */

    public boolean ownsShip(Ship ship) {
        return ship != null && ownedShips.contains(ship);
    }

    public void addShip(Ship ship) {
        if (ship != null) {
            ownedShips.add(ship);
        }
    }

    public void removeShip(Ship ship) {
        if (ship != null) {
            ownedShips.remove(ship);
        }
    }

    public Set<Ship> getShips() {
        return ownedShips;
    }
}
