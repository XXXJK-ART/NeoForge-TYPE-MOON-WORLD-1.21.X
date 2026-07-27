package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashStellaControllerEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class ServantCardArashSkills {
   public static final double CHANT_MOVE_RADIUS = 0.0;
   public static final int STELLA_LOCKED_HOTBAR_SLOT = 0;
   public static final float CHANT_LOOK_TOLERANCE = 3.0F;
   public static final float BASE_DODGE_CHANCE = 0.10F;

   private static final ResourceLocation STOUT_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "servant_card_arash_stout_health");
   private static final String CHANTING = "ServantCardArashStellaChanting";
   private static final String CONTROLLER = "ServantCardArashStellaController";
   private static final String LOCK_X = "ServantCardArashStellaLockX";
   private static final String LOCK_Z = "ServantCardArashStellaLockZ";
   private static final String LOCK_YAW = "ServantCardArashStellaLockYaw";
   private static final String LOCK_PITCH = "ServantCardArashStellaLockPitch";
   private static final String LOCK_SLOT = "ServantCardArashStellaLockSlot";
   private static final String REFUND_READY = "ServantCardArashStellaRefundReady";
   private static final String REFUND_MANA = "ServantCardArashStellaRefundMana";
   private static final String REFUND_MASTER = "ServantCardArashStellaRefundMaster";
   private static final String REFUND_MASTER_MANA = "ServantCardArashStellaRefundMasterMana";
   private static final String REFUND_COOLDOWN = "ServantCardArashStellaRefundCooldown";
   private static final String REFUND_COOLDOWN_END = "ServantCardArashStellaRefundCooldownEnd";
   private static final String SACRIFICE_ACTIVE = "ServantCardArashStellaSacrifice";
   private static final String SACRIFICE_TICKS = "ServantCardArashStellaSacrificeTicks";
   private static final String SACRIFICE_HEALTH = "ServantCardArashStellaSacrificeHealth";

   private ServantCardArashSkills() { }

   public static boolean isArash(ServerPlayer player) {
      if (player == null) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "arash".equals(vars.servant_card_id);
   }

   public static void initialize(ServerPlayer player) {
      clearRuntimeTags(player);
      applyStoutHealth(player, true);
   }

   public static void clear(ServerPlayer player) {
      removeStoutHealth(player);
      clearRuntimeTags(player);
      clearRefundSnapshot(player);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"arash".equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      applyStoutHealth(player, false);
      if (isPlayerChanting(player)) maintainChantLock(player);
   }

   public static boolean hasRequiredBow(ServerPlayer player) {
      return player != null && player.getMainHandItem().is(ModItems.ARASH_BOW.get());
   }

   public static boolean performArrowRain(ServerPlayer player) {
      if (!hasRequiredBow(player) || !(player.level() instanceof ServerLevel level)) return false;
      Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.65));
      Vec3 target = start.add(player.getLookAngle().normalize().scale(70.0));
      for (int i = 0; i < ArashCombatRules.RAIN_ARROW_COUNT; i++) {
         Vec3 spread = new Vec3(player.getRandom().nextGaussian() * 2.2,
            player.getRandom().nextGaussian() * 1.1, player.getRandom().nextGaussian() * 2.2);
         Vec3 delta = target.add(spread).subtract(start);
         double time = Math.max(12.0, delta.horizontalDistance() / 2.2);
         Vec3 motion = new Vec3(delta.x / time, delta.y / time + 0.025 * time, delta.z / time);
         ArashParticleArrowEntity arrow = new ArashParticleArrowEntity(level, player,
            ArashParticleArrowEntity.RAIN, ArashCombatRules.RAIN_ARROW_DAMAGE);
         arrow.setPos(start.x, start.y, start.z);
         arrow.setDeltaMovement(motion);
         level.addFreshEntity(arrow);
      }
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 32, 0.4, 0.35, 0.4, 0.12);
      return true;
   }

   public static boolean performSmallEnergyArrow(ServerPlayer player) {
      return fireDirect(player, ArashParticleArrowEntity.SMALL_ENERGY,
         (float)ArashCombatRules.SMALL_ENERGY_DAMAGE, 3.0);
   }

   public static boolean performLargeEnergyArrow(ServerPlayer player) {
      return fireDirect(player, ArashParticleArrowEntity.LARGE_ENERGY,
         (float)ArashCombatRules.LARGE_ENERGY_DAMAGE, 2.7);
   }

   public static boolean performBowChargedArrowNoCooldown(ServerPlayer player, boolean heavy) {
      if (!isArash(player) || isPlayerChanting(player) || !hasRequiredBow(player)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double cost = heavy ? ArashCombatRules.LARGE_ENERGY_MANA : ArashCombatRules.SMALL_ENERGY_MANA;
      ServantCardManaService.ManaSnapshot snapshot = ServantCardManaService.snapshot(player, vars);
      if (!ServantCardManaService.consume(player, vars, cost)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      boolean fired = heavy ? performLargeEnergyArrow(player) : performSmallEnergyArrow(player);
      if (!fired) {
         ServantCardManaService.restore(player, vars, snapshot);
         return false;
      }
      ServantCardVoiceHelper.tryPlaySkill(player, heavy ? "arash_energy_large" : "arash_energy_small");
      return true;
   }

   private static boolean fireDirect(ServerPlayer player, int variant, float damage, double speed) {
      if (!hasRequiredBow(player) || !(player.level() instanceof ServerLevel level)) return false;
      Vec3 direction = player.getLookAngle().normalize();
      Vec3 start = player.getEyePosition().add(direction.scale(0.8));
      ArashParticleArrowEntity arrow = new ArashParticleArrowEntity(level, player, variant, damage);
      arrow.setPos(start.x, start.y - 0.1, start.z);
      arrow.setDeltaMovement(direction.scale(speed));
      level.addFreshEntity(arrow);
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z,
         variant == ArashParticleArrowEntity.LARGE_ENERGY ? 44 : 24, 0.3, 0.3, 0.3, 0.1);
      return true;
   }

   public static boolean performStellaAction(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars,
                                              ServantCardSkillAction action) {
      if (!hasRequiredBow(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.arash_bow_required"), true);
         return false;
      }
      captureRefundSnapshot(player, vars);
      if (!ServantCardManaService.consumeNoblePhantasm(player, vars, action.mpCost())) {
         clearRefundSnapshot(player);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      if (!ArashStellaControllerEntity.beginPlayerStella(player)) {
         restoreRefundSnapshot(player);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.arash_stella_blocked"), true);
         return false;
      }
      ServantCardTransformManager.setNoblePhantasmCooldown(player, vars, Math.max(3600, action.cooldownTicks()));
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.skill_activated",
         Component.translatable("skill.typemoonworld.servant_card.arash_stella")), true);
      return true;
   }

   public static void beginPlayerChant(ServerPlayer player, UUID controllerId) {
      CompoundTag data = player.getPersistentData();
      data.putBoolean(CHANTING, true);
      data.putUUID(CONTROLLER, controllerId);
      data.putDouble(LOCK_X, player.getX());
      data.putDouble(LOCK_Z, player.getZ());
      data.putFloat(LOCK_YAW, player.getYRot());
      data.putFloat(LOCK_PITCH, player.getXRot());
      data.putInt(LOCK_SLOT, STELLA_LOCKED_HOTBAR_SLOT);
      ensureBowInLockedSlot(player, STELLA_LOCKED_HOTBAR_SLOT);
      player.getInventory().selected = STELLA_LOCKED_HOTBAR_SLOT;
      player.stopUsingItem();
   }

   public static boolean isPlayerChanting(ServerPlayer player) {
      return player != null && player.getPersistentData().getBoolean(CHANTING);
   }

   public static void finishPlayerChant(ServerPlayer player) {
      clearChantTags(player);
      clearRefundSnapshot(player);
   }

   public static void beginPlayerStellaSacrifice(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.putBoolean(SACRIFICE_ACTIVE, true);
      data.putInt(SACRIFICE_TICKS, 0);
      data.putFloat(SACRIFICE_HEALTH, Math.max(1.0F, player.getHealth()));
   }

   public static boolean isPlayerStellaSacrificing(ServerPlayer player) {
      return player != null && player.getPersistentData().getBoolean(SACRIFICE_ACTIVE);
   }

   private static void tickPlayerStellaSacrifice(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(SACRIFICE_ACTIVE)) return;
      if (!player.isAlive()) {
         clearPlayerStellaSacrifice(player);
         return;
      }
      int elapsed = data.getInt(SACRIFICE_TICKS) + 1;
      data.putInt(SACRIFICE_TICKS, elapsed);
      float initialHealth = Math.max(1.0F, data.getFloat(SACRIFICE_HEALTH));
      if (elapsed >= ArashCombatRules.STELLA_SACRIFICE_TICKS) {
         clearPlayerStellaSacrifice(player);
         player.kill();
         return;
      }
      player.setHealth(Math.min(player.getHealth(), ArashCombatRules.stellaRemainingHealth(initialHealth, elapsed)));
      if (player.level() instanceof ServerLevel level && elapsed % 3 == 0) {
         level.sendParticles(elapsed < 120 ? ParticleTypes.END_ROD : ParticleTypes.FIREWORK,
            player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(),
            8 + elapsed / 12, 0.42, player.getBbHeight() * 0.5, 0.42, 0.035);
      }
   }

   private static void clearPlayerStellaSacrifice(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(SACRIFICE_ACTIVE);
      data.remove(SACRIFICE_TICKS);
      data.remove(SACRIFICE_HEALTH);
   }

   public static void abortPlayerChantTechnical(ServerPlayer player) {
      clearChantTags(player);
      restoreRefundSnapshot(player);
   }

   public static void abortPlayerChantNoRefund(ServerPlayer player) {
      clearChantTags(player);
      clearRefundSnapshot(player);
   }

   private static void maintainChantLock(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!isArash(player)) {
         abortPlayerChantNoRefund(player);
         return;
      }
      if (player.containerMenu != player.inventoryMenu) player.closeContainer();
      int slot = STELLA_LOCKED_HOTBAR_SLOT;
      player.getInventory().selected = slot;
      ensureBowInLockedSlot(player, slot);
      player.stopUsingItem();

      double anchorX = data.getDouble(LOCK_X), anchorZ = data.getDouble(LOCK_Z);
      player.setPos(anchorX, player.getY(), anchorZ);
      Vec3 motion = player.getDeltaMovement();
      player.setDeltaMovement(0.0, motion.y, 0.0);

      float lockedYaw = data.getFloat(LOCK_YAW), lockedPitch = data.getFloat(LOCK_PITCH);
      float yaw = lockedYaw + Mth.clamp(Mth.wrapDegrees(player.getYRot() - lockedYaw), -CHANT_LOOK_TOLERANCE, CHANT_LOOK_TOLERANCE);
      float pitch = lockedPitch + Mth.clamp(player.getXRot() - lockedPitch, -CHANT_LOOK_TOLERANCE, CHANT_LOOK_TOLERANCE);
      player.setYRot(yaw);
      player.setYHeadRot(yaw);
      player.setYBodyRot(yaw);
      player.setXRot(pitch);
   }

   private static void ensureBowInLockedSlot(ServerPlayer player, int slot) {
      if (player.getInventory().getItem(slot).is(ModItems.ARASH_BOW.get())) return;
      int found = -1;
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         if (player.getInventory().getItem(i).is(ModItems.ARASH_BOW.get())) { found = i; break; }
      }
      if (found >= 0) {
         ItemStack displaced = player.getInventory().getItem(slot);
         player.getInventory().setItem(slot, player.getInventory().getItem(found));
         player.getInventory().setItem(found, displaced);
      } else {
         player.getInventory().setItem(slot, ServantCardTransformManager.markGeneratedItem(
            new ItemStack(ModItems.ARASH_BOW.get()), true, false));
      }
      player.getInventory().setChanged();
   }

   private static void applyStoutHealth(ServerPlayer player, boolean healToFull) {
      var health = player.getAttribute(Attributes.MAX_HEALTH);
      if (health == null) return;
      AttributeModifier existing = health.getModifier(STOUT_HEALTH_ID);
      if (existing == null || existing.amount() != 100.0) {
         if (existing != null) health.removeModifier(STOUT_HEALTH_ID);
         health.addPermanentModifier(new AttributeModifier(STOUT_HEALTH_ID, 100.0, AttributeModifier.Operation.ADD_VALUE));
      }
      if (healToFull) player.setHealth(player.getMaxHealth());
   }

   private static void removeStoutHealth(ServerPlayer player) {
      var health = player.getAttribute(Attributes.MAX_HEALTH);
      if (health != null) health.removeModifier(STOUT_HEALTH_ID);
   }

   private static void captureRefundSnapshot(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      data.putBoolean(REFUND_READY, true);
      data.putDouble(REFUND_MANA, vars.servant_card_mana);
      data.putInt(REFUND_COOLDOWN, vars.servant_card_np_cooldown);
      data.putLong(REFUND_COOLDOWN_END, vars.servant_card_np_cooldown_end);
      ServerPlayer master = ServantCardManaService.getMaster(player, vars);
      if (master != null) {
         data.putUUID(REFUND_MASTER, master.getUUID());
         data.putDouble(REFUND_MASTER_MANA,
            master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana);
      }
   }

   private static void restoreRefundSnapshot(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(REFUND_READY)) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_mana = data.getDouble(REFUND_MANA);
      vars.servant_card_np_cooldown = data.getInt(REFUND_COOLDOWN);
      vars.servant_card_np_cooldown_end = data.getLong(REFUND_COOLDOWN_END);
      vars.syncPlayerVariables(player);
      if (data.hasUUID(REFUND_MASTER) && player.getServer() != null) {
         ServerPlayer master = player.getServer().getPlayerList().getPlayer(data.getUUID(REFUND_MASTER));
         if (master != null) {
            TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            masterVars.player_mana = data.getDouble(REFUND_MASTER_MANA);
            masterVars.syncMana(master);
         }
      }
      clearRefundSnapshot(player);
   }

   private static void clearRuntimeTags(ServerPlayer player) {
      clearChantTags(player);
   }

   private static void clearChantTags(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(CHANTING);
      data.remove(CONTROLLER);
      data.remove(LOCK_X);
      data.remove(LOCK_Z);
      data.remove(LOCK_YAW);
      data.remove(LOCK_PITCH);
      data.remove(LOCK_SLOT);
   }

   private static void clearRefundSnapshot(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(REFUND_READY);
      data.remove(REFUND_MANA);
      data.remove(REFUND_MASTER);
      data.remove(REFUND_MASTER_MANA);
      data.remove(REFUND_COOLDOWN);
      data.remove(REFUND_COOLDOWN_END);
   }

   private static boolean isDiseaseEffect(Holder<MobEffect> effect) {
      return effect == MobEffects.POISON || effect == MobEffects.WITHER || effect == MobEffects.WEAKNESS
         || effect == MobEffects.MOVEMENT_SLOWDOWN || effect == MobEffects.DIG_SLOWDOWN
         || effect == MobEffects.HUNGER || effect == MobEffects.CONFUSION
         || effect == ModMobEffects.PALE_RIDER_INFECTION || effect == ModMobEffects.PALE_RIDER_FEAR
         || effect == ModMobEffects.FANATIC_TOXIN || effect == ModMobEffects.FANATIC_WOUNDED
         || effect == ModMobEffects.FANATIC_CIRCUIT_DISRUPTION;
   }

   private static boolean isDiseaseDamage(DamageSource source) {
      return source.typeHolder().unwrapKey().map(key -> {
         String path = key.location().getPath();
         return path.contains("infection") || path.contains("toxin") || path.contains("poison")
            || path.contains("wither") || path.contains("wound");
      }).orElse(false);
   }

   private static boolean blockedInteraction(ServerPlayer player) {
      return isPlayerChanting(player);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || !isArash(player)) return;
      if (isDiseaseDamage(event.getSource())) {
         event.setAmount(0.0F);
         event.setCanceled(true);
         return;
      }
      if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
      if (player.getRandom().nextFloat() < BASE_DODGE_CHANCE) {
         event.setAmount(0.0F);
         event.setCanceled(true);
         if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
         }
         return;
      }
      event.setAmount(event.getAmount() * (float)ArashCombatRules.STOUT_DAMAGE_MULTIPLIER);
   }

   @SubscribeEvent
   public static void onEffectApplicable(MobEffectEvent.Applicable event) {
      if (event.getEntity() instanceof ServerPlayer player && isArash(player)
         && isDiseaseEffect(event.getEffectInstance().getEffect())) {
         event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onAttack(AttackEntityEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && blockedInteraction(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
      if (event.getEntity() instanceof ServerPlayer player && blockedInteraction(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player && blockedInteraction(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
      if (event.getEntity() instanceof ServerPlayer player && blockedInteraction(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player && blockedInteraction(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onUseItem(LivingEntityUseItemEvent.Start event) {
      if (event.getEntity() instanceof ServerPlayer player && blockedInteraction(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onToss(ItemTossEvent event) {
      if (event.getPlayer() instanceof ServerPlayer player && blockedInteraction(player)) event.setCanceled(true);
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (isPlayerChanting(player)) abortPlayerChantNoRefund(player);
         clearPlayerStellaSacrifice(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (event.getEntity() instanceof ServerPlayer player) tickPlayerStellaSacrifice(player);
   }

   @SubscribeEvent
   public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && isPlayerChanting(player)) abortPlayerChantNoRefund(player);
   }

   @SubscribeEvent
   public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && isPlayerChanting(player)) abortPlayerChantNoRefund(player);
   }
}
