package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.SoulLibrary;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.SoulSnapshot;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public final class PaleRiderEntity extends ServantEntity {
   public static final String SERVANT_KEY = "pale_rider";
   private final SoulLibrary soulLibrary = new SoulLibrary();
   private final Map<UUID, SoulSnapshot> manifestedSouls = new HashMap<>();
   private UUID possessedHostUuid;
   private UUID combatTargetUuid;
   private long nextPossessedAttackTick;
   private UUID masterUuid;

   public PaleRiderEntity(EntityType<? extends PaleRiderEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   public void bindMaster(net.minecraft.server.level.ServerPlayer master) {
      super.bindMaster(master);
      this.masterUuid = master.getUUID();
   }

   @Override
   public void unbindMaster() {
      super.unbindMaster();
      this.masterUuid = null;
   }

   @Override
   public UUID getMasterUuid() {
      return this.masterUuid;
   }

   @Override
   public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
      net.minecraft.world.level.ServerLevelAccessor level,
      net.minecraft.world.DifficultyInstance difficulty,
      net.minecraft.world.entity.MobSpawnType spawnType,
      net.minecraft.world.entity.SpawnGroupData spawnData
   ) {
      net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
      this.applyBaseStats(true);
      return result;
   }

   @Override
   protected void customServerAiStep() {
      if (this.getPersistentData().getBoolean("PaleRiderCardProxy")) {
         this.getNavigation().stop();
         return;
      }
      super.customServerAiStep();
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.getPersistentData().getBoolean("PaleRiderCardProxy")) {
         this.discard();
         return;
      }
      if (this.level() instanceof ServerLevel level && this.isAlive() && !this.hasPossessedHost()
         && this.tickCount % 8 == Math.floorMod(this.getId(), 8)) {
         level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 0.9, this.getZ(), 4, 0.28, 0.85, 0.28, 0.015);
         level.sendParticles(new DustParticleOptions(new Vector3f(0.025F, 0.025F, 0.03F), 1.5F),
            this.getX(), this.getY() + 0.9, this.getZ(), 5, 0.3, 0.9, 0.3, 0.01);
         level.sendParticles(ParticleTypes.ASH, this.getX(), this.getY() + 1.1, this.getZ(), 2, 0.3, 0.8, 0.3, 0.015);
      }
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) return true;
      if (this.masterUuid != null && this.masterUuid.equals(other.getUUID())) return true;
      if (other instanceof OwnedPaleRiderMob owned && this.getUUID().equals(owned.getPaleRiderOwnerUuid())) return true;
      return other.getPersistentData().getBoolean(PaleRiderInfectionService.TAG_CONTROLLED)
         && other.getPersistentData().hasUUID(PaleRiderInfectionService.TAG_OWNER)
         && this.getUUID().equals(other.getPersistentData().getUUID(PaleRiderInfectionService.TAG_OWNER));
   }

   @Override
   protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
      net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
      if (stack.is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.SERVANT_MASTER_CONTRACT.get()) && !this.level().isClientSide()) {
         this.masterUuid = player.getUUID();
         if (!player.getAbilities().instabuild) stack.shrink(1);
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.pale_rider.contract"), true);
         return net.minecraft.world.InteractionResult.SUCCESS;
      }
      return super.mobInteract(player, hand);
   }

   public LivingEntity getMaster() {
      if (this.masterUuid == null || !(this.level() instanceof ServerLevel level)) return null;
      return level.getEntity(this.masterUuid) instanceof LivingEntity living ? living : null;
   }

   public LivingEntity findPaleRiderEnemy(double radius) {
      LivingEntity current = this.resolveCombatTarget();
      if (current != null && current.isAlive() && !current.isAlliedTo(this) && !this.isAlliedTo(current) && !EntityUtils.isImmunePlayerTarget(current)) {
         if (this.getTarget() != current) this.setTarget(current);
         return current;
      }
      if (current != null) this.clearCombatTarget();
      return null;
   }

   public void lockCombatTarget(LivingEntity target) {
      if (target == null || target == this || !target.isAlive() || this.isAlliedTo(target) || target.isAlliedTo(this)
         || EntityUtils.isImmunePlayerTarget(target)) return;
      this.combatTargetUuid = target.getUUID();
      this.setTarget(target);
   }

   public void clearCombatTarget() {
      this.combatTargetUuid = null;
      this.setTarget(null);
   }

   private LivingEntity resolveCombatTarget() {
      if (!(this.level() instanceof ServerLevel level)) return this.getTarget();
      if (this.combatTargetUuid != null && level.getEntity(this.combatTargetUuid) instanceof LivingEntity living) return living;
      LivingEntity current = this.getTarget();
      if (current != null && current.isAlive()) this.combatTargetUuid = current.getUUID();
      return current;
   }

   public boolean captureSoul(LivingEntity defeated) {
      if (defeated == null || defeated == this || defeated instanceof OwnedPaleRiderMob || defeated instanceof PaleRiderCrowEntity
         || defeated instanceof net.minecraft.world.entity.decoration.ArmorStand) {
         return false;
      }
      if (net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper.isServantLike(defeated)) {
         return false;
      }
      return this.soulLibrary.add(SoulSnapshot.capture(defeated));
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      ServantVoiceHelper.tryPlayAttack(this);
      return super.doHurtTarget(target);
   }

   public SoulLibrary getSoulLibrary() {
      return this.soulLibrary;
   }

   public void markManifested(SoulSnapshot snapshot) {
      this.manifestedSouls.put(snapshot.id(), snapshot);
   }

   public void consumeManifestedSoul(UUID id) {
      this.manifestedSouls.remove(id);
   }

   public void returnManifestedSoul(UUID id) {
      SoulSnapshot snapshot = this.manifestedSouls.remove(id);
      if (snapshot != null) this.soulLibrary.add(snapshot);
   }

   public void returnAllLivingSouls() {
      if (!(this.level() instanceof ServerLevel level)) return;
      for (SoulSnapshot snapshot : this.manifestedSouls.values()) {
         this.soulLibrary.add(snapshot);
      }
      this.manifestedSouls.clear();
      List<SoulEchoEntity> echoes = level.getEntitiesOfClass(SoulEchoEntity.class, this.getBoundingBox().inflate(96.0),
         echo -> this.getUUID().equals(echo.getPaleRiderOwnerUuid()));
      for (SoulEchoEntity echo : echoes) {
         echo.discard();
      }
   }

   public boolean isUnderworldActive() {
      return this.getPersistentData().getBoolean(PaleRiderCombatHelper.TAG_UNDERWORLD_ACTIVE)
         || this.getPersistentData().getLong(PaleRiderCombatHelper.TAG_UNDERWORLD_UNTIL) > this.level().getGameTime();
   }

   public boolean isCalamityActive() {
      return this.getPersistentData().getBoolean(PaleRiderCombatHelper.TAG_CALAMITY_ACTIVE);
   }

   public boolean hasAnyDomain() {
      return this.isUnderworldActive() || this.isCalamityActive();
   }

   public void disableCalamity(ApocalypseHorsemanEntity.Calamity calamity) {
      this.getPersistentData().putBoolean(PaleRiderCombatHelper.calamityEnabledTag(calamity), false);
   }

   public void spawnHorsemanParticles(ApocalypseHorsemanEntity horseman) {
      if (!(this.level() instanceof ServerLevel level)) return;
      Vector3f color = horseman.isPaleRiderProxy() ? new Vector3f(0.12F, 0.5F, 1.0F) : switch (horseman.getCalamity()) {
         case SWORD -> new Vector3f(0.85F, 0.88F, 0.92F);
         case FAMINE -> new Vector3f(0.45F, 0.025F, 0.02F);
         case BEAST -> new Vector3f(0.035F, 0.035F, 0.035F);
      };
      level.sendParticles(new DustParticleOptions(color, 1.5F), horseman.getX(), horseman.getY() + 0.9, horseman.getZ(), 5, 0.3, 0.9, 0.3, 0.015);
      level.sendParticles(ParticleTypes.ASH, horseman.getX(), horseman.getY() + 1.0, horseman.getZ(), 2, 0.25, 0.8, 0.25, 0.01);
   }

   public boolean beginPossession(Mob host) {
      if (host == null || host == this || !host.isAlive() || host instanceof ServantEntity
         || PaleRiderInfectionService.isForbiddenPossessionHost(host)
         || host.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)) return false;
      if (!(host instanceof SoulEchoEntity) && !PaleRiderInfectionService.isControlled(host)
         && !PaleRiderInfectionService.hasControlCapacity(this, 1)) return false;
      if (this.possessedHostUuid != null && this.possessedHostUuid.equals(host.getUUID()) && this.getVehicle() == host) return true;
      this.endPossession();
      if (!this.startRiding(host, true)) return false;
      this.possessedHostUuid = host.getUUID();
      host.getPersistentData().putBoolean("PaleRiderPossessed", true);
      if (!(host instanceof SoulEchoEntity) && !PaleRiderInfectionService.forceControl(host, this)) {
         this.endPossession();
         return false;
      }
      return true;
   }

   public void tickPossession() {
      if (this.possessedHostUuid == null || !(this.level() instanceof ServerLevel level)) return;
      Entity hostEntity = level.getEntity(this.possessedHostUuid);
      if (!(hostEntity instanceof Mob host) || !host.isAlive() || this.getVehicle() != host
         || PaleRiderInfectionService.isForbiddenPossessionHost(host)) {
         this.endPossession();
         return;
      }
      if (!(host instanceof SoulEchoEntity) && !PaleRiderInfectionService.isInfectedBy(host, this)) {
         this.endPossession();
         return;
      }
      LivingEntity retaliator = host.getLastHurtByMob();
      if (retaliator != null && retaliator.isAlive()) this.lockCombatTarget(retaliator);
      LivingEntity target = this.findPaleRiderEnemy(64.0);
      if (target != null) {
         host.setTarget(target);
         host.setAggressive(true);
         host.getLookControl().setLookAt(target, 30.0F, 30.0F);
         host.getNavigation().moveTo(target, 1.25);
         double reach = host.getBbWidth() + target.getBbWidth() + 1.25;
         long now = this.level().getGameTime();
         if (host.distanceToSqr(target) <= reach * reach && host.getSensing().hasLineOfSight(target) && now >= this.nextPossessedAttackTick) {
            this.nextPossessedAttackTick = now + 20L;
            host.swing(InteractionHand.MAIN_HAND, true);
            if (host.getAttribute(Attributes.ATTACK_DAMAGE) != null) host.doHurtTarget(target);
            else target.hurt(host.damageSources().mobAttack(host), 2.0F);
            PaleRiderInfectionService.infect(target, this, 2);
         }
      } else {
         host.setTarget(null);
         host.setAggressive(false);
      }
   }

   public void endPossession() {
      if (this.getVehicle() instanceof Mob host) {
         host.getPersistentData().remove("PaleRiderPossessed");
         if (host instanceof SoulEchoEntity echo && !this.isUnderworldActive() && echo.isAlive() && echo.getSoulId() != null) {
            this.returnManifestedSoul(echo.getSoulId());
            echo.discard();
         }
      }
      this.stopRiding();
      this.possessedHostUuid = null;
   }

   public boolean hasPossessedHost() {
      return this.possessedHostUuid != null && this.getVehicle() != null && this.possessedHostUuid.equals(this.getVehicle().getUUID());
   }

   public boolean isPossessing(Entity entity) {
      return entity != null && this.possessedHostUuid != null && this.possessedHostUuid.equals(entity.getUUID());
   }

   public Mob getPossessedHost() {
      return this.getVehicle() instanceof Mob host && this.isPossessing(host) ? host : null;
   }

   public boolean redirectPossessedDamage(DamageSource source, float amount) {
      Mob host = this.getPossessedHost();
      if (host == null || !host.isAlive()) return false;
      if (source.getEntity() == host || source.getDirectEntity() == host) return true;
      host.invulnerableTime = 0;
      host.hurt(source, amount);
      return true;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (this.redirectPossessedDamage(source, amount)) return false;
      if (net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper.isMagicDamage(source)) amount *= 0.8F;
      int soulTiers = PaleRiderCombatHelper.countLivingSoulTiers(this);
      if (this.isUnderworldActive() && soulTiers > 0) amount *= Math.max(0.0F, 1.0F - soulTiers * 0.05F);
      return super.hurt(source, amount);
   }

   @Override
   public void die(DamageSource cause) {
      PaleRiderCombatHelper.cleanupAll(this, false);
      this.soulLibrary.clear();
      this.manifestedSouls.clear();
      super.die(cause);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.put("PaleRiderSoulLibrary", this.soulLibrary.save());
      ListTag manifested = new ListTag();
      for (SoulSnapshot snapshot : this.manifestedSouls.values()) manifested.add(snapshot.save());
      tag.put("PaleRiderManifestedSouls", manifested);
      if (this.possessedHostUuid != null) tag.putUUID("PaleRiderPossessedHost", this.possessedHostUuid);
      if (this.combatTargetUuid != null) tag.putUUID("PaleRiderCombatTarget", this.combatTargetUuid);
      if (this.masterUuid != null) tag.putUUID("PaleRiderMaster", this.masterUuid);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.soulLibrary.load(tag.getCompound("PaleRiderSoulLibrary"));
      this.manifestedSouls.clear();
      ListTag manifested = tag.getList("PaleRiderManifestedSouls", Tag.TAG_COMPOUND);
      for (int index = 0; index < manifested.size(); index++) {
         SoulSnapshot soul = SoulSnapshot.load(manifested.getCompound(index));
         if (soul.kind() != SoulSnapshot.SoulKind.SERVANT) {
            this.manifestedSouls.put(soul.id(), soul);
         }
      }
      this.possessedHostUuid = tag.hasUUID("PaleRiderPossessedHost") ? tag.getUUID("PaleRiderPossessedHost") : null;
      this.combatTargetUuid = tag.hasUUID("PaleRiderCombatTarget") ? tag.getUUID("PaleRiderCombatTarget") : null;
      this.masterUuid = tag.hasUUID("PaleRiderMaster") ? tag.getUUID("PaleRiderMaster") : null;
      this.applyBaseStats(false);
   }

   private void applyBaseStats(boolean refill) {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0);
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36);
      this.getAttribute(Attributes.ARMOR).setBaseValue(12.0);
      if (refill) {
         this.setHealth(400.0F);
         this.setCurrentMp(1000.0);
      } else if (this.getHealth() > 400.0F) {
         this.setHealth(400.0F);
      }
   }
}
