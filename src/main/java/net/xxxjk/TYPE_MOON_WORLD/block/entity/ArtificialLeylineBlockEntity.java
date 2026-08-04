package net.xxxjk.TYPE_MOON_WORLD.block.entity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public class ArtificialLeylineBlockEntity extends BlockEntity {
   public static final int BIND_TICKS = 30 * 20;
   private static final int BIND_INTERACTION_GRACE_TICKS = 8;
   private UUID ownerUuid;
   private UUID capturedServantUuid;
   private UUID bindingPlayerUuid;
   private long lastBindInteractTick;
   private int bindProgress;

   public ArtificialLeylineBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.ARTIFICIAL_LEYLINE_BLOCK_ENTITY.get(), pos, state);
   }

   public static void tick(ServerLevel level, BlockPos pos, BlockState state, ArtificialLeylineBlockEntity blockEntity) {
      blockEntity.tickBinding(level);
      blockEntity.validateOwner(level);
      if (blockEntity.shouldShowBeacon() && level.getGameTime() % 10L == 0L) {
         blockEntity.spawnBeaconParticles(level, pos);
      }
      if (level.getGameTime() % 20L == 0L) {
         blockEntity.tryCaptureServant(level, pos);
         blockEntity.tickGuardian(level, pos);
      }
   }

   public boolean isBoundToPlayer() {
      return this.ownerUuid != null;
   }

   public boolean shouldShowBeacon() {
      return this.ownerUuid == null;
   }

   public void resetBindingProgress() {
      this.bindingPlayerUuid = null;
      this.lastBindInteractTick = 0L;
      this.bindProgress = 0;
      this.setChanged();
   }

   public void bindTick(ServerPlayer player) {
      if (this.level == null || this.level.isClientSide() || !this.canBind(player)) {
         this.resetBindingProgress();
         return;
      }
      if (!player.getUUID().equals(this.bindingPlayerUuid)) {
         this.bindProgress = 0;
         this.bindingPlayerUuid = player.getUUID();
      }
      this.lastBindInteractTick = this.level.getGameTime();
      this.setChanged();
   }

   private void tickBinding(ServerLevel level) {
      if (this.bindingPlayerUuid == null) {
         return;
      }
      ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.bindingPlayerUuid);
      if (player == null
         || !player.isAlive()
         || player.distanceToSqr(Vec3.atCenterOf(this.worldPosition)) > 36.0
         || !this.canBind(player)
         || level.getGameTime() - this.lastBindInteractTick > BIND_INTERACTION_GRACE_TICKS) {
         this.resetBindingProgress();
         return;
      }
      this.bindProgress++;
      if (this.bindProgress % 20 == 0) {
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.artificial_leyline.binding", this.bindProgress / 20, 30), true);
      }
      if (this.bindProgress >= BIND_TICKS) {
         ServerPlayer previousOwner = this.ownerUuid == null ? null
            : level.getServer().getPlayerList().getPlayer(this.ownerUuid);
         if (previousOwner != null && !previousOwner.getUUID().equals(player.getUUID())) {
            clearPlayerBonus(previousOwner);
         }
         this.ownerUuid = player.getUUID();
         this.capturedServantUuid = null;
         this.bindingPlayerUuid = null;
         this.lastBindInteractTick = 0L;
         this.bindProgress = 0;
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.master_active) {
            vars.master_artificial_leyline_dimension = player.level().dimension().location().toString();
            vars.master_artificial_leyline_x = this.worldPosition.getX();
            vars.master_artificial_leyline_y = this.worldPosition.getY();
            vars.master_artificial_leyline_z = this.worldPosition.getZ();
            vars.master_artificial_leyline_bonus_active = true;
         } else {
            vars.servant_card_artificial_leyline_dimension = player.level().dimension().location().toString();
            vars.servant_card_artificial_leyline_x = this.worldPosition.getX();
            vars.servant_card_artificial_leyline_y = this.worldPosition.getY();
            vars.servant_card_artificial_leyline_z = this.worldPosition.getZ();
            vars.servant_card_artificial_leyline_bonus_active = true;
         }
         vars.syncPlayerVariables(player);
         player.level().playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2F, 1.15F);
         this.setChanged();
      }
   }

   public void clearPlayerBinding(ServerPlayer player) {
      if (player == null || this.ownerUuid == null || !this.ownerUuid.equals(player.getUUID())) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      clearPlayerBonus(player);
      vars.syncPlayerVariables(player);
      this.ownerUuid = null;
      this.setChanged();
   }

   private boolean canBind(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return (vars.master_active || vars.servant_card_transformed && !vars.master_active)
         && this.capturedServantUuid == null
         && !player.isSpectator();
   }

   private void validateOwner(ServerLevel level) {
      if (this.ownerUuid == null) {
         return;
      }
      ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.ownerUuid);
      if (player == null || !player.isAlive()) {
         this.ownerUuid = null;
         this.setChanged();
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      boolean masterBinding = vars.master_active
         && vars.master_artificial_leyline_bonus_active
         && level.dimension().location().toString().equals(vars.master_artificial_leyline_dimension)
         && vars.master_artificial_leyline_x == this.worldPosition.getX()
         && vars.master_artificial_leyline_y == this.worldPosition.getY()
         && vars.master_artificial_leyline_z == this.worldPosition.getZ();
      boolean servantBinding = vars.servant_card_transformed && !vars.master_active
         && vars.servant_card_artificial_leyline_bonus_active
         && level.dimension().location().toString().equals(vars.servant_card_artificial_leyline_dimension)
         && vars.servant_card_artificial_leyline_x == this.worldPosition.getX()
         && vars.servant_card_artificial_leyline_y == this.worldPosition.getY()
         && vars.servant_card_artificial_leyline_z == this.worldPosition.getZ();
      boolean valid = masterBinding || servantBinding;
      if (!valid) {
         clearPlayerBonus(player);
         this.ownerUuid = null;
         this.setChanged();
      }
   }

   public boolean isOwnedBy(UUID uuid) {
      return uuid != null && uuid.equals(this.ownerUuid);
   }

   public void clearBindingOnDestroy(ServerLevel level) {
      if (this.ownerUuid == null) return;
      ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.ownerUuid);
      if (player != null) clearPlayerBonus(player);
      this.ownerUuid = null;
      this.setChanged();
   }

   private static void clearPlayerBonus(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.master_artificial_leyline_bonus_active = false;
      vars.master_artificial_leyline_dimension = "";
      vars.servant_card_artificial_leyline_bonus_active = false;
      vars.servant_card_artificial_leyline_dimension = "";
      vars.syncPlayerVariables(player);
   }

   private void tryCaptureServant(ServerLevel level, BlockPos pos) {
      if (this.ownerUuid != null || this.capturedServantUuid != null) {
         return;
      }
      AABB box = new AABB(pos).inflate(25.0);
      for (ServantEntity servant : level.getEntitiesOfClass(ServantEntity.class, box, s -> s.isAlive() && isUnboundServant(s))) {
         this.capturedServantUuid = servant.getUUID();
         servant.getPersistentData().putBoolean("ArtificialLeylineGuardian", true);
         servant.getPersistentData().putInt("ArtificialLeylineX", pos.getX());
         servant.getPersistentData().putInt("ArtificialLeylineY", pos.getY());
         servant.getPersistentData().putInt("ArtificialLeylineZ", pos.getZ());
         level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 0.8F);
         this.setChanged();
         break;
      }
   }

   private void tickGuardian(ServerLevel level, BlockPos pos) {
      if (this.capturedServantUuid == null) {
         return;
      }
      if (!(level.getEntity(this.capturedServantUuid) instanceof ServantEntity servant)) {
         this.capturedServantUuid = null;
         this.setChanged();
         return;
      }
      if (!servant.isAlive()) {
         clearGuardianClaim(servant);
         this.capturedServantUuid = null;
         this.setChanged();
         return;
      }
      Vec3 center = Vec3.atCenterOf(pos);
      double distanceSqr = servant.distanceToSqr(center);
      if (distanceSqr > 25.0 * 25.0) {
         servant.getNavigation().moveTo(center.x, center.y, center.z, 1.15);
         if (distanceSqr > 34.0 * 34.0) {
            servant.teleportTo(center.x, center.y + 1.0, center.z);
         }
      }
      LivingEntity current = servant.getTarget();
      if (current != null && current.isAlive() && current.distanceToSqr(center) <= 25.0 * 25.0) {
         return;
      }
      Player target = nearestIntruder(level, pos, servant);
      if (target != null) {
         servant.setTarget(target);
      } else if (distanceSqr <= 25.0 * 25.0) {
         servant.setTarget(null);
      }
   }

   private static Player nearestIntruder(ServerLevel level, BlockPos pos, ServantEntity guardian) {
      AABB box = new AABB(pos).inflate(25.0);
      Player best = null;
      double bestDistance = Double.MAX_VALUE;
      Vec3 center = Vec3.atCenterOf(pos);
      for (Player player : level.getEntitiesOfClass(Player.class, box, p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
         double distance = player.distanceToSqr(center);
         if (distance < bestDistance && !guardian.isAlliedTo(player)) {
            bestDistance = distance;
            best = player;
         }
      }
      return best;
   }

   private void spawnBeaconParticles(ServerLevel level, BlockPos pos) {
      double x = pos.getX() + 0.5;
      double z = pos.getZ() + 0.5;
      double top = Math.min(level.getMaxBuildHeight() - 2.0, pos.getY() + 34.0);
      for (double y = pos.getY() + 1.15; y <= top; y += 2.0) {
         float phase = (level.getGameTime() + (float)y * 5.0F) * 0.08F;
         double swirlX = x + Mth.cos(phase) * 0.18;
         double swirlZ = z + Mth.sin(phase) * 0.18;
         level.sendParticles(ParticleTypes.END_ROD, swirlX, y, swirlZ, 1, 0.01, 0.05, 0.01, 0.0);
      }
      level.sendParticles(ParticleTypes.ENCHANT, x, pos.getY() + 1.0, z, 8, 0.42, 0.25, 0.42, 0.02);
   }

   private static boolean isUnboundServant(ServantEntity servant) {
      CompoundTag data = servant.getPersistentData();
      return !data.getBoolean("TypeMoonHelperClone")
         && !data.getBoolean("TypeMoonSummonedFull")
         && !data.getBoolean("magic_summon")
         && !data.getBoolean("ArtificialLeylineGuardian")
         && !data.contains("ServantMasterUuid")
         && !data.contains("MasterUuid")
         && !data.contains("Owner");
   }

   private static void clearGuardianClaim(ServantEntity servant) {
      CompoundTag data = servant.getPersistentData();
      data.remove("ArtificialLeylineGuardian");
      data.remove("ArtificialLeylineX");
      data.remove("ArtificialLeylineY");
      data.remove("ArtificialLeylineZ");
   }

   @Override
   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }
      if (this.capturedServantUuid != null) {
         tag.putUUID("CapturedServant", this.capturedServantUuid);
      }
      tag.putInt("BindProgress", this.bindProgress);
   }

   @Override
   protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.capturedServantUuid = tag.hasUUID("CapturedServant") ? tag.getUUID("CapturedServant") : null;
      this.bindProgress = tag.getInt("BindProgress");
   }
}
