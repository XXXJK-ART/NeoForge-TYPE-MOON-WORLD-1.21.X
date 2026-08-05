package net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle.NeroChaosRules;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class NeroChaosBeastLogic {
   public static final String TAG_OWNER = "NeroChaosBeastOwner";
   public static final String TAG_LIFE_DEBITED = "NeroChaosBeastLifeDebited";
   private static final String TAG_REGROUP_UNTIL = "NeroChaosBeastRegroupUntil";
   private static final String TAG_VARIANT = "NeroChaosBeastVariantV1";
   private static final String TAG_NEXT_ATTACK = "NeroChaosBeastNextAttack";

   private NeroChaosBeastLogic() {
   }

   public enum Kind {
      HOUND(80.0, 0.34, 10.0F, 10L, 2.0),
      SERPENT(100.0, 0.28, 12.0F, 18L, 2.2),
      STAG(120.0, 0.24, 25.0F, 28L, 2.7),
      BIRD(80.0, 0.30, 10.0F, 20L, 2.0),
      BEAR(150.0, 0.23, 28.0F, 32L, 3.1),
      CAT(60.0, 0.42, 8.0F, 8L, 1.8),
      BAT(45.0, 0.36, 7.0F, 9L, 1.7);

      private final double baseHealth;
      private final double baseSpeed;
      private final float damage;
      private final long attackCooldown;
      private final double reach;

      Kind(double baseHealth, double baseSpeed, float damage, long attackCooldown, double reach) {
         this.baseHealth = baseHealth;
         this.baseSpeed = baseSpeed;
         this.damage = damage;
         this.attackCooldown = attackCooldown;
         this.reach = reach;
      }
   }

   private enum Variant {
      BALANCED(1.00, 1.00, 1.00, 1.00),
      SWIFT(0.85, 1.24, 0.90, 0.96),
      BRUTAL(1.12, 0.92, 1.30, 1.08),
      TOUGH(1.42, 0.78, 0.95, 1.02),
      FERAL(0.96, 1.10, 1.18, 0.90);

      private final double health;
      private final double speed;
      private final double damage;
      private final double cooldown;

      Variant(double health, double speed, double damage, double cooldown) {
         this.health = health;
         this.speed = speed;
         this.damage = damage;
         this.cooldown = cooldown;
      }
   }

   public static boolean isBeast(LivingEntity entity) {
      return entity instanceof NeroChaosHoundEntity
         || entity instanceof NeroChaosSerpentEntity
         || entity instanceof NeroChaosStagEntity
         || entity instanceof NeroChaosBirdEntity
         || entity instanceof NeroChaosBearEntity
         || entity instanceof NeroChaosCatEntity
         || entity instanceof NeroChaosBatEntity;
   }

   public static Kind kind(Mob beast) {
      if (beast instanceof NeroChaosSerpentEntity) return Kind.SERPENT;
      if (beast instanceof NeroChaosStagEntity) return Kind.STAG;
      if (beast instanceof NeroChaosBirdEntity) return Kind.BIRD;
      if (beast instanceof NeroChaosBearEntity) return Kind.BEAR;
      if (beast instanceof NeroChaosCatEntity) return Kind.CAT;
      if (beast instanceof NeroChaosBatEntity) return Kind.BAT;
      return Kind.HOUND;
   }

   public static void setOwner(Mob beast, NeroChaosEntity owner) {
      if (owner != null) {
         beast.getPersistentData().putUUID(TAG_OWNER, owner.getUUID());
         beast.setPersistenceRequired();
      }
   }

   public static UUID ownerUuid(Entity beast) {
      CompoundTag data = beast.getPersistentData();
      return data.hasUUID(TAG_OWNER) ? data.getUUID(TAG_OWNER) : null;
   }

   public static NeroChaosEntity owner(Mob beast) {
      UUID uuid = ownerUuid(beast);
      if (uuid == null || !(beast.level() instanceof ServerLevel level)) return null;
      return level.getEntity(uuid) instanceof NeroChaosEntity owner ? owner : null;
   }

   public static boolean isAllied(Entity beast, Entity other) {
      NeroChaosEntity owner = beast instanceof Mob mob ? owner(mob) : null;
      return other == owner || isAlliedToOwner(owner, other);
   }

   public static boolean isAlliedToOwner(NeroChaosEntity owner, Entity other) {
      return owner != null && other instanceof NeroChaosBeastEntityMarker marker
         && owner.getUUID().equals(marker.neroChaosOwnerUuid());
   }

   public static boolean tick(Mob beast) {
      NeroChaosEntity owner = owner(beast);
      long now = beast.level().getGameTime();
      if (beast.tickCount % 5 == 0 && beast.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SMOKE, beast.getX(), beast.getY() + beast.getBbHeight() * 0.55,
            beast.getZ(), 2, 0.18, 0.22, 0.18, 0.01);
      }
      if (owner == null) {
         beast.setTarget(null);
         return false;
      }
      ensureVariantStats(beast);
      Kind kind = kind(beast);
      long regroupUntil = Math.max(
         beast.getPersistentData().getLong(TAG_REGROUP_UNTIL),
         owner.getPersistentData().getLong(NeroChaosEntity.TAG_BEAST_REGROUP_UNTIL)
      );
      if (now < regroupUntil) {
         beast.setTarget(null);
         returnToOwner(beast, owner, kind);
         return true;
      }

      LivingEntity target = beast.getTarget();
      if (target == null || !target.isAlive() || isAllied(beast, target)
         || EntityUtils.isImmunePlayerTarget(target)) {
         target = findTarget(beast, owner);
         beast.setTarget(target);
      }

      if (target != null) {
         beast.getLookControl().setLookAt(target, 30.0F, 30.0F);
         double distanceSqr = beast.distanceToSqr(target);
         if (kind == Kind.BIRD || kind == Kind.BAT) {
            flyToward(beast, target, movementSpeed(beast, kind));
         } else if (distanceSqr > kind.reach * kind.reach) {
            beast.getNavigation().moveTo(target, movementSpeed(beast, kind));
         } else {
            beast.getNavigation().stop();
            if (now >= beast.getPersistentData().getLong(TAG_NEXT_ATTACK)) {
               attack(beast, target, kind);
               beast.getPersistentData().putLong(TAG_NEXT_ATTACK, now + attackCooldown(beast, kind));
            }
         }
      } else if (kind == Kind.BIRD || kind == Kind.BAT) {
         if (beast.distanceToSqr(owner) <= 16.0) {
            owner.reabsorbBeast(beast);
            return false;
         }
         flyToward(beast, owner, movementSpeed(beast, kind));
      } else if (beast.distanceToSqr(owner) > 16.0) {
         beast.getNavigation().moveTo(owner, Math.max(1.0, movementSpeed(beast, kind) * 0.92));
      } else {
         owner.reabsorbBeast(beast);
         return false;
      }
      return true;
   }

   public static void onDeath(LivingEntity beast) {
      if (beast.getPersistentData().getBoolean(TAG_LIFE_DEBITED)) return;
      beast.getPersistentData().putBoolean(TAG_LIFE_DEBITED, true);
      if (!(beast instanceof Mob mob)) return;
      NeroChaosEntity owner = owner(mob);
      if (owner != null && owner.isAlive()) {
         owner.queueBeastRevival();
      }
   }

   public static void regroupOwnedBeasts(NeroChaosEntity owner) {
      if (owner == null) return;
      long until = owner.level().getGameTime() + 100L;
      owner.getPersistentData().putLong(NeroChaosEntity.TAG_BEAST_REGROUP_UNTIL, until);
      owner.level().getEntitiesOfClass(
         LivingEntity.class,
         owner.getBoundingBox().inflate(128.0),
         entity -> entity instanceof NeroChaosBeastEntityMarker marker
            && owner.getUUID().equals(marker.neroChaosOwnerUuid())
      ).forEach(entity -> {
         entity.getPersistentData().putLong(TAG_REGROUP_UNTIL, until);
         if (entity instanceof Mob mob) {
            mob.setTarget(null);
            returnToOwner(mob, owner, kind(mob));
         }
      });
   }

   private static LivingEntity findTarget(Mob beast, NeroChaosEntity owner) {
      return beast.level().getEntitiesOfClass(
            LivingEntity.class,
            owner.getBoundingBox().inflate(48.0),
            target -> target != beast && target != owner && target.isAlive()
               && !EntityUtils.isImmunePlayerTarget(target)
               && !isAllied(beast, target)
         ).stream()
         .min((left, right) -> Double.compare(left.distanceToSqr(beast), right.distanceToSqr(beast)))
         .orElse(null);
   }

   private static void attack(Mob beast, LivingEntity target, Kind kind) {
      beast.swing(InteractionHand.MAIN_HAND);
      if (!target.hurt(beast.damageSources().mobAttack(beast), attackDamage(beast, kind))) return;
      if (kind == Kind.SERPENT) {
         target.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0, false, true, true));
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
      } else if (kind == Kind.STAG) {
         Vec3 push = target.position().subtract(beast.position()).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() > 1.0E-6) {
            push = push.normalize().scale(0.75);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.18, push.z));
            target.hurtMarked = true;
         }
      }
   }

   private static void flyToward(Mob beast, LivingEntity target, double speed) {
      Vec3 delta = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(beast.position());
      if (delta.lengthSqr() > 1.0E-6) {
         Vec3 velocity = delta.normalize().scale(0.16 + speed * 0.18);
         beast.setDeltaMovement(velocity);
         beast.hurtMarked = true;
      }
   }

   private static void returnToOwner(Mob beast, NeroChaosEntity owner, Kind kind) {
      double distanceSqr = beast.distanceToSqr(owner);
      beast.getLookControl().setLookAt(owner, 30.0F, 30.0F);
      if (distanceSqr <= 4.0) {
         beast.getNavigation().stop();
         beast.setDeltaMovement(beast.getDeltaMovement().multiply(0.45, 0.8, 0.45));
         return;
      }
      double speed = Math.max(1.0, movementSpeed(beast, kind));
      if (kind == Kind.BIRD || kind == Kind.BAT) {
         flyToward(beast, owner, speed);
      } else {
         beast.getNavigation().moveTo(owner, speed);
      }
   }

   private static void ensureVariantStats(Mob beast) {
      CompoundTag data = beast.getPersistentData();
      if (data.contains(TAG_VARIANT)) return;
      Kind kind = kind(beast);
      Variant variant = Variant.values()[beast.getRandom().nextInt(NeroChaosRules.BEAST_VARIANT_COUNT)];
      double jitter = 0.92 + beast.getRandom().nextDouble() * 0.16;
      setBaseAttribute(beast, Attributes.MAX_HEALTH, kind.baseHealth * variant.health * jitter);
      setBaseAttribute(beast, Attributes.MOVEMENT_SPEED, kind.baseSpeed * variant.speed);
      setBaseAttribute(beast, Attributes.ATTACK_DAMAGE, kind.damage * variant.damage * jitter);
      beast.setHealth(beast.getMaxHealth());
      data.putInt(TAG_VARIANT, variant.ordinal());
   }

   private static Variant variant(Mob beast) {
      int index = NeroChaosRules.clampBeastVariant(beast.getPersistentData().getInt(TAG_VARIANT));
      return Variant.values()[index];
   }

   private static double movementSpeed(Mob beast, Kind kind) {
      double base = switch (kind) {
         case STAG, BEAR -> 1.05;
         case CAT -> 1.38;
         case BAT -> 1.32;
         default -> 1.25;
      };
      return base * variant(beast).speed;
   }

   private static long attackCooldown(Mob beast, Kind kind) {
      return Math.max(4L, Math.round(kind.attackCooldown * variant(beast).cooldown));
   }

   private static float attackDamage(Mob beast, Kind kind) {
      return (float)(kind.damage * variant(beast).damage);
   }

   private static void setBaseAttribute(Mob beast, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
      var instance = beast.getAttribute(attribute);
      if (instance != null) instance.setBaseValue(value);
   }

   /**
    * Small adapter implemented by every beast carrier so ally checks do not
    * depend on a shared vanilla superclass.
    */
   public interface NeroChaosBeastEntityMarker {
      UUID neroChaosOwnerUuid();
   }
}
