package org.valkyrienskies.vscrews.helm;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import org.valkyrienskies.vscrews.crew.Crew;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stores helm ownership data on the helm's TileEntity persistent NBT so it moves with ships.
 */
public final class HelmOwnership {
    public static final String TAG_OWNER_UUID = "vscrews_owner_uuid";
    public static final String TAG_CREW_NAME = "vscrews_crew_name";
    private static final String TAG_SHIP_ID    = "ShipId";

    private static final Set<ResourceLocation> HELM_ITEM_IDS = new HashSet<>(Arrays.asList(
            new ResourceLocation("vs_eureka", "acacia_ship_helm"),
            new ResourceLocation("vs_eureka", "oak_ship_helm"),
            new ResourceLocation("vs_eureka", "birch_ship_helm"),
            new ResourceLocation("vs_eureka", "jungle_ship_helm"),
            new ResourceLocation("vs_eureka", "dark_oak_ship_helm"),
            new ResourceLocation("vs_eureka", "crimson_ship_helm"),
            new ResourceLocation("vs_eureka", "warped_ship_helm"),
            new ResourceLocation("vs_eureka", "ship_helm")
    ));

    private HelmOwnership() {}

    public static boolean setOwnership(TileEntity te, UUID owner, Crew crew) {
        if (te == null || owner == null) return false;

        // Owner (player who placed the helm)
        te.getTileData().putUUID(TAG_OWNER_UUID, owner);

        // Crew (if any)
        if (crew != null) {
            te.getTileData().putString(TAG_CREW_NAME, crew.getName());
        } else {
            te.getTileData().remove(TAG_CREW_NAME);
        }

        te.setChanged();
        return true;
    }

    public static UUID getOwner(TileEntity te) {
        if (te == null) return null;
        if (!te.getTileData().contains(TAG_OWNER_UUID, Constants.NBT.TAG_INT_ARRAY)) {
            return null;
        }
        return te.getTileData().getUUID(TAG_OWNER_UUID);
    }

    public static String getCrewName(TileEntity te) {
        if (te == null) return null;
        if (!te.getTileData().contains(TAG_CREW_NAME, Constants.NBT.TAG_STRING)) {
            return null;
        }
        String name = te.getTileData().getString(TAG_CREW_NAME);
        return name.isEmpty() ? null : name;
    }

    public static boolean isHelm(TileEntity te) {
        if (te == null) return false;
        ResourceLocation id = te.getType().getRegistryName();
        return HELM_ITEM_IDS.contains(id);
    }
}
