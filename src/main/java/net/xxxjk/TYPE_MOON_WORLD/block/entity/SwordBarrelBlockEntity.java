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

public class SwordBarrelBlockEntity extends BlockEntity {
    private ItemStack storedItem = ItemStack.EMPTY;

    // Store precise rotation for "侧着扎在方块上"
    private float customPitch = 0f;
    private float customYaw = 0f;
    private long spawnTime = 0L;

    public SwordBarrelBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SWORD_BARREL_BLOCK_ENTITY.get(), pos, blockState);
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
    
    public void setCustomRotation(float pitch, float yaw) {
        this.customPitch = pitch;
        this.customYaw = yaw;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    public float getCustomPitch() {
        return customPitch;
    }
    
    public float getCustomYaw() {
        return customYaw;
    }

    public long getSpawnTime() {
        return spawnTime;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("StoredItem")) {
            this.storedItem = ItemStack.parseOptional(registries, tag.getCompound("StoredItem"));
        } else {
            this.storedItem = ItemStack.EMPTY;
        }
        if (tag.contains("CustomPitch")) {
            this.customPitch = tag.getFloat("CustomPitch");
        }
        if (tag.contains("CustomYaw")) {
            this.customYaw = tag.getFloat("CustomYaw");
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
        tag.putFloat("CustomPitch", this.customPitch);
        tag.putFloat("CustomYaw", this.customYaw);
        tag.putLong("SpawnTime", this.spawnTime);
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
