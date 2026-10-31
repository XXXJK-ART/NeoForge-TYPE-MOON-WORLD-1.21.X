package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaStateService;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaThrownWeaponEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.ProjectionDataHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardCuChulainnSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

public final class PlayerNoblePhantasmHelper {
   public static final String ONE_SHOT_TSUBAME_TAG = "TypeMoonOneShotTsubame";
   public static final String ONE_SHOT_NINE_LIVES_TAG = "TypeMoonOneShotNineLives";
   private static final String SERVANT_CARD_COPIED_NP_TAG = "TypeMoonServantCardCopiedNoblePhantasm";
   private static final String SERVANT_CARD_COPIED_NP_TOKEN_TAG = "TypeMoonServantCardCopiedNoblePhantasmToken";
   private static final String OVEREDGE_USE_COUNT_TAG = "TypeMoonOveredgeUseCount";
   private static final String GAE_DEATH_FLIGHT_TAG = "TypeMoonGaeBulgDeathFlight";
   private static final String GAE_DEATH_FLIGHT_PAID_TAG = "TypeMoonGaeBulgDeathFlightPaid";
   private static final String GAE_LOCKED_MELEE_CHARGE_TAG = "TypeMoonGaeBulgLockedMeleeCharge";
   private static final String GAE_LOCKED_MELEE_TARGET_TAG = "TypeMoonGaeBulgLockedMeleeTarget";
   private static final String GAE_LOCKED_MELEE_TARGET_UNTIL_TAG = "TypeMoonGaeBulgLockedMeleeTargetUntil";
   private static final String GAE_LOCK_CREATED_THIS_USE_TAG = "TypeMoonGaeBulgLockCreatedThisUse";
   private static final String GAE_LOCKED_MELEE_PURSUIT_TAG = "TypeMoonGaeBulgLockedMeleePursuit";
   private static final String GAE_LOCKED_MELEE_PURSUIT_UNTIL_TAG = "TypeMoonGaeBulgLockedMeleePursuitUntil";
   private static final String EXCALIBUR_CHARGE_TAG = "TypeMoonExcaliburCharge";
   private static final String EXCALIBUR_SHORT_CHARGE_TAG = "TypeMoonExcaliburShortCharge";
   private static final String EXCALIBUR_SHORT_LAST_VFX_TAG = "TypeMoonExcaliburShortLastChargeVfx";
   private static final String EXCALIBUR_LAST_CHARGE_VFX_TAG = "TypeMoonExcaliburLastChargeVfx";
   private static final String EXCALIBUR_MIN_CHARGE_PAID_TAG = "TypeMoonExcaliburMinChargePaid";
   private static final String GOLDEN_EXCALIBUR_CHARGE_TAG = "TypeMoonGoldenExcaliburCharge";
   private static final String GOLDEN_EXCALIBUR_LAST_CHARGE_VFX_TAG = "TypeMoonGoldenExcaliburLastChargeVfx";
   private static final String GOLDEN_EXCALIBUR_MIN_CHARGE_PAID_TAG = "TypeMoonGoldenExcaliburMinChargePaid";
   private static final String ARTORIA_WIND_REVEAL_UNTIL_TAG = "ServantCardArtoriaWindRevealUntil";
   private static final String ARTORIA_EXCALIBUR_WIND_LOCK_UNTIL_TAG = "ServantCardArtoriaExcaliburWindLockUntil";
   private static final String GALLATIN_CHARGE_TAG = "TypeMoonGallatinCharge";
   private static final String GALLATIN_LAST_CHARGE_VFX_TAG = "TypeMoonGallatinLastChargeVfx";
   private static final String GALLATIN_MIN_CHARGE_PAID_TAG = "TypeMoonGallatinMinChargePaid";
   private static final String SERVANT_CARD_NP_CHARGE_VOICE_TAG = "TypeMoonServantCardNpChargeVoice";
   private static final int ONE_SHOT_PROJECTION_NP_COOLDOWN = 1200;
   private static final int SERVANT_CARD_CHARGE_SHORT_VOICE_TICKS = 60;
   private static final double SERVANT_CARD_CHARGE_VOICE_STOP_RADIUS = 96.0;
   private static final DustParticleOptions GAE_BULG_CHARGE_PARTICLE = new DustParticleOptions(new Vector3f(0.85F, 0.0F, 0.03F), 1.35F);
   private static final int GAE_BULG_MELEE_MIN_CHARGE_TICKS = 20;
   private static final int GAE_DEATH_FLIGHT_CHARGE_TICKS = 40;
   private static final int GAE_BULG_LOCK_TICKS = 1200;
   private static final int GAE_BULG_MELEE_PURSUIT_TICKS = CuChulainnCombatRules.MELEE_GAE_BOLG_PURSUIT_TICKS;
   private static final double GAE_BULG_MELEE_LOCK_RANGE = 4.0;
   private static final double GAE_BULG_MELEE_HIT_DISTANCE = 4.0;
   private static final double GAE_BULG_MELEE_PURSUIT_START_SPEED = 2.05;
   private static final double GAE_BULG_MELEE_PURSUIT_END_SPEED = 0.35;
   private static final int MIN_CHARGE_NP_RELEASE_TICKS = 30;
   private static final int EXCALIBUR_MAX_CHARGE_TICKS = 100;
   private static final int EXCALIBUR_SHORT_MAX_CHARGE_TICKS = ArtoriaRules.EXCALIBUR_SHORT_CHARGE_TICKS;
   private static final int EXCALIBUR_RELEASE_TICKS = 150;
   private static final int EXCALIBUR_DAMAGE_START_TICK = 0;
   private static final int EXCALIBUR_PLAYER_COOLDOWN = 3600;
   private static final double EXCALIBUR_MANA_COST = 150.0;
   private static final int GOLDEN_EXCALIBUR_MIN_CHARGE_TICKS = 10;
   private static final int GOLDEN_EXCALIBUR_MAX_CHARGE_TICKS = 40;
   private static final int GOLDEN_EXCALIBUR_RELEASE_TICKS = 30;
   private static final int GOLDEN_EXCALIBUR_PLAYER_COOLDOWN = 400;
   private static final double GOLDEN_EXCALIBUR_MANA_PER_TICK = 3.0;
   private static final int GALLATIN_MAX_CHARGE_TICKS = 100;
   private static final int GALLATIN_PLAYER_COOLDOWN = 1200;
   private static final int GAE_BULG_SINGLE_PLAYER_COOLDOWN = 600;
   private static final int GAE_BULG_ARMY_PLAYER_COOLDOWN = 1200;
   private static final double GALLATIN_RANGE = 100.0;
   private static final double GALLATIN_HALF_ANGLE_COS = Math.cos(Math.toRadians(35.0));
   private static final double CHARGE_MANA_PER_TICK = 10.0;
   private static final double GAE_BULG_ARMY_BASE_MANA = CHARGE_MANA_PER_TICK * 30.0;
   private static final float GAE_BULG_ARMY_BASE_DAMAGE = 500.0F;
   private static final float GAE_BULG_ARMY_MAX_DAMAGE = 1000.0F;

   private PlayerNoblePhantasmHelper() {
   }

   public static boolean consumeStrict(ServerPlayer player, double amount) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) {
         var before = ServantCardManaService.snapshot(player, vars);
         if (ServantCardManaService.consume(player, vars, amount)) {
            net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.notePlayerPayment(player, player, true, before.servantMana() - vars.servantCardState().mana());
            if (before.master() != null) net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.notePlayerPayment(player, before.master(), false, before.masterMana() - before.master().getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).magicState().mana());
            return true;
         }
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return false;
      }
      double before = vars.magicState().mana();
      if (ManaHelper.consumeManaStrict(player, amount, false)) {
         net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.notePlayerPayment(player, player, false, before - vars.magicState().mana());
         return true;
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
      return false;
   }

   public static boolean hasOneShotTsubame(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return tag != null && tag.getBoolean(ONE_SHOT_TSUBAME_TAG);
   }

   public static boolean isInfiniteProjectedBizen(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return !stack.isEmpty()
         && stack.getItem() instanceof BizenNagamitsuItem
         && tag != null
         && tag.getBoolean("is_projected")
         && tag.getBoolean("is_infinite_projection");
   }

   public static void armTsubameAfterAnalysis(ServerPlayer player, ItemStack stack) {
      if (stack.isEmpty() || !(stack.getItem() instanceof BizenNagamitsuItem)) {
         return;
      }
      updateCustomData(stack, tag -> tag.putBoolean(ONE_SHOT_TSUBAME_TAG, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2, false, true));
      player.displayClientMessage(Component.translatable("message.typemoonworld.tsubame_ready"), true);
   }

   public static void clearOneShotTsubame(ItemStack stack) {
      updateCustomData(stack, tag -> tag.remove(ONE_SHOT_TSUBAME_TAG));
   }

   public static boolean hasOneShotNineLives(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return tag != null && tag.getBoolean(ONE_SHOT_NINE_LIVES_TAG);
   }

   public static void armNineLives(ItemStack stack) {
      if (!stack.isEmpty()) {
         updateCustomData(stack, tag -> tag.putBoolean(ONE_SHOT_NINE_LIVES_TAG, true));
      }
   }

   public static void clearOneShotNineLives(ItemStack stack) {
      updateCustomData(stack, tag -> tag.remove(ONE_SHOT_NINE_LIVES_TAG));
   }

   public static boolean useOneShotNineLives(ServerPlayer player, ItemStack stack) {
      if (!hasOneShotNineLives(stack)) {
         return false;
      }
      if (!consumeStrict(player, 90.0)) {
         return true;
      }
      clearOneShotNineLives(stack);
      LivingEntity target = findLookTarget(player, 7.0, 1.7);
      performNineLives(player, target);
      if (!player.getAbilities().instabuild) stack.shrink(1);
      player.getCooldowns().addCooldown(stack.getItem(), ONE_SHOT_PROJECTION_NP_COOLDOWN);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.setNoblePhantasmCooldown(
            player, vars, Math.max(vars.servant_card_np_cooldown, ONE_SHOT_PROJECTION_NP_COOLDOWN)
         );
         vars.syncPlayerVariables(player);
      }
      return true;
   }

   public static boolean hasOneShotProjectionNoblePhantasm(ServerPlayer player) {
      return player != null
         && (hasOneShotTsubame(player.getMainHandItem())
            || hasOneShotTsubame(player.getOffhandItem())
            || hasOneShotNineLives(player.getMainHandItem())
            || hasOneShotNineLives(player.getOffhandItem()));
   }

   public static boolean useOneShotProjectionNoblePhantasm(ServerPlayer player) {
      if (player == null) {
         return false;
      }
      ItemStack main = player.getMainHandItem();
      if (hasOneShotTsubame(main)) {
         return useOneShotTsubame(player, main);
      }
      if (hasOneShotNineLives(main)) {
         return useOneShotNineLives(player, main);
      }
      ItemStack off = player.getOffhandItem();
      if (hasOneShotTsubame(off)) {
         return useOneShotTsubame(player, off);
      }
      if (hasOneShotNineLives(off)) {
         return useOneShotNineLives(player, off);
      }
      return false;
   }

   public static boolean useOneShotTsubame(ServerPlayer player, ItemStack stack) {
      if (!hasOneShotTsubame(stack)) {
         return false;
      }
      LivingEntity target = findLookTarget(player, 8.0, 2.0);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return true;
      }
      clearOneShotTsubame(stack);
      performTsubame(player, target);
      if (!player.getAbilities().instabuild) stack.shrink(1);
      player.getCooldowns().addCooldown(stack.getItem(), 600);
      return true;
   }

   public static boolean triggerTsubameOnHit(ServerPlayer player, ItemStack stack, LivingEntity target) {
      if (!hasOneShotTsubame(stack) || target == null || !target.isAlive() || player.distanceToSqr(target) > 64.0) {
         return false;
      }
      clearOneShotTsubame(stack);
      performTsubame(player, target);
      if (!player.getAbilities().instabuild) stack.shrink(1);
      player.getCooldowns().addCooldown(stack.getItem(), 600);
      return true;
   }

   public static boolean hasTsubameGaeshiTarget(ServerPlayer player) {
      return findLookTarget(player, 8.0, 2.0) != null;
   }

   public static boolean useTsubameGaeshi(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 8.0, 2.0);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      performTsubame(player, target);
      return true;
   }

   public static boolean useRuleBreaker(ServerPlayer player) {
      ItemStack dagger = new ItemStack(ModItems.RULE_BREAKER.get());
      markUbwProjection(dagger);
      net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.markGeneratedItem(dagger, true, false);
      player.setItemInHand(InteractionHand.MAIN_HAND, dagger);
      LivingEntity target = findLookTarget(player, 5.0, 1.0);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return true;
      }
      MedeaCombatHelper.applyRuleBreakerHit(target, player);
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().magic(), 8.0F);
      target.invulnerableTime = 0;
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 28, 0.35, 0.45, 0.35, 0.08);
         level.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
      }
      return true;
   }

   public static boolean useDirk(ServerPlayer player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (held.isEmpty() || !held.is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.DIRK_SMALL_KNIFE.get())
         || held.getDamageValue() >= held.getMaxDamage()) return false;
      DirkProjectileEntity projectile = new DirkProjectileEntity(player.level(), player);
      projectile.setDamage(10.0F);
      projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
      Vec3 look = player.getLookAngle();
      projectile.shoot(look.x, look.y, look.z, 1.9F, 0.0F);
      player.level().addFreshEntity(projectile);
      player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.75F, 1.45F);
      if (!player.isCreative()) {
         held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
      }
      return true;
   }

   public static boolean useGaeBulgMelee(ServerPlayer player) {
      return net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnWeaponService.noble(player,false,20);
   }

   public static void startGaeBulgDeathFlight(ServerPlayer player) {
      net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnWeaponService.begin(player);
   }

   public static void tickGaeBulgUse(Level level, LivingEntity living, int useTicks) {
      if(living instanceof ServerPlayer player) net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnWeaponService.charge(player,useTicks);
   }

   public static boolean releaseGaeBulg(ServerPlayer player) {
      return net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnWeaponService.release(player);
   }

   public static void tickGaeBulgMeleePursuit(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(GAE_LOCKED_MELEE_PURSUIT_TAG)) {
         return;
      }
      if (!(player.level() instanceof ServerLevel level) || !player.isAlive()) {
         clearGaeBulgMeleeState(player);
         return;
      }
      long until = data.getLong(GAE_LOCKED_MELEE_PURSUIT_UNTIL_TAG);
      LivingEntity target = resolveGaeBulgLockedTarget(player);
      if (target == null || level.getGameTime() > until) {
         clearGaeBulgMeleeState(player);
         return;
      }
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      Vec3 playerCenter = player.position().add(0.0, player.getBbHeight() * 0.45, 0.0);
      if (player.distanceToSqr(target) <= GAE_BULG_MELEE_HIT_DISTANCE * GAE_BULG_MELEE_HIT_DISTANCE) {
         resolveGaeBulgHit(player, target);
         clearGaeBulgMeleeState(player);
         return;
      }
      Vec3 toTarget = targetCenter.subtract(playerCenter);
      if (toTarget.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 direction = toTarget.normalize();
      long pursuitDuration = Math.max(1L, GAE_BULG_MELEE_PURSUIT_TICKS);
      double elapsed = pursuitDuration - Math.max(0L, until - level.getGameTime());
      double progress = Mth.clamp(elapsed / pursuitDuration, 0.0, 1.0);
      double speed = GAE_BULG_MELEE_PURSUIT_START_SPEED
         + (GAE_BULG_MELEE_PURSUIT_END_SPEED - GAE_BULG_MELEE_PURSUIT_START_SPEED) * progress;
      Vec3 velocity = player.getDeltaMovement().scale(0.25).add(direction.scale(speed));
      player.setDeltaMovement(velocity.x, Mth.clamp(velocity.y, -0.25, 0.72), velocity.z);
      player.hurtMarked = true;
      player.fallDistance = 0.0F;
      facePlayerToward(player, targetCenter);
      if (player.tickCount % 2 == 0) {
         level.sendParticles(GAE_BULG_CHARGE_PARTICLE, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.32, 0.42, 0.32, 0.04);
         level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.26, 0.32, 0.26, 0.07);
      }
   }

   private static void scheduleGaeBulgMeleePursuit(ServerPlayer player) {
      TYPE_MOON_WORLD.queueServerWork(1, () -> {
         if (player == null || player.isRemoved()) {
            return;
         }
         tickGaeBulgMeleePursuit(player);
         if (player.getPersistentData().getBoolean(GAE_LOCKED_MELEE_PURSUIT_TAG)) {
            scheduleGaeBulgMeleePursuit(player);
         }
      });
   }

   public static void clearGaeBulgMeleeState(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(GAE_LOCKED_MELEE_CHARGE_TAG);
      data.remove(GAE_LOCKED_MELEE_CHARGE_TAG + "Ticks");
      data.remove(GAE_LOCK_CREATED_THIS_USE_TAG);
      data.remove(GAE_LOCKED_MELEE_TARGET_TAG);
      data.remove(GAE_LOCKED_MELEE_TARGET_UNTIL_TAG);
      data.remove(GAE_LOCKED_MELEE_PURSUIT_TAG);
      data.remove(GAE_LOCKED_MELEE_PURSUIT_UNTIL_TAG);
   }

   public static void clearGaeBulgUseState(ServerPlayer player) {
      clearGaeBulgMeleeState(player);
      CompoundTag data = player.getPersistentData();
      data.remove(GAE_DEATH_FLIGHT_TAG);
      data.remove(GAE_DEATH_FLIGHT_TAG + "Ticks");
      data.remove(GAE_DEATH_FLIGHT_PAID_TAG);
   }

   public static boolean startExcaliburCharge(ServerPlayer player) {
      long now = player.level().getGameTime();
      var d = player.getPersistentData();
      if (NoblePhantasmCastService.chanting(player) || d.hasUUID(ArtoriaStateService.NP_BEAM)
         || now < d.getLong(ArtoriaStateService.NP_READY) || !consumeStrict(player, ArtoriaRules.EXCALIBUR_MP)) return false;
      NoblePhantasmCastService.begin(player, "artoria_excalibur", ArtoriaRules.EXCALIBUR_COOLDOWN,
         ticks -> { d.putLong(ArtoriaStateService.NP_READY, player.level().getGameTime() + ticks); addExcaliburCooldown(player, ticks); },
          () -> {
             d.remove(EXCALIBUR_CHARGE_TAG); d.remove(EXCALIBUR_LAST_CHARGE_VFX_TAG); clearArtoriaExcaliburWindLock(player);
             net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAnimationService.play(
                player, net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAction.CANCEL);
          });
      d.putInt(EXCALIBUR_CHARGE_TAG, 0);
      d.putLong(ArtoriaStateService.NP_READY, now + ArtoriaRules.EXCALIBUR_COOLDOWN);
      addExcaliburCooldown(player, ArtoriaRules.EXCALIBUR_COOLDOWN);
      net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAnimationService.play(
         player, net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAction.FULL_NP);
      ArtoriaStateService.autoReleaseWindBarrier(player);
      revealArtoriaWindVeiledExcaliburForNp(player, (int)(NoblePhantasmCastService.releaseAt(player) - now) + 1);
      TYPE_MOON_WORLD.queueServerWork(20, () -> {
         if (NoblePhantasmCastService.chanting(player)) ArtoriaStateService.autoReleaseWindBarrier(player);
      });
      NoblePhantasmCastService.deferEffect(player, () -> fireExcalibur(player));
      return true;
   }

   /** Starts the three-second, quarter-power Excalibur release used by a normal right click. */
   public static boolean startShortExcaliburCharge(ServerPlayer player) {
      long now = player.level().getGameTime();
      var d = player.getPersistentData();
      if (!ArtoriaStateService.isArtoria(player) || NoblePhantasmCastService.chanting(player)
         || d.hasUUID(ArtoriaStateService.NP_BEAM) || now < d.getLong(ArtoriaStateService.NP_SHORT_READY)
         || !consumeStrict(player, ArtoriaRules.EXCALIBUR_SHORT_MP)) return false;
      NoblePhantasmCastService.begin(player, "artoria_excalibur_short", ArtoriaRules.EXCALIBUR_SHORT_COOLDOWN,
         ticks -> { d.putLong(ArtoriaStateService.NP_SHORT_READY, player.level().getGameTime() + ticks); },
          () -> {
             d.remove(EXCALIBUR_SHORT_CHARGE_TAG); d.remove(EXCALIBUR_SHORT_LAST_VFX_TAG);
             net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAnimationService.play(
                player, net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAction.CANCEL);
          });
      d.putInt(EXCALIBUR_SHORT_CHARGE_TAG, 0);
      d.putLong(ArtoriaStateService.NP_SHORT_READY, now + ArtoriaRules.EXCALIBUR_SHORT_COOLDOWN);
      net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAnimationService.play(
         player, net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAction.SHORT_RELEASE);
      ArtoriaStateService.autoReleaseWindBarrier(player);
      ArtoriaAbilityService.reveal(player, ArtoriaRules.EXCALIBUR_SHORT_CHARGE_TICKS + ArtoriaRules.REVEAL_AFTER_NP);
      TYPE_MOON_WORLD.queueServerWork(20, () -> {
         if (NoblePhantasmCastService.chanting(player)) ArtoriaStateService.autoReleaseWindBarrier(player);
      });
      NoblePhantasmCastService.deferEffect(player, () -> fireShortExcalibur(player));
      return true;
   }

   private static void fireExcalibur(ServerPlayer player) {
      if (!player.getPersistentData().contains(EXCALIBUR_CHARGE_TAG) || !player.isUsingItem()
         || !player.getUseItem().is(ModItems.EXCALIBUR.get()) || !(player.level() instanceof ServerLevel level)) return;
      var d = player.getPersistentData();
      d.remove(EXCALIBUR_CHARGE_TAG);
      d.remove(EXCALIBUR_LAST_CHARGE_VFX_TAG);
      Vec3 start = player.position().add(0, player.getBbHeight() * .66, 0).add(player.getLookAngle().scale(1.2));
      ArtoriaExcaliburBeamEntity beam = new ArtoriaExcaliburBeamEntity(level, player, start, ArtoriaRules.EXCALIBUR_TICKS, 0, 1);
      d.putUUID(ArtoriaStateService.NP_BEAM, beam.getUUID());
      level.addFreshEntity(beam);
      ArtoriaAbilityService.reveal(player, ArtoriaRules.EXCALIBUR_TICKS + ArtoriaRules.REVEAL_AFTER_NP);
      revealArtoriaWindVeiledExcaliburForNp(player, ArtoriaRules.EXCALIBUR_TICKS + ArtoriaRules.REVEAL_AFTER_NP);
      VFXServerEffects.spawn(level, "artoria_excalibur_beam", player, 150);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, .85F);
   }

   private static void fireShortExcalibur(ServerPlayer player) {
      if (!player.getPersistentData().contains(EXCALIBUR_SHORT_CHARGE_TAG)
         || !player.isUsingItem() || !player.getUseItem().is(ModItems.EXCALIBUR.get())
         || !(player.level() instanceof ServerLevel level)) return;
      var d = player.getPersistentData();
      d.remove(EXCALIBUR_SHORT_CHARGE_TAG);
      d.remove(EXCALIBUR_SHORT_LAST_VFX_TAG);
      Vec3 start = player.position().add(0, player.getBbHeight() * .66, 0).add(player.getLookAngle().scale(1.2));
      ArtoriaExcaliburBeamEntity beam = new ArtoriaExcaliburBeamEntity(level, player, start,
         ArtoriaRules.EXCALIBUR_SHORT_TICKS, 0, 1.0F, false, true);
      d.putUUID(ArtoriaStateService.NP_BEAM, beam.getUUID());
      level.addFreshEntity(beam);
      ArtoriaAbilityService.reveal(player, ArtoriaRules.EXCALIBUR_SHORT_TICKS + ArtoriaRules.REVEAL_AFTER_NP);
      level.playSound(null, player.blockPosition(), ModSounds.ARTORIA_VOICE_EXCALIBUR_RELEASE.get(), SoundSource.VOICE, 1.1F, 1.0F);
      VFXServerEffects.spawn(level, "artoria_excalibur_beam", player, ArtoriaRules.EXCALIBUR_SHORT_TICKS);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 1.15F);
   }

   public static void tickExcaliburCharge(Level level, LivingEntity living, int useTicks) {
      if (!(living instanceof ServerPlayer player) || !NoblePhantasmCastService.chanting(player)) return;
      applyNoblePhantasmChargeSlow(player);
      var d = player.getPersistentData();
      d.putInt(EXCALIBUR_CHARGE_TAG, useTicks);
      if (level.getGameTime() - d.getLong(EXCALIBUR_LAST_CHARGE_VFX_TAG) >= 32) {
         d.putLong(EXCALIBUR_LAST_CHARGE_VFX_TAG, level.getGameTime());
         VFXServerEffects.spawn((ServerLevel)level, "artoria_excalibur_charge", player, 128);
      }
   }

   public static void tickShortExcaliburCharge(Level level, LivingEntity living, int useTicks) {
      if (!(living instanceof ServerPlayer player) || !NoblePhantasmCastService.chanting(player)) return;
      applyNoblePhantasmChargeSlow(player);
      var d = player.getPersistentData();
      int charge = Math.min(EXCALIBUR_SHORT_MAX_CHARGE_TICKS, useTicks);
      d.putInt(EXCALIBUR_SHORT_CHARGE_TAG, charge);
      if (level instanceof ServerLevel serverLevel && serverLevel.getGameTime() - d.getLong(EXCALIBUR_SHORT_LAST_VFX_TAG) >= 12) {
         d.putLong(EXCALIBUR_SHORT_LAST_VFX_TAG, serverLevel.getGameTime());
         VFXServerEffects.spawn(serverLevel, "artoria_excalibur_charge", player, 96);
      }
   }

   public static void releaseExcalibur(ServerPlayer player) {
      // Releasing before the shared release point cancels; it never queues a short beam.
      if (NoblePhantasmCastService.chanting(player)) NoblePhantasmCastService.cancel(player, true);
      player.getPersistentData().remove(EXCALIBUR_CHARGE_TAG);
      player.getPersistentData().remove(EXCALIBUR_LAST_CHARGE_VFX_TAG);
   }

   public static void releaseShortExcalibur(ServerPlayer player) {
      if (NoblePhantasmCastService.chanting(player)) NoblePhantasmCastService.cancel(player, true);
      player.getPersistentData().remove(EXCALIBUR_SHORT_CHARGE_TAG);
      player.getPersistentData().remove(EXCALIBUR_SHORT_LAST_VFX_TAG);
   }

   public static void startGoldenExcaliburCharge(ServerPlayer player) {
      player.getPersistentData().putInt(GOLDEN_EXCALIBUR_CHARGE_TAG, 0);
      player.getPersistentData().remove(GOLDEN_EXCALIBUR_LAST_CHARGE_VFX_TAG);
      player.getPersistentData().remove(GOLDEN_EXCALIBUR_MIN_CHARGE_PAID_TAG);
   }

   public static void tickGoldenExcaliburCharge(Level level, LivingEntity living, int useTicks) {
      if (!(living instanceof ServerPlayer player)) return;
      int charged = Math.min(GOLDEN_EXCALIBUR_MAX_CHARGE_TICKS, useTicks);
      player.getPersistentData().putInt(GOLDEN_EXCALIBUR_CHARGE_TAG, charged);
      applyNoblePhantasmChargeSlow(player);
      if (useTicks >= GOLDEN_EXCALIBUR_MIN_CHARGE_TICKS && useTicks <= GOLDEN_EXCALIBUR_MAX_CHARGE_TICKS) {
         double cost = player.getPersistentData().getBoolean(GOLDEN_EXCALIBUR_MIN_CHARGE_PAID_TAG)
            ? GOLDEN_EXCALIBUR_MANA_PER_TICK
            : GOLDEN_EXCALIBUR_MANA_PER_TICK * GOLDEN_EXCALIBUR_MIN_CHARGE_TICKS;
         if (!consumeStrict(player, cost)) {
            player.releaseUsingItem();
         } else {
            player.getPersistentData().putBoolean(GOLDEN_EXCALIBUR_MIN_CHARGE_PAID_TAG, true);
            if (level instanceof ServerLevel serverLevel) {
               long now = serverLevel.getGameTime();
               serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.45, 0.5, 0.45, 0.05);
               serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 0.9, player.getZ(), 8, 0.35, 0.45, 0.35, 0.04);
               if (now - player.getPersistentData().getLong(GOLDEN_EXCALIBUR_LAST_CHARGE_VFX_TAG) >= 20L) {
                  player.getPersistentData().putLong(GOLDEN_EXCALIBUR_LAST_CHARGE_VFX_TAG, now);
                  VFXServerEffects.spawn(serverLevel, "artoria_excalibur_charge", player, 128.0);
                  serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.35F, 1.45F);
               }
            }
         }
      }
   }

   public static void releaseGoldenExcalibur(ServerPlayer player) {
      int charged = player.getPersistentData().getInt(GOLDEN_EXCALIBUR_CHARGE_TAG);
      boolean paid = player.getPersistentData().getBoolean(GOLDEN_EXCALIBUR_MIN_CHARGE_PAID_TAG);
      player.getPersistentData().remove(GOLDEN_EXCALIBUR_CHARGE_TAG);
      player.getPersistentData().remove(GOLDEN_EXCALIBUR_LAST_CHARGE_VFX_TAG);
      player.getPersistentData().remove(GOLDEN_EXCALIBUR_MIN_CHARGE_PAID_TAG);
      if (!(player.level() instanceof ServerLevel level)) return;
      if (charged < GOLDEN_EXCALIBUR_MIN_CHARGE_TICKS || !paid) {
         level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.45F, 1.65F);
         return;
      }
      float powerScale = chargePower(charged, GOLDEN_EXCALIBUR_MAX_CHARGE_TICKS);
      Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.66, 0.0).add(player.getLookAngle().normalize().scale(1.2));
      ArtoriaExcaliburBeamEntity beam = new ArtoriaExcaliburBeamEntity(level, player, start, GOLDEN_EXCALIBUR_RELEASE_TICKS, 0, powerScale, true);
      level.addFreshEntity(beam);
      VFXServerEffects.spawn(level, "artoria_excalibur_beam", player, 150.0);
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 2, 0.05, 0.05, 0.05, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 70, 0.55, 0.55, 0.55, 0.18);
      addGoldenExcaliburCooldown(player, scaledCooldown(GOLDEN_EXCALIBUR_PLAYER_COOLDOWN, powerScale));
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F + powerScale, 1.35F);
      level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.35F + powerScale * 0.45F, 1.85F);
   }

   public static void startGallatinCharge(ServerPlayer player) {
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.begin(player, "gawain_gallatin", GALLATIN_PLAYER_COOLDOWN, ticks -> addGallatinCooldown(player, ticks), () -> { player.getPersistentData().remove(GALLATIN_CHARGE_TAG); });
      player.getPersistentData().putInt(GALLATIN_CHARGE_TAG, 0);
      player.getPersistentData().remove(GALLATIN_LAST_CHARGE_VFX_TAG);
      player.getPersistentData().remove(GALLATIN_MIN_CHARGE_PAID_TAG);
      startServantCardChargeVoice(player, "gawain", ModSounds.GAWAIN_VOICE_NP.get());
   }

   public static void tickGallatinCharge(Level level, LivingEntity living, int useTicks) {
      if (!(living instanceof ServerPlayer player)) {
         return;
      }
      int charged = Math.min(GALLATIN_MAX_CHARGE_TICKS, useTicks);
      player.getPersistentData().putInt(GALLATIN_CHARGE_TAG, charged);
      applyNoblePhantasmChargeSlow(player);
      tickServantCardChargeVoice(player, "gawain", ModSounds.GAWAIN_VOICE_NP.get(), ModSounds.GAWAIN_VOICE_GALLATIN_SHORT.get());
      if (useTicks >= MIN_CHARGE_NP_RELEASE_TICKS && useTicks <= GALLATIN_MAX_CHARGE_TICKS) {
         double cost = player.getPersistentData().getBoolean(GALLATIN_MIN_CHARGE_PAID_TAG)
            ? CHARGE_MANA_PER_TICK
            : CHARGE_MANA_PER_TICK * MIN_CHARGE_NP_RELEASE_TICKS;
         if (!consumeStrict(player, cost)) {
            stopServantCardChargeVoice(player, "gawain", ModSounds.GAWAIN_VOICE_NP.get(), ModSounds.GAWAIN_VOICE_GALLATIN_SHORT.get());
            player.releaseUsingItem();
         } else {
            player.getPersistentData().putBoolean(GALLATIN_MIN_CHARGE_PAID_TAG, true);
            if (level instanceof ServerLevel serverLevel) {
            long now = serverLevel.getGameTime();
            if (now - player.getPersistentData().getLong(GALLATIN_LAST_CHARGE_VFX_TAG) >= 28L) {
               player.getPersistentData().putLong(GALLATIN_LAST_CHARGE_VFX_TAG, now);
               VFXServerEffects.spawn(serverLevel, "servant_gawain_gallatin_charge", player, 128.0);
            }
            if (useTicks % 2 == 0) {
               Vec3 front = player.position().add(horizontalLook(player).scale(1.2)).add(0.0, player.getBbHeight() * 0.72, 0.0);
               serverLevel.sendParticles(ParticleTypes.FLAME, front.x, front.y, front.z, 4, 0.24, 0.2, 0.24, 0.03);
               serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() + 2.2, player.getZ(), 3, 0.7, 0.2, 0.7, 0.015);
            }
            }
         }
      }
   }

   public static void releaseGallatin(ServerPlayer player) {
      if (net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.deferEffect(player, () -> releaseGallatin(player))) return;
      int charged = player.getPersistentData().getInt(GALLATIN_CHARGE_TAG);
      stopServantCardChargeVoice(player, "gawain", ModSounds.GAWAIN_VOICE_NP.get(), ModSounds.GAWAIN_VOICE_GALLATIN_SHORT.get());
      player.getPersistentData().remove(GALLATIN_CHARGE_TAG);
      player.getPersistentData().remove(GALLATIN_LAST_CHARGE_VFX_TAG);
      player.getPersistentData().remove(GALLATIN_MIN_CHARGE_PAID_TAG);
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      if (charged < MIN_CHARGE_NP_RELEASE_TICKS) {
         level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.45F, 1.65F);
         return;
      }
      float powerScale = chargePower(charged, GALLATIN_MAX_CHARGE_TICKS);
      Vec3 look = horizontalLook(player);
      boolean sunlight = isUnderGallatinSun(level, player.blockPosition());
      VFXServerEffects.spawnReplayable(level, "servant_gawain_gallatin", player, 3.0F);
      level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F + powerScale * 1.4F, 0.62F);
      level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F + powerScale * 0.9F, 0.78F);
      performGallatinCone(player, level, look, (sunlight ? 3000.0F : 1000.0F) * powerScale, powerScale);
      addGallatinCooldown(player, scaledCooldown(GALLATIN_PLAYER_COOLDOWN, powerScale));
   }

   public static boolean isChargingMovementLocked(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      return data.getInt(EXCALIBUR_CHARGE_TAG) > 0 || data.getInt(GALLATIN_CHARGE_TAG) > 0;
   }

   public static void applyNoblePhantasmChargeSlow(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 1, false, false, true));
   }

   public static boolean usePseudoSpiralDash(ServerPlayer player) {
      if (!consumeStrict(player, 50.0)) {
         return false;
      }
      if (!(player.level() instanceof ServerLevel level)) {
         return true;
      }
      Vec3 dir = horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.55, 0.18, dir.z * 1.55));
      player.hurtMarked = true;
      Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.45, 0.0);
      Vec3 end = start.add(dir.scale(5.0));
      AABB box = new AABB(start, end).inflate(1.1, 1.0, 1.1);
      boolean hit = false;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         hit = true;
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().mobAttack(player), 10F);
         target.invulnerableTime = 0;
         triggerPseudoSpiralExplosion(level, player, target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
         break;
      }
      if (!hit) {
         triggerPseudoSpiralExplosion(level, player, end);
      }
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 24, 0.35, 0.35, 0.35, 0.12);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.PLAYERS, 1.1F, 1.2F);
      return true;
   }

   public static boolean isUbwProjection(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return tag != null && tag.getBoolean("is_infinite_projection");
   }

   public static void markUbwProjection(ItemStack stack) {
      updateCustomData(stack, tag -> {
         tag.putBoolean("is_projected", true);
         tag.putBoolean("is_infinite_projection", true);
      });
      stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
   }

   public static void markServantCardCopiedNoblePhantasm(ItemStack stack) {
      if (stack.isEmpty()) return;
      updateCustomData(stack, tag -> {
         tag.putBoolean(SERVANT_CARD_COPIED_NP_TAG, true);
         tag.putString(SERVANT_CARD_COPIED_NP_TOKEN_TAG, UUID.randomUUID().toString());
      });
   }

   public static boolean isServantCardCopiedNoblePhantasm(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return !stack.isEmpty() && stack.getItem() instanceof NoblePhantasmItem && tag != null
         && tag.getBoolean(SERVANT_CARD_COPIED_NP_TAG);
   }

   public static String servantCardCopiedNoblePhantasmToken(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return tag == null ? "" : tag.getString(SERVANT_CARD_COPIED_NP_TOKEN_TAG);
   }

   public static void tryCompleteProjectedKanshouBakuyaPair(ServerPlayer player, ItemStack stack, InteractionHand hand) {
      String projectionId = kanshouBakuyaId(stack);
      if (projectionId == null || !isUbwProjection(stack)) {
         return;
      }
      InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      if (!player.getItemInHand(otherHand).isEmpty()) {
         return;
      }
      ItemStack paired = new ItemStack(otherKanshouBakuyaItem(projectionId));
      copyProjectionComponents(player.getItemInHand(hand), paired);
      markUbwProjection(paired);
      player.setItemInHand(otherHand, paired);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.65, player.getZ(), 18, 0.28, 0.42, 0.28, 0.03);
         level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.65F, 1.45F);
      }
   }

   public static boolean useKanshouBakuya(ServerPlayer player, InteractionHand hand, String projectionId) {
      ItemStack stack = player.getItemInHand(hand);
      if (!isKanshouBakuyaId(projectionId) || !isUbwProjection(stack)) {
         return false;
      }
      if (player.isCrouching() && !isOveredgeKanshouBakuyaId(projectionId)) {
         evolveKanshouBakuyaPair(player);
      } else if (isOveredgeKanshouBakuyaId(projectionId)) {
         dashSlashOveredge(player, hand);
      } else {
         throwKanshouBakuya(player, hand);
      }
      player.getCooldowns().addCooldown(stack.getItem(), 10);
      return true;
   }

   private static void throwGaeBulgSingle(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 32.0, 1.5);
      if (target == null) {
         target = findNearestCombatTarget(player, 32.0);
      }
      GaeBulgProjectileEntity projectile = new GaeBulgProjectileEntity(level, player);
      projectile.setMode(GaeBulgProjectileEntity.Mode.SINGLE);
      projectile.setTrackedTarget(target);
      projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
      Vec3 aim = target != null ? target.position().add(0.0, target.getBbHeight() * 0.45, 0.0) : player.getEyePosition().add(player.getLookAngle().scale(32.0));
      Vec3 dir = aim.subtract(projectile.position()).normalize();
      projectile.shoot(dir.x, dir.y + 0.06, dir.z, 2.4F, 0.0F);
      level.addFreshEntity(projectile);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 0.75F);
   }

   private static void addGaeBulgCooldown(ServerPlayer player, int cooldownTicks) {
      if (player.getMainHandItem().is(ModItems.GAE_BULG.get())) {
         player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), cooldownTicks);
      }
      if (player.getOffhandItem().is(ModItems.GAE_BULG.get())) {
         player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), cooldownTicks);
      }
   }

   private static float chargePower(int chargedTicks, int maxTicks) {
      float ratio = Mth.clamp(chargedTicks / (float)Math.max(1, maxTicks), 0.0F, 1.0F);
      return 0.2F + ratio * 0.8F;
   }

   private static int scaledCooldown(int fullCooldown, float powerScale) {
      return Math.max(200, Mth.floor(fullCooldown * (0.25F + powerScale * 0.75F)));
   }

   private static void addExcaliburCooldown(ServerPlayer player, int cooldownTicks) {
      if (player.getMainHandItem().is(ModItems.EXCALIBUR.get())) {
         player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), cooldownTicks);
      }
      if (player.getOffhandItem().is(ModItems.EXCALIBUR.get())) {
         player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), cooldownTicks);
      }
   }

   private static void addGoldenExcaliburCooldown(ServerPlayer player, int cooldownTicks) {
      if (player.getMainHandItem().is(ModItems.EXCALIBUR2.get())) {
         player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), cooldownTicks);
      }
      if (player.getOffhandItem().is(ModItems.EXCALIBUR2.get())) {
         player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), cooldownTicks);
      }
   }

   private static void addGallatinCooldown(ServerPlayer player, int cooldownTicks) {
      if (player.getMainHandItem().is(ModItems.EXCALIBUR_GALLATIN.get())) {
         player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), cooldownTicks);
      }
      if (player.getOffhandItem().is(ModItems.EXCALIBUR_GALLATIN.get())) {
         player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), cooldownTicks);
      }
   }

   private static boolean isKanshouBakuyaId(String projectionId) {
      return "gan_jiang".equals(projectionId)
         || "mo_ye".equals(projectionId)
         || "gan_jiang_overedge".equals(projectionId)
         || "mo_ye_overedge".equals(projectionId);
   }

   private static boolean isOveredgeKanshouBakuyaId(String projectionId) {
      return "gan_jiang_overedge".equals(projectionId) || "mo_ye_overedge".equals(projectionId);
   }

   private static String kanshouBakuyaId(ItemStack stack) {
      if (stack.is(ModItems.GAN_JIANG.get())) {
         return "gan_jiang";
      } else if (stack.is(ModItems.MO_YE.get())) {
         return "mo_ye";
      } else if (stack.is(ModItems.GAN_JIANG_OVEREDGE.get())) {
         return "gan_jiang_overedge";
      } else {
         return stack.is(ModItems.MO_YE_OVEREDGE.get()) ? "mo_ye_overedge" : null;
      }
   }

   private static net.minecraft.world.item.Item otherKanshouBakuyaItem(String projectionId) {
      return switch (projectionId) {
         case "gan_jiang" -> ModItems.MO_YE.get();
         case "mo_ye" -> ModItems.GAN_JIANG.get();
         case "gan_jiang_overedge" -> ModItems.MO_YE_OVEREDGE.get();
         case "mo_ye_overedge" -> ModItems.GAN_JIANG_OVEREDGE.get();
         default -> ModItems.MO_YE.get();
      };
   }

   private static void evolveKanshouBakuyaPair(ServerPlayer player) {
      setKanshouBakuyaHand(player, InteractionHand.MAIN_HAND, true);
      setKanshouBakuyaHand(player, InteractionHand.OFF_HAND, true);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() * 0.62, player.getZ(), 36, 0.45, 0.45, 0.45, 0.08);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.75F);
      }
   }

   private static void setKanshouBakuyaHand(ServerPlayer player, InteractionHand hand, boolean overedge) {
      ItemStack current = player.getItemInHand(hand);
      boolean ganJiang = current.is(ModItems.GAN_JIANG.get()) || current.is(ModItems.GAN_JIANG_OVEREDGE.get());
      boolean moYe = current.is(ModItems.MO_YE.get()) || current.is(ModItems.MO_YE_OVEREDGE.get());
      if (!ganJiang && !moYe) {
         return;
      }
      ItemStack evolved = new ItemStack(ganJiang
         ? overedge ? ModItems.GAN_JIANG_OVEREDGE.get() : ModItems.GAN_JIANG.get()
         : overedge ? ModItems.MO_YE_OVEREDGE.get() : ModItems.MO_YE.get());
      copyProjectionComponents(current, evolved);
      markUbwProjection(evolved);
      player.setItemInHand(hand, evolved);
   }

   private static void copyProjectionComponents(ItemStack source, ItemStack destination) {
      if (source == null || destination == null || source.isEmpty()) return;
      copyComponent(source, destination, DataComponents.CUSTOM_NAME);
      copyComponent(source, destination, DataComponents.LORE);
      copyComponent(source, destination, DataComponents.ENCHANTMENTS);
      copyComponent(source, destination, DataComponents.ATTRIBUTE_MODIFIERS);
      copyComponent(source, destination, DataComponents.DAMAGE);
      copyComponent(source, destination, DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
      ProjectionDataHelper.inherit(source, destination, 0L);
   }

   private static <T> void copyComponent(ItemStack source, ItemStack destination,
      net.minecraft.core.component.DataComponentType<T> type) {
      T value = source.get(type);
      if (value != null) destination.set(type, value);
   }

   private static void throwKanshouBakuya(ServerPlayer player, InteractionHand hand) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      ItemStack thrownStack = player.getItemInHand(hand).copy();
      thrownStack.setCount(1);
      EmiyaThrownWeaponEntity thrown = new EmiyaThrownWeaponEntity(level, player, thrownStack);
      Vec3 look = player.getLookAngle();
      Vec3 horizontal = horizontalLook(player);
      Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x);
      double sideOffset = (hand == InteractionHand.MAIN_HAND ? 1.45 : -1.45);
      Vec3 spawn = player.getEyePosition().add(look.scale(0.65)).add(side.scale(sideOffset));
      thrown.setPos(spawn.x, spawn.y - 0.12, spawn.z);
      thrown.setFixedDamage(32.0F);
      thrown.setBreakLowHardnessBlocks(true);
      thrown.setNoGravity(true);
      thrown.setArcingFlight(horizontal, sideOffset, 7.0);
      thrown.shoot(look.x, look.y + 0.04, look.z, 2.6F, 0.0F);
      level.addFreshEntity(thrown);
      if (!player.isCreative()) {
         player.getItemInHand(hand).shrink(1);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8F, 1.55F);
      level.sendParticles(ParticleTypes.CRIT, spawn.x, spawn.y, spawn.z, 10, 0.12, 0.12, 0.12, 0.06);
   }

   private static void dashSlashOveredge(ServerPlayer player, InteractionHand hand) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 dir = horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.85, 0.16, dir.z * 1.85));
      player.hurtMarked = true;
      Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.48, 0.0);
      Vec3 end = start.add(dir.scale(5.5));
      AABB box = new AABB(start, end).inflate(1.25, 0.9, 1.25);
      Set<Integer> hit = new HashSet<>();
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         if (hit.add(target.getId())) {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().mobAttack(player), 144.0F);
            target.invulnerableTime = 0;
         }
      }
      for (double t = 0.0; t <= 1.0; t += 0.12) {
         Vec3 pos = start.lerp(end, t);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 2, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 2, 0.12, 0.12, 0.12, 0.03);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0F, 1.25F);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.85F);
      consumeProjectedOveredgeUse(player, hand);
   }

   private static void consumeProjectedOveredgeUse(ServerPlayer player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      CompoundTag tag = customTag(stack);
      if (tag == null || !tag.getBoolean("is_projected")) {
         return;
      }
      int uses = tag.getInt(OVEREDGE_USE_COUNT_TAG) + 1;
      if (uses >= 2 && !player.isCreative()) {
         player.setItemInHand(hand, ItemStack.EMPTY);
         player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.75F);
         if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 12, 0.28, 0.34, 0.28, 0.04);
         }
      } else {
         updateCustomData(stack, data -> data.putInt(OVEREDGE_USE_COUNT_TAG, uses));
      }
   }

   private static Float consumeGaeBulgArmyManaAndDamage(ServerPlayer player, int chargedTicks) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return consumeStrict(player, GAE_BULG_ARMY_BASE_MANA) ? GAE_BULG_ARMY_BASE_DAMAGE : null;
      }
      double desiredMana = GAE_BULG_ARMY_BASE_MANA
         + Math.max(0, chargedTicks - GAE_DEATH_FLIGHT_CHARGE_TICKS) * CHARGE_MANA_PER_TICK;
      double available = ServantCardManaService.availableForConsume(player, vars);
      if (available + 1.0E-6 < GAE_BULG_ARMY_BASE_MANA) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return null;
      }
      double paid = ServantCardManaService.consumeUpTo(player, vars, Math.min(desiredMana, available));
      if (paid + 1.0E-6 < GAE_BULG_ARMY_BASE_MANA) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return null;
      }
      float damage = (float)(GAE_BULG_ARMY_BASE_DAMAGE * paid / GAE_BULG_ARMY_BASE_MANA);
      return Mth.clamp(damage, GAE_BULG_ARMY_BASE_DAMAGE, GAE_BULG_ARMY_MAX_DAMAGE);
   }

   private static void throwGaeBulgArmy(ServerPlayer player, LivingEntity target, float damage) {
      if (!(player.level() instanceof ServerLevel level)) {
          return;
       }
       GaeBulgArmyProjectileEntity projectile = new GaeBulgArmyProjectileEntity(level, player);
      projectile.setArmyDamage(damage);
      projectile.setTrackedTarget(target);
      projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
      Vec3 aim = target != null
         ? target.position().add(0.0, target.getBbHeight() * 0.3, 0.0)
         : player.getEyePosition().add(player.getLookAngle().scale(48.0));
      Vec3 dir = aim.subtract(projectile.position()).normalize();
      projectile.shoot(dir.x, dir.y + 0.14, dir.z, 2.65F, 0.0F);
      level.addFreshEntity(projectile);
      level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.5F, 0.65F);
   }

   private static void resolveGaeBulgHit(ServerPlayer player, LivingEntity target) {
      if (EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }
      if (ArtoriaPendragonCombatHelper.tryNegateCertainHitOrDeath(target, "gae_bolg_player")) {
         return;
      }
      boolean deathThorn = target.isAlive()
         && !(target instanceof EmiyaArcherEntity)
         && !(target instanceof EnkiduEntity)
         && player.getRandom().nextFloat() < CuChulainnCombatHelper.getDeathThornChance(target);
      DamageSource source = player.damageSources().mobAttack(player);
      if (net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService.conceptHit(target,source,250,"cu_chulainn:gae_bolg",deathThorn)) return;
      int livesBeforeInitialHit = target.getPersistentData().getInt("GodHandLives");
      target.invulnerableTime = 0;
      target.hurt(source, 250.0F);
      target.invulnerableTime = 0;
      boolean initialHitRevived = HeraclesGodHandHelper.consumedLifeAfterLethalHit(target, livesBeforeInitialHit);
      if (target.isAlive() && deathThorn && !initialHitRevived) {
         int livesBeforeDeathThorn = target.getPersistentData().getInt("GodHandLives");
         target.hurt(source, Math.max(target.getMaxHealth() * 2.0F, 500.0F));
         if (target.isAlive() && !HeraclesGodHandHelper.consumedLifeAfterLethalHit(target, livesBeforeDeathThorn)) {
            target.setHealth(0.0F);
            target.die(player.damageSources().genericKill());
         }
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 4, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 18, 0.25, 0.25, 0.25, 0.12);
         level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.1F, 0.7F);
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.gae_bulg.death_thorn_hit"), true);
   }

   private static void performTsubame(ServerPlayer player, LivingEntity target) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService.performTsubameGaeshi(player, target);
   }

   private static void performNineLives(ServerPlayer player, LivingEntity target) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 forward = horizontalLook(player);
      Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.45, 0.0);
      Vec3 end = start.add(forward.scale(7.0));
      AABB box = new AABB(start, end).inflate(2.0, 1.2, 2.0);
      Set<Integer> hit = new HashSet<>();
      for (int i = 0; i < 9; i++) {
         LivingEntity victim = target;
         if (victim == null || !victim.isAlive() || victim.distanceToSqr(player) > 81.0) {
            victim = null;
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
               if (hit.add(candidate.getId())) {
                  victim = candidate;
                  break;
               }
            }
         }
         if (victim != null) {
            victim.invulnerableTime = 0;
            victim.hurt(player.damageSources().mobAttack(player), 144.0F);
            victim.invulnerableTime = 0;
            Vec3 away = victim.position().subtract(player.position());
            if (away.lengthSqr() < 1.0E-4) {
               away = forward;
            }
            victim.setDeltaMovement(victim.getDeltaMovement().add(away.normalize().scale(0.5)).add(0.0, 0.12, 0.0));
            victim.hurtMarked = true;
         }
      }
      player.setDeltaMovement(player.getDeltaMovement().add(forward.scale(1.2)).add(0.0, 0.1, 0.0));
      player.hurtMarked = true;
      VFXServerEffects.spawn(level, "servant_heracles_nine_lives", player, 128.0);
      level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 80, 1.6, 0.8, 1.6, 0.18);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 0.62F);
      level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.25F);
   }

   private static LivingEntity findLookTarget(ServerPlayer player, double range, double inflate) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 end = eye.add(look.scale(range));
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(inflate);
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         player,
         eye,
         end,
         box,
         e -> e instanceof LivingEntity living && living.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e),
         range * range
      );
      return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
   }

   private static LivingEntity findGaeBulgMeleeLockTarget(ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      Vec3 end = eye.add(look.scale(GAE_BULG_MELEE_LOCK_RANGE));
      AABB search = player.getBoundingBox().expandTowards(look.scale(GAE_BULG_MELEE_LOCK_RANGE)).inflate(1.15, 0.75, 1.15);
      LivingEntity best = null;
      double bestScore = Double.MAX_VALUE;
      for (LivingEntity candidate : player.level().getEntitiesOfClass(
         LivingEntity.class,
         search,
         living -> living.isAlive() && living != player && EntityUtils.isValidCombatTarget(player, living)
      )) {
         if (player.distanceToSqr(candidate) > GAE_BULG_MELEE_LOCK_RANGE * GAE_BULG_MELEE_LOCK_RANGE
            || !isAimingAtGaeBulgMeleeLockArea(candidate, eye, end)
            || !canSeeGaeBulgMeleeLockArea(player, candidate)) {
            continue;
         }
         Vec3 center = candidate.position().add(0.0, candidate.getBbHeight() * 0.5, 0.0);
         double along = Math.max(0.0, center.subtract(eye).dot(look));
         double score = eye.add(look.scale(along)).distanceToSqr(center) + candidate.distanceToSqr(player) * 0.02;
         if (score < bestScore) {
            bestScore = score;
            best = candidate;
         }
      }
      return best;
   }

   private static boolean isAimingAtGaeBulgMeleeLockArea(LivingEntity target, Vec3 eye, Vec3 end) {
      return target.getBoundingBox().inflate(0.65, 0.45, 0.65).clip(eye, end).isPresent();
   }

   private static boolean canSeeGaeBulgMeleeLockArea(ServerPlayer player, LivingEntity target) {
      if (player.hasLineOfSight(target)) {
         return true;
      }
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      double side = Math.max(0.32, target.getBbWidth() * 0.42);
      return hasClearLine(player, center)
         || hasClearLine(player, center.add(side, 0.0, 0.0))
         || hasClearLine(player, center.add(-side, 0.0, 0.0))
         || hasClearLine(player, center.add(0.0, 0.0, side))
         || hasClearLine(player, center.add(0.0, 0.0, -side))
         || hasClearLine(player, center.add(0.0, target.getBbHeight() * 0.28, 0.0));
   }

   private static boolean hasClearLine(ServerPlayer player, Vec3 point) {
      HitResult hit = player.level().clip(new ClipContext(
         player.getEyePosition(),
         point,
         ClipContext.Block.COLLIDER,
         ClipContext.Fluid.NONE,
         player
      ));
      return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(point) <= 0.16;
   }

   private static LivingEntity resolveGaeBulgLockedTarget(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.hasUUID(GAE_LOCKED_MELEE_TARGET_TAG) || !(player.level() instanceof ServerLevel level)) {
         return null;
      }
      long until = data.getLong(GAE_LOCKED_MELEE_TARGET_UNTIL_TAG);
      if (until > 0L && level.getGameTime() > until) {
         data.remove(GAE_LOCKED_MELEE_TARGET_TAG);
         data.remove(GAE_LOCKED_MELEE_TARGET_UNTIL_TAG);
         return null;
      }
      UUID targetId = data.getUUID(GAE_LOCKED_MELEE_TARGET_TAG);
      Entity entity = level.getEntity(targetId);
      if (!(entity instanceof LivingEntity target)
         || !target.isAlive()
         || target.level() != player.level()
         || !isValidGaeBulgTarget(player, target)) {
         return null;
      }
      return target;
   }

   private static LivingEntity findGaeBulgLookTarget(ServerPlayer player, double range, double inflate) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 end = eye.add(look.scale(range));
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(inflate);
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         player,
         eye,
         end,
         box,
         e -> e instanceof LivingEntity living && isValidGaeBulgTarget(player, living),
         range * range
      );
      return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
   }

   private static LivingEntity findGaeBulgDeathFlightTarget(ServerPlayer player) {
      LivingEntity target = findGaeBulgLookTarget(player, 48.0, 2.0);
      if (target != null) {
         return target;
      }
      Vec3 look = player.getLookAngle().normalize();
      AABB search = player.getBoundingBox().expandTowards(look.scale(48.0)).inflate(4.5);
      List<LivingEntity> candidates = player.level().getEntitiesOfClass(
         LivingEntity.class,
         search,
         living -> isValidGaeBulgTarget(player, living)
      );
      return candidates.isEmpty() ? null : candidates.get(player.getRandom().nextInt(candidates.size()));
   }

   private static LivingEntity findRandomGaeBulgMeleeTarget(ServerPlayer player, double range) {
      if (!(range > 0.0)) {
         return null;
      }
      List<LivingEntity> candidates = player.level().getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(range),
         living -> isValidGaeBulgTarget(player, living)
      );
      return candidates.isEmpty() ? null : candidates.get(player.getRandom().nextInt(candidates.size()));
   }

   private static boolean isValidGaeBulgTarget(ServerPlayer player, LivingEntity target) {
      return EntityUtils.isValidCombatTarget(player, target) && !ServantMasterTargeting.isContractMaster(player, target);
   }

   private static void spawnGaeBulgLockFx(ServerPlayer player, LivingEntity target) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 from = player.position().add(0.0, player.getBbHeight() * 0.65, 0.0);
      Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      ServantCardSkillUtils.spawnLineParticles(level, from, to, ParticleTypes.CRIT);
      level.sendParticles(GAE_BULG_CHARGE_PARTICLE, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 12, 0.28, 0.34, 0.28, 0.02);
      level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.8F, 1.35F);
   }

   private static void startGaeBulgMeleePursuit(ServerPlayer player, LivingEntity target) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      Vec3 toTarget = targetCenter.subtract(player.position().add(0.0, player.getBbHeight() * 0.45, 0.0));
      Vec3 direction = toTarget.lengthSqr() < 1.0E-4 ? horizontalLook(player) : toTarget.normalize();
      player.setDeltaMovement(direction.x * 1.9, Mth.clamp(direction.y * 1.9, 0.08, 0.64), direction.z * 1.9);
      player.hurtMarked = true;
      player.fallDistance = 0.0F;
      facePlayerToward(player, targetCenter);
      level.sendParticles(GAE_BULG_CHARGE_PARTICLE, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.55, 0.7, 0.55, 0.08);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.1F, 0.8F);
   }

   private static void facePlayerToward(ServerPlayer player, Vec3 target) {
      Vec3 toTarget = target.subtract(player.getEyePosition());
      if (toTarget.lengthSqr() < 1.0E-4) {
         return;
      }
      double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
      float yRot = (float)(Mth.atan2(toTarget.z, toTarget.x) * 180.0F / (float)Math.PI) - 90.0F;
      float xRot = (float)(-(Mth.atan2(toTarget.y, horizontal) * 180.0F / (float)Math.PI));
      player.setYRot(yRot);
      player.setXRot(xRot);
      player.yHeadRot = yRot;
      player.yBodyRot = yRot;
   }

   private static LivingEntity findNearestCombatTarget(ServerPlayer player, double range) {
      if (!(range > 0.0)) {
         return null;
      }
      AABB box = player.getBoundingBox().inflate(range);
      LivingEntity nearest = null;
      double nearestDistance = Double.MAX_VALUE;
      for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, box, living -> EntityUtils.isValidCombatTarget(player, living))) {
         double distance = candidate.distanceToSqr(player);
         if (distance < nearestDistance) {
            nearestDistance = distance;
            nearest = candidate;
         }
      }
      return nearest;
   }

   private static void triggerPseudoSpiralExplosion(ServerLevel level, LivingEntity owner, Vec3 center) {
      double radius = 4.5;
      Set<Integer> damaged = new HashSet<>();
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(center, center).inflate(radius),
         e -> e.isAlive() && e != owner && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         if (damaged.add(living.getId())) {
            living.invulnerableTime = 0;
            living.hurt(owner.damageSources().explosion(null, owner), 10.0F);
            living.invulnerableTime = 0;
            Vec3 push = living.position().subtract(center).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-4) {
               push = push.normalize();
               living.push(push.x * 1.2, 0.42, push.z * 1.2);
               living.hurtMarked = true;
            }
         }
      }
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 2, 0.2, 0.2, 0.2, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 2, 0.0, 0.0, 0.0, 0.0);
      breakSmallExplosionTerrain(level, owner, center);
   }

   private static void performGallatinCone(ServerPlayer player, ServerLevel level, Vec3 look, float damage, float powerScale) {
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0);
      double range = GALLATIN_RANGE * (0.28 + powerScale * 0.72);
      double halfAngleCos = Math.cos(Math.toRadians(12.0 + 23.0 * powerScale));
      Set<Integer> hit = new HashSet<>();
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(range + 3.0),
         e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.45, 0.0).subtract(origin);
         Vec3 horizontal = new Vec3(to.x, 0.0, to.z);
         double distance = horizontal.length();
         if (distance > range || distance < 0.2) {
            continue;
         }
         Vec3 dir = horizontal.normalize();
         if (dir.dot(look) < halfAngleCos || !hit.add(living.getId())) {
            continue;
         }
         applyFixedDamageOverTicks(player, living, damage, 20);
         living.igniteForSeconds(5.0F);
         living.push(look.x * 5.0, 0.32, look.z * 5.0);
         living.hurtMarked = true;
      }
      spawnGallatinReleaseParticles(level, origin, look, range, powerScale);
      breakGallatinPath(level, origin, look, range, powerScale);
   }

   private static void applyFixedDamageOverTicks(ServerPlayer player, LivingEntity target, float totalDamage, int ticks) {
      int duration = Math.max(1, ticks);
      float perTick = totalDamage / duration;
      for (int delay = 0; delay < duration; delay++) {
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (player.isAlive() && target.isAlive()) {
               applyFixedDamage(player, target, perTick);
            }
         });
      }
   }

   private static void applyFixedDamage(ServerPlayer player, LivingEntity target, float damage) {
      if (EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }
      float before = target.getHealth();
      DamageSource source = player.damageSources().playerAttack(player);
      source = net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier.withNoblePhantasm(source, "excalibur_gallatin");
      target.invulnerableTime = 0;
      target.hurt(source, damage);
      target.invulnerableTime = 0;
      if (net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaCombatEvents.isOda(target)) return;
      if (target.getPersistentData().getBoolean("GodHandActive")) {
         return;
      }
      float desired = Math.max(0.0F, before - damage);
      if (target.getHealth() > desired && target.getHealth() <= before) {
         boolean lethal = desired <= 0.0F && target.isAlive();
         target.setHealth(desired);
         if (lethal) {
            target.die(source);
         }
      }
   }

   private static void spawnGallatinReleaseParticles(ServerLevel level, Vec3 origin, Vec3 look, double range, float powerScale) {
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      for (double dist = 1.0; dist <= range; dist += 3.0) {
         double halfWidth = Math.min(20.0 * powerScale, dist * (0.18 + powerScale * 0.52));
         Vec3 center = origin.add(look.scale(dist));
         level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 9, halfWidth * 0.3, 0.3, halfWidth * 0.3, 0.09);
         if (((int)dist) % 6 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y - 0.2, center.z, 1, halfWidth * 0.16, 0.08, halfWidth * 0.16, 0.0);
         }
         if (((int)dist) % 9 == 0) {
            Vec3 edge = center.add(right.scale(level.random.nextBoolean() ? halfWidth : -halfWidth));
            level.sendParticles(ParticleTypes.FLAME, edge.x, edge.y, edge.z, 5, 0.2, 0.35, 0.2, 0.08);
         }
      }
   }

   private static void breakGallatinPath(ServerLevel level, Vec3 origin, Vec3 look, double range, float powerScale) {
      int broken = 0;
      int limit = Math.max(24, Mth.floor(220.0F * powerScale));
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      for (double dist = 2.0; dist <= range && broken < limit; dist += 2.0) {
         double halfWidth = Math.min(20.0 * powerScale, dist * (0.18 + powerScale * 0.52));
         for (double side = -halfWidth; side <= halfWidth && broken < limit; side += 2.0) {
            BlockPos center = BlockPos.containing(origin.add(look.scale(dist)).add(right.scale(side)));
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(0, -1, 0), center.offset(0, 2, 0))) {
               BlockState state = level.getBlockState(pos);
               float hardness = state.getDestroySpeed(level, pos);
               if (!state.isAir()
                  && hardness >= 0.0F
                  && hardness < 55.0F
                  && !state.is(Blocks.BEDROCK)
                  && state.getExplosionResistance(level, pos, null) < 1200.0F
                  && level.removeBlock(pos, false)) {
                  broken++;
                  if (broken >= limit) {
                     break;
                  }
               }
            }
         }
      }
   }

   private static boolean isUnderGallatinSun(ServerLevel level, BlockPos pos) {
      long dayTime = level.getDayTime() % 24000L;
      return level.dimensionType().hasSkyLight()
         && dayTime >= 0L && dayTime < 12000L
         && !level.isRaining()
         && !level.isThundering()
         && level.canSeeSky(pos.above());
   }

   private static void breakSmallExplosionTerrain(ServerLevel level, LivingEntity owner, Vec3 center) {
      int broken = 0;
      for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(center).offset(-3, -3, -3), BlockPos.containing(center).offset(3, 3, 3))) {
         if (broken >= 48 || !pos.closerToCenterThan(center, 3.5)) {
            continue;
         }
         BlockState state = level.getBlockState(pos);
         float hardness = state.getDestroySpeed(level, pos);
         if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0.0F || hardness > 50.0F || state.getExplosionResistance(level, pos, null) >= 1200.0F) {
            continue;
         }
         if (net.xxxjk.TYPE_MOON_WORLD.world.terrain.CombatTerrainProtection.removeBlock(level, pos, false)) {
            broken++;
         }
      }
   }

   public static Vec3 horizontalLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   public static void startServantCardVoiceSession(ServerPlayer player, String servantId, SoundEvent sound) {
      startServantCardChargeVoice(player, servantId, sound);
   }

   public static boolean finishServantCardVoiceSession(ServerPlayer player, String servantId, SoundEvent longSound, SoundEvent shortSound) {
      return stopServantCardChargeVoice(player, servantId, longSound, shortSound);
   }

   private static void startServantCardChargeVoice(ServerPlayer player, String servantId, SoundEvent sound) {
      if (net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.suppressLegacyVoice(player, sound)) return;
      if (sound == null || !isServantCard(player, servantId) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      String baseKey = chargeVoiceKey(servantId);
      long now = level.getGameTime();
      if (data.getBoolean(baseKey + "_active")) {
         return;
      }
      data.putBoolean(baseKey + "_active", true);
      data.putBoolean(baseKey + "_short", false);
      data.putBoolean(baseKey + "_ready_short", false);
      data.putLong(baseKey + "_start", now);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.VOICE, 1.0F, 1.0F);
   }

   private static void tickServantCardChargeVoice(ServerPlayer player, String servantId, SoundEvent longSound, SoundEvent shortSound) {
      if (!isServantCard(player, servantId) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      String baseKey = chargeVoiceKey(servantId);
      if (!data.getBoolean(baseKey + "_active")) {
         return;
      }
      long start = data.getLong(baseKey + "_start");
      if (level.getGameTime() - start >= SERVANT_CARD_CHARGE_SHORT_VOICE_TICKS) {
         data.putBoolean(baseKey + "_ready_short", true);
         data.putLong(baseKey + "_short_until", level.getGameTime() + 40L);
      }
   }

   private static boolean stopServantCardChargeVoice(ServerPlayer player, String servantId, SoundEvent longSound, SoundEvent shortSound) {
      CompoundTag data = player.getPersistentData();
      String baseKey = chargeVoiceKey(servantId);
      if (net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.chanting(player) || player.getPersistentData().contains("TypeMoonNpCastAction")) return false;
      boolean active = data.getBoolean(baseKey + "_active");
      boolean playShort = false;
      long now = player.level() instanceof ServerLevel level ? level.getGameTime() : 0L;
      if (active) {
         stopSound(player, longSound);
         if (isServantCard(player, servantId) && shortSound != null && !data.getBoolean(baseKey + "_short") && player.level() instanceof ServerLevel level) {
            long start = data.getLong(baseKey + "_start");
            playShort = data.getBoolean(baseKey + "_ready_short") || level.getGameTime() - start >= SERVANT_CARD_CHARGE_SHORT_VOICE_TICKS;
            if (playShort) {
               data.putBoolean(baseKey + "_short", true);
               level.playSound(null, player.getX(), player.getY(), player.getZ(), shortSound, SoundSource.VOICE, 1.0F, 1.0F);
            }
         }
      } else if (isServantCard(player, servantId)
         && shortSound != null
         && !data.getBoolean(baseKey + "_short")
         && data.getLong(baseKey + "_short_until") >= now
         && player.level() instanceof ServerLevel level) {
         playShort = true;
         data.putBoolean(baseKey + "_short", true);
         level.playSound(null, player.getX(), player.getY(), player.getZ(), shortSound, SoundSource.VOICE, 1.0F, 1.0F);
      }
      long shortUntil = data.getLong(baseKey + "_short_until");
      data.remove(baseKey + "_active");
      data.remove(baseKey + "_short");
      data.remove(baseKey + "_ready_short");
      data.remove(baseKey + "_start");
      if (!playShort && shortUntil >= now) {
         data.putLong(baseKey + "_short_until", shortUntil);
      } else {
         data.remove(baseKey + "_short_until");
      }
      return playShort;
   }

   public static void revealArtoriaWindVeiledExcalibur(ServerPlayer player, int ticks) {
      if (player == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long until = level.getGameTime() + Math.max(1, ticks);
      player.getPersistentData().putLong(ARTORIA_WIND_REVEAL_UNTIL_TAG, until);
      revealArtoriaWindVeiledStack(player.getMainHandItem(), until);
      revealArtoriaWindVeiledStack(player.getOffhandItem(), until);
      player.getInventory().setChanged();
   }

   public static void revealArtoriaWindVeiledExcaliburForNp(ServerPlayer player, int ticks) {
      if (player == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long until = level.getGameTime() + Math.max(1, ticks);
      player.getPersistentData().putLong(ARTORIA_EXCALIBUR_WIND_LOCK_UNTIL_TAG, until);
      revealArtoriaWindVeiledExcalibur(player, ticks);
   }

   public static boolean isArtoriaExcaliburWindLocked(ServerPlayer player) {
      return player != null
         && player.level() instanceof ServerLevel level
         && player.getPersistentData().getLong(ARTORIA_EXCALIBUR_WIND_LOCK_UNTIL_TAG) > level.getGameTime();
   }

   public static boolean isArtoriaExcaliburRevealed(ServerPlayer player) {
      return player != null && player.level() instanceof ServerLevel level
         && player.getPersistentData().getLong(ARTORIA_WIND_REVEAL_UNTIL_TAG) > level.getGameTime();
   }

   public static void clearArtoriaExcaliburWindLock(ServerPlayer player) {
      if (player != null) {
         player.getPersistentData().remove(ARTORIA_EXCALIBUR_WIND_LOCK_UNTIL_TAG);
      }
   }

   private static void revealArtoriaWindVeiledStack(ItemStack stack, long until) {
      if (stack.isEmpty() || !stack.is(ModItems.EXCALIBUR.get())) {
         return;
      }
      updateCustomData(stack, tag -> {
         if (tag.getBoolean("ServantCardArtoriaWindVeiled")) {
            tag.putLong(ARTORIA_WIND_REVEAL_UNTIL_TAG, until);
         }
      });
   }

   private static void stopSound(ServerPlayer player, SoundEvent sound) {
      if (sound == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      ResourceLocation location = sound.getLocation();
      ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(location, SoundSource.VOICE);
      double radiusSqr = SERVANT_CARD_CHARGE_VOICE_STOP_RADIUS * SERVANT_CARD_CHARGE_VOICE_STOP_RADIUS;
      for (ServerPlayer listener : level.getPlayers(listener -> listener.distanceToSqr(player) <= radiusSqr)) {
         listener.connection.send(packet);
      }
   }

   private static String chargeVoiceKey(String servantId) {
      return SERVANT_CARD_NP_CHARGE_VOICE_TAG + "_" + servantId;
   }

   private static boolean isServantCard(ServerPlayer player, String servantId) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && servantId.equals(vars.servant_card_id);
   }

   private static CompoundTag customTag(ItemStack stack) {
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? null : data.copyTag();
   }

   private static void updateCustomData(ItemStack stack, TagUpdater updater) {
      CompoundTag tag = customTag(stack);
      if (tag == null) {
         tag = new CompoundTag();
      }
      updater.update(tag);
      if (tag.isEmpty()) {
         stack.remove(DataComponents.CUSTOM_DATA);
      } else {
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }
   }

   @FunctionalInterface
   private interface TagUpdater {
      void update(CompoundTag tag);
   }
}
