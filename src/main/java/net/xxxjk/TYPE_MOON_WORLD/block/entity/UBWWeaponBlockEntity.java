package net.xxxjk.TYPE_MOON_WORLD.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class UBWWeaponBlockEntity extends BlockEntity {
    private ItemStack storedItem = ItemStack.EMPTY;
    private long spawnTime = 0L;

    public UBWWeaponBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.UBW_WEAPON_BLOCK_ENTITY.get(), pos, blockState);
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public void setStoredItem(ItemStack itemStack) {
        this.storedItem = itemStack;
        if (this.level != null && this.spawnTime == 0L) {
            this.spawnTime = this.level.getGameTime();
        }
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("StoredItem")) {
            this.storedItem = ItemStack.parseOptional(registries, tag.getCompound("StoredItem"));
        } else {
            this.storedItem = ItemStack.EMPTY;
        }
        if (tag.contains("SpawnTime")) {
            this.spawnTime = tag.getLong("SpawnTime");
        } else {
            this.spawnTime = 0L;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.storedItem.isEmpty()) {
            tag.put("StoredItem", this.storedItem.save(registries));
        }
        tag.putLong("SpawnTime", this.spawnTime);
    }

    public long getSpawnTime() {
        return spawnTime;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
