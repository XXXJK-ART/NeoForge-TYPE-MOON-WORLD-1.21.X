package net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDiarmuidSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

public final class DiarmuidCombatHelper {
   public static final int SPEAR_MAX_DURABILITY = 200;
   public static final int MAX_YELLOW_ROSE_STACKS = 5;
   public static final int YELLOW_ROSE_DAMAGE_INTERVAL_TICKS = 5 * 20;
   public static final double MAX_HEALTH_REDUCTION_PER_STACK = 0.10;
   public static final float DIRECT_DAMAGE_PER_STACK = 1.0F;
   public static final double LIMB_DISABLE_CHANCE = 0.10;
   public static final double FULL_STACK_LIMB_DISABLE_CHANCE = 0.50;
   public static final String RED_SPEAR_DURABILITY_TAG = "DiarmuidGaeDeargDurability";
   public static final String YELLOW_SPEAR_DURABILITY_TAG = "DiarmuidGaeBuidheDurability";
   public static final String YELLOW_CURSES_TAG = "DiarmuidYellowRoseCurses";
   public static final String CURSE_STACKS_TAG = "DiarmuidYellowRoseStacks";
   public static final String CURSE_OWNER_TAG = "DiarmuidYellowRoseOwner";
   public static final String CURSE_SPEAR_TAG = "DiarmuidYellowRoseSpear";
   public static final String CURSE_NEXT_DAMAGE_TICK_TAG = "DiarmuidYellowRoseNextDamageTick";
   public static final String LIMB_LEFT_HAND_TAG = "DiarmuidLimbLeftHand";
   public static final String LIMB_RIGHT_HAND_TAG = "DiarmuidLimbRightHand";
   public static final String LIMB_LEFT_LEG_TAG = "DiarmuidLimbLeftLeg";
   public static final String LIMB_RIGHT_LEG_TAG = "DiarmuidLimbRightLeg";
   public static final String MAGIC_DEFENSE_BROKEN_TAG = "DiarmuidMagicDefenseBrokenUntil";
   private static final ResourceLocation YELLOW_ROSE_HEALTH_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "yellow_rose_max_health");
   private static final ResourceLocation YELLOW_ROSE_LEG_SPEED_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "yellow_rose_limb_speed");
   private static final DustParticleOptions RED_ROSE_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.05F, 0.04F), 1.25F);
   private static final DustParticleOptions RED_ROSE_GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.18F), 1.05F);
   private static final DustParticleOptions YELLOW_ROSE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.86F, 0.12F), 1.15F);
   private static final DustParticleOptions YELLOW_ROSE_CURSE = new DustParticleOptions(new Vector3f(0.78F, 0.42F, 0.02F), 0.9F);
   private static final DustParticleOptions KNIGHT_DUST = new DustParticleOptions(new Vector3f(0.65F, 0.9F, 1.0F), 0.85F);

   private DiarmuidCombatHelper() {
   }

   public static int redDurability(LivingEntity owner) {
      return durability(owner, RED_SPEAR_DURABILITY_TAG);
   }

   public static int yellowDurability(LivingEntity owner) {
      return durability(owner, YELLOW_SPEAR_DURABILITY_TAG);
   }

   public static void ensureDurability(LivingEntity owner) {
      CompoundTag data = owner.getPersistentData();
      if (!data.contains(RED_SPEAR_DURABILITY_TAG)) data.putInt(RED_SPEAR_DURABILITY_TAG, SPEAR_MAX_DURABILITY);
      if (!data.contains(YELLOW_SPEAR_DURABILITY_TAG)) data.putInt(YELLOW_SPEAR_DURABILITY_TAG, SPEAR_MAX_DURABILITY);
   }

   public static ItemStack createSpearStack(DiarmuidSpearItem.SpearType type, int remaining) {
      ItemStack stack = new ItemStack(type == DiarmuidSpearItem.SpearType.GAE_DEARG ? ModItems.GAE_DEARG.get() : ModItems.GAE_BUIDHE.get());
      stack.setDamageValue(Math.max(0, SPEAR_MAX_DURABILITY - Math.min(SPEAR_MAX_DURABILITY, Math.max(0, remaining))));
      return stack;
   }

   public static void syncSpearItemToOwner(LivingEntity owner, DiarmuidSpearItem.SpearType type) {
      if (owner == null) return;
      ItemStack stack = findSpear(owner, type);
      if (stack.isEmpty()) return;
      stack.setDamageValue(Math.max(0, SPEAR_MAX_DURABILITY - (type == DiarmuidSpearItem.SpearType.GAE_DEARG
         ? redDurability(owner) : yellowDurability(owner))));
   }

   public static boolean isRedActive(LivingEntity owner) {
      return redDurability(owner) > 0;
   }

   public static boolean isYellowActive(LivingEntity owner) {
      return yellowDurability(owner) > 0;
   }

   public static void applyRedRoseHit(LivingEntity owner, LivingEntity target) {
      if (owner == null || target == null || !isRedActive(owner)) return;
      consumeDurability(owner, RED_SPEAR_DURABILITY_TAG, 1);
      spawnRedRoseHitVfx(owner, target);
      breakMagicDefense(owner, target);
   }

   public static void applyYellowRoseHit(LivingEntity owner, LivingEntity target) {
      if (owner == null || target == null || target == owner || !isYellowActive(owner)) return;
      consumeDurability(owner, YELLOW_SPEAR_DURABILITY_TAG, 1);
      int stacks = applyYellowRoseCurse(owner, target);
      if (target.level().random.nextDouble() < limbDisableChance(stacks)) {
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
      long now = level.getGameTime();
      long nextDamageTick = data.getLong(CURSE_NEXT_DAMAGE_TICK_TAG);
      if (nextDamageTick <= 0L) {
         data.putLong(CURSE_NEXT_DAMAGE_TICK_TAG, now + YELLOW_ROSE_DAMAGE_INTERVAL_TICKS);
         return;
      }
      if (now < nextDamageTick) {
         return;
      }
      data.putLong(CURSE_NEXT_DAMAGE_TICK_TAG, now + YELLOW_ROSE_DAMAGE_INTERVAL_TICKS);
      target.invulnerableTime = 0;
      target.hurt(target.damageSources().magic(), stacks * DIRECT_DAMAGE_PER_STACK);
      target.invulnerableTime = 0;
      spawnCurseTickVfx(level, target, stacks);
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
      data.remove(CURSE_NEXT_DAMAGE_TICK_TAG);
      data.remove(LIMB_LEFT_HAND_TAG);
      data.remove(LIMB_RIGHT_HAND_TAG);
      data.remove(LIMB_LEFT_LEG_TAG);
      data.remove(LIMB_RIGHT_LEG_TAG);
      removeModifier(target.getAttribute(Attributes.MAX_HEALTH), YELLOW_ROSE_HEALTH_ID);
      removeModifier(target.getAttribute(Attributes.MOVEMENT_SPEED), YELLOW_ROSE_LEG_SPEED_ID);
   }

   public static void clearCursesFromOwner(LivingEntity owner) {
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

   private static int durability(LivingEntity owner, String key) {
      if (owner == null) return 0;
      ensureDurability(owner);
      return Math.max(0, Math.min(SPEAR_MAX_DURABILITY, owner.getPersistentData().getInt(key)));
   }

   private static void consumeDurability(LivingEntity owner, String key, int amount) {
      int next = Math.max(0, durability(owner, key) - Math.max(0, amount));
      owner.getPersistentData().putInt(key, next);
      if (next <= 0) {
         spawnSpearBreakVfx(owner, YELLOW_SPEAR_DURABILITY_TAG.equals(key));
      }
      if (next <= 0 && YELLOW_SPEAR_DURABILITY_TAG.equals(key)) {
         clearCursesFromOwner(owner);
      }
   }

   private static ItemStack findSpear(LivingEntity owner, DiarmuidSpearItem.SpearType type) {
      ItemStack main = owner.getItemInHand(InteractionHand.MAIN_HAND);
      if (main.getItem() instanceof DiarmuidSpearItem spear && spear.spearType() == type) return main;
      ItemStack off = owner.getItemInHand(InteractionHand.OFF_HAND);
      if (off.getItem() instanceof DiarmuidSpearItem spear && spear.spearType() == type) return off;
      return ItemStack.EMPTY;
   }

   private static void breakMagicDefense(LivingEntity owner, LivingEntity target) {
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
            16, 0.35, 0.35, 0.35, 0.03);
         level.sendParticles(RED_ROSE_GOLD, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
            18, 0.32, 0.38, 0.32, 0.02);
         level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.56, target.getZ(),
            8, 0.24, 0.24, 0.24, 0.04);
         level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.7F, 1.25F);
      }
   }

   private static int applyYellowRoseCurse(LivingEntity owner, LivingEntity target) {
      if (target instanceof DiarmuidUaDuibhneEntity) return stacks(target);
      CompoundTag data = target.getPersistentData();
      data.putUUID(CURSE_OWNER_TAG, owner.getUUID());
      data.putUUID(CURSE_SPEAR_TAG, owner.getUUID());
      int next = Math.min(MAX_YELLOW_ROSE_STACKS, data.getInt(CURSE_STACKS_TAG) + 1);
      data.putInt(CURSE_STACKS_TAG, next);
      if (!data.contains(CURSE_NEXT_DAMAGE_TICK_TAG) || data.getLong(CURSE_NEXT_DAMAGE_TICK_TAG) <= owner.level().getGameTime()) {
         data.putLong(CURSE_NEXT_DAMAGE_TICK_TAG, owner.level().getGameTime() + YELLOW_ROSE_DAMAGE_INTERVAL_TICKS);
      }
      applyCurseModifiers(target, next);
      if (target.level() instanceof ServerLevel level) {
         level.sendParticles(YELLOW_ROSE_DUST, target.getX(), target.getY() + target.getBbHeight() * 0.54, target.getZ(),
            16, 0.28, 0.28, 0.28, 0.018);
         level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + target.getBbHeight() * 0.54, target.getZ(),
            8, 0.22, 0.22, 0.22, 0.03);
         level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + target.getBbHeight() * 0.64, target.getZ(),
            Math.min(8, 2 + next), 0.2, 0.28, 0.2, 0.01);
      }
      return next;
   }

   private static double limbDisableChance(int stacks) {
      return stacks >= MAX_YELLOW_ROSE_STACKS ? FULL_STACK_LIMB_DISABLE_CHANCE : LIMB_DISABLE_CHANCE;
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
      if (target.level() instanceof ServerLevel level) {
         level.sendParticles(YELLOW_ROSE_CURSE, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            24, 0.34, 0.42, 0.34, 0.035);
         level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            10, 0.2, 0.2, 0.2, 0.0);
         level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 0.55F, 1.45F);
      }
   }

   public static void spawnStrategyVfx(DiarmuidUaDuibhneEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      double y = entity.getY() + 0.12;
      level.sendParticles(KNIGHT_DUST, entity.getX(), y, entity.getZ(), 22, 0.65, 0.05, 0.65, 0.03);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.62, entity.getZ(), 14, 0.32, 0.42, 0.32, 0.02);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.HOSTILE, 0.45F, 1.6F);
   }

   public static void spawnJumpBurstVfx(DiarmuidUaDuibhneEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.08, entity.getZ(), 18, 0.42, 0.06, 0.42, 0.055);
      level.sendParticles(KNIGHT_DUST, entity.getX(), entity.getY() + 0.16, entity.getZ(), 14, 0.36, 0.08, 0.36, 0.045);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.45F, 1.25F);
   }

   public static void spawnRepositionVfx(DiarmuidUaDuibhneEntity entity, boolean urgent) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      int count = urgent ? 14 : 8;
      level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.08, entity.getZ(), count, 0.22, 0.04, 0.22, urgent ? 0.045 : 0.025);
      level.sendParticles(KNIGHT_DUST, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), urgent ? 10 : 5, 0.2, 0.24, 0.2, 0.02);
   }

   public static void spawnCombatAura(DiarmuidUaDuibhneEntity entity, boolean red) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      var dust = red ? RED_ROSE_DUST : YELLOW_ROSE_DUST;
      var look = entity.getLookAngle();
      double x = entity.getX() + look.x * 0.55;
      double y = entity.getY() + entity.getBbHeight() * 0.62;
      double z = entity.getZ() + look.z * 0.55;
      level.sendParticles(dust, x, y, z, 3, 0.1, 0.12, 0.1, 0.0);
      if (red) {
         level.sendParticles(ParticleTypes.CRIT, x, y, z, 1, 0.08, 0.08, 0.08, 0.01);
      } else {
         level.sendParticles(ParticleTypes.WITCH, x, y, z, 1, 0.06, 0.08, 0.06, 0.0);
      }
   }

   private static void spawnRedRoseHitVfx(LivingEntity owner, LivingEntity target) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      level.sendParticles(RED_ROSE_DUST, target.getX(), target.getY() + target.getBbHeight() * 0.56, target.getZ(),
         18, 0.24, 0.28, 0.24, 0.025);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.52, target.getZ(),
         1, 0.0, 0.0, 0.0, 0.0);
   }

   private static void spawnCurseTickVfx(ServerLevel level, LivingEntity target, int stacks) {
      level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
         Math.max(1, stacks), 0.2, 0.25, 0.2, 0.02);
      level.sendParticles(YELLOW_ROSE_CURSE, target.getX(), target.getY() + target.getBbHeight() * 0.58, target.getZ(),
         Math.min(10, 2 + stacks * 2), 0.18, 0.22, 0.18, 0.01);
   }

   private static void spawnSpearBreakVfx(LivingEntity owner, boolean yellow) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      var dust = yellow ? YELLOW_ROSE_DUST : RED_ROSE_DUST;
      level.sendParticles(dust, owner.getX(), owner.getY() + owner.getBbHeight() * 0.6, owner.getZ(),
         28, 0.32, 0.38, 0.32, 0.06);
      level.sendParticles(ParticleTypes.SMOKE, owner.getX(), owner.getY() + owner.getBbHeight() * 0.55, owner.getZ(),
         14, 0.24, 0.24, 0.24, 0.035);
      level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 0.75F, yellow ? 0.9F : 1.1F);
   }

   private static boolean hasLivingActiveOwner(ServerLevel level, CompoundTag data) {
      if (!data.hasUUID(CURSE_OWNER_TAG)) return false;
      Entity entity = level.getEntity(data.getUUID(CURSE_OWNER_TAG));
      if (entity instanceof DiarmuidUaDuibhneEntity owner) {
         return owner.isAlive() && isYellowActive(owner);
      }
      if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
         return player.isAlive() && ServantCardDiarmuidSkills.isActiveCard(player) && isYellowActive(player);
      }
      return false;
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
