package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ServantCardSkillUtils {
   private ServantCardSkillUtils() {
   }

   public static LivingEntity findLookTarget(ServerPlayer player, double range, double inflate) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(inflate);
      LivingEntity best = null;
      double bestScore = 0.78;
      for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(eye);
         double distance = to.length();
         if (distance <= 0.01 || distance > range) {
            continue;
         }
         double score = look.dot(to.normalize());
         if (score > bestScore) {
            bestScore = score;
            best = living;
         }
      }
      return best;
   }

   public static void hitForwardArc(ServerPlayer player, Vec3 dir, double range, float damage) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
      Vec3 forward = dir.lengthSqr() < 1.0E-4 ? PlayerNoblePhantasmHelper.horizontalLook(player) : new Vec3(dir.x, 0.0, dir.z).normalize();
      AABB box = player.getBoundingBox().inflate(range, 3.0, range);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(origin);
         if (to.lengthSqr() > range * range || new Vec3(to.x, 0.0, to.z).normalize().dot(forward) < 0.45) {
            continue;
         }
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().playerAttack(player), damage);
         target.invulnerableTime = 0;
         target.push(forward.x * 0.75, 0.18, forward.z * 0.75);
         target.hurtMarked = true;
      }
   }

   public static void buffNearby(ServerPlayer player, double radius, MobEffectInstance effect) {
      player.addEffect(new MobEffectInstance(effect));
      if (player.level() instanceof ServerLevel level) {
         for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(radius), p -> p != player)) {
            other.addEffect(new MobEffectInstance(effect));
         }
      }
   }

   public static void spawnLineParticles(ServerLevel level, Vec3 start, Vec3 end, SimpleParticleType particle) {
      Vec3 delta = end.subtract(start);
      double length = delta.length();
      if (length <= 0.01) {
         return;
      }
      Vec3 step = delta.normalize();
      for (double d = 0.0; d <= length; d += 0.45) {
         Vec3 pos = start.add(step.scale(d));
         level.sendParticles(particle, pos.x, pos.y, pos.z, 2, 0.03, 0.03, 0.03, 0.0);
      }
   }

   public static boolean hasTrait(LivingEntity entity, ServantTraitTag trait) {
      return ServantIdentityHelper.hasTrait(entity, trait);
   }

   public static boolean isDayOrBright(ServerPlayer player) {
      return player.level().isDay() || player.level().getMaxLocalRawBrightness(player.blockPosition()) >= 13;
   }

   public static void clearHarmfulEffects(ServerPlayer player) {
      java.util.List<MobEffectInstance> active = java.util.List.copyOf(player.getActiveEffects());
      for (MobEffectInstance effect : active) {
         if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            player.removeEffect(effect.getEffect());
         }
      }
   }

   public static void addOrReplaceMultiplied(AttributeInstance attribute, ResourceLocation id, double value) {
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(id);
      attribute.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
   }

   public static void remove(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }

   public static void markProjectionPair(ItemStack mainStack, ItemStack offStack) {
      PlayerNoblePhantasmHelper.markUbwProjection(mainStack);
      PlayerNoblePhantasmHelper.markUbwProjection(offStack);
   }

   public static boolean trySafeHorizontalTeleport(ServerPlayer player, Vec3 desired) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      Vec3 origin = player.position();
      Vec3 offset = desired.subtract(origin);
      for (double scale : new double[]{1.0, 0.75, 0.5, 0.25}) {
         Vec3 candidate = origin.add(offset.scale(scale));
         BlockPos blockPos = BlockPos.containing(candidate);
         AABB movedBox = player.getBoundingBox().move(candidate.subtract(origin));
         if (level.getWorldBorder().isWithinBounds(blockPos) && level.noCollision(player, movedBox)) {
            player.teleportTo(candidate.x, candidate.y, candidate.z);
            return true;
         }
      }
      return false;
   }
}
