package org.valkyrienskies.vscrews.crew;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import org.valkyrienskies.core.api.Ship;
import org.valkyrienskies.vscrews.VSCrewsConfig;
import org.valkyrienskies.vscrews.helm.HelmOwnership;

import java.util.*;

import static org.valkyrienskies.mod.common.VSGameUtilsKt.getShipManagingPos;

public class CrewManager {

    private static final Map<String, Crew> crews = new HashMap<>();
    private static CrewSavedData savedData;

    /* ==========================================================
     * INIT / PERSIST
     * ========================================================== */

    public static void init(ServerWorld world) {
        if (world == null) return;

        DimensionSavedDataManager storage = world.getDataStorage();
        savedData = storage.computeIfAbsent(CrewSavedData::new, CrewSavedData.DATA_NAME);

        crews.clear();
        for (CrewSavedData.CrewRecord rec : savedData.getCrews().values()) {
            Crew crew = new Crew(rec.name, rec.owner);
            for (UUID u : rec.members) crew.addMember(u);
            crews.put(rec.name, crew);
        }

        // 🔴 REQUIRED: rebuild ship ownership from helms after load
        for (Crew crew : crews.values()) {
            reconcileHelmsForCrew(world, crew);
        }
    }

    public static void persist() {
        if (savedData == null) return;

        for (Crew crew : crews.values()) {
            savedData.putCrew(crew.getName(), crew.getOwner(), crew.getMembers());
        }
    }

    /* ==========================================================
     * CORE RECONCILIATION LOGIC
     * ========================================================== */

    public static void reconcileHelmsForCrew(ServerWorld world, Crew crew) {
        if (world == null || crew == null) return;

        for (TileEntity te : world.blockEntityList) {
            if (!HelmOwnership.isHelm(te)) continue;

            UUID helmOwner = HelmOwnership.getOwner(te);
            if (helmOwner == null) continue;

            Ship ship = getShipManagingPos(world, te.getBlockPos());
            if (ship == null) continue;

            boolean ownerInCrew = crew.isMember(helmOwner);

            if (ownerInCrew && !crew.ownsShip(ship)) {
                crew.addShip(ship);
            }

            if (!ownerInCrew && crew.ownsShip(ship)) {
                crew.removeShip(ship);
            }
        }
    }

    /* ==========================================================
     * CREW LOOKUPS
     * ========================================================== */

    public static Crew findCrewByMember(UUID playerId) {
        if (playerId == null) return null;
        for (Crew crew : crews.values()) {
            if (crew.isMember(playerId)) return crew;
        }
        return null;
    }

    public static Crew findCrewByOwner(UUID owner) {
        if (owner == null) return null;
        for (Crew crew : crews.values()) {
            if (owner.equals(crew.getOwner())) return crew;
        }
        return null;
    }

    public static Crew findCrewByShip(Ship ship) {
        if (ship == null) return null;
        for (Crew crew : crews.values()) {
            if (crew.ownsShip(ship)) return crew;
        }
        return null;
    }

    public static Crew getCrew(String name) {
        return crews.get(name);
    }

    public static Collection<Crew> listCrews() {
        return Collections.unmodifiableCollection(crews.values());
    }

    private static boolean nameExistsIgnoreCase(String name) {
        for (String existing : crews.keySet()) {
            if (existing.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static UUID findHelmOwner(World world, BlockPos origin) {
        int r = 8; // helms are close; adjust if needed

        for (BlockPos p : BlockPos.betweenClosed(
                origin.offset(-r, -r, -r),
                origin.offset(r, r, r))) {

            TileEntity te = world.getBlockEntity(p);
            if (te == null) continue;

            UUID owner = HelmOwnership.getOwner(te);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    /* ==========================================================
     * CREW LIFECYCLE
     * ========================================================== */

    public static boolean createCrew(ServerWorld world, String name, UUID owner) {
        if (world == null || name == null) return false;

        String normalized = name.trim();
        if (normalized.isEmpty()) return false;

        synchronized (crews) {
            if (nameExistsIgnoreCase(normalized)) return false;

            if (VSCrewsConfig.ONLY_ONE_CREW_PER_PLAYER.get()) {
                if (findCrewByOwner(owner) != null || findCrewByMember(owner) != null) {
                    return false;
                }
            }

            Crew crew = new Crew(normalized, owner);
            crews.put(normalized, crew);

            reconcileHelmsForCrew(world, crew);
            persist();
            return true;
        }
    }

    public static void addMember(ServerWorld world, String crewName, UUID player) {
        Crew crew = crews.get(crewName);
        if (crew == null) return;

        crew.addMember(player);
        reconcileHelmsForCrew(world, crew);
        persist();
    }

    public static void removeMember(ServerWorld world, String crewName, UUID player) {
        Crew crew = crews.get(crewName);
        if (crew == null) return;

        crew.removeMember(player);
        reconcileHelmsForCrew(world, crew);
        persist();
    }

    public static boolean leaveCrew(ServerWorld world, String crewName, UUID player) {
        Crew crew = crews.get(crewName);
        if (crew == null) return false;
        if (player.equals(crew.getOwner())) return false;
        if (!crew.isMember(player)) return false;

        crew.removeMember(player);
        reconcileHelmsForCrew(world, crew);
        persist();
        return true;
    }

    public static boolean deleteCrew(ServerWorld world, String name, UUID requester) {
        Crew crew = crews.get(name);
        if (crew == null) return false;
        if (!crew.getOwner().equals(requester)) return false;

        for (Ship ship : new HashSet<>(crew.getShips())) {
            crew.removeShip(ship);
        }

        crews.remove(name);
        persist();
        return true;
    }

    public static boolean renameCrew(String oldName, String newName, UUID requester) {
        if (oldName == null || newName == null) return false;

        String normalizedNew = newName.trim();
        if (normalizedNew.isEmpty()) return false;

        Crew crew = crews.get(oldName);
        if (crew == null) return false;
        if (!crew.getOwner().equals(requester)) return false;

        for (String existing : crews.keySet()) {
            if (existing.equalsIgnoreCase(normalizedNew)
                    && !existing.equalsIgnoreCase(oldName)) {
                return false;
            }
        }

        Crew renamed = new Crew(normalizedNew, crew.getOwner());
        for (UUID u : crew.getMembers()) renamed.addMember(u);
        for (Ship ship : crew.getShips()) renamed.addShip(ship);

        crews.remove(oldName);
        crews.put(normalizedNew, renamed);
        persist();
        return true;
    }

    /* ==========================================================
     * NAME CACHE
     * ========================================================== */

    public static void cacheName(UUID id, String name) {
        if (id == null || name == null || name.isEmpty()) return;
        if (savedData != null) {
            savedData.cacheName(id, name);
        }
    }

    public static String getCachedName(UUID id) {
        return savedData != null ? savedData.getCachedName(id) : null;
    }

    /* ==========================================================
     * NAME CHECK
     * ========================================================== */

    public static boolean isNameTaken(String name) {
        if (name == null) return false;
        String normalized = name.trim();
        for (String existing : crews.keySet()) {
            if (existing.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }
}
