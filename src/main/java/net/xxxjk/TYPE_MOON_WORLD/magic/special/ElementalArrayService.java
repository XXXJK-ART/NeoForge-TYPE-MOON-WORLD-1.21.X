package net.xxxjk.TYPE_MOON_WORLD.magic.special;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class ElementalArrayService {
   private static final String TAG_ACTIVE = "TypeMoonElementalArrayActive";
   private static final String TAG_MAGIC_ID = "TypeMoonElementalArrayMagicId";
   private static final String TAG_MODE = "TypeMoonElementalArrayMode";
   private static final String TAG_TICKS = "TypeMoonElementalArrayTicks";
   private static final String TAG_OWNER = "TypeMoonElementalArrayOwner";
   private static final String TAG_SHIELD_ACTIVE = "TypeMoonElementalArrayShieldActive";
   private static final String TAG_SHIELD_HP = "TypeMoonElementalArrayShieldHp";
   private static final int CHARGE_TICKS = 100;
   private static final float DAMAGE = 30.0F;
   private static final float SHIELD_HP = 500.0F;
   private static final double RANGE = 8.0;
   private static final double WIDTH_PER_BLOCK = 0.35;

   private static final ElementPalette FIRE = new ElementPalette(
      new DustParticleOptions(new Vector3f(1.0F, 0.28F, 0.03F), 1.35F),
      new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.10F), 1.2F),
      new DustParticleOptions(new Vector3f(1.0F, 0.28F, 0.03F), 1.15F),
      new DustParticleOptions(new Vector3f(0.50F, 0.86F, 1.0F), 1.25F),
      new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.10F), 1.2F),
      ModParticles.ELEMENTAL_FLAME.get(),
      ParticleTypes.LAVA,
      ParticleTypes.SMOKE,
      ParticleTypes.END_ROD,
      ParticleTypes.END_ROD
   );
   private static final ElementPalette WATER = new ElementPalette(
      new DustParticleOptions(new Vector3f(0.30F, 0.68F, 1.0F), 1.25F),
      new DustParticleOptions(new Vector3f(0.14F, 0.86F, 1.0F), 1.2F),
      new DustParticleOptions(new Vector3f(0.30F, 0.68F, 1.0F), 1.08F),
      new DustParticleOptions(new Vector3f(0.20F, 0.52F, 1.0F), 1.15F),
      new DustParticleOptions(new Vector3f(0.14F, 0.86F, 1.0F), 1.08F),
      ModParticles.ELEMENTAL_FOAM.get(),
      ModParticles.ELEMENTAL_FOAM.get(),
      ModParticles.ELEMENTAL_FOAM.get(),
      ParticleTypes.END_ROD,
      ParticleTypes.END_ROD
   );
   private static final ElementPalette WIND = new ElementPalette(
      new DustParticleOptions(new Vector3f(0.46F, 0.96F, 0.84F), 1.25F),
      new DustParticleOptions(new Vector3f(0.90F, 0.98F, 1.0F), 1.15F),
      new DustParticleOptions(new Vector3f(0.46F, 0.96F, 0.84F), 1.1F),
      new DustParticleOptions(new Vector3f(0.90F, 0.98F, 1.0F), 1.05F),
      new DustParticleOptions(new Vector3f(0.76F, 1.0F, 0.90F), 1.0F),
      ParticleTypes.CLOUD,
      ParticleTypes.POOF,
      ParticleTypes.WAX_OFF,
      ParticleTypes.END_ROD,
      ParticleTypes.END_ROD
   );
   private static final ElementPalette EARTH = new ElementPalette(
      new DustParticleOptions(new Vector3f(0.60F, 0.44F, 0.24F), 1.25F),
      new DustParticleOptions(new Vector3f(0.72F, 0.60F, 0.36F), 1.15F),
      new DustParticleOptions(new Vector3f(0.60F, 0.44F, 0.24F), 1.1F),
      new DustParticleOptions(new Vector3f(0.72F, 0.60F, 0.36F), 1.05F),
      new DustParticleOptions(new Vector3f(0.80F, 0.72F, 0.52F), 1.0F),
      ParticleTypes.CRIT,
      ParticleTypes.POOF,
      ParticleTypes.SMOKE,
      ParticleTypes.END_ROD,
      ParticleTypes.END_ROD
   );

   private ElementalArrayService() {
   }

   public enum Kind {
      FIRE,
      WATER,
      WIND,
      EARTH;

      public static Kind fromMagicId(String magicId) {
         if (magicId == null) {
            return null;
         }
         return switch (magicId) {
            case "flame_array" -> FIRE;
            case "azure_water_array" -> WATER;
            case "gale_wind_array" -> WIND;
            case "rock_earth_array" -> EARTH;
            default -> null;
         };
      }
   }

   public static int chargeTicks() {
      return CHARGE_TICKS;
   }

   public static float shieldHp() {
      return SHIELD_HP;
   }

   @SubscribeEvent
   public static void onEntityTick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
         return;
      }
      tick(player);
   }

   @SubscribeEvent
   public static void onDamage(LivingIncomingDamageEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) {
         return;
      }
      tryAbsorbShield(player, event);
   }

   public static boolean beginCharge(ServerPlayer player) {
      if (player == null || player.level().isClientSide() || !player.isAlive()) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      String magicId = PlayerMagicSelectionService.getCurrentMagicId(vars);
      Kind kind = Kind.fromMagicId(magicId);
      if (kind == null || !vars.is_magus || !vars.is_magic_circuit_open) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      data.putBoolean(TAG_ACTIVE, true);
      data.putString(TAG_MAGIC_ID, magicId);
      data.putInt(TAG_MODE, Mth.clamp(PlayerMagicSelectionService.getElementMode(vars, magicId), 0, 1));
      data.putInt(TAG_TICKS, 0);
      data.putBoolean(TAG_SHIELD_ACTIVE, false);
      data.putFloat(TAG_SHIELD_HP, 0.0F);
      data.putUUID(TAG_OWNER, player.getUUID());
      MagicProficiencyService.add(vars, magicId, 0.12);
      return true;
   }

   public static boolean releaseCharge(ServerPlayer player, int pressedMs) {
      if (player == null || player.level().isClientSide()) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_ACTIVE)) {
         return false;
      }
      String magicId = data.getString(TAG_MAGIC_ID);
      Kind kind = Kind.fromMagicId(magicId);
      if (kind == null) {
         clear(player);
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int ticks = data.getInt(TAG_TICKS);
      boolean charged = ticks >= CHARGE_TICKS;
      if (!charged) {
         clear(player);
         return false;
      }
      if (data.getInt(TAG_MODE) == 0) {
         releaseAttack(player, kind);
      }
      MagicProficiencyService.add(vars, magicId, 0.18);
      vars.syncMana(player);
      clear(player);
      return true;
   }

   public static void tick(ServerPlayer player) {
      if (player == null || player.level().isClientSide()) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_ACTIVE)) {
         return;
      }
      if (!player.isAlive()) {
         clear(player);
         return;
      }
      String magicId = data.getString(TAG_MAGIC_ID);
      Kind kind = Kind.fromMagicId(magicId);
      if (kind == null) {
         clear(player);
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.is_magus || !vars.is_magic_circuit_open || !PlayerMagicSelectionService.getCurrentMagicId(vars).equals(magicId)) {
         clear(player);
         return;
      }
      if (!drainSustainedMana(player, magicId)) {
         clear(player);
         return;
      }
      int ticks = data.getInt(TAG_TICKS);
      if (ticks < CHARGE_TICKS) {
         if (ticks % 5 == 0 || ticks == CHARGE_TICKS - 1) {
            spawnChargeArray(player, kind, Math.min(1.0F, ticks / (float)CHARGE_TICKS), data.getInt(TAG_MODE) == 1);
         }
         data.putInt(TAG_TICKS, ticks + 1);
         ticks++;
      }
      if (data.getInt(TAG_MODE) == 1) {
         if (!data.getBoolean(TAG_SHIELD_ACTIVE) && ticks >= CHARGE_TICKS) {
            activateShield(player, kind);
         }
         if (data.getBoolean(TAG_SHIELD_ACTIVE)) {
            if (player.tickCount % 4 == 0) {
               spawnShieldArray(player, kind, data.getFloat(TAG_SHIELD_HP) / SHIELD_HP);
            }
            if (data.getFloat(TAG_SHIELD_HP) <= 0.0F) {
               clear(player);
            }
         }
      }
   }

   public static boolean tryAbsorbShield(ServerPlayer player, LivingIncomingDamageEvent event) {
      if (player == null || event == null || !player.isAlive()) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_ACTIVE) || !data.getBoolean(TAG_SHIELD_ACTIVE) || event.getAmount() <= 0.0F) {
         return false;
      }
      if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      Kind kind = Kind.fromMagicId(data.getString(TAG_MAGIC_ID));
      if (kind == null) {
         return false;
      }
      float hp = data.getFloat(TAG_SHIELD_HP);
      if (hp <= 0.0F) {
         clear(player);
         return false;
      }
      float absorbed = Math.min(hp, event.getAmount());
      float remainingDamage = Math.max(0.0F, event.getAmount() - absorbed);
      float remainingShield = hp - absorbed;
      event.setAmount(remainingDamage);
      data.putFloat(TAG_SHIELD_HP, remainingShield);
      spawnShieldHit(player, kind, remainingShield);
      if (remainingShield <= 0.0F) {
         player.stopUsingItem();
         clear(player);
      }
      if (remainingDamage <= 0.0F) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return true;
      }
      return false;
   }

   public static void spawnChargeArray(ServerPlayer player, Kind kind, float progress, boolean shieldMode) {
      if (player.level() instanceof ServerLevel level) {
         spawnArray(level, player, kind, progress, shieldMode);
      }
   }

   public static void spawnShieldArray(ServerPlayer player, Kind kind, float strength) {
      if (player.level() instanceof ServerLevel level) {
         spawnShieldArray(level, player, kind, strength);
      }
   }

   public static void releaseAttack(ServerPlayer player, Kind kind) {
      if (player.level() instanceof ServerLevel level) {
         releaseAttack(level, player, kind, null);
      }
   }

   /** Instantaneous NPC cast used by the magician AI. */
   public static boolean castNpcArray(LivingEntity caster, LivingEntity explicitTarget, Kind kind, CompoundTag payload) {
      if (caster == null || kind == null || !(caster.level() instanceof ServerLevel level) || !caster.isAlive()) {
         return false;
      }
      boolean shieldMode = payload != null && payload.getInt("element_mode") == 1;
      if (shieldMode) {
         spawnShieldArray(level, caster, kind, 1.0F);
         caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 4, false, true, true));
      } else {
         releaseAttack(level, caster, kind, explicitTarget);
      }
      return true;
   }

   public static void spawnShieldHit(ServerPlayer player, Kind kind, float remainingShield) {
      if (player.level() instanceof ServerLevel level) {
         spawnShieldHit(level, player, kind, remainingShield);
      }
   }

   private static void activateShield(ServerPlayer player, Kind kind) {
      CompoundTag data = player.getPersistentData();
      data.putBoolean(TAG_SHIELD_ACTIVE, true);
      data.putFloat(TAG_SHIELD_HP, SHIELD_HP);
      if (player.level() instanceof ServerLevel level) {
         level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.35F);
         spawnShieldArray(level, player, kind, 1.0F);
      }
   }

   private static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(TAG_ACTIVE);
      data.remove(TAG_MAGIC_ID);
      data.remove(TAG_MODE);
      data.remove(TAG_TICKS);
      data.remove(TAG_OWNER);
      data.remove(TAG_SHIELD_ACTIVE);
      data.remove(TAG_SHIELD_HP);
   }

   private static boolean drainSustainedMana(ServerPlayer player, String magicId) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double perTick = Math.max(0.0, MagicDefinitionRegistry.sustainedManaCost(magicId)) / 20.0;
      if (perTick <= 1.0E-6) {
         return true;
      }
      if (vars.player_mana + 1.0E-6 < perTick) {
         return false;
      }
      vars.player_mana = Math.max(0.0, vars.player_mana - perTick);
      vars.syncMana(player);
      return true;
   }

   private static void releaseAttack(ServerLevel level, LivingEntity player, Kind kind, LivingEntity explicitTarget) {
      Vec3 dir = player.getLookAngle().normalize();
      Vec3 start = player.getEyePosition().add(dir.scale(0.7));
      Set<Integer> damaged = new HashSet<>();
      playReleaseSound(level, player, kind);
      spawnReleaseBurst(level, player, kind, start, dir);
      AABB area = player.getBoundingBox().inflate(RANGE, 3.0, RANGE).move(dir.scale(RANGE * 0.45));
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
            e -> isValidTarget(player, e) && (explicitTarget == null || e == explicitTarget))) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         Vec3 toTarget = center.subtract(start);
         double along = toTarget.dot(dir);
         if (along < -0.2 || along > RANGE + 0.8) {
            continue;
         }
         double sideSqr = Math.max(0.0, toTarget.lengthSqr() - along * along);
         double allowed = 0.9 + along * WIDTH_PER_BLOCK;
         if (sideSqr > allowed * allowed || !damaged.add(target.getId())) {
            continue;
         }
         target.hurt(player.damageSources().magic(), DAMAGE);
         applyFinalEffect(target, kind, dir);
      }
   }

   private static void applyFinalEffect(LivingEntity target, Kind kind, Vec3 dir) {
      switch (kind) {
         case FIRE -> {
            target.igniteForSeconds(4.0F);
            target.push(dir.x * 0.32, 0.08, dir.z * 0.32);
         }
         case WATER -> {
            target.clearFire();
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 2, false, true, true));
            target.push(dir.x * 0.22, 0.04, dir.z * 0.22);
         }
         case WIND -> {
            target.push(dir.x * 0.62, 0.28, dir.z * 0.62);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 2, 0, false, true, true));
         }
         case EARTH -> {
            target.setDeltaMovement(target.getDeltaMovement().multiply(0.35, 0.20, 0.35));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 4, 3, false, true, true));
         }
      }
      target.hurtMarked = true;
   }

   private static void spawnReleaseBurst(ServerLevel level, LivingEntity player, Kind kind, Vec3 start, Vec3 dir) {
      ElementPalette palette = palette(kind);
      for (int i = 0; i <= 16; i++) {
         double along = RANGE * i / 16.0;
         Vec3 center = start.add(dir.scale(along));
         double spread = 0.18 + along * WIDTH_PER_BLOCK;
         level.sendParticles(palette.releaseParticles, center.x, center.y, center.z, 12, spread, spread * 0.45, spread, 0.08);
         level.sendParticles(palette.releaseSecondary, center.x, center.y, center.z, 8, spread * 0.8, spread * 0.35, spread * 0.8, 0.015);
         if (i % 3 == 0) {
            level.sendParticles(palette.releaseTertiary, center.x, center.y - 0.15, center.z, 2, spread * 0.35, 0.08, spread * 0.35, 0.0);
         }
      }
   }

   private static void spawnArray(ServerLevel level, LivingEntity caster, Kind kind, float progress, boolean shieldMode) {
      ElementPalette palette = palette(kind);
      Vec3 forward = caster.getLookAngle().normalize();
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(up);
      if (right.lengthSqr() < 1.0E-4) {
         right = new Vec3(1.0, 0.0, 0.0);
      }
      right = right.normalize();
      Vec3 center = caster.getEyePosition().add(forward.scale(0.65)).subtract(0.0, 0.18, 0.0);
      float radius = 0.85F + progress * 1.15F;
      drawVerticalRing(level, center, right, up, radius, palette.primary, 48);
      if (progress > 0.65F) {
         drawVerticalRing(level, center, right, up, radius * 0.68F, shieldMode ? palette.shieldSecondary : palette.secondary, 32);
         drawDiamond(level, center, right, up, radius * 0.88F, shieldMode ? palette.shieldPrimary : palette.finisher);
      }
   }

   private static void spawnShieldArray(ServerLevel level, LivingEntity player, Kind kind, float strength) {
      ElementPalette palette = palette(kind);
      Vec3 forward = player.getLookAngle().normalize();
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(up);
      if (right.lengthSqr() < 1.0E-4) {
         right = new Vec3(1.0, 0.0, 0.0);
      }
      right = right.normalize();
      Vec3 center = player.getEyePosition().add(forward.scale(1.05)).subtract(0.0, 0.18, 0.0);
      float radius = 1.65F;
      drawVerticalRing(level, center, right, up, radius, palette.shieldPrimary, 48);
      drawVerticalRing(level, center, right, up, radius * 0.72F, palette.shieldSecondary, 36);
      drawDiamond(level, center, right, up, radius * 0.92F, palette.shieldFinisher);
      if (strength <= 0.35F) {
         level.sendParticles(palette.finisher, center.x, center.y, center.z, 5, 0.25, 0.25, 0.25, 0.01);
      }
   }

   private static void spawnShieldHit(ServerLevel level, ServerPlayer player, Kind kind, float remainingShield) {
      ElementPalette palette = palette(kind);
      Vec3 center = player.getEyePosition().add(player.getLookAngle().normalize().scale(1.0)).subtract(0.0, 0.18, 0.0);
      level.sendParticles(palette.shieldPrimary, center.x, center.y, center.z, 28, 0.7, 0.7, 0.7, 0.05);
      level.sendParticles(palette.shieldFinisher, center.x, center.y, center.z, 12, 0.45, 0.45, 0.45, 0.04);
      level.playSound(null, player.blockPosition(), remainingShield > 0.0F ? SoundEvents.SHIELD_BLOCK : SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9F, remainingShield > 0.0F ? 1.2F : 0.7F);
   }

   private static void playReleaseSound(ServerLevel level, LivingEntity player, Kind kind) {
      SoundEvent sound = switch (kind) {
         case FIRE -> SoundEvents.BLAZE_SHOOT;
         case WATER -> SoundEvents.BUCKET_FILL;
         case WIND -> SoundEvents.GENERIC_EXPLODE.value();
         case EARTH -> SoundEvents.ANVIL_PLACE;
      };
      level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.05F, 0.78F);
   }

   private static boolean isValidTarget(LivingEntity player, LivingEntity entity) {
      return entity != null && entity.isAlive() && entity != player && !player.isAlliedTo(entity)
         && !entity.isAlliedTo(player) && !EntityUtils.isImmunePlayerTarget(entity);
   }

   private static ElementPalette palette(Kind kind) {
      return switch (kind) {
         case FIRE -> FIRE;
         case WATER -> WATER;
         case WIND -> WIND;
         case EARTH -> EARTH;
      };
   }

   private static void drawVerticalRing(ServerLevel level, Vec3 center, Vec3 right, Vec3 up, float radius, ParticleOptions particle, int points) {
      for (int i = 0; i < points; i++) {
         double angle = Math.PI * 2.0 * i / points;
         Vec3 point = center.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
         level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void drawDiamond(ServerLevel level, Vec3 center, Vec3 right, Vec3 up, float radius, ParticleOptions particle) {
      Vec3[] corners = new Vec3[]{
         center.add(up.scale(radius)),
         center.add(right.scale(radius)),
         center.add(up.scale(-radius)),
         center.add(right.scale(-radius))
      };
      for (int i = 0; i < corners.length; i++) {
         Vec3 from = corners[i];
         Vec3 to = corners[(i + 1) % corners.length];
         for (int step = 0; step <= 10; step++) {
            Vec3 point = from.lerp(to, step / 10.0);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private record ElementPalette(
      ParticleOptions primary,
      ParticleOptions secondary,
      ParticleOptions tertiary,
      ParticleOptions shieldPrimary,
      ParticleOptions shieldSecondary,
      ParticleOptions releaseParticles,
      ParticleOptions releaseSecondary,
      ParticleOptions releaseTertiary,
      ParticleOptions shieldFinisher,
      ParticleOptions finisher
   ) {
   }
}
