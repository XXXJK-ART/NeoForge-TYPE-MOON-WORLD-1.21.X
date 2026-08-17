package com.example.typemoonaddon.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class SakuraCompoundData implements INBTSerializable<CompoundTag> {
    private CompoundTag data = new CompoundTag();

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return data.copy();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        data = tag == null ? new CompoundTag() : tag.copy();
    }
}
