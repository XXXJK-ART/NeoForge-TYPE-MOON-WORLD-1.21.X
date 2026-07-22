package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import org.jetbrains.annotations.Nullable;

public abstract class OwnedPaleRiderMob extends PathfinderMob {
   private static final String TAG_OWNER = "PaleRiderOwner";
   @Nullable
   private UUID ownerUuid;

   protected OwnedPaleRiderMob(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   public void setPaleRiderOwner(PaleRiderEntity owner) {
      this.ownerUuid = owner.getUUID();
   }

   @Nullable
   public UUID getPaleRiderOwnerUuid() {
      return this.ownerUuid;
   }

   @Nullable
   public PaleRiderEntity getPaleRiderOwner() {
      if (this.ownerUuid == null || !(this.level() instanceof ServerLevel level)) {
         return null;
      }
      return level.getEntity(this.ownerUuid) instanceof PaleRiderEntity owner ? owner : null;
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      PaleRiderEntity owner = this.getPaleRiderOwner();
      if (owner == null) {
         return false;
      }
      return other == owner || owner.isAlliedTo(other)
         || other instanceof OwnedPaleRiderMob owned && this.ownerUuid != null && this.ownerUuid.equals(owned.ownerUuid)
         || other.getPersistentData().hasUUID(PaleRiderInfectionService.TAG_OWNER)
            && this.ownerUuid != null
            && this.ownerUuid.equals(other.getPersistentData().getUUID(PaleRiderInfectionService.TAG_OWNER))
            && other.getPersistentData().getBoolean(PaleRiderInfectionService.TAG_CONTROLLED);
   }

   @Override
   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.ownerUuid != null) {
         tag.putUUID(TAG_OWNER, this.ownerUuid);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.ownerUuid = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
   }
}
