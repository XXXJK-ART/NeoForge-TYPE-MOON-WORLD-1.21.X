package net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class ShadowHassanRules {
   public static final TagKey<MobEffect> REVEALING_EFFECTS = TagKey.create(Registries.MOB_EFFECT,
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "presence_concealment_revealing"));
   public static final int CONCEALMENT_EXPOSURE_TICKS = 60;
   public static final int SHADOW_STEP_COOLDOWN_TICKS = 40;
   public static final int MANA_RESTORE_INTERVAL_TICKS = 20;
   public static final double MANA_RESTORE_PER_SECOND = 5.0;
   public static final double NOBLE_PHANTASM_HEALTH_RATIO = 0.60;
   public static final double DEATH_SHADOW_SPEED_PER_TICK = 0.70;
   public static final double DEATH_SHADOW_CONTACT_DISTANCE = 1.0;
   public static final double MAX_MELEE_DISTANCE = 6.0;

   private ShadowHassanRules() {
   }

   public static boolean isShadowLight(int brightness) {
      return brightness >= 1 && brightness <= 11;
   }

   public static boolean isTotalDarkness(int brightness) {
      return brightness <= 0;
   }

   public static boolean isSunlightShadow(boolean daytime, boolean skyVisible, boolean raining, int skyLight) {
      return daytime && skyVisible && !raining && skyLight > 0;
   }

   public static boolean castsSunlightShadow(ServerLevel level, LivingEntity entity) {
      if (level == null || entity == null || !entity.isAlive()) return false;
      BlockPos exposedPos = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
      return isSunlightShadow(level.isDay(), level.canSeeSky(exposedPos), level.isRainingAt(exposedPos),
         level.getBrightness(LightLayer.SKY, exposedPos));
   }

   public static boolean shouldTriggerNoblePhantasm(float health, float maxHealth) {
      return maxHealth > 0.0F && health > 0.0F && health <= maxHealth * NOBLE_PHANTASM_HEALTH_RATIO;
   }

   public static Vec3 advanceDeathShadow(Vec3 position, Vec3 destination) {
      Vec3 delta = destination.subtract(position);
      double distance = delta.length();
      if (distance <= DEATH_SHADOW_SPEED_PER_TICK || distance == 0.0) return destination;
      return position.add(delta.scale(DEATH_SHADOW_SPEED_PER_TICK / distance));
   }
}
