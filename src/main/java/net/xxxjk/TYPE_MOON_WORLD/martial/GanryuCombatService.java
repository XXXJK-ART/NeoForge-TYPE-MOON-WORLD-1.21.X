package net.xxxjk.TYPE_MOON_WORLD.martial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.advancement.TypeMoonAdvancementHelper;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class GanryuCombatService {
   public static final String MAGIC_ID = "ganryu";
   public static final int INPUT_A = 0;
   public static final int INPUT_B_START = 1;
   public static final int INPUT_B_END = 2;
   public static final int INPUT_JUMP = 3;
   public static final int INPUT_DOWN = 4;
   public static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM,
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "ganryu_weapons"));
   public static final ResourceKey<DamageType> TSUBAME_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "ganryu_tsubame"));

   private static final ResourceLocation STANCE_SLOW_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "ganryu_stance_slow");
   private static final String TAG_RECOVERY = "TypeMoonGanryuRecoveryUntil";
   private static final String TAG_STANCE_START = "TypeMoonGanryuStanceStart";
   private static final String TAG_STANCE_STAGE = "TypeMoonGanryuStanceStage";
   private static final String TAG_STANCE_POWER = "TypeMoonGanryuStancePower";
   private static final String TAG_STANCE_CHAIN = "TypeMoonGanryuStanceChainUntil";
   private static final String TAG_A_COUNT = "TypeMoonGanryuACount";
   private static final String TAG_LAST_SEQUENCE = "TypeMoonGanryuLastSequence";
   private static final String TAG_DOWN_UNTIL = "TypeMoonGanryuDownUntil";
   private static final String TAG_MARTIAL_DAMAGE = "TypeMoonGanryuDamage";
   private static final String TAG_BASIC_ATTACK = "TypeMoonGanryuBasicAttack";
   private static final String TAG_PENDING_TARGET = "TypeMoonGanryuPendingTarget";
   private static final String TAG_PENDING_STRIKES = "TypeMoonGanryuPendingStrikes";
   private static final String TAG_PENDING_NEXT = "TypeMoonGanryuPendingNext";
   private static final String TAG_PENDING_DAMAGE = "TypeMoonGanryuPendingDamage";
   private static final String TAG_PENDING_AWARDED = "TypeMoonGanryuPendingAwarded";
   private static final int SEQUENCE_WINDOW = 30;
   private static final int STANCE_CHARGE_TICKS = 60;

   private GanryuCombatService() {}

   public static boolean isAllowedBlade(ItemStack stack) {
      return stack != null && !stack.isEmpty() && (stack.is(WEAPONS)
         || stack.is(ModItems.WAKIZASHI.get()) || stack.is(ModItems.KATANA.get()) || stack.is(ModItems.NODACHI.get())
         || stack.is(ModItems.BIZEN_NAGAMITSU.get()));
   }

   public static ItemStack activeBlade(ServerPlayer player) {
      if (isAllowedBlade(player.getMainHandItem())) return player.getMainHandItem();
      return isAllowedBlade(player.getOffhandItem()) ? player.getOffhandItem() : ItemStack.EMPTY;
   }

   public static boolean hasValidHands(ServerPlayer player) {
      ItemStack main = player.getMainHandItem();
      ItemStack off = player.getOffhandItem();
      return isValidHandCombination(main.isEmpty(), isAllowedBlade(main), off.isEmpty(), isAllowedBlade(off));
   }

   static boolean isValidHandCombination(boolean mainEmpty, boolean mainAllowed, boolean offEmpty, boolean offAllowed) {
      return (mainAllowed || offAllowed) && (mainEmpty || mainAllowed) && (offEmpty || offAllowed);
   }

   public static boolean isActive(ServerPlayer player) {
      if (player == null) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.ganryu_learned && vars.is_magic_circuit_open && !vars.servant_card_transformed
         && hasValidHands(player) && PlayerMagicSelectionService.isCurrentSelection(vars, MAGIC_ID);
   }

   public static boolean isReadyWithInvitation(ServerPlayer player) {
      ItemStack main = player.getMainHandItem();
      ItemStack off = player.getOffhandItem();
      boolean invitationAndBlade = main.is(ModItems.SPARRING_INVITATION.get()) && isAllowedBlade(off)
         || off.is(ModItems.SPARRING_INVITATION.get()) && isAllowedBlade(main);
      if (!invitationAndBlade) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.ganryu_learned && vars.is_magic_circuit_open && !vars.servant_card_transformed
         && PlayerMagicSelectionService.isCurrentSelection(vars, MAGIC_ID);
   }

   public static boolean learn(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.ganryu_learned) return false;
      vars.ganryu_learned = true;
      if (!vars.learned_magics.contains(MAGIC_ID)) vars.learned_magics.add(MAGIC_ID);
      vars.syncPlayerVariables(player);
      TypeMoonAdvancementHelper.grant(player, TypeMoonAdvancementHelper.GANRYU);
      player.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.learned"), false);
      return true;
   }

   public static void forget(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.ganryu_learned = false;
      vars.ganryu_proficiency = 0.0;
      vars.ganryu_tsubame_unlocked = false;
      vars.learned_magics.remove(MAGIC_ID);
      vars.rebuildSelectedMagicsFromActiveWheel();
      clearRuntime(player);
      vars.syncPlayerVariables(player);
   }

   public static boolean isUnlocked(TypeMoonWorldModVariables.PlayerVariables vars, GanryuMove move) {
      if (vars == null) return false;
      if (move == GanryuMove.UKEMI) return vars.ganryu_learned && MartialUkemiService.isLearned(vars);
      return isUnlocked(vars.ganryu_learned, vars.ganryu_proficiency, vars.ganryu_tsubame_unlocked, move);
   }

   static boolean isUnlocked(boolean learned, double proficiency, boolean tsubameUnlocked, GanryuMove move) {
      return learned && move != null && proficiency + 1.0E-6 >= move.requiredProficiency()
         && (move != GanryuMove.TSUBAME_GAESHI || tsubameUnlocked);
   }

   public static void handleInput(ServerPlayer player, int input, boolean down, boolean up) {
      if (input == INPUT_B_END) {
         endStance(player);
         return;
      }
      if (!isActive(player)) {
         clearRuntime(player);
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (player.hasEffect(ModMobEffects.STAGGER) || player.hasEffect(ModMobEffects.OFF_BALANCE)) return;
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (input == INPUT_DOWN) {
         data.putLong(TAG_DOWN_UNTIL, now + 8L);
         return;
      }
      if (input == INPUT_JUMP) {
         if ((down || data.getLong(TAG_DOWN_UNTIL) >= now) && now >= data.getLong(TAG_RECOVERY)) {
            perform(player, vars, GanryuMove.HIGH_JUMP, 0.0F);
         }
         return;
      }
      if (input == INPUT_B_START) {
         if (MartialUkemiService.tryUse(player, true)) return;
         if (now < data.getLong(TAG_RECOVERY)) return;
         if (!isUnlocked(vars, GanryuMove.STANCE)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.move_locked"), true);
            return;
         }
         beginStance(player, now);
         return;
      }
      if (input != INPUT_A || now < data.getLong(TAG_RECOVERY)) return;
      if (isInStance(player)) {
         handleStanceAttack(player, vars, down, now);
         return;
      }
      boolean sequenceExpired = now - data.getLong(TAG_LAST_SEQUENCE) > SEQUENCE_WINDOW;
      if (sequenceExpired) {
         data.putInt(TAG_A_COUNT, 0);
      }
      GanryuMove move;
      if (up && !player.onGround()) {
         move = GanryuMove.STONE_FLOWER_SECOND;
      } else {
         int stage = nextComboStage(data.getInt(TAG_A_COUNT), down, sequenceExpired);
         data.putInt(TAG_A_COUNT, stage);
         move = comboMove(stage, down);
      }
      if (!isUnlocked(vars, move)) {
         if (usesBasicAttack(vars.ganryu_proficiency)) {
            performBasicAttack(player);
            return;
         }
         player.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.move_locked"), true);
         return;
      }
      data.putLong(TAG_LAST_SEQUENCE, now);
      perform(player, vars, move, 0.0F);
   }

   private static void performBasicAttack(ServerPlayer player) {
      LivingEntity target = findTarget(player, GanryuMove.STONE_FLOWER.range());
      if (target == null) return;
      CompoundTag data = player.getPersistentData();
      boolean useOffhandBlade = player.getMainHandItem().isEmpty() && isAllowedBlade(player.getOffhandItem());
      ItemStack offhandBlade = useOffhandBlade ? player.getOffhandItem() : ItemStack.EMPTY;
      List<AppliedAttributeModifier> temporaryModifiers = useOffhandBlade
         ? applyMainHandModifiers(player, offhandBlade)
         : List.of();
      if (useOffhandBlade) {
         player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
         player.setItemInHand(InteractionHand.MAIN_HAND, offhandBlade);
      }
      data.putBoolean(TAG_BASIC_ATTACK, true);
      data.putBoolean(TAG_MARTIAL_DAMAGE, true);
      try {
         player.attack(target);
      } finally {
         data.remove(TAG_BASIC_ATTACK);
         data.remove(TAG_MARTIAL_DAMAGE);
         if (useOffhandBlade) {
            ItemStack attackedBlade = player.getMainHandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, attackedBlade);
            temporaryModifiers.forEach(AppliedAttributeModifier::remove);
         }
      }
   }

   private static List<AppliedAttributeModifier> applyMainHandModifiers(ServerPlayer player, ItemStack blade) {
      List<AppliedAttributeModifier> applied = new ArrayList<>();
      blade.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
         AttributeInstance instance = player.getAttribute(attribute);
         if (instance != null && instance.getModifier(modifier.id()) == null) {
            instance.addTransientModifier(modifier);
            applied.add(new AppliedAttributeModifier(instance, modifier));
         }
      });
      return applied;
   }

   private static void beginStance(ServerPlayer player, long now) {
      CompoundTag data = player.getPersistentData();
      if (!data.contains(TAG_STANCE_START)) {
         data.putLong(TAG_STANCE_START, now);
         spawnStanceFx(player.serverLevel(), player, 0.0F);
      }
      applyStanceSlow(player, true);
   }

   private static void handleStanceAttack(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, boolean down, long now) {
      CompoundTag data = player.getPersistentData();
      if (down) {
         if (!isUnlocked(vars, GanryuMove.TSUBAME_GAESHI)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.move_locked"), true);
            return;
         }
         LivingEntity target = findTarget(player, GanryuMove.TSUBAME_GAESHI.range());
         if (target == null) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
            return;
         }
         float power = chargeRatio(data, now);
         performTsubame(player, vars, target, power);
         resetStanceChain(player, now);
         return;
      }
      int stage = data.getInt(TAG_STANCE_STAGE);
      GanryuMove move = stanceComboMove(stage, data.getLong(TAG_STANCE_CHAIN), now);
      if (!isUnlocked(vars, move)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.move_locked"), true);
         return;
      }
      if (move == GanryuMove.SPARROW_SLASH) {
         float power = chargeRatio(data, now);
         data.putFloat(TAG_STANCE_POWER, power);
         data.putInt(TAG_STANCE_STAGE, 1);
         data.putLong(TAG_STANCE_CHAIN, now + 40L);
         perform(player, vars, move, power);
      } else {
         float power = data.getFloat(TAG_STANCE_POWER);
         perform(player, vars, move, power);
         resetStanceChain(player, now);
      }
   }

   private static void perform(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, GanryuMove move, float stancePower) {
      if (!isUnlocked(vars, move)) return;
      if (move == GanryuMove.HIGH_JUMP && !MartialHighJumpService.tryConsume(player)) return;
      long now = player.level().getGameTime();
      int recovery = recoveryTicks(move.recoveryTicks(), vars.body_technique);
      player.getPersistentData().putLong(TAG_RECOVERY, now + recovery);
      if (move == GanryuMove.HIGH_JUMP) {
         player.setDeltaMovement(player.getDeltaMovement().x, 1.05 + BodyTrainingService.stagedPercent(vars.body_technique) * 0.4, player.getDeltaMovement().z);
         player.hurtMarked = true;
      } else {
         LivingEntity target = findTarget(player, move.range());
         applyMovement(player, move, target);
         if (move == GanryuMove.SPARROW_SLASH) {
            float damage = Mth.lerp(Mth.clamp(stancePower, 0.0F, 1.0F), 4.0F, 8.0F);
            queueSparrowSlash(player, target, damage);
         } else if (target != null) {
            float damage = move == GanryuMove.FLOWER_BUD
               ? stanceDamage(10.0F, 20.0F, stancePower)
               : scaledDamage(vars, move.damage());
            hit(player, target, damage, move, true);
         }
      }
      if (move != GanryuMove.HIGH_JUMP) damageBlade(player);
      spawnMoveFx(player.serverLevel(), player, move);
   }

   private static void performTsubame(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, LivingEntity target, float power) {
      long now = player.level().getGameTime();
      int recovery = recoveryTicks(GanryuMove.TSUBAME_GAESHI.recoveryTicks(), vars.body_technique);
      player.getPersistentData().putLong(TAG_RECOVERY, now + recovery);
      boolean landed = tsubameLands(player.getRandom().nextFloat());
      if (landed) {
         target.stopUsingItem();
         target.invulnerableTime = 0;
         hit(player, target, Mth.lerp(Mth.clamp(power, 0.0F, 1.0F), 60.0F, 120.0F), GanryuMove.TSUBAME_GAESHI, true);
      } else {
         player.serverLevel().sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.45, 0.6, 0.45, 0.06);
      }
      net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects.spawn(player.serverLevel(), "servant_sasaki_tsubame", player, 96.0);
      damageBlade(player);
   }

   private static void queueSparrowSlash(ServerPlayer player, LivingEntity target, float damage) {
      if (target == null) return;
      CompoundTag data = player.getPersistentData();
      data.putUUID(TAG_PENDING_TARGET, target.getUUID());
      data.putInt(TAG_PENDING_STRIKES, 3);
      data.putLong(TAG_PENDING_NEXT, player.level().getGameTime());
      data.putFloat(TAG_PENDING_DAMAGE, damage);
      data.putBoolean(TAG_PENDING_AWARDED, false);
   }

   public static void tickPlayer(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (!isActive(player)) {
         endStance(player);
         clearPending(data);
      } else if (isInStance(player)) {
         applyStanceSlow(player, true);
         if (data.getInt(TAG_STANCE_STAGE) == 1 && data.getLong(TAG_STANCE_CHAIN) < now) resetStanceChain(player, now);
         if (now % 20L == 0L) spawnStanceFx(player.serverLevel(), player, chargeRatio(data, now));
      }
      if (data.getInt(TAG_PENDING_STRIKES) <= 0 || data.getLong(TAG_PENDING_NEXT) > now || !data.hasUUID(TAG_PENDING_TARGET)) return;
      LivingEntity target = player.serverLevel().getEntity(data.getUUID(TAG_PENDING_TARGET)) instanceof LivingEntity living ? living : null;
      if (target == null || !target.isAlive() || player.distanceToSqr(target) > 36.0) {
         clearPending(data);
         return;
      }
      int strikesBefore = data.getInt(TAG_PENDING_STRIKES);
      boolean award = !data.getBoolean(TAG_PENDING_AWARDED);
      boolean landed = hit(player, target, data.getFloat(TAG_PENDING_DAMAGE), GanryuMove.SPARROW_SLASH, award);
      if (landed && award) data.putBoolean(TAG_PENDING_AWARDED, true);
      int strike = 4 - strikesBefore;
      spawnSparrowStrikeFx(player.serverLevel(), player, target, strike, landed);
      int remaining = strikesBefore - 1;
      data.putInt(TAG_PENDING_STRIKES, remaining);
      data.putLong(TAG_PENDING_NEXT, now + 3L);
      Vec3 dir = horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.scale(0.15)));
      player.hurtMarked = true;
      if (remaining <= 0) clearPending(data);
   }

   private static boolean hit(ServerPlayer player, LivingEntity target, float damage, GanryuMove move, boolean award) {
      if (target == null || !canHit(player, target)) return false;
      target.invulnerableTime = 0;
      float finalDamage = damage;
      if (activeBlade(player).is(ModItems.NODACHI.get())) {
         finalDamage += souwaBonus(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
      }
      player.getPersistentData().putBoolean(TAG_MARTIAL_DAMAGE, true);
      boolean landed;
      try {
         landed = target.hurt(move == GanryuMove.TSUBAME_GAESHI
            ? player.damageSources().source(TSUBAME_DAMAGE, player)
            : player.damageSources().playerAttack(player), finalDamage);
      } finally {
         player.getPersistentData().remove(TAG_MARTIAL_DAMAGE);
      }
      target.invulnerableTime = 0;
      if (!landed) return false;
      Vec3 direction = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) direction = horizontalLook(player);
      direction = direction.normalize();
      double lift = switch (move) {
         case SPARROW_THRUST, SPARROW_THRUST_SECOND, FLOWER_BUD -> 0.34;
         case STONE_FLOWER_SECOND -> -0.12;
         default -> 0.08;
      };
      target.push(direction.x * 0.15, lift, direction.z * 0.15);
      target.hurtMarked = true;
      if (award) addProficiency(player, isSparring(player) ? 0.25 : 0.05);
      return true;
   }

   private static void applyMovement(ServerPlayer player, GanryuMove move, LivingEntity target) {
      Vec3 dir = target == null ? horizontalLook(player) : target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
      double speed = switch (move) {
         case SPRING_BUD -> 0.55;
         case SPRING_BUD_SECOND -> 0.70;
         case SPARROW_SLASH -> 0.45;
         case SPARROW_THRUST_SECOND -> -0.45;
         default -> 0.0;
      };
      if (speed != 0.0 && dir.lengthSqr() > 1.0E-4) {
         player.setDeltaMovement(player.getDeltaMovement().add(dir.x * speed, 0.04, dir.z * speed));
         player.hurtMarked = true;
      }
      if (move == GanryuMove.STONE_FLOWER_SECOND) {
         Vec3 motion = player.getDeltaMovement();
         player.setDeltaMovement(motion.x + dir.x * 0.18, Math.min(motion.y, -0.48), motion.z + dir.z * 0.18);
         player.hurtMarked = true;
      }
   }

   private static LivingEntity findTarget(ServerPlayer player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      return player.level().getEntitiesOfClass(LivingEntity.class,
         player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0), e -> canHit(player, e)).stream()
         .filter(e -> {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            return to.lengthSqr() <= range * range && to.normalize().dot(look) > 0.70 && player.hasLineOfSight(e);
         }).min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
   }

   private static boolean canHit(ServerPlayer player, LivingEntity target) {
      return target != player && target.isAlive() && !target.isAlliedTo(player) && !EntityUtils.isImmunePlayerTarget(target);
   }

   public static int souwaBonus(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars == null ? 0 : souwaBonus(vars.ganryu_learned, vars.ganryu_proficiency);
   }

   static int souwaBonus(boolean learned, double proficiency) {
      if (!learned || proficiency < 25.0) return 0;
      return proficiency >= 100.0 ? 6 : proficiency >= 50.0 ? 3 : 1;
   }

   static float scaledDamage(TypeMoonWorldModVariables.PlayerVariables vars, float baseDamage) {
      if (vars == null) return Math.max(0.0F, baseDamage);
      return scaledDamage(baseDamage, vars.body_strength, vars.ganryu_proficiency);
   }

   static float scaledDamage(float baseDamage, int bodyStrength, double proficiency) {
      return BajiquanCombatService.scaledDamage(baseDamage, bodyStrength, proficiency);
   }

   static float stanceDamage(float minimum, float maximum, float power) {
      return Mth.lerp(Mth.clamp(power, 0.0F, 1.0F), minimum, maximum);
   }

   static boolean tsubameLands(float roll) {
      return roll >= 0.0F && roll < 0.8F;
   }

   static int recoveryTicks(int baseTicks, int bodyTechnique) {
      return Math.max(1, (int)Math.ceil(Math.max(0, baseTicks) * (1.0 - BodyTrainingService.stagedPercent(bodyTechnique))));
   }

   static int nextSequenceStage(int current, int stages, boolean expired) {
      if (stages <= 1 || expired) return 1;
      return Math.floorMod(current, stages) + 1;
   }

   static int nextComboStage(int current, boolean down, boolean expired) {
      if (expired || current <= 0) return 1;
      int stages = down ? 2 : 3;
      return current >= stages ? 1 : current + 1;
   }

   static GanryuMove comboMove(int stage, boolean down) {
      if (down) return stage == 2 ? GanryuMove.SPARROW_THRUST_SECOND : GanryuMove.SPRING_BUD_SECOND;
      return stage == 2 ? GanryuMove.SPARROW_THRUST : stage == 3 ? GanryuMove.SPRING_BUD : GanryuMove.STONE_FLOWER;
   }

   static GanryuMove stanceComboMove(int stage, long chainUntil, long now) {
      return stage == 1 && chainUntil >= now ? GanryuMove.FLOWER_BUD : GanryuMove.SPARROW_SLASH;
   }

   static boolean usesBasicAttack(double proficiency) {
      return proficiency + 1.0E-6 < GanryuMove.STONE_FLOWER.requiredProficiency();
   }

   public static void addProficiency(ServerPlayer player, double amount) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double before = vars.ganryu_proficiency;
      vars.ganryu_proficiency = Mth.clamp(Math.round((before + amount) * 100.0) / 100.0, 0.0, 100.0);
      if (vars.ganryu_proficiency >= 50.0) vars.martial_ukemi_learned = true;
      notifyUnlocks(player, before, vars.ganryu_proficiency);
      if (before < 100.0 && vars.ganryu_proficiency >= 100.0) {
         TypeMoonAdvancementHelper.grant(player, TypeMoonAdvancementHelper.GANRYU_SOUWA_PERFECT);
      }
      if ((int)(before * 20.0) != (int)(vars.ganryu_proficiency * 20.0)) vars.syncPlayerVariables(player);
   }

   private static void notifyUnlocks(ServerPlayer player, double before, double after) {
      double[] thresholds = {0.5, 5.0, 10.0, 25.0, 50.0, 55.0, 60.0, 100.0};
      String[] keys = {"05", "50", "100", "250", "500", "550", "600", "1000"};
      for (int i = 0; i < thresholds.length; i++) {
         if (before < thresholds[i] && after >= thresholds[i]) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.moves_unlocked",
               Component.translatable("message.typemoonworld.ganryu.unlock." + keys[i])), false);
         }
      }
   }

   public static boolean isMartialDamage(ServerPlayer player) {
      return player != null && player.getPersistentData().getBoolean(TAG_MARTIAL_DAMAGE);
   }

   public static boolean consumeBasicAttackAward(ServerPlayer player) {
      if (player == null || !player.getPersistentData().getBoolean(TAG_BASIC_ATTACK)) return false;
      player.getPersistentData().remove(TAG_BASIC_ATTACK);
      return true;
   }

   public static boolean isSparring(ServerPlayer player) {
      return player.getPersistentData().getBoolean("TypeMoonGanryuSparring");
   }

   public static boolean isInStance(ServerPlayer player) {
      return player != null && player.getPersistentData().contains(TAG_STANCE_START);
   }

   public static void endStance(ServerPlayer player) {
      if (player == null) return;
      CompoundTag data = player.getPersistentData();
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      boolean hadState = data.contains(TAG_STANCE_START) || data.contains(TAG_STANCE_STAGE)
         || speed != null && speed.getModifier(STANCE_SLOW_ID) != null;
      data.remove(TAG_STANCE_START);
      data.remove(TAG_STANCE_STAGE);
      data.remove(TAG_STANCE_POWER);
      data.remove(TAG_STANCE_CHAIN);
      applyStanceSlow(player, false);
   }

   public static void clearRuntime(ServerPlayer player) {
      if (player == null) return;
      endStance(player);
      clearPending(player.getPersistentData());
      player.getPersistentData().remove(TAG_RECOVERY);
      player.getPersistentData().remove(TAG_A_COUNT);
      player.getPersistentData().remove(TAG_LAST_SEQUENCE);
      player.getPersistentData().remove(TAG_DOWN_UNTIL);
   }

   private static void resetStanceChain(ServerPlayer player, long now) {
      CompoundTag data = player.getPersistentData();
      data.putInt(TAG_STANCE_STAGE, 0);
      data.remove(TAG_STANCE_POWER);
      data.remove(TAG_STANCE_CHAIN);
      data.putLong(TAG_STANCE_START, now);
   }

   private static float chargeRatio(CompoundTag data, long now) {
      return stanceCharge(now - data.getLong(TAG_STANCE_START));
   }

   static float stanceCharge(long elapsedTicks) {
      return Mth.clamp(elapsedTicks / (float)STANCE_CHARGE_TICKS, 0.0F, 1.0F);
   }

   private static void applyStanceSlow(ServerPlayer player, boolean active) {
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed == null) return;
      AttributeModifier old = speed.getModifier(STANCE_SLOW_ID);
      if (old != null) speed.removeModifier(STANCE_SLOW_ID);
      if (active) speed.addTransientModifier(new AttributeModifier(STANCE_SLOW_ID, -0.60, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
   }

   private static void damageBlade(ServerPlayer player) {
      ItemStack blade = activeBlade(player);
      if (!blade.isDamageableItem() || player.getAbilities().instabuild) return;
      EquipmentSlot slot = blade == player.getMainHandItem() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
      blade.hurtAndBreak(1, player, slot);
   }

   private static void clearPending(CompoundTag data) {
      data.remove(TAG_PENDING_TARGET);
      data.remove(TAG_PENDING_STRIKES);
      data.remove(TAG_PENDING_NEXT);
      data.remove(TAG_PENDING_DAMAGE);
      data.remove(TAG_PENDING_AWARDED);
   }

   private static Vec3 horizontalLook(ServerPlayer player) {
      Vec3 dir = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      return dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
   }

   private static void spawnMoveFx(ServerLevel level, ServerPlayer player, GanryuMove move) {
      Vec3 look = horizontalLook(player);
      Vec3 pos = player.position().add(look.scale(1.3)).add(0.0, 1.0, 0.0);
      switch (move) {
         case HIGH_JUMP -> {
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.08, player.getZ(), 9, 0.34, 0.04, 0.34, 0.045);
            playMoveSound(level, player, SoundEvents.PLAYER_ATTACK_WEAK, 0.75F, 1.45F);
         }
         case STONE_FLOWER -> {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y - 0.2, pos.z, 1, 0.16, 0.28, 0.16, 0.0);
            level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y - 0.35, pos.z, 5, 0.14, 0.25, 0.14, 0.035);
            playMoveSound(level, player, SoundEvents.PLAYER_ATTACK_STRONG, 0.82F, 0.82F);
         }
         case SPARROW_THRUST -> {
            spawnThrustLine(level, player, look, 3, 0.0);
            playMoveSound(level, player, SoundEvents.PLAYER_ATTACK_WEAK, 0.78F, 1.42F);
         }
         case SPRING_BUD, SPRING_BUD_SECOND -> {
            double height = move == GanryuMove.SPRING_BUD_SECOND ? 0.62 : 1.0;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, player.getY() + height, pos.z, 2, 0.42, 0.08, 0.42, 0.0);
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.08, player.getZ(), move == GanryuMove.SPRING_BUD_SECOND ? 7 : 4,
               0.28, 0.03, 0.28, 0.035);
            playMoveSound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.86F, move == GanryuMove.SPRING_BUD_SECOND ? 0.78F : 0.96F);
         }
         case SPARROW_THRUST_SECOND, FLOWER_BUD -> {
            for (int i = 0; i < 3; i++) {
               level.sendParticles(ParticleTypes.END_ROD, pos.x, player.getY() + 0.45 + i * 0.48, pos.z, 2, 0.12, 0.1, 0.12, 0.012);
            }
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.15, pos.z, 1, 0.18, 0.3, 0.18, 0.0);
            playMoveSound(level, player, SoundEvents.PLAYER_ATTACK_CRIT, 0.88F, move == GanryuMove.FLOWER_BUD ? 0.72F : 1.08F);
         }
         case STONE_FLOWER_SECOND -> {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y - 0.45, pos.z, 2, 0.18, 0.36, 0.18, 0.0);
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.12, player.getZ(), 5, 0.2, 0.08, 0.2, 0.025);
            playMoveSound(level, player, SoundEvents.PLAYER_ATTACK_CRIT, 0.9F, 0.7F);
         }
         case SPARROW_SLASH -> {
            level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 4, 0.16, 0.16, 0.16, 0.015);
            playMoveSound(level, player, SoundEvents.PLAYER_ATTACK_WEAK, 0.68F, 1.55F);
         }
         default -> {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0.25, 0.18, 0.25, 0.0);
            playMoveSound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.82F, 1.0F);
         }
      }
   }

   private static void spawnThrustLine(ServerLevel level, ServerPlayer player, Vec3 look, int points, double sideOffset) {
      Vec3 side = new Vec3(-look.z, 0.0, look.x).scale(sideOffset);
      for (int i = 0; i < points; i++) {
         Vec3 point = player.position().add(0.0, 1.05, 0.0).add(look.scale(0.75 + i * 0.55)).add(side);
         level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.025, 0.025, 0.025, 0.0);
      }
   }

   private static void spawnStanceFx(ServerLevel level, ServerPlayer player, float charge) {
      int count = charge >= 1.0F ? 5 : 2;
      level.sendParticles(charge >= 1.0F ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT,
         player.getX(), player.getY() + 0.75, player.getZ(), count, 0.38, 0.32, 0.38, charge >= 1.0F ? 0.015 : 0.08);
      if (charge <= 0.0F) playMoveSound(level, player, SoundEvents.PLAYER_ATTACK_WEAK, 0.55F, 0.62F);
   }

   private static void spawnSparrowStrikeFx(ServerLevel level, ServerPlayer player, LivingEntity target, int strike, boolean landed) {
      Vec3 look = horizontalLook(player);
      double side = (strike - 2) * 0.18;
      spawnThrustLine(level, player, look, 3, side);
      Vec3 impact = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      level.sendParticles(landed ? ParticleTypes.CRIT : ParticleTypes.SMOKE,
         impact.x, impact.y, impact.z, landed ? 4 : 2, 0.12, 0.16, 0.12, landed ? 0.04 : 0.02);
      playMoveSound(level, player, landed ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK,
         0.62F, 1.18F + strike * 0.12F);
   }

   private static void playMoveSound(ServerLevel level, ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
      level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
   }

   private record AppliedAttributeModifier(AttributeInstance instance, AttributeModifier modifier) {
      private void remove() {
         this.instance.removeModifier(this.modifier);
      }
   }
}
