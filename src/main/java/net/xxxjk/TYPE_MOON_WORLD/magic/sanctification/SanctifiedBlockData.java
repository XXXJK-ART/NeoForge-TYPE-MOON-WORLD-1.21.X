package net.xxxjk.TYPE_MOON_WORLD.magic.sanctification;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;

/** Position-level persistence for blocks touched by Sanctification. */
public final class SanctifiedBlockData extends SavedData {
    public static final String NAME = "typemoonworld_sanctified_blocks";
    public static final Factory<SanctifiedBlockData> FACTORY = new Factory<>(SanctifiedBlockData::new, SanctifiedBlockData::load);
    private final Set<Long> positions = new HashSet<>();

    public static SanctifiedBlockData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }
    public static SanctifiedBlockData load(CompoundTag tag, HolderLookup.Provider provider) {
        SanctifiedBlockData data = new SanctifiedBlockData();
        ListTag list = tag.getList("Positions", 4);
        for (int i = 0; i < list.size(); i++) data.positions.add(((LongTag) list.get(i)).getAsLong());
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        positions.forEach(value -> list.add(LongTag.valueOf(value)));
        tag.put("Positions", list);
        return tag;
    }
    public boolean contains(BlockPos pos) { return positions.contains(pos.asLong()); }
    public void add(BlockPos pos) { if (positions.add(pos.asLong())) setDirty(); }
    public void remove(BlockPos pos) { if (positions.remove(pos.asLong())) setDirty(); }
    public void cleanup(ServerLevel level) {
        positions.removeIf(value -> level.isLoaded(BlockPos.of(value)) && level.getBlockState(BlockPos.of(value)).isAir());
        setDirty();
    }
}
