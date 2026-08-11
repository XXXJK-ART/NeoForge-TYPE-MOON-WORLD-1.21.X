package net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;
import org.jetbrains.annotations.Nullable;

public final class DiarmuidCombatHelper {
   public static final int SPEAR_MAX_DURABILITY = 100;
   public static final int MAX_YELLOW_ROSE_STACKS = 5;
   public static final double MAX_HEALTH_REDUCTION_PER_STACK = 0.10;
   public static final float DIRECT_DAMAGE_PER_STACK = 1.0F;
   public static final double LIMB_DISABLE_CHANCE = 0.04;
   public static final String RED_SPEAR_DURABILITY_TAG = "DiarmuidGaeDeargDurability";
   public static final String YELLOW_SPEAR_DURABILITY_TAG = "DiarmuidGaeBuidheDurability";
   public static final String YELLOW_CURSES_TAG = "DiarmuidYellowRoseCurses";
   public static final String CURSE_STACKS_TAG = "DiarmuidYellowRoseStacks";
   public static final String CURSE_OWNER_TAG = "DiarmuidYellowRoseOwner";
   public static final String CURSE_SPEAR_TAG = "DiarmuidYellowRoseSpear";
   public static final String LIMB_LEFT_HAND_TAG = "DiarmuidLimbLeftHand";
   public static final String LIMB_RIGHT_HAND_TAG = "DiarmuidLimbRightHand";
   public static final String LIMB_LEFT_LEG_TAG = "DiarmuidLimbLeftLeg";
   public static final String LIMB_RIGHT_LEG_TAG = "DiarmuidLimbRightLeg";
   public static final String MAGIC_DEFENSE_BROKEN_TAG = "DiarmuidMagicDefenseBrokenUntil";
   private static final ResourceLocation YELLOW_ROSE_HEALTH_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "yellow_rose_max_health");
   private static final ResourceLocation YELLOW_ROSE_LEG_SPEED_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "yellow_rose_limb_speed");

   private DiarmuidCombatHelper() {
   }

   public static int redDurability(DiarmuidUaDuibhneEntity owner) {
      return durability(owner, RED_SPEAR_DURABILITY_TAG);
   }

   public static int yellowDurability(DiarmuidUaDuibhneEntity owner) {
      return durability(owner, YELLOW_SPEAR_DURABILITY_TAG);
   }

   public static void ensureDurability(DiarmuidUaDuibhneEntity owner) {
      CompoundTag data = owner.getPersistentData();
      if (!data.contains(RED_SPEAR_DURABILITY_TAG)) data.putInt(RED_SPEAR_DURABILITY_TAG, SPEAR_MAX_DURABILITY);
      if (!data.contains(YELLOW_SPEAR_DURABILITY_TAG)) data.putInt(YELLOW_SPEAR_DURABILITY_TAG, SPEAR_MAX_DURABILITY);
   }

   public static boolean isRedActive(DiarmuidUaDuibhneEntity owner) {
      return redDurability(owner) > 0;
   }

   public static boolean isYellowActive(DiarmuidUaDuibhneEntity owner) {
      return yellowDurability(owner) > 0;
   }

   public static void applyRedRoseHit(DiarmuidUaDuibhneEntity owner, LivingEntity target) {
      if (owner == null || target == null || !isRedActive(owner)) return;
      consumeDurability(owner, RED_SPEAR_DURABILITY_TAG, 1);
      breakMagicDefense(owner, target);
   }

   public static void applyYellowRoseHit(DiarmuidUaDuibhneEntity owner, LivingEntity target) {
      if (owner == null || target == null || target == owner || !isYellowActive(owner)) return;
      consumeDurability(owner, YELLOW_SPEAR_DURABILITY_TAG, 1);
      applyYellowRoseCurse(owner, target);
      if (target.level().random.nextDouble() < LIMB_DISABLE_CHANCE) {
         applyRandomLimbDisable(target);
      }
   }

   public static void tickCurse(ServerLevel level, LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      if (!data.contains(CURSE_STACKS_TAG)) {
         return;
      }
      int stacks = stacks(target);
      if (stacks <= 0) {
         clearYellowRoseCurse(target);
         return;
      }
      if (!hasLivingActiveOwner(level, data)) {
         clearYellowRoseCurse(target);
         return;
      }
      applyCurseModifiers(target, stacks);
      target.invulnerableTime = 0;
      target.hurt(target.damageSources().magic(), stacks * DIRECT_DAMAGE_PER_STACK);
      target.invulnerableTime = 0;
      level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
         Math.max(1, stacks), 0.2, 0.25, 0.2, 0.02);
   }

   public static void applyCurseModifiers(LivingEntity target, int stacks) {
      int clamped = Math.max(0, Math.min(MAX_YELLOW_ROSE_STACKS, stacks));
      AttributeInstance health = target.getAttribute(Attributes.MAX_HEALTH);
      updateModifier(health, YELLOW_ROSE_HEALTH_ID, -MAX_HEALTH_REDUCTION_PER_STACK * clamped, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
      int disabledLegs = (target.getPersistentData().getBoolean(LIMB_LEFT_LEG_TAG) ? 1 : 0)
         + (target.getPersistentData().getBoolean(LIMB_RIGHT_LEG_TAG) ? 1 : 0);
      double speedPenalty = disabledLegs >= 2 ? -0.95 : disabledLegs == 1 ? -0.50 : 0.0;
      updateModifier(speed, YELLOW_ROSE_LEG_SPEED_ID, speedPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      if (target.getHealth() > target.getMaxHealth()) target.setHealth(target.getMaxHealth());
   }

   public static void clearYellowRoseCurse(LivingEntity target) {
      if (target == null) return;
      CompoundTag data = target.getPersistentData();
      data.remove(CURSE_STACKS_TAG);
      data.remove(CURSE_OWNER_TAG);
      data.remove(CURSE_SPEAR_TAG);
      data.remove(LIMB_LEFT_HAND_TAG);
      data.remove(LIMB_RIGHT_HAND_TAG);
      data.remove(LIMB_LEFT_LEG_TAG);
      data.remove(LIMB_RIGHT_LEG_TAG);
      removeModifier(target.getAttribute(Attributes.MAX_HEALTH), YELLOW_ROSE_HEALTH_ID);
      removeModifier(target.getAttribute(Attributes.MOVEMENT_SPEED), YELLOW_ROSE_LEG_SPEED_ID);
   }

   public static void clearCursesFromOwner(DiarmuidUaDuibhneEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      UUID ownerId = owner.getUUID();
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(128.0),
         entity -> entity.getPersistentData().hasUUID(CURSE_OWNER_TAG)
            && ownerId.equals(entity.getPersistentData().getUUID(CURSE_OWNER_TAG)))) {
         clearYellowRoseCurse(living);
      }
      owner.getPersistentData().putInt(YELLOW_SPEAR_DURABILITY_TAG, 0);
   }

   public static int stacks(LivingEntity target) {
      return Math.max(0, Math.min(MAX_YELLOW_ROSE_STACKS, target.getPersistentData().getInt(CURSE_STACKS_TAG)));
   }

   public static boolean hasDisabledRightHand(LivingEntity target) {
      return target != null && target.getPersistentData().getBoolean(LIMB_RIGHT_HAND_TAG);
   }

   public static boolean hasDisabledLeftHand(LivingEntity target) {
      return target != null && target.getPersistentData().getBoolean(LIMB_LEFT_HAND_TAG);
   }

   public static boolean hasAnyDisabledLimb(LivingEntity target) {
      if (target == null) return false;
      CompoundTag data = target.getPersistentData();
      return data.getBoolean(LIMB_LEFT_HAND_TAG) || data.getBoolean(LIMB_RIGHT_HAND_TAG)
         || data.getBoolean(LIMB_LEFT_LEG_TAG) || data.getBoolean(LIMB_RIGHT_LEG_TAG);
   }

   public static boolean shouldPreferRedRose(LivingEntity target) {
      return target != null && hasMagicDefense(target);
   }

   public static boolean shouldPreferYellowRose(LivingEntity target) {
      return target != null && (target.hasEffect(MobEffects.REGENERATION) || target.getMaxHealth() >= 160.0F || stacks(target) < MAX_YELLOW_ROSE_STACKS);
   }

   public static boolean isDiarmuidSpear(ItemStackAccess stackAccess) {
      return stackAccess != null && (stackAccess.is(ModItems.GAE_DEARG.get()) || stackAccess.is(ModItems.GAE_BUIDHE.get()));
   }

   private static int durability(DiarmuidUaDuibhneEntity owner, String key) {
      if (owner == null) return 0;
      ensureDurability(owner);
      return Math.max(0, Math.min(SPEAR_MAX_DURABILITY, owner.getPersistentData().getInt(key)));
   }

   private static void consumeDurability(DiarmuidUaDuibhneEntity owner, String key, int amount) {
      int next = Math.max(0, durability(owner, key) - Math.max(0, amount));
      owner.getPersistentData().putInt(key, next);
      if (next <= 0 && YELLOW_SPEAR_DURABILITY_TAG.equals(key)) {
         clearCursesFromOwner(owner);
      }
   }

   private static void breakMagicDefense(DiarmuidUaDuibhneEntity owner, LivingEntity target) {
      List<Holder<net.minecraft.world.effect.MobEffect>> remove = new ArrayList<>();
      for (MobEffectInstance effect : target.getActiveEffects()) {
         boolean magicDefense = effect.getEffect() == MobEffects.ABSORPTION
            || effect.getEffect() == MobEffects.DAMAGE_RESISTANCE
            || effect.getEffect() == ModMobEffects.REINFORCEMENT_SELF_DEFENSE
            || effect.getEffect() == ModMobEffects.REINFORCEMENT_OTHER_DEFENSE
            || effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL
               && effect.getEffect().unwrapKey().map(key -> {
                  String path = key.location().getPath();
                  return path.contains("shield") || path.contains("magic_armor") || path.contains("reinforcement");
               }).orElse(false);
         if (magicDefense) remove.add(effect.getEffect());
      }
      for (Holder<net.minecraft.world.effect.MobEffect> effect : remove) target.removeEffect(effect);
      CompoundTag data = target.getPersistentData();
      data.remove("RhoAiasProtected");
      data.remove("UshiwakamaruShield");
      data.remove("UshiwakamaruNpShield");
      data.remove("TypeMoonMagicShield");
      data.remove("TypeMoonMagicArmor");
      data.putLong(MAGIC_DEFENSE_BROKEN_TAG, target.level().getGameTime() + 100L);
      if (target.level() instanceof ServerLevel level) {
         for (RhoAiasEntity shield : level.getEntitiesOfClass(RhoAiasEntity.class, target.getBoundingBox().inflate(8.0),
            shield -> shield.isAlive() && shield.protects(target))) {
            shield.absorb(2000.0F);
         }
         level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
            20, 0.35, 0.35, 0.35, 0.03);
         level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.7F, 1.25F);
      }
   }

   private static void applyYellowRoseCurse(DiarmuidUaDuibhneEntity owner, LivingEntity target) {
      if (target instanceof DiarmuidUaDuibhneEntity) return;
      CompoundTag data = target.getPersistentData();
      data.putUUID(CURSE_OWNER_TAG, owner.getUUID());
      data.putUUID(CURSE_SPEAR_TAG, owner.getUUID());
      int next = Math.min(MAX_YELLOW_ROSE_STACKS, data.getInt(CURSE_STACKS_TAG) + 1);
      data.putInt(CURSE_STACKS_TAG, next);
      applyCurseModifiers(target, next);
      if (target.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            10, 0.25, 0.25, 0.25, 0.015);
      }
   }

   private static void applyRandomLimbDisable(LivingEntity target) {
      int limb = target.getRandom().nextInt(4);
      CompoundTag data = target.getPersistentData();
      switch (limb) {
         case 0 -> data.putBoolean(LIMB_LEFT_HAND_TAG, true);
         case 1 -> data.putBoolean(LIMB_RIGHT_HAND_TAG, true);
         case 2 -> data.putBoolean(LIMB_LEFT_LEG_TAG, true);
         default -> data.putBoolean(LIMB_RIGHT_LEG_TAG, true);
      }
      applyCurseModifiers(target, stacks(target));
   }

   private static boolean hasLivingActiveOwner(ServerLevel level, CompoundTag data) {
      if (!data.hasUUID(CURSE_OWNER_TAG)) return false;
      Entity entity = level.getEntity(data.getUUID(CURSE_OWNER_TAG));
      return entity instanceof DiarmuidUaDuibhneEntity owner && owner.isAlive() && isYellowActive(owner);
   }

   private static boolean hasMagicDefense(LivingEntity target) {
      if (target.hasEffect(MobEffects.ABSORPTION) || target.hasEffect(MobEffects.DAMAGE_RESISTANCE)
         || target.hasEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE)
         || target.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE)) {
         return true;
      }
      if (target.level() instanceof ServerLevel level) {
         return !level.getEntitiesOfClass(RhoAiasEntity.class, target.getBoundingBox().inflate(8.0),
            shield -> shield.isAlive() && shield.protects(target)).isEmpty();
      }
      return false;
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) return;
      AttributeModifier existing = attribute.getModifier(id);
      if (Math.abs(amount) < 1.0E-6) {
         if (existing != null) attribute.removeModifier(id);
         return;
      }
      if (existing != null && Math.abs(existing.amount() - amount) < 1.0E-6 && existing.operation() == operation) return;
      if (existing != null) attribute.removeModifier(id);
      attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null && attribute.getModifier(id) != null) attribute.removeModifier(id);
   }

   public interface ItemStackAccess {
      boolean is(net.minecraft.world.item.Item item);
   }
}
