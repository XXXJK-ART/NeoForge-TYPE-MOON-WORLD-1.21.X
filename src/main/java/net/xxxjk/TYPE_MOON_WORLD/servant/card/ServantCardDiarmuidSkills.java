package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.Comparator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ServantCardDiarmuidSkills {
   private static final ResourceLocation STRATEGY_ATTACK_SPEED_ID = id("servant_card_diarmuid_strategy_attack_speed");
   private static final ResourceLocation STRATEGY_MOVE_SPEED_ID = id("servant_card_diarmuid_strategy_move_speed");
   private static final String FOCUS_SPEAR_TAG = "ServantCardDiarmuidFocusSpear";
   private static final String FOCUS_UNTIL_TAG = "ServantCardDiarmuidFocusUntil";
   private static final String STRATEGY_UNTIL_TAG = "ServantCardDiarmuidStrategyUntil";
   private static final int ACTION_MODE_DUAL_WIELD = 1;
   private static final int RED = 1;
   private static final int YELLOW = 2;

   private ServantCardDiarmuidSkills() {
   }

   public static boolean isActiveCard(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "diarmuid_ua_duibhne".equals(vars.servant_card_id);
   }

   public static void initialize(ServerPlayer player) {
      DiarmuidCombatHelper.ensureDurability(player);
      equipSpears(player, DiarmuidSpearItem.SpearType.GAE_DEARG);
      clearRuntimeTags(player);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_action_mode = 0;
      vars.syncPlayerVariables(player);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"diarmuid_ua_duibhne".equals(vars.servant_card_id)) {
         clear(player);
         return;
      }
      DiarmuidCombatHelper.ensureDurability(player);
      long now = player.level().getGameTime();
      CompoundTag data = player.getPersistentData();
      if (data.getLong(FOCUS_UNTIL_TAG) > 0L && now >= data.getLong(FOCUS_UNTIL_TAG)) {
         data.remove(FOCUS_UNTIL_TAG);
         data.remove(FOCUS_SPEAR_TAG);
      }
      if (data.getLong(STRATEGY_UNTIL_TAG) > 0L && now >= data.getLong(STRATEGY_UNTIL_TAG)) {
         clearStrategy(player);
      }
      if (player.tickCount % 12 == 0 && data.getLong(STRATEGY_UNTIL_TAG) > now && player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() * 0.62, player.getZ(), 2, 0.28, 0.36, 0.28, 0.01);
      }
   }

   public static void clear(ServerPlayer player) {
      DiarmuidCombatHelper.clearCursesFromOwner(player);
      clearRuntimeTags(player);
      clearStrategy(player);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if ("diarmuid_ua_duibhne".equals(vars.servant_card_id)) {
         vars.servant_card_action_mode = 0;
         vars.syncPlayerVariables(player);
      }
      player.getPersistentData().remove(DiarmuidCombatHelper.RED_SPEAR_DURABILITY_TAG);
      player.getPersistentData().remove(DiarmuidCombatHelper.YELLOW_SPEAR_DURABILITY_TAG);
   }

   public static boolean isDualWieldActive(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "diarmuid_ua_duibhne".equals(vars.servant_card_id)
         && vars.servant_card_action_mode == ACTION_MODE_DUAL_WIELD;
   }

   public static void performDualWieldToggle(ServerPlayer player) {
      ensureTwinSpears(player);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_action_mode = vars.servant_card_action_mode == ACTION_MODE_DUAL_WIELD ? 0 : ACTION_MODE_DUAL_WIELD;
      vars.syncPlayerVariables(player);
      player.displayClientMessage(net.minecraft.network.chat.Component.translatable(vars.servant_card_action_mode == ACTION_MODE_DUAL_WIELD
         ? "message.typemoonworld.diarmuid.dual_wield.on"
         : "message.typemoonworld.diarmuid.dual_wield.off"), true);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.56, player.getZ(), 14, 0.34, 0.42, 0.34, 0.02);
         level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.5F,
            vars.servant_card_action_mode == ACTION_MODE_DUAL_WIELD ? 1.65F : 0.95F);
      }
   }

   public static void performTwinSpearCombo(ServerPlayer player) {
      ensureTwinSpears(player);
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 0.45, 0.05, dir.z * 0.45));
      player.hurtMarked = true;
      DiarmuidSpearItem.SpearType first = consumeFocus(player, DiarmuidSpearItem.SpearType.GAE_DEARG);
      DiarmuidSpearItem.SpearType second = first == DiarmuidSpearItem.SpearType.GAE_DEARG ? DiarmuidSpearItem.SpearType.GAE_BUIDHE : DiarmuidSpearItem.SpearType.GAE_DEARG;
      strikeForward(player, first, 4.8, 20.0F, 0.38, 2);
      TYPE_MOON_WORLD.queueServerWork(5, () -> {
         if (player.isAlive()) strikeForward(player, second, 4.4, 17.0F, 0.42, 2);
      });
      swingAndVfx(player, dir, true, InteractionHand.MAIN_HAND);
   }

   public static void performRedRoseFocus(ServerPlayer player) {
      ensureTwinSpears(player);
      equipSpears(player, DiarmuidSpearItem.SpearType.GAE_DEARG);
      setFocus(player, RED);
      focusVfx(player, true);
   }

   public static void performYellowRoseFocus(ServerPlayer player) {
      ensureTwinSpears(player);
      equipSpears(player, DiarmuidSpearItem.SpearType.GAE_BUIDHE);
      setFocus(player, YELLOW);
      focusVfx(player, false);
   }

   public static void performKnightStrategy(ServerPlayer player) {
      long until = player.level().getGameTime() + 15L * 20L;
      player.getPersistentData().putLong(STRATEGY_UNTIL_TAG, until);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_SPEED), STRATEGY_ATTACK_SPEED_ID, 0.25);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), STRATEGY_MOVE_SPEED_ID, 0.18);
      player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 15 * 20, 0, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 15 * 20, 0, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() * 0.58, player.getZ(), 22, 0.46, 0.52, 0.46, 0.03);
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.62, player.getZ(), 16, 0.34, 0.42, 0.34, 0.02);
         level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.55F, 1.55F);
      }
   }

   public static void performManaBurstJump(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.65, 0.72, dir.z * 1.65));
      player.fallDistance = 0.0F;
      player.hurtMarked = true;
      player.addEffect(new MobEffectInstance(MobEffects.JUMP, 10 * 20, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10 * 20, 1, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.08, player.getZ(), 24, 0.5, 0.08, 0.5, 0.07);
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.26, player.getZ(), 12, 0.3, 0.16, 0.3, 0.035);
         level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.65F, 1.25F);
      }
   }

   public static void performFlowerStep(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findAutomaticLookTarget(player, 9.0, 1.8);
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      if (target != null) {
         Vec3 fromTarget = player.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
         if (fromTarget.lengthSqr() < 1.0E-4) fromTarget = dir.scale(-1.0);
         Vec3 side = new Vec3(-fromTarget.z, 0.0, fromTarget.x).normalize().scale(player.getRandom().nextBoolean() ? 2.2 : -2.2);
         Vec3 destination = target.position().add(fromTarget.normalize().scale(2.4)).add(side).add(0.0, 0.1, 0.0);
         ServantCardSkillUtils.trySafeHorizontalTeleport(player, destination);
         dir = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
      } else {
         Vec3 side = new Vec3(-dir.z, 0.0, dir.x).scale(player.getRandom().nextBoolean() ? 3.6 : -3.6);
         ServantCardSkillUtils.trySafeHorizontalTeleport(player, player.position().add(side).add(0.0, 0.1, 0.0));
      }
      strikeForward(player, consumeFocus(player, DiarmuidSpearItem.SpearType.GAE_DEARG), 4.2, 18.0F, 0.30, 3);
      swingAndVfx(player, dir, false, InteractionHand.MAIN_HAND);
   }

   public static void performDisengage(ServerPlayer player) {
      ServantCardSkillUtils.clearHarmfulEffects(player);
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player).scale(-1.0);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.55, 0.18, dir.z * 1.55));
      player.hurtMarked = true;
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10 * 20, 1, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.08, player.getZ(), 18, 0.35, 0.08, 0.35, 0.055);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.55F, 0.8F);
      }
   }

   public static void performOffhandSpearThrust(ServerPlayer player) {
      if (!isActiveCard(player)) return;
      ensureTwinSpears(player);
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 0.35, 0.03, dir.z * 0.35));
      player.hurtMarked = true;
      strikeForward(player, spearTypeInHand(player, InteractionHand.OFF_HAND, DiarmuidSpearItem.SpearType.GAE_BUIDHE), 4.6, 17.0F, 0.55, 1);
      swingAndVfx(player, dir, false, InteractionHand.OFF_HAND);
      ServantCardVoiceHelper.tryPlayAttack(player);
   }

   public static void performMainhandSpearThrust(ServerPlayer player) {
      if (!isActiveCard(player)) return;
      ensureTwinSpears(player);
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 0.40, 0.03, dir.z * 0.40));
      player.hurtMarked = true;
      strikeForward(player, spearTypeInHand(player, InteractionHand.MAIN_HAND, DiarmuidSpearItem.SpearType.GAE_DEARG), 4.8, 18.0F, 0.52, 1);
      swingAndVfx(player, dir, false, InteractionHand.MAIN_HAND);
      ServantCardVoiceHelper.tryPlayAttack(player);
   }

   public static void applySpearItemHit(ServerPlayer player, LivingEntity target, DiarmuidSpearItem.SpearType spearType) {
      if (!isActiveCard(player) || target == null || target == player) return;
      if (spearType == DiarmuidSpearItem.SpearType.GAE_DEARG) {
         DiarmuidCombatHelper.applyRedRoseHit(player, target);
      } else {
         DiarmuidCombatHelper.applyYellowRoseHit(player, target);
      }
      if (isStrategyActive(player)) {
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().playerAttack(player), target.isBlocking() ? 8.0F : 4.0F);
         target.invulnerableTime = 0;
      }
   }

   private static void strikeForward(ServerPlayer player, DiarmuidSpearItem.SpearType spearType, double range, float damage, double minDot, int maxHits) {
      if (!(player.level() instanceof ServerLevel level)) return;
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.52, 0.0);
      Vec3 forward = PlayerNoblePhantasmHelper.horizontalLook(player);
      AABB box = player.getBoundingBox().inflate(range, 2.5, range);
      int[] hits = {0};
      level.getEntitiesOfClass(LivingEntity.class, box, target -> validTarget(player, target)).stream()
         .sorted(Comparator.comparingDouble(player::distanceToSqr))
         .forEach(target -> {
            if (hits[0] >= maxHits) return;
            Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(origin);
            if (to.lengthSqr() > range * range) return;
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            if (flat.lengthSqr() > 1.0E-4 && flat.normalize().dot(forward) < minDot) return;
            float finalDamage = damage + (isStrategyActive(player) && target.isBlocking() ? 8.0F : 0.0F);
            target.invulnerableTime = 0;
            boolean hurt = target.hurt(player.damageSources().playerAttack(player), finalDamage);
            target.invulnerableTime = 0;
            if (hurt || !target.isAlive() || target.hurtTime > 0) {
               applySpearItemHit(player, target, spearType);
               target.push(forward.x * 0.45, 0.08, forward.z * 0.45);
               target.hurtMarked = true;
               hits[0]++;
            }
         });
   }

   private static boolean validTarget(ServerPlayer player, LivingEntity target) {
      return target != null && target != player && target.isAlive()
         && !EntityUtils.isImmunePlayerTarget(target)
         && !ServantMasterTargeting.isContractMaster(player, target)
         && !player.isAlliedTo(target) && !target.isAlliedTo(player);
   }

   private static void ensureTwinSpears(ServerPlayer player) {
      DiarmuidCombatHelper.ensureDurability(player);
      if (!(player.getMainHandItem().getItem() instanceof DiarmuidSpearItem)
         || !(player.getOffhandItem().getItem() instanceof DiarmuidSpearItem)) {
         equipSpears(player, DiarmuidSpearItem.SpearType.GAE_DEARG);
      }
   }

   private static void equipSpears(ServerPlayer player, DiarmuidSpearItem.SpearType mainType) {
      ItemStack main = new ItemStack(mainType == DiarmuidSpearItem.SpearType.GAE_DEARG ? ModItems.GAE_DEARG.get() : ModItems.GAE_BUIDHE.get());
      ItemStack off = new ItemStack(mainType == DiarmuidSpearItem.SpearType.GAE_DEARG ? ModItems.GAE_BUIDHE.get() : ModItems.GAE_DEARG.get());
      ServantCardTransformManager.markGeneratedItem(main, true, false);
      ServantCardTransformManager.markGeneratedItem(off, true, false);
      player.setItemInHand(InteractionHand.MAIN_HAND, main);
      player.setItemInHand(InteractionHand.OFF_HAND, off);
   }

   private static DiarmuidSpearItem.SpearType spearTypeInHand(ServerPlayer player, InteractionHand hand, DiarmuidSpearItem.SpearType fallback) {
      ItemStack stack = player.getItemInHand(hand);
      return stack.getItem() instanceof DiarmuidSpearItem spear ? spear.spearType() : fallback;
   }

   private static void setFocus(ServerPlayer player, int spear) {
      CompoundTag data = player.getPersistentData();
      data.putInt(FOCUS_SPEAR_TAG, spear);
      data.putLong(FOCUS_UNTIL_TAG, player.level().getGameTime() + 12L * 20L);
   }

   private static DiarmuidSpearItem.SpearType consumeFocus(ServerPlayer player, DiarmuidSpearItem.SpearType fallback) {
      CompoundTag data = player.getPersistentData();
      if (data.getLong(FOCUS_UNTIL_TAG) <= player.level().getGameTime()) {
         data.remove(FOCUS_UNTIL_TAG);
         data.remove(FOCUS_SPEAR_TAG);
         return fallback;
      }
      int spear = data.getInt(FOCUS_SPEAR_TAG);
      data.remove(FOCUS_UNTIL_TAG);
      data.remove(FOCUS_SPEAR_TAG);
      return spear == YELLOW ? DiarmuidSpearItem.SpearType.GAE_BUIDHE : spear == RED ? DiarmuidSpearItem.SpearType.GAE_DEARG : fallback;
   }

   private static boolean isStrategyActive(ServerPlayer player) {
      return player.getPersistentData().getLong(STRATEGY_UNTIL_TAG) > player.level().getGameTime();
   }

   private static void clearStrategy(ServerPlayer player) {
      player.getPersistentData().remove(STRATEGY_UNTIL_TAG);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_SPEED), STRATEGY_ATTACK_SPEED_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), STRATEGY_MOVE_SPEED_ID);
   }

   private static void clearRuntimeTags(ServerPlayer player) {
      player.getPersistentData().remove(FOCUS_SPEAR_TAG);
      player.getPersistentData().remove(FOCUS_UNTIL_TAG);
   }

   private static void focusVfx(ServerPlayer player, boolean red) {
      if (!(player.level() instanceof ServerLevel level)) return;
      level.sendParticles(red ? ParticleTypes.CRIT : ParticleTypes.WITCH, player.getX(), player.getY() + player.getBbHeight() * 0.58, player.getZ(),
         red ? 16 : 12, 0.32, 0.36, 0.32, 0.02);
      level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.62, player.getZ(), 10, 0.28, 0.32, 0.28, 0.02);
      level.playSound(null, player.blockPosition(), red ? SoundEvents.TRIDENT_RETURN : SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6F, red ? 1.35F : 0.95F);
   }

   private static void swingAndVfx(ServerPlayer player, Vec3 dir, boolean heavy, InteractionHand hand) {
      player.swing(hand, true);
      if (!(player.level() instanceof ServerLevel level)) return;
      Vec3 fx = player.position().add(dir.normalize().scale(2.0)).add(0.0, player.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, heavy ? 4 : 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, fx.x, fx.y - 0.25, fx.z, heavy ? 12 : 7, 0.42, 0.1, 0.42, 0.03);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, heavy ? 0.9F : 0.7F, heavy ? 1.18F : 1.45F);
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}
