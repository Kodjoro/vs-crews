package org.valkyrienskies.vscrews.crew;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import org.valkyrienskies.vscrews.VSCrewsConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CrewManager {

    private static final Map<String, Crew> crews = new HashMap<>();
    private static final Map<BlockPos, String> helmOwners = new HashMap<>();
    private static final Map<BlockPos, UUID> helmOwnerUUIDs = new HashMap<>();

    private static CrewSavedData savedData;

    // Initialize from world saved data
    public static void init(ServerWorld world) {
        if (world == null) return;
        DimensionSavedDataManager storage = world.getDataStorage();
        savedData = storage.computeIfAbsent(CrewSavedData::new, CrewSavedData.DATA_NAME);
        // load into runtime map
        crews.clear();
        for (CrewSavedData.CrewRecord rec : savedData.getCrews().values()) {
            Crew c = new Crew(rec.name, rec.owner);
            for (UUID u : rec.members) c.addMember(u);
            crews.put(rec.name, c);
        }
    }

    private static void persist() {
        if (savedData == null) return;
        // write runtime map into savedData
        for (Map.Entry<String, Crew> e : crews.entrySet()) {
            Crew c = e.getValue();
            savedData.putCrew(c.getName(), c.getOwner(), c.getMembers());
        }
    }

    private static boolean nameExistsIgnoreCase(String normalized) {
        for (String existing : crews.keySet()) {
            if (existing.equalsIgnoreCase(normalized)) return true;
        }
        return false;
    }

    // Utility: find the crew containing the given player (member or owner)
    public static Crew findCrewByMember(UUID playerId) {
        if (playerId == null) return null;
        for (Crew c : crews.values()) {
            if (c.isMember(playerId)) return c;
        }
        return null;
    }

    public static boolean createCrew(String name, UUID owner) {
        if (name == null) return false;
        String normalized = name.trim();
        if (normalized.isEmpty()) return false;
        synchronized (crews) {
            if (nameExistsIgnoreCase(normalized)) return false;
            if (VSCrewsConfig.ONLY_ONE_CREW_PER_PLAYER.get()) {
                // Block if player already owns OR is a member of a crew
                if (findCrewByOwner(owner) != null || findCrewByMember(owner) != null) {
                    return false;
                }
            }
            crews.put(normalized, new Crew(normalized, owner));
            persist();
            return true;
        }
    }

    public static boolean crewExists(String name) {
        return crews.containsKey(name);
    }

    public static Crew getCrew(String name) {
        return crews.get(name);
    }

    public static void addMember(String crewName, UUID player) {
        Crew crew = crews.get(crewName);
        if (crew == null) return;
        crew.addMember(player);
        persist();
    }

    public static void removeMember(String crewName, UUID player) {
        Crew crew = crews.get(crewName);
        if (crew == null) return;
        crew.removeMember(player);
        persist();
    }

    public static Collection<Crew> listCrews() {
        return Collections.unmodifiableCollection(crews.values());
    }

    // Helm ownership mapping helpers (used by events elsewhere)
    public static void setHelmOwner(BlockPos pos, String crewName) {
        if (pos == null) return;
        if (crewName == null) {
            helmOwners.remove(pos);
        } else {
            helmOwners.put(pos, crewName);
        }
    }

    public static String getHelmOwner(BlockPos pos) {
        return helmOwners.get(pos);
    }

    public static void setHelmOwnerUUID(BlockPos pos, UUID owner) {
        if (pos == null) return;
        if (owner == null) {
            helmOwnerUUIDs.remove(pos);
        } else {
            helmOwnerUUIDs.put(pos, owner);
        }
    }

    public static UUID getHelmOwnerUUID(BlockPos pos) {
        return helmOwnerUUIDs.get(pos);
    }

    // Utility: find the first crew owned by the given player (if any)
    public static Crew findCrewByOwner(UUID owner) {
        if (owner == null) return null;
        for (Crew c : crews.values()) {
            if (owner.equals(c.getOwner())) return c;
        }
        return null;
    }

    public static void cacheName(UUID id, String name) {
        if (savedData != null) {
            savedData.cacheName(id, name);
        }
    }

    public static String getCachedName(UUID id) {
        return savedData != null ? savedData.getCachedName(id) : null;
    }

    public static boolean isNameTaken(String name) {
        if (name == null) return false;
        String normalized = name.trim();
        for (String existing : crews.keySet()) {
            if (existing.equalsIgnoreCase(normalized)) return true;
        }
        return false;
    }

    public static boolean deleteCrew(String name, UUID requester) {
        if (name == null) return false;
        Crew c = crews.get(name);
        if (c == null) return false;
        // Only owner can delete
        if (!c.getOwner().equals(requester)) return false;
        crews.remove(name);
        persist();
        return true;
    }

    public static boolean leaveCrew(String crewName, UUID player) {
        Crew crew = crews.get(crewName);
        if (crew == null || player == null) return false;
        // Owner cannot leave; must delete the crew
        if (player.equals(crew.getOwner())) return false;
        if (!crew.isMember(player)) return false;
        crew.removeMember(player);
        persist();
        return true;
    }

    public static boolean renameCrew(String oldName, String newName, UUID requester) {
        if (oldName == null || newName == null) return false;
        String normalizedNew = newName.trim();
        if (normalizedNew.isEmpty()) return false;
        Crew c = crews.get(oldName);
        if (c == null) return false;
        if (!c.getOwner().equals(requester)) return false;
        // deny if new name conflicts case-insensitively with any existing crew (excluding self if names equal ignoring case)
        for (String existing : crews.keySet()) {
            if (existing.equalsIgnoreCase(normalizedNew) && !existing.equalsIgnoreCase(oldName)) {
                return false;
            }
        }
        // perform rename: remove old, insert new with same members/owner
        Crew renamed = new Crew(normalizedNew, c.getOwner());
        for (UUID u : c.getMembers()) {
            renamed.addMember(u);
        }
        crews.remove(oldName);
        crews.put(normalizedNew, renamed);
        persist();
        return true;
    }
}
