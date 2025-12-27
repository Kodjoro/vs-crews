package org.valkyrienskies.vscrews.crew;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores crew data (owner + members) persistently in the world save.
 */
public class CrewSavedData extends WorldSavedData {
    public static final String DATA_NAME = "vscrews_crews";

    private final Map<String, CrewRecord> crews = new HashMap<>();
    private final Map<UUID, String> nameCache = new HashMap<>();

    public CrewSavedData() {
        super(DATA_NAME);
    }

    @Override
    public void load(CompoundNBT nbt) {
        crews.clear();
        nameCache.clear();
        CompoundNBT crewsTag = nbt.getCompound("crews");
        for (String name : crewsTag.getAllKeys()) {
            CompoundNBT ctag = crewsTag.getCompound(name);
            UUID owner = ctag.contains("owner") ? UUID.fromString(ctag.getString("owner")) : null;
            Set<UUID> members = new HashSet<>();
            ListNBT mlist = ctag.getList("members", 8); // 8 = StringNBT
            for (int i = 0; i < mlist.size(); i++) {
                String s = mlist.getString(i);
                try {
                    members.add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {}
            }
            crews.put(name, new CrewRecord(name, owner, members));
        }
        CompoundNBT namesTag = nbt.getCompound("names");
        for (String key : namesTag.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                nameCache.put(id, namesTag.getString(key));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        CompoundNBT crewsTag = new CompoundNBT();
        for (Map.Entry<String, CrewRecord> e : crews.entrySet()) {
            CrewRecord rec = e.getValue();
            CompoundNBT ctag = new CompoundNBT();
            if (rec.owner != null) ctag.putString("owner", rec.owner.toString());
            ListNBT mlist = new ListNBT();
            for (UUID u : rec.members) {
                mlist.add(StringNBT.valueOf(u.toString()));
            }
            ctag.put("members", mlist);
            crewsTag.put(e.getKey(), ctag);
        }
        nbt.put("crews", crewsTag);

        CompoundNBT namesTag = new CompoundNBT();
        for (Map.Entry<UUID, String> e : nameCache.entrySet()) {
            namesTag.putString(e.getKey().toString(), e.getValue());
        }
        nbt.put("names", namesTag);
        return nbt;
    }

    public Map<String, CrewRecord> getCrews() {
        return crews;
    }

    public void putCrew(String name, UUID owner, Set<UUID> members) {
        crews.put(name, new CrewRecord(name, owner, members));
        setDirty();
    }

    public void removeCrew(String name) {
        crews.remove(name);
        setDirty();
    }

    public void cacheName(UUID id, String name) {
        if (id == null || name == null || name.isEmpty()) return;
        nameCache.put(id, name);
        setDirty();
    }

    public String getCachedName(UUID id) {
        return id == null ? null : nameCache.get(id);
    }

    public static class CrewRecord {
        public final String name;
        public final UUID owner;
        public final Set<UUID> members;
        public CrewRecord(String name, UUID owner, Set<UUID> members) {
            this.name = name;
            this.owner = owner;
            this.members = new HashSet<>(members);
        }
    }
}
