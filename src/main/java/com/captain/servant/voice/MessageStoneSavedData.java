package com.captain.servant.voice;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class MessageStoneSavedData extends SavedData {
    private static final String DATA_NAME = "fatesupport_message_stones";
    private static final Factory<MessageStoneSavedData> FACTORY =
            new Factory<>(MessageStoneSavedData::new, MessageStoneSavedData::load);

    private final Set<UUID> invalidPairs = new HashSet<>();

    public static MessageStoneSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public boolean isInvalid(UUID pairId) {
        return pairId != null && invalidPairs.contains(pairId);
    }

    public boolean invalidate(UUID pairId) {
        if (pairId != null && invalidPairs.add(pairId)) {
            setDirty();
            return true;
        }
        return false;
    }

    private static MessageStoneSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        MessageStoneSavedData data = new MessageStoneSavedData();
        ListTag pairs = tag.getList("InvalidPairs", 8);
        for (int i = 0; i < pairs.size(); i++) {
            try {
                data.invalidPairs.add(UUID.fromString(pairs.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed entries instead of making the world unloadable.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag pairs = new ListTag();
        invalidPairs.forEach(pairId -> pairs.add(StringTag.valueOf(pairId.toString())));
        tag.put("InvalidPairs", pairs);
        return tag;
    }
}
