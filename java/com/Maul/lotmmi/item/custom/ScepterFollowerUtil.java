package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.data.ModDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScepterFollowerUtil {

    private static final String KEY_FOLLOWERS = "Followers";

    public record Follower(UUID id, String name, boolean summoned, long lastBlessTick, long lastAttackTick,
                            String returnDimension, double returnX, double returnY, double returnZ,
                            String lastKnownDimension, double lastKnownX, double lastKnownY, double lastKnownZ) {

        CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putString("Name", name);
            tag.putBoolean("Summoned", summoned);
            tag.putLong("LastBlessTick", lastBlessTick);
            tag.putLong("LastAttackTick", lastAttackTick);
            tag.putString("ReturnDimension", returnDimension);
            tag.putDouble("ReturnX", returnX);
            tag.putDouble("ReturnY", returnY);
            tag.putDouble("ReturnZ", returnZ);
            tag.putString("LastKnownDimension", lastKnownDimension);
            tag.putDouble("LastKnownX", lastKnownX);
            tag.putDouble("LastKnownY", lastKnownY);
            tag.putDouble("LastKnownZ", lastKnownZ);
            return tag;
        }

        static Follower fromTag(CompoundTag tag) {
            return new Follower(
                    tag.getUUID("Id"),
                    tag.getString("Name"),
                    tag.getBoolean("Summoned"),
                    tag.getLong("LastBlessTick"),
                    tag.getLong("LastAttackTick"),
                    tag.getString("ReturnDimension"),
                    tag.getDouble("ReturnX"),
                    tag.getDouble("ReturnY"),
                    tag.getDouble("ReturnZ"),
                    tag.contains("LastKnownDimension") ? tag.getString("LastKnownDimension") : tag.getString("ReturnDimension"),
                    tag.contains("LastKnownX") ? tag.getDouble("LastKnownX") : tag.getDouble("ReturnX"),
                    tag.contains("LastKnownY") ? tag.getDouble("LastKnownY") : tag.getDouble("ReturnY"),
                    tag.contains("LastKnownZ") ? tag.getDouble("LastKnownZ") : tag.getDouble("ReturnZ"));
        }

        public Follower withCooldowns(long blessTick, long attackTick) {
            return new Follower(id, name, summoned, blessTick, attackTick, returnDimension, returnX, returnY, returnZ,
                    lastKnownDimension, lastKnownX, lastKnownY, lastKnownZ);
        }

        public Follower withSummoned(boolean newSummoned, ResourceKey<Level> dimension, double x, double y, double z) {
            return new Follower(id, name, newSummoned, lastBlessTick, lastAttackTick,
                    dimension.location().toString(), x, y, z, lastKnownDimension, lastKnownX, lastKnownY, lastKnownZ);
        }

        public Follower withLastKnown(ResourceKey<Level> dimension, double x, double y, double z) {
            return new Follower(id, name, summoned, lastBlessTick, lastAttackTick, returnDimension, returnX, returnY, returnZ,
                    dimension.location().toString(), x, y, z);
        }
    }

    private static CompoundTag get(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SCEPTER_FOLLOWERS.get(), new CompoundTag()).copy();
    }

    private static void save(ItemStack stack, CompoundTag tag) {
        stack.set(ModDataComponents.SCEPTER_FOLLOWERS.get(), tag);
    }

    public static List<Follower> getFollowers(ItemStack stack) {
        CompoundTag root = get(stack);
        List<Follower> out = new ArrayList<>();
        if (!root.contains(KEY_FOLLOWERS)) return out;

        ListTag list = root.getList(KEY_FOLLOWERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            out.add(Follower.fromTag(list.getCompound(i)));
        }
        return out;
    }

    private static void setFollowers(ItemStack stack, List<Follower> followers) {
        CompoundTag root = get(stack);
        ListTag list = new ListTag();
        for (Follower follower : followers) {
            list.add(follower.toTag());
        }
        root.put(KEY_FOLLOWERS, list);
        save(stack, root);
    }

    public static boolean isFollower(ItemStack stack, UUID id) {
        for (Follower follower : getFollowers(stack)) {
            if (follower.id().equals(id)) return true;
        }
        return false;
    }

    public static void addFollower(ItemStack stack, UUID id, String name, ResourceKey<Level> dimension,
                                    double x, double y, double z) {
        List<Follower> followers = getFollowers(stack);
        followers.removeIf(f -> f.id().equals(id));
        String dimensionKey = dimension.location().toString();
        followers.add(new Follower(id, name, false, 0L, 0L, dimensionKey, x, y, z, dimensionKey, x, y, z));
        setFollowers(stack, followers);
    }

    public static void removeFollower(ItemStack stack, UUID id) {
        List<Follower> followers = getFollowers(stack);
        followers.removeIf(f -> f.id().equals(id));
        setFollowers(stack, followers);
    }

    public static void updateFollower(ItemStack stack, Follower updated) {
        List<Follower> followers = getFollowers(stack);
        for (int i = 0; i < followers.size(); i++) {
            if (followers.get(i).id().equals(updated.id())) {
                followers.set(i, updated);
                setFollowers(stack, followers);
                return;
            }
        }
    }

    public static Follower getFollower(ItemStack stack, UUID id) {
        for (Follower follower : getFollowers(stack)) {
            if (follower.id().equals(id)) return follower;
        }
        return null;
    }
}
