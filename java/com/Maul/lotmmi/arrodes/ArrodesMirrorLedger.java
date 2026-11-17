package com.Maul.lotmmi.arrodes;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How many questions each player has had answered through mirrors, and when those questions return.
 * Saved with the world, so dying, logging out or restarting the server does not refill a player's questions.
 * Times are overworld game time, which keeps running while players sleep.
 */
public class ArrodesMirrorLedger extends SavedData {

    private static final String NAME = "lotmmi_arrodes_mirror";

    private static final class Entry {
        int used;
        long resetAt;
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public ArrodesMirrorLedger() {
    }

    public static ArrodesMirrorLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ArrodesMirrorLedger::new, ArrodesMirrorLedger::load, null), NAME);
    }

    public static ArrodesMirrorLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        ArrodesMirrorLedger ledger = new ArrodesMirrorLedger();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            try {
                Entry entry = new Entry();
                entry.used = item.getInt("used");
                entry.resetAt = item.getLong("reset");
                ledger.entries.put(UUID.fromString(item.getString("id")), entry);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putString("id", e.getKey().toString());
            item.putInt("used", e.getValue().used);
            item.putLong("reset", e.getValue().resetAt);
            list.add(item);
        }
        tag.put("entries", list);
        return tag;
    }

    /** Questions this player may still have answered, after dropping an entry whose day has passed. */
    public synchronized int remaining(UUID id, long now, int limit) {
        Entry entry = entries.get(id);
        if (entry != null && entry.resetAt <= now) {
            entries.remove(id);
            setDirty();
            entry = null;
        }
        return entry == null ? limit : Math.max(0, limit - entry.used);
    }

    /** Counts one settled question. The day starts at the first one. Returns how many are left. */
    public synchronized int record(UUID id, long now, int limit, long cooldownTicks) {
        remaining(id, now, limit);
        Entry entry = entries.computeIfAbsent(id, key -> new Entry());
        entry.used++;
        if (entry.used == 1) entry.resetAt = now + cooldownTicks;
        setDirty();
        return Math.max(0, limit - entry.used);
    }

    public synchronized void reset(UUID id) {
        if (entries.remove(id) != null) setDirty();
    }
}
