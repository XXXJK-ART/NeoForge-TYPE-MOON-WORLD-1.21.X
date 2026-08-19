package net.xxxjk.TYPE_MOON_WORLD.magic.special;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class ArcaneMobilityService {
   private static final String TAG_AERIAL_MODE = "TypeMoonAerialMode";
   private static final String TAG_AERIAL_START_Y = "TypeMoonAerialStartY";
   private static final String TAG_AERIAL_MAX_HEIGHT = "TypeMoonAerialMaxHeight";
   private static final String TAG_AERIAL_TICKS = "TypeMoonAerialTicks";
   private static final String TAG_AERIAL_CASTER = "TypeMoonAerialCaster";
   private static final String TAG_TOUKO_ACTIVE = "TypeMoonToukoActive";
   private static final String TAG_TOUKO_X = "TypeMoonToukoX";
   private static final String TAG_TOUKO_Y = "TypeMoonToukoY";
   private static final String TAG_TOUKO_Z = "TypeMoonToukoZ";
   private static final String TAG_TOUKO_START_X = "TypeMoonToukoStartX";
   private static final String TAG_TOUKO_START_Y = "TypeMoonToukoStartY";
   private static final String TAG_TOUKO_START_Z = "TypeMoonToukoStartZ";
   private static final String TAG_TOUKO_TICKS = "TypeMoonToukoTicks";
   private static final String TAG_TOUKO_DURATION = "TypeMoonToukoDuration";
   private static final String TAG_TOUKO_ARC_HEIGHT = "TypeMoonToukoArcHeight";
   private static final String TAG_TOUKO_OWNER = "TypeMoonToukoOwner";
   private static final String TAG_FLIGHT_ACTIVE = "TypeMoonFlightActive";
   private static final String TAG_FLIGHT_TICKS = "TypeMoonFlightTicks";

   private static final int MODE_NONE = 0;
   private static final int MODE_STASIS = 1;
   private static final int MODE_ASCENT = 2;
   private static final double AERIAL_ASCENT_ACTIVATION_COST = 20.0;
   private static final double FLIGHT_ACTIVATION_COST = 60.0;

   private ArcaneMobilityService() {
   }

   @SubscribeEvent
   public static void onEntityTick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
         return;
      }
      tickAerialState(living);
      tickToukoTravel(living);
      tickFlight(living);
   }

   public static boolean toggleStasis(LivingEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency, CompoundTag payload) {
      LivingEntity target = resolveTarget(caster, payload);
      if (target == null) {
         return false;
      }
      CompoundTag data = target.getPersistentData();
      int mode = data.getInt(TAG_AERIAL_MODE);
      if (mode == MODE_STASIS) {
         clearAerial(target);
         return true;
      }
      if (!isAirborne(target)) {
         return false;
      }
      if (distanceToGround(target, 5) > 5) {
         return false;
      }
      data.putInt(TAG_AERIAL_MODE, MODE_STASIS);
      data.putDouble(TAG_AERIAL_START_Y, target.getY());
      data.putDouble(TAG_AERIAL_MAX_HEIGHT, Math.min(5.0, 1.0 + proficiency / 25.0));
      data.putInt(TAG_AERIAL_TICKS, 0);
      if (caster != null) {
         data.putUUID(TAG_AERIAL_CASTER, caster.getUUID());
      }
      MagicProficiencyService.add(vars, "aerial_stasis", Math.max(0.05, 0.2 - proficiency / 1000.0));
      return true;
   }

   public static boolean toggleAscent(LivingEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency, CompoundTag payload) {
      LivingEntity target = resolveTarget(caster, payload);
      if (target == null) {
         return false;
      }
      CompoundTag data = target.getPersistentData();
      int mode = data.getInt(TAG_AERIAL_MODE);
      if (mode == MODE_ASCENT) {
         data.putInt(TAG_AERIAL_MODE, MODE_STASIS);
         data.putDouble(TAG_AERIAL_START_Y, target.getY());
         data.putDouble(TAG_AERIAL_MAX_HEIGHT, Math.min(5.0, 1.0 + proficiency / 25.0));
         data.putInt(TAG_AERIAL_TICKS, 0);
         enterAerialStasis(target, data, caster);
         return true;
      }
      if (!isAirborne(target) && target != caster) {
         return false;
      }
      if (!consumeActivationMana(caster, vars, AERIAL_ASCENT_ACTIVATION_COST)) {
         return false;
      }
      data.putInt(TAG_AERIAL_MODE, MODE_ASCENT);
      data.putDouble(TAG_AERIAL_START_Y, target.getY());
      data.putDouble(TAG_AERIAL_MAX_HEIGHT, Math.min(5.0, 1.0 + proficiency / 20.0));
      data.putInt(TAG_AERIAL_TICKS, 0);
      if (caster != null) {
         data.putUUID(TAG_AERIAL_CASTER, caster.getUUID());
      }
      MagicProficiencyService.add(vars, "aerial_ascent", Math.max(0.05, 0.25 - proficiency / 800.0));
      MagicProficiencyService.add(vars, "aerial_stasis", Math.max(0.03, 0.12 - proficiency / 1200.0));
      return true;
   }

   public static boolean toggleFlight(LivingEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency) {
      if (!(caster instanceof Player player)) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      boolean active = data.getBoolean(TAG_FLIGHT_ACTIVE);
      if (active) {
         clearFlight(player);
         return true;
      }
      if (!consumeActivationMana(caster, vars, FLIGHT_ACTIVATION_COST)) {
         return false;
      }
      data.putBoolean(TAG_FLIGHT_ACTIVE, true);
      data.putInt(TAG_FLIGHT_TICKS, flightDurationTicks(vars, proficiency));
      player.getAbilities().mayfly = true;
      player.onUpdateAbilities();
      MagicProficiencyService.add(vars, "flight_magic", Math.max(0.05, 0.24 - proficiency / 600.0));
      return true;
   }

   public static boolean startToukoTravel(LivingEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency, CompoundTag payload) {
      if (caster == null || payload == null || !(payload.contains("x") && payload.contains("y") && payload.contains("z"))) {
         if (caster instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.touko_travel.not_configured"), true);
         }
         return false;
      }
      double x = payload.getDouble("x");
      double y = payload.getDouble("y");
      double z = payload.getDouble("z");
      if (caster.distanceToSqr(x, y, z) > 1000.0 * 1000.0) {
         return false;
      }
      CompoundTag data = caster.getPersistentData();
      Vec3 start = toukoBodyCenter(caster);
      double horizontalDistance = Math.sqrt((x - caster.getX()) * (x - caster.getX()) + (z - caster.getZ()) * (z - caster.getZ()));
      double verticalDistance = Math.abs(y - caster.getY());
      int duration = Mth.clamp((int)Math.ceil(Math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance) / 3.15), 8, 360);
      double arcHeight = Mth.clamp(horizontalDistance * 0.12 + verticalDistance * 0.25, 2.4, 28.0);
      data.putBoolean(TAG_TOUKO_ACTIVE, true);
      data.putDouble(TAG_TOUKO_X, x);
      data.putDouble(TAG_TOUKO_Y, y);
      data.putDouble(TAG_TOUKO_Z, z);
      data.putDouble(TAG_TOUKO_START_X, start.x);
      data.putDouble(TAG_TOUKO_START_Y, start.y);
      data.putDouble(TAG_TOUKO_START_Z, start.z);
      data.putInt(TAG_TOUKO_TICKS, 0);
      data.putInt(TAG_TOUKO_DURATION, duration);
      data.putDouble(TAG_TOUKO_ARC_HEIGHT, arcHeight);
      data.putUUID(TAG_TOUKO_OWNER, caster.getUUID());
      if (caster instanceof Player player) {
         player.getAbilities().flying = false;
         player.onUpdateAbilities();
      }
      MagicProficiencyService.add(vars, "touko_travel", Math.max(0.05, 0.18 - proficiency / 700.0));
      return true;
   }

   private static void tickAerialState(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      int mode = data.getInt(TAG_AERIAL_MODE);
      if (mode == MODE_NONE) {
         return;
      }
      if (!living.isAlive()) {
         clearAerial(living);
         return;
      }
      String drainMagicId = mode == MODE_ASCENT ? "aerial_ascent" : "aerial_stasis";
      if (!drainSustainedMana(living, drainMagicId)) {
         clearAerial(living);
         return;
      }
      if (mode == MODE_STASIS) {
         if (living.onGround() || living.isInWaterOrBubble() || living.isSwimming()) {
            clearAerial(living);
            return;
         }
         living.setNoGravity(true);
         living.setDeltaMovement(0.0, Math.max(0.0, living.getDeltaMovement().y), 0.0);
         living.setDeltaMovement(0.0, 0.0, 0.0);
         living.fallDistance = 0.0F;
         living.hurtMarked = true;
         return;
      }
      if (mode == MODE_ASCENT) {
         double startY = data.getDouble(TAG_AERIAL_START_Y);
         double maxHeight = Math.max(1.0, data.getDouble(TAG_AERIAL_MAX_HEIGHT));
         double climbed = living.getY() - startY;
         if (climbed >= maxHeight) {
            enterAerialStasis(living, data, resolveSustainedCaster(living));
            return;
         }
         if (living.horizontalCollision || living.getDeltaMovement().horizontalDistanceSqr() > 0.08) {
            clearAerial(living);
            return;
         }
         double upward = 0.09 + maxHeight * 0.03;
         living.setDeltaMovement(living.getDeltaMovement().x, Math.max(living.getDeltaMovement().y, upward), living.getDeltaMovement().z);
         living.setNoGravity(true);
         living.hurtMarked = true;
      }
   }

   private static void tickFlight(LivingEntity living) {
      if (!(living instanceof Player player)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_FLIGHT_ACTIVE)) {
         if (!player.getAbilities().mayfly && !player.getAbilities().flying) {
            return;
         }
         if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
         }
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double proficiency = MagicProficiencyService.get(vars, "flight_magic");
      boolean equipped = isFlightEquipped(vars);
      boolean infiniteSuper = proficiency >= 75.0 && equipped;
      int ticks = data.getInt(TAG_FLIGHT_TICKS);
      if (!drainSustainedMana(player, "flight_magic")) {
         clearFlight(player);
         return;
      }
      if (!infiniteSuper) {
         if (ticks <= 0) {
            clearFlight(player);
            return;
         }
         data.putInt(TAG_FLIGHT_TICKS, ticks - 1);
      }
      player.getAbilities().mayfly = true;
      player.getAbilities().flying = true;
      player.onUpdateAbilities();
      player.fallDistance = 0.0F;
      if (player.getDeltaMovement().y < 0.0) {
         player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y * 0.35, player.getDeltaMovement().z);
      }
   }

   private static void tickToukoTravel(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      if (!data.getBoolean(TAG_TOUKO_ACTIVE)) {
         return;
      }
      if (!living.isAlive()) {
         clearTouko(living);
         return;
      }
      Vec3 target = new Vec3(data.getDouble(TAG_TOUKO_X), data.getDouble(TAG_TOUKO_Y), data.getDouble(TAG_TOUKO_Z));
      Vec3 targetCenter = target.add(0.0, living.getBbHeight() * 0.5, 0.0);
      Vec3 start = new Vec3(data.getDouble(TAG_TOUKO_START_X), data.getDouble(TAG_TOUKO_START_Y), data.getDouble(TAG_TOUKO_START_Z));
      if (start.lengthSqr() <= 1.0E-6) {
         start = toukoBodyCenter(living);
         data.putDouble(TAG_TOUKO_START_X, start.x);
         data.putDouble(TAG_TOUKO_START_Y, start.y);
         data.putDouble(TAG_TOUKO_START_Z, start.z);
      }
      int ticks = Math.max(0, data.getInt(TAG_TOUKO_TICKS));
      int duration = Math.max(1, data.getInt(TAG_TOUKO_DURATION));
      double t0 = Mth.clamp(ticks / (double)duration, 0.0, 1.0);
      double t1 = Mth.clamp((ticks + 1) / (double)duration, 0.0, 1.0);
      Vec3 current = toukoBodyCenter(living);
      Vec3 expectedCurrent = toukoParabola(start, targetCenter, data.getDouble(TAG_TOUKO_ARC_HEIGHT), t0);
      if (current.distanceToSqr(expectedCurrent) > 4.0) {
         current = expectedCurrent;
      }
      Vec3 next = toukoParabola(start, targetCenter, data.getDouble(TAG_TOUKO_ARC_HEIGHT), t1);
      Vec3 step = next.subtract(current);
      if (t0 >= 1.0 || current.distanceToSqr(targetCenter) <= 0.75 * 0.75) {
         finishTouko(living, true);
         return;
      }
      HitResult blockHit = living.level().clip(new net.minecraft.world.level.ClipContext(
         current, next, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, living
      ));
      if (blockHit.getType() != HitResult.Type.MISS) {
         finishTouko(living, true);
         return;
      }
      AABB box = living.getBoundingBox().expandTowards(step).inflate(0.35);
      EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
         living.level(),
         living,
         current,
         next,
         box,
         candidate -> candidate instanceof LivingEntity && candidate != living && candidate.isAlive()
      );
      if (entityHit != null && entityHit.getEntity() != living) {
         damageTouko(living, entityHit.getEntity());
         return;
      }
      living.setDeltaMovement(step);
      living.hurtMarked = true;
      Vec3 nextFeet = next.subtract(0.0, living.getBbHeight() * 0.5, 0.0);
      AABB nextBox = living.getBoundingBox().move(nextFeet.subtract(living.position())).deflate(0.05);
      if (!living.level().noCollision(living, nextBox)) {
         finishTouko(living, true);
         return;
      }
      living.setPos(nextFeet.x, nextFeet.y, nextFeet.z);
      data.putInt(TAG_TOUKO_TICKS, ticks + 1);
   }

   private static Vec3 toukoBodyCenter(LivingEntity living) {
      return living.position().add(0.0, living.getBbHeight() * 0.5, 0.0);
   }

   private static Vec3 toukoParabola(Vec3 start, Vec3 target, double arcHeight, double progress) {
      double t = Mth.clamp(progress, 0.0, 1.0);
      Vec3 base = start.lerp(target, t);
      return base.add(0.0, Math.max(0.0, arcHeight) * 4.0 * t * (1.0 - t), 0.0);
   }

   private static void damageTouko(LivingEntity caster, Entity hit) {
      if (hit instanceof LivingEntity living) {
         living.hurt(caster.damageSources().magic(), 20.0F);
      }
      finishTouko(caster, true);
   }

   private static void finishTouko(LivingEntity living, boolean damageCaster) {
      if (damageCaster) {
         living.hurt(living.damageSources().magic(), 20.0F);
      }
      clearTouko(living);
   }

   private static void clearAerial(LivingEntity living) {
      living.setNoGravity(false);
      living.getPersistentData().remove(TAG_AERIAL_MODE);
      living.getPersistentData().remove(TAG_AERIAL_START_Y);
      living.getPersistentData().remove(TAG_AERIAL_MAX_HEIGHT);
      living.getPersistentData().remove(TAG_AERIAL_TICKS);
      living.getPersistentData().remove(TAG_AERIAL_CASTER);
   }

   private static void clearTouko(LivingEntity living) {
      living.getPersistentData().remove(TAG_TOUKO_ACTIVE);
      living.getPersistentData().remove(TAG_TOUKO_X);
      living.getPersistentData().remove(TAG_TOUKO_Y);
      living.getPersistentData().remove(TAG_TOUKO_Z);
      living.getPersistentData().remove(TAG_TOUKO_START_X);
      living.getPersistentData().remove(TAG_TOUKO_START_Y);
      living.getPersistentData().remove(TAG_TOUKO_START_Z);
      living.getPersistentData().remove(TAG_TOUKO_TICKS);
      living.getPersistentData().remove(TAG_TOUKO_DURATION);
      living.getPersistentData().remove(TAG_TOUKO_ARC_HEIGHT);
      living.getPersistentData().remove(TAG_TOUKO_OWNER);
   }

   private static void clearFlight(Player player) {
      CompoundTag data = player.getPersistentData();
      data.remove(TAG_FLIGHT_ACTIVE);
      data.remove(TAG_FLIGHT_TICKS);
      if (!player.isCreative() && !player.isSpectator()) {
         player.getAbilities().mayfly = false;
         player.getAbilities().flying = false;
         player.onUpdateAbilities();
      }
   }

   private static boolean drainSustainedMana(LivingEntity stateCarrier, String magicId) {
      double perTick = Math.max(0.0, MagicDefinitionRegistry.sustainedManaCost(magicId)) / 20.0;
      if (perTick <= 1.0E-6) {
         return true;
      }
      LivingEntity caster = resolveSustainedCaster(stateCarrier);
      if (caster == null) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = caster.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.player_mana + 1.0E-6 < perTick) {
         return false;
      }
      vars.player_mana = Math.max(0.0, vars.player_mana - perTick);
      vars.syncMana(caster);
      return true;
   }

   private static boolean consumeActivationMana(LivingEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, double cost) {
      if (cost <= 1.0E-6) {
         return true;
      }
      if (vars == null || vars.player_mana + 1.0E-6 < cost) {
         return false;
      }
      vars.player_mana = Math.max(0.0, vars.player_mana - cost);
      if (caster != null) {
         vars.syncMana(caster);
      }
      return true;
   }

   private static LivingEntity resolveSustainedCaster(LivingEntity stateCarrier) {
      if (stateCarrier == null) {
         return null;
      }
      CompoundTag data = stateCarrier.getPersistentData();
      if (data.hasUUID(TAG_AERIAL_CASTER) && stateCarrier.level() instanceof ServerLevel serverLevel) {
         Entity caster = serverLevel.getEntity(data.getUUID(TAG_AERIAL_CASTER));
         if (caster instanceof LivingEntity living && living.isAlive()) {
            return living;
         }
      }
      return stateCarrier.isAlive() ? stateCarrier : null;
   }

   private static boolean isAirborne(LivingEntity living) {
      return living != null && !living.onGround() && !living.isInWaterOrBubble() && !living.isSwimming();
   }

   private static int distanceToGround(LivingEntity living, int max) {
      BlockPos pos = living.blockPosition();
      if (living.level() == null) {
         return max + 1;
      }
      for (int i = 0; i <= max; i++) {
         if (!living.level().isEmptyBlock(pos.below(i))) {
            return i;
         }
      }
      return max + 1;
   }

   private static LivingEntity resolveTarget(LivingEntity caster, CompoundTag payload) {
      if (caster instanceof ServerPlayer player) {
         HitResult result = EntityUtils.getRayTraceTarget(player, 8.0);
         if (result instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            return living;
         }
      }
      return caster;
   }

   private static boolean isFlightEquipped(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return false;
      }
      if (vars.selected_magics.isEmpty()) {
         return false;
      }
      int index = Mth.clamp(vars.current_magic_index, 0, vars.selected_magics.size() - 1);
      return "flight_magic".equals(vars.selected_magics.get(index));
   }

   private static int flightDurationTicks(TypeMoonWorldModVariables.PlayerVariables vars, double proficiency) {
      double p = Math.max(0.0, Math.min(100.0, proficiency));
      boolean equipped = isFlightEquipped(vars);
      if (p >= 75.0 && equipped) {
         return 20 * 20;
      }
      return 80 + (int)Math.round(p * 3.0);
   }

   private static void enterAerialStasis(LivingEntity living, CompoundTag data, LivingEntity caster) {
      data.putInt(TAG_AERIAL_MODE, MODE_STASIS);
      data.putDouble(TAG_AERIAL_START_Y, living.getY());
      data.putInt(TAG_AERIAL_TICKS, 0);
      if (caster != null) {
         data.putUUID(TAG_AERIAL_CASTER, caster.getUUID());
      }
      living.setNoGravity(true);
      living.setDeltaMovement(Vec3.ZERO);
      living.fallDistance = 0.0F;
      living.hurtMarked = true;
   }
}
