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

    public UBWWeaponBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.UBW_WEAPON_BLOCK_ENTITY.get(), pos, blockState);
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public void setStoredItem(ItemStack itemStack) {
        this.storedItem = itemStack;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("StoredItem")) {
            // Need to handle registry lookup for itemstack in 1.21+ if possible, 
            // but ItemStack.parse or ItemStack.of is standard.
            // In 1.21 it might be ItemStack.parse(registries, tag.getCompound("StoredItem"))
            // Let's try standard way or fallback
            // Note: ItemStack.of(CompoundTag) is deprecated/removed in some versions, replaced by parse
            // Checking reference... assuming 1.21 standard:
            // ItemStack.parse(registries, tag.getCompound("StoredItem")).ifPresent(s -> this.storedItem = s);
            
            // For safety in generated code without full classpath access, I'll use the likely method:
            // ItemStack.parseOptional(registries, tag.getCompound("StoredItem"))
            this.storedItem = ItemStack.parseOptional(registries, tag.getCompound("StoredItem"));
        } else {
            this.storedItem = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.storedItem.isEmpty()) {
            tag.put("StoredItem", this.storedItem.save(registries));
        }
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
