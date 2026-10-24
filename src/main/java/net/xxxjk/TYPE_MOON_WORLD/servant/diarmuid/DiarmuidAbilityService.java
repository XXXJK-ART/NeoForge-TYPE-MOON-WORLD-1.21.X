package net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.BindingService;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
import net.xxxjk.TYPE_MOON_WORLD.network.DiarmuidActionAnimationMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardActionTimeline;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDiarmuidSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardUnlimitedMode;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** One cancellable server timeline; no delayed lambdas survive an entity's lifecycle. */
public final class DiarmuidAbilityService {
   private static final Map<LivingEntity, Cast> CASTS = new WeakHashMap<>();
   /** Basic attacks in double-spear stance are independent timelines, one per physical hand. */
   private static final Map<LivingEntity, EnumMap<InteractionHand, Cast>> HAND_CASTS = new WeakHashMap<>();
   private static final Map<LivingEntity, DiarmuidAction> NPC_HAND_ANIMATIONS = new WeakHashMap<>();
   private static final Map<LivingEntity, Long> VISUAL_UNTIL = new WeakHashMap<>();
   private static final String PREFIX = "DiarmuidAction:";
   private static final String LAST_INPUT = PREFIX + "lastInput";
   private static final String FALL_PROTECTION = PREFIX + "fallProtection";
   private static final String FALL_DIMENSION = PREFIX + "fallDimension";
   private static final String DOUBLE_SPEAR_STANCE = PREFIX + "doubleSpearStance";
   public static final String DANCE_UNTIL_TAG = PREFIX + "danceUntil";

   private static final class Cast {
      final DiarmuidAction action;
      final long start;
      final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
      final Set<String> hits = new HashSet<>();
      final InteractionHand basicHand;
      final boolean animationPlayed;
      int processed = -1;
      boolean selected;
      UUID target;
      long counterAt = -1;
      long ends;
      boolean launched;
      boolean airborne;

      Cast(LivingEntity owner, DiarmuidAction action, InteractionHand basicHand,
           boolean animationPlayed) {
         this.action = action;
         start = owner.level().getGameTime();
         ends = start + action.duration();
         dimension = owner.level().dimension();
         this.basicHand = basicHand;
         this.animationPlayed = animationPlayed;
      }
   }

   private DiarmuidAbilityService() { }

   public static boolean isOwner(LivingEntity owner) {
      return owner instanceof DiarmuidUaDuibhneEntity
         || owner instanceof ServerPlayer player && ServantCardDiarmuidSkills.isActiveCard(player);
   }

   /** True when a shared action is active; independent stance-hand attacks do not block skills. */
   public static boolean active(LivingEntity owner) {
      return CASTS.containsKey(owner);
   }

   /** Includes the two independent hand timelines for lifecycle and combat arbitration. */
   public static boolean anyActive(LivingEntity owner) {
      EnumMap<InteractionHand, Cast> hands = HAND_CASTS.get(owner);
      return CASTS.containsKey(owner) || hands != null && !hands.isEmpty();
   }

   public static DiarmuidAction currentAction(LivingEntity owner) {
      Cast cast = CASTS.get(owner);
      if (cast != null) return cast.action;
      EnumMap<InteractionHand, Cast> hands = HAND_CASTS.get(owner);
      if (hands != null) {
         for (Cast handCast : hands.values()) if (handCast != null) return handCast.action;
      }
      if (owner instanceof DiarmuidUaDuibhneEntity) return NPC_HAND_ANIMATIONS.get(owner);
      return null;
   }

   public static boolean handActionActive(LivingEntity owner, InteractionHand hand) {
      EnumMap<InteractionHand, Cast> hands = HAND_CASTS.get(owner);
      return hands != null && hands.containsKey(hand);
   }

   public static long handCooldown(LivingEntity owner, InteractionHand hand) {
      return owner.getPersistentData().getLong(cooldownKey(DiarmuidAction.RED_BASIC, hand));
   }

   public static DiarmuidAction handAction(LivingEntity owner, InteractionHand hand) {
      EnumMap<InteractionHand, Cast> hands = HAND_CASTS.get(owner);
      Cast cast = hands == null ? null : hands.get(hand);
      return cast == null ? null : cast.action;
   }

   public static float handActionAge(LivingEntity owner, InteractionHand hand, float partialTick) {
      EnumMap<InteractionHand, Cast> hands = HAND_CASTS.get(owner);
      Cast cast = hands == null ? null : hands.get(hand);
      return cast == null ? -1 : owner.level().getGameTime() - cast.start + partialTick;
   }

   public static void releaseDance(LivingEntity owner) {
      Cast cast = CASTS.get(owner);
      if (cast != null && cast.action == DiarmuidAction.FIGURE_EIGHT) finish(owner);
      if (owner instanceof ServerPlayer) {
         EnumMap<InteractionHand, Cast> hands = HAND_CASTS.get(owner);
         if (hands != null) for (InteractionHand hand : InteractionHand.values()) {
            Cast handCast = hands.get(hand);
            if (handCast != null && handCast.action.dance()) finishHand(owner, hand);
         }
      }
   }

   public static boolean isDoubleSpearStance(LivingEntity owner) {
      return owner != null && owner.getPersistentData().getBoolean(DOUBLE_SPEAR_STANCE);
   }

   public static boolean deflectLowDamageProjectile(LivingEntity defender, Projectile projectile) {
      if (defender == null || projectile == null || !isOwner(defender) || !isDoubleSpearStance(defender)
         || defender.level().isClientSide() || !hasActiveDance(defender)
         || projectile.getOwner() == defender) return false;
      boolean deflectable = projectile instanceof Arrow arrow && arrow.getBaseDamage() * projectile.getDeltaMovement().length() <= 6.0 && !arrow.isOnFire()
         || projectile instanceof Snowball || projectile instanceof ThrownEgg;
      if (!deflectable) return false;
      Vec3 reflected = projectile.getDeltaMovement().scale(-0.85).add(defender.getLookAngle().scale(0.12));
      projectile.setOwner(defender);
      projectile.setDeltaMovement(reflected);
      projectile.hurtMarked = true;
      if (defender.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, defender.getX(), defender.getY() + defender.getBbHeight() * 0.62,
            defender.getZ(), 5, 0.16, 0.18, 0.16, 0.02);
         level.playSound(null, defender.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.35F, 1.65F);
      }
      return true;
   }

   private static boolean hasActiveDance(LivingEntity owner) {
      EnumMap<InteractionHand, Cast> hands = HAND_CASTS.get(owner);
      if (hands == null) return false;
      long now = owner.level().getGameTime();
      return hands.values().stream().anyMatch(cast -> cast.action.dance() && now < cast.ends);
   }

   public static boolean canAct(LivingEntity owner) {
      if (owner == null || owner.isRemoved() || !owner.isAlive() || owner.isSleeping() || owner.isPassenger()
         || BindingService.isBound(owner.getUUID()) || ServantCombatSystem.cannotAct(owner)
         || owner.hasEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.PETRIFIED)
         || net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService.isLaunched(owner)
         || NoblePhantasmCastService.chanting(owner)) return false;
      long now = owner.level().getGameTime();
      var data = owner.getPersistentData();
      return data.getLong("ServantCardCombatStunUntil") <= now
         && data.getLong("TypeMoonCombatStunUntil") <= now
         && data.getLong("TypeMoonCombatSuppressedUntil") <= now
         && !ServantCombatSystem.skillsSuppressed(owner)
         && !(owner instanceof ServerPlayer player && ServantMasterCarryService.isCarryingMaster(player));
   }

   public static boolean start(LivingEntity owner, DiarmuidAction action) {
      return start(owner, action, null);
   }

   public static boolean basic(LivingEntity owner, InteractionHand hand) {
      ItemStack stack = owner.getItemInHand(hand);
      if (!(stack.getItem() instanceof DiarmuidSpearItem spear)) return false;
      DiarmuidAction action = spear.spearType() == DiarmuidSpearItem.SpearType.GAE_DEARG
         ? DiarmuidAction.RED_BASIC : DiarmuidAction.YELLOW_BASIC;
      if (isDoubleSpearStance(owner) && owner.getRandom().nextFloat() < DiarmuidAction.BASIC_DANCE_CHANCE)
         action = action == DiarmuidAction.RED_BASIC ? DiarmuidAction.RED_DANCE : DiarmuidAction.YELLOW_DANCE;
      return start(owner, action, hand, true);
   }

   public static boolean basicSpear(LivingEntity owner, boolean red) {
      InteractionHand hand = spearHand(owner, red);
      return hand != null && basic(owner, hand);
   }

   private static boolean start(LivingEntity owner, DiarmuidAction action, InteractionHand hand) {
      return start(owner, action, hand, false);
   }

   private static boolean start(LivingEntity owner, DiarmuidAction action, InteractionHand hand, boolean randomBasicAnimation) {
      boolean stanceHandAction = hand != null && randomBasicAnimation && isDoubleSpearStance(owner);
      if (action == null || action == DiarmuidAction.GUARD_COUNTER || action == DiarmuidAction.LANDING_SWEEP || !(owner.level() instanceof ServerLevel) || !isOwner(owner)
          || !canAct(owner) || (!stanceHandAction && anyActive(owner))
          || (stanceHandAction && active(owner))
         || owner instanceof ServerPlayer player && ServantCardActionTimeline.isActive(player)) return false;
      EnumMap<InteractionHand, Cast> activeHands = HAND_CASTS.get(owner);
      var data = owner.getPersistentData();
      long now = owner.level().getGameTime();
      boolean cooldownDisabled = !action.basic() && ServantCardUnlimitedMode.isCooldownDisabled(owner);
      if (stanceHandAction && activeHands != null) {
         Cast sameHand = activeHands.get(hand);
         if (sameHand != null && now >= sameHand.ends) finishHand(owner, hand);
         activeHands = HAND_CASTS.get(owner);
      }
      if (stanceHandAction && activeHands != null && activeHands.containsKey(hand)) return false;
      if (!stanceHandAction && data.contains(LAST_INPUT) && data.getLong(LAST_INPUT) == now
         || !cooldownDisabled && cooldown(owner, action, hand) > now) return false;
      boolean stanceToggle = action == DiarmuidAction.DOUBLE_SPEAR_STANCE;
      boolean enteringStance = stanceToggle && !isDoubleSpearStance(owner);
      if (enteringStance && (!hasSpear(owner, true) || !hasSpear(owner, false))) return false;
      if (!stanceToggle && (action.requiresRed() && !hasSpear(owner, true) || action.requiresYellow() && !hasSpear(owner, false))) return false;
      if (hand != null && handDisabled(owner, hand)) return false;
      if (action == DiarmuidAction.MANA_LEAP && !canLaunch(owner)) return false;
      if (owner instanceof ServerPlayer player) {
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!ServantCardManaService.consume(player, vars, action.mana())) return false;
      } else if (owner instanceof ServantEntity servant) {
         if (servant.getCurrentMp() < action.mana()) return false;
         servant.setCurrentMp(servant.getCurrentMp() - action.mana());
         servant.getNavigation().stop();
      }
      boolean stance = isDoubleSpearStance(owner);
      int animationId = -1;
      boolean animationPlayed = !randomBasicAnimation || stance;
      if (stanceToggle) {
         stance = enteringStance;
         data.putBoolean(DOUBLE_SPEAR_STANCE, stance);
         if (owner instanceof DiarmuidUaDuibhneEntity npc) npc.setDoubleSpearStance(stance);
         data.remove(DANCE_UNTIL_TAG);
         animationPlayed = true;
         animationId = action.networkId();
       } else if (randomBasicAnimation && stance) {
         boolean leftArm = (hand == InteractionHand.MAIN_HAND ? owner.getMainArm() : owner.getMainArm().getOpposite())
            == net.minecraft.world.entity.HumanoidArm.LEFT;
         if (action.dance()) {
            animationId = (leftArm ? DiarmuidAction.YELLOW_DANCE : DiarmuidAction.RED_DANCE).networkId();
            data.putLong(DANCE_UNTIL_TAG, now + action.duration());
         } else {
            animationId = leftArm
               ? DiarmuidAction.YELLOW_BASIC.networkId() : DiarmuidAction.RED_BASIC.networkId();
         }
      } else if (randomBasicAnimation) {
         animationPlayed = false;
      } else {
         data.remove(DANCE_UNTIL_TAG);
         animationId = action.networkId();
      }
      if (action.dance() && stance) data.putLong(DANCE_UNTIL_TAG + hand.name(), now + action.duration());
      if (!stanceHandAction) data.putLong(LAST_INPUT, now);
      int cooldown = randomBasicAnimation && stance && !action.dance() ? DiarmuidAction.STANCE_HAND_COOLDOWN : action.cooldown();
      if (cooldownDisabled) data.remove(cooldownKey(action, hand));
      else data.putLong(cooldownKey(action, hand), now + cooldown);
      Cast cast = new Cast(owner, action, hand, animationPlayed);
      if (stanceHandAction) HAND_CASTS.computeIfAbsent(owner, ignored -> new EnumMap<>(InteractionHand.class)).put(hand, cast);
      else CASTS.put(owner, cast);
      publishThreats(owner, action, now);
      if (!stanceHandAction) {
         ServantCombatDefenseService.initialize(owner);
         ServantCombatDefenseService.setGuarding(owner, false);
      }
      if (owner instanceof ServerPlayer player) {
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         vars.servant_card_action_mode = stance ? 1 : 0;
         if (action.slot() >= 0 && action.slot() <= 9) {
            ServantCardTransformManager.setSkillCooldownUntil(player, vars, action.slot(), now + action.cooldown());
         }
         vars.syncPlayerVariables(player);
         if (!action.requiresYellow() || !DiarmuidVoiceService.yellowReady(owner)) ServantCardVoiceHelper.tryPlayAttack(player);
      } else if (owner instanceof DiarmuidUaDuibhneEntity npc
         && (!action.requiresYellow() || !DiarmuidVoiceService.yellowReady(owner))) ServantVoiceHelper.tryPlayAttack(npc);
       if (animationPlayed) play(owner, animationId, stanceHandAction ? hand : null);
      return true;
   }

   private static void publishThreats(LivingEntity owner, DiarmuidAction action, long now) {
      publishThreats(owner, action, now, -1);
   }

   private static void publishThreats(LivingEntity owner, DiarmuidAction action, long now, int afterTick) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      var strikes = action.hits().stream().filter(hit -> hit.tick() > afterTick)
         .limit(action == DiarmuidAction.FIGURE_EIGHT ? 3 : Integer.MAX_VALUE).toList();
      for (var hit : strikes) {
         long ticksUntilHit = afterTick < 0 ? hit.tick() : hit.tick() - afterTick;
         var shape = action.angle() >= 240 ? net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat.Shape.SPHERE
            : net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat.Shape.CONE;
         net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService.publish(level,
            new net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat(net.minecraft.resources.ResourceLocation.parse(action.actionId()),
               owner.getUUID(), null, owner.getEyePosition(), horizontalLook(owner), shape,
               action.angle() >= 180 ? hit.range() : hit.range() * Math.tan(Math.toRadians(action.angle() / 2.0)),
               hit.range(), action == DiarmuidAction.ROSE_DANCE ? 4 : 3, now, now + ticksUntilHit, now + ticksUntilHit, true, true, true));
      }
   }

   private static String cooldownKey(DiarmuidAction action, InteractionHand hand) {
      return PREFIX + (hand == null ? action.path() : "hand_" + hand.name());
   }

   public static long cooldown(LivingEntity owner, DiarmuidAction action) { return cooldown(owner, action, null); }
   private static long cooldown(LivingEntity owner, DiarmuidAction action, InteractionHand hand) {
      return owner.getPersistentData().getLong(cooldownKey(action, hand));
   }

   public static void clearCooldowns(ServerPlayer player) {
      var data=player.getPersistentData();
      for(DiarmuidAction action:DiarmuidAction.values()) {
         if(!action.basic()) data.remove(cooldownKey(action,null));
      }
   }

   public static boolean hasSpear(LivingEntity owner, boolean red) { return spearHand(owner, red) != null; }

   private static InteractionHand spearHand(LivingEntity owner, boolean red) {
      if ((red ? DiarmuidCombatHelper.redDurability(owner) : DiarmuidCombatHelper.yellowDurability(owner)) <= 0) return null;
      for (InteractionHand hand : InteractionHand.values()) {
         ItemStack stack = owner.getItemInHand(hand);
         if (!handDisabled(owner, hand) && stack.getItem() instanceof DiarmuidSpearItem spear
            && (spear.spearType() == DiarmuidSpearItem.SpearType.GAE_DEARG) == red
            && stack.getDamageValue() < stack.getMaxDamage()) return hand;
      }
      return null;
   }

   private static boolean handDisabled(LivingEntity owner, InteractionHand hand) {
      return hand == InteractionHand.MAIN_HAND ? DiarmuidCombatHelper.hasDisabledRightHand(owner)
         : DiarmuidCombatHelper.hasDisabledLeftHand(owner);
   }

   public static void tick(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      var data = owner.getPersistentData();
      if (data.getBoolean(FALL_PROTECTION)) {
         if (!owner.isAlive() || owner.isRemoved() || !isOwner(owner)
            || !level.dimension().location().toString().equals(data.getString(FALL_DIMENSION))) clearFallProtection(owner);
         else {
            owner.fallDistance = 0;
            if (owner.onGround()) clearFallProtection(owner);
         }
      }
      long now = level.getGameTime();
      Long visualEnd = VISUAL_UNTIL.get(owner);
      if (visualEnd != null && now >= visualEnd) VISUAL_UNTIL.remove(owner);
      if (!isOwner(owner)) { cancel(owner); return; }
      if (visualEnd != null && !canAct(owner)) {
         VISUAL_UNTIL.remove(owner);
         stopAnimation(owner);
      }
       EnumMap<InteractionHand, Cast> handCasts = HAND_CASTS.get(owner);
      if (handCasts != null && !handCasts.isEmpty()) {
         if (handCasts.values().stream().anyMatch(handCast -> handCast.dimension != level.dimension()) || !canAct(owner)) finishHands(owner);
         else for (InteractionHand hand : InteractionHand.values()) {
            Cast handCast = handCasts.get(hand);
            if (handCast != null) tickHand(owner, level, handCast, now);
         }
      }
       Cast cast = CASTS.get(owner);
      if (cast == null) return;
      if (cast.dimension != level.dimension()) { cancel(owner); return; }
      if (!canAct(owner)) { finish(owner); finishHands(owner); return; }
      if ((cast.action == DiarmuidAction.FIGURE_EIGHT || cast.action == DiarmuidAction.MANA_LEAP
         || cast.action == DiarmuidAction.LANDING_SWEEP) && (!hasSpear(owner, true) || !hasSpear(owner, false))) { finish(owner); return; }
      if (cast.action == DiarmuidAction.FIGURE_EIGHT && owner instanceof DiarmuidUaDuibhneEntity npc
         && !npc.isNoAi() && (!validTarget(owner, npc.getTarget()) || distance(owner, npc.getTarget()) > 5)) { finish(owner); return; }
      int age = (int)(now - cast.start);
      if (age < 0 || age > cast.action.duration() + 20 || now >= cast.ends) { finish(owner); return; }
      if (cast.processed == age) return;
      cast.processed = age;
      if (owner instanceof ServantEntity npc) npc.getNavigation().stop();
      if (cast.action == DiarmuidAction.CROSS_GUARD) {
         boolean window = age >= 2 && age <= 12 && cast.counterAt < 0;
         ServantCombatDefenseService.setGuarding(owner, window);
         if (cast.counterAt >= 0 && now == cast.counterAt && cast.target != null) {
            Entity entity = level.getEntity(cast.target);
            if (entity instanceof LivingEntity target) hit(owner, cast, -1, DiarmuidAction.GUARD_COUNTER.hits().getFirst(), target);
         }
      }
      if (cast.action == DiarmuidAction.FIGURE_EIGHT && age >= 12 && (age - 12) % 8 == 0)
         publishThreats(owner, cast.action, now, age);
      if (!move(owner, cast, age)) return;
      var strikes = cast.action.hits();
      for (int i = 0; i < strikes.size(); i++) {
         var strike = strikes.get(i);
         if (age != strike.tick()) continue;
         if (cast.action.targets() > 1) {
            for (var target : targets(owner, strike.range(), cast.action.angle(), cast.action.targets())) hit(owner, cast, i, strike, target);
         } else {
            if (!cast.selected) {
               cast.selected = true;
               double range = cast.action == DiarmuidAction.ROSE_DANCE ? 3 : strike.range();
               var selected = targets(owner, range, cast.action.angle(), 1);
               cast.target = selected.isEmpty() ? null : selected.getFirst().getUUID();
            }
            if (cast.target != null && level.getEntity(cast.target) instanceof LivingEntity target) hit(owner, cast, i, strike, target);
         }
          InteractionHand hand = cast.basicHand != null ? cast.basicHand : spearHand(owner, strike.red());
         if (hand != null) owner.swing(hand, true);
         level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.45F, strike.red() ? 1.0F : 1.5F);
      }
   }

   private static void tickHand(LivingEntity owner, ServerLevel level, Cast cast, long now) {
      if (!isDoubleSpearStance(owner) || now >= cast.ends || !hasActiveHandSpear(owner, cast.basicHand)) {
         finishHand(owner, cast.basicHand);
         return;
      }
      int age = (int)(now - cast.start);
      if (age < 0 || age > cast.action.duration() + 20 || cast.processed == age) return;
      cast.processed = age;
      var strikes = cast.action.hits();
      for (int i = 0; i < strikes.size(); i++) {
         DiarmuidAction.Hit strike = strikes.get(i);
         if (age != strike.tick()) continue;
         if (cast.action.targets() > 1) {
            for (LivingEntity target : targets(owner, strike.range(), cast.action.angle(), cast.action.targets())) hit(owner, cast, i, strike, target);
         } else {
            if (!cast.selected) {
               cast.selected = true;
               var selected = targets(owner, strike.range(), cast.action.angle(), 1);
               cast.target = selected.isEmpty() ? null : selected.getFirst().getUUID();
            }
            if (cast.target != null && level.getEntity(cast.target) instanceof LivingEntity target) hit(owner, cast, i, strike, target);
         }
         owner.swing(cast.basicHand, true);
         level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
            0.45F, strike.red() ? 1.0F : 1.5F);
      }
   }

   private static boolean hasActiveHandSpear(LivingEntity owner, InteractionHand hand) {
      if (hand == null || handDisabled(owner, hand)) return false;
      ItemStack stack = owner.getItemInHand(hand);
      return stack.getItem() instanceof DiarmuidSpearItem spear
         && (spear.spearType() == DiarmuidSpearItem.SpearType.GAE_DEARG
            ? DiarmuidCombatHelper.redDurability(owner) > 0 : DiarmuidCombatHelper.yellowDurability(owner) > 0)
         && stack.getDamageValue() < stack.getMaxDamage();
   }

   private static void finishHand(LivingEntity owner, InteractionHand hand) {
      EnumMap<InteractionHand, Cast> hands = HAND_CASTS.get(owner);
      Cast cast = hands == null ? null : hands.remove(hand);
      if (cast == null) return;
      if (hands.isEmpty()) HAND_CASTS.remove(owner);
      owner.getPersistentData().remove(DANCE_UNTIL_TAG + hand.name());
      if (cast.animationPlayed) stopAnimation(owner, hand, owner.level().getGameTime() >= cast.ends);
   }

   private static void finishHands(LivingEntity owner) {
      for (InteractionHand hand : InteractionHand.values()) finishHand(owner, hand);
      HAND_CASTS.remove(owner);
   }

   /** Called only from the shared guard transaction, after evasion checks. */
   public static void onSuccessfulGuard(LivingEntity owner, Entity attacker) {
      Cast cast = CASTS.get(owner);
      if (cast == null || cast.action != DiarmuidAction.CROSS_GUARD || cast.counterAt >= 0
         || !(attacker instanceof LivingEntity target) || !validTarget(owner, target)) return;
      long now = owner.level().getGameTime();
      long age = now - cast.start;
      if (age < 2 || age > 12 || !hasSpear(owner, true) || !hasSpear(owner, false)) return;
      cast.target = target.getUUID();
      cast.selected = true;
      cast.counterAt = now + 3;
      cast.ends = cast.counterAt + 8;
      ServantCombatDefenseService.setGuarding(owner, false);
      play(owner, DiarmuidAction.GUARD_COUNTER);
      publishThreats(owner, DiarmuidAction.GUARD_COUNTER, now);
   }

   private static void hit(LivingEntity owner, Cast cast, int index, DiarmuidAction.Hit strike, LivingEntity target) {
      if (!canHit(owner, target, strike.range()) || !cast.hits.add(index + ":" + target.getUUID())) return;
      InteractionHand hand = spearHand(owner, strike.red());
      if (cast.basicHand != null) hand = cast.basicHand;
      if (hand == null || !hasActiveHandSpear(owner, hand)
         || !(owner.getItemInHand(hand).getItem() instanceof DiarmuidSpearItem heldSpear)
         || (heldSpear.spearType() == DiarmuidSpearItem.SpearType.GAE_DEARG) != strike.red()) return;
      ItemStack stack = owner.getItemInHand(hand);
      var source = new CombatDamageSource(owner instanceof ServerPlayer player ? owner.damageSources().playerAttack(player)
         : owner.damageSources().mobAttack(owner), AttackOrigin.skill("diarmuid_ua_duibhne", cast.action.skillId()), 1.0F, strike.red());
      int previous = target.invulnerableTime;
      boolean damaged;
      try {
         target.invulnerableTime = 0;
         damaged = target.hurt(source, strike.damage());
      } finally {
         target.invulnerableTime = Math.max(previous, target.invulnerableTime);
      }
      if (!damaged) return;
      if (strike.red()) DiarmuidCombatHelper.applyRedRoseHit(owner, target);
      else DiarmuidCombatHelper.applyYellowRoseHit(owner, target);
      stack.hurtAndBreak(1, owner, LivingEntity.getSlotForHand(hand));
      DiarmuidCombatHelper.syncSpearItemToOwner(owner, strike.red() ? DiarmuidSpearItem.SpearType.GAE_DEARG : DiarmuidSpearItem.SpearType.GAE_BUIDHE);
      if (cast.action == DiarmuidAction.RISING_THRUST) {
         double scale = Math.max(0, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
         target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45 * scale, 0));
         target.hurtMarked = true;
      }
   }

   public static Vec3 nearestPoint(LivingEntity owner, LivingEntity target) {
      Vec3 eye = owner.getEyePosition();
      var box = target.getBoundingBox();
      return new Vec3(Mth.clamp(eye.x, box.minX, box.maxX), Mth.clamp(eye.y, box.minY, box.maxY), Mth.clamp(eye.z, box.minZ, box.maxZ));
   }

   public static double distance(LivingEntity owner, LivingEntity target) { return owner.getEyePosition().distanceTo(nearestPoint(owner, target)); }

   public static boolean validTarget(LivingEntity owner, LivingEntity target) {
      return target != null && target != owner && target.isAlive() && !target.isRemoved() && target.level() == owner.level()
         && !EntityUtils.isImmunePlayerTarget(target) && !ServantCombatSystem.isUntargetable(target)
         && !owner.isAlliedTo(target) && !target.isAlliedTo(owner)
         && !(owner instanceof ServerPlayer player && ServantMasterTargeting.isContractMaster(player, target))
         && EntityUtils.isValidCombatTarget(owner, target);
   }

   public static boolean canHit(LivingEntity owner, LivingEntity target, double range) {
      if (!validTarget(owner, target) || distance(owner, target) > range + 1.0E-6) return false;
      return owner.level().clip(new ClipContext(owner.getEyePosition(), nearestPoint(owner, target),
         ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS;
   }

   private static java.util.List<LivingEntity> targets(LivingEntity owner, double range, int angle, int max) {
      Vec3 look = horizontalLook(owner);
      return owner.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(range + 1), target -> canHit(owner, target, range)).stream()
         .filter(target -> {
            Vec3 direction = target.getBoundingBox().getCenter().subtract(owner.getEyePosition()).multiply(1, 0, 1);
            return angle >= 360 || direction.lengthSqr() < 1.0E-8 || direction.normalize().dot(look) >= Math.cos(Math.toRadians(angle * 0.5)) - 1.0E-8;
         })
         .sorted(Comparator.comparingDouble((LivingEntity target) -> distance(owner, target)).thenComparingInt(Entity::getId))
         .limit(max).toList();
   }

   public static Vec3 horizontalLook(LivingEntity owner) {
      Vec3 direction = owner.getLookAngle().multiply(1, 0, 1);
      if (direction.lengthSqr() < 1.0E-8) return new Vec3(-Math.sin(Math.toRadians(owner.getYRot())), 0, Math.cos(Math.toRadians(owner.getYRot())));
      return direction.normalize();
   }

   private static boolean canLaunch(LivingEntity owner) {
      return owner.onGround() && !owner.isInWaterOrBubble() && !owner.isInLava()
         && owner.level().noCollision(owner, owner.getBoundingBox().expandTowards(0, 0.5, 0));
   }

   /** Uses the same parameter scaling and impulse as the servant-card big jump. */
   public static Vec3 leapVelocity(LivingEntity owner) {
      var definition = net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry.get(
         DiarmuidUaDuibhneEntity.SERVANT_KEY);
      double scale = definition == null ? 1.0 : Mth.clamp(definition.parameters().movementSpeed() / 0.28, 0.8, 1.8);
      return horizontalLook(owner).scale(0.55 * scale)
         .add(0, Mth.clamp(1.08 + (scale - 1.0) * 0.22, 1.05, 1.32), 0);
   }

   private static void beginLandingSweep(LivingEntity owner) {
      clearFallProtection(owner);
      CASTS.put(owner, new Cast(owner, DiarmuidAction.LANDING_SWEEP, null, true));
      play(owner, DiarmuidAction.LANDING_SWEEP);
      publishThreats(owner, DiarmuidAction.LANDING_SWEEP, owner.level().getGameTime());
      if (owner.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, owner.getX(), owner.getY() + 0.05, owner.getZ(), 16, 0.6, 0.05, 0.6, 0.025);
         level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.7F, 0.85F);
      }
   }

   private static boolean move(LivingEntity owner, Cast cast, int age) {
      double impulse = cast.action == DiarmuidAction.ADVANCING_THRUST && age == 1 ? 0.55
         : cast.action == DiarmuidAction.RETREATING_THRUST && age == 1 ? -0.85
         : cast.action == DiarmuidAction.ROSE_DANCE && age == 22 ? 0.55 : 0;
      if (impulse != 0) {
         owner.addDeltaMovement(horizontalLook(owner).scale(impulse));
         owner.hurtMarked = true;
         if (owner.level() instanceof ServerLevel level)
            level.sendParticles(ParticleTypes.CLOUD, owner.getX(), owner.getY() + 0.04, owner.getZ(), 2, 0.12, 0.01, 0.12, 0.01);
      }
      if (cast.action != DiarmuidAction.MANA_LEAP || age < 4) return true;
      if (!cast.launched) {
         if (!canLaunch(owner)) { finish(owner); return false; }
         cast.launched = true;
         owner.getPersistentData().putBoolean(FALL_PROTECTION, true);
         owner.getPersistentData().putString(FALL_DIMENSION, owner.level().dimension().location().toString());
         Vec3 leap = leapVelocity(owner);
         Vec3 velocity = owner.getDeltaMovement();
         owner.setDeltaMovement(velocity.x + leap.x, Math.max(velocity.y, leap.y), velocity.z + leap.z);
         owner.setOnGround(false);
         owner.fallDistance = 0;
         owner.hurtMarked = true;
         if (owner.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, owner.getX(), owner.getY() + 0.1, owner.getZ(), 12, 0.3, 0.1, 0.3, 0.03);
            level.playSound(null, owner.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.6F, 1.3F);
         }
         return true;
      }
      // Vanilla travel and client input own the flight; the skill only observes its landing.
      owner.fallDistance = 0;
      if (!owner.onGround()) cast.airborne = true;
      if (cast.airborne && owner.onGround()) { beginLandingSweep(owner); return false; }
      if (owner.isInWaterOrBubble() || owner.isInLava() || owner.onClimbable() || owner.isFallFlying()
         || owner instanceof ServerPlayer player && player.getAbilities().flying) { finish(owner); return false; }
      if (age == 14) stopAnimation(owner);
      return true;
   }

   public static boolean protectFall(LivingEntity owner) {
      boolean protectedFall = owner.getPersistentData().getBoolean(FALL_PROTECTION);
      if (protectedFall) { owner.fallDistance = 0; clearFallProtection(owner); }
      return protectedFall;
   }

   private static void clearFallProtection(LivingEntity owner) {
      owner.getPersistentData().remove(FALL_PROTECTION);
      owner.getPersistentData().remove(FALL_DIMENSION);
   }

   private static void finish(LivingEntity owner) {
      Cast cast = CASTS.remove(owner);
      if (cast == null) return;
      if (owner.level() instanceof ServerLevel level)
         net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService.clearForSource(level, owner.getUUID());
      if (cast.action == DiarmuidAction.CROSS_GUARD) ServantCombatDefenseService.setGuarding(owner, false);
      if (cast.action == DiarmuidAction.MANA_LEAP) {
         clearFallProtection(owner);
      }
      owner.getPersistentData().remove(DANCE_UNTIL_TAG);
      if (cast.animationPlayed) stopAnimation(owner, null, owner.level().getGameTime() >= cast.ends);
   }

   public static void cancel(LivingEntity owner) {
      boolean hadVisual = VISUAL_UNTIL.remove(owner) != null;
      finish(owner);
      finishHands(owner);
      NPC_HAND_ANIMATIONS.remove(owner);
      clearFallProtection(owner);
      owner.getPersistentData().remove(DOUBLE_SPEAR_STANCE);
      owner.getPersistentData().remove(DANCE_UNTIL_TAG);
      if (owner instanceof DiarmuidUaDuibhneEntity npc) npc.setDoubleSpearStance(false);
      // An explicit cancellation also stops a presentation-only recovery after its cast ended.
      if (hadVisual) stopAnimation(owner);
   }

   private static void play(LivingEntity owner, DiarmuidAction action) {
      play(owner, action.networkId(), null);
   }

   private static void play(LivingEntity owner, int animationId) {
      play(owner, animationId, null);
   }

   private static void play(LivingEntity owner, int animationId, InteractionHand hand) {
      DiarmuidAction visual = DiarmuidAction.byNetworkId(animationId);
      if (visual != null) VISUAL_UNTIL.merge(owner, owner.level().getGameTime() + visual.visualDuration(), Math::max);
      if (owner instanceof DiarmuidUaDuibhneEntity npc) {
         if (hand == null) npc.triggerDiarmuidAction(DiarmuidAction.byNetworkId(animationId));
         else NPC_HAND_ANIMATIONS.put(owner, DiarmuidAction.byNetworkId(animationId));
      }
      var message = new DiarmuidActionAnimationMessage(owner.getUUID(), animationId, hand == null ? -1 : hand.ordinal());
      PacketDistributor.sendToPlayersTrackingEntity(owner, message);
      if (owner instanceof ServerPlayer player && player.connection != null) ModNetwork.sendToPlayer(player, message);
   }

   private static void stopAnimation(LivingEntity owner) {
      stopAnimation(owner, null);
   }

   private static void stopAnimation(LivingEntity owner, InteractionHand hand) {
      stopAnimation(owner, hand, false);
   }

   private static void stopAnimation(LivingEntity owner, InteractionHand hand, boolean completed) {
      if (owner.level().isClientSide()) return;
      if (owner instanceof DiarmuidUaDuibhneEntity npc) {
         if (hand == null) { npc.triggerDiarmuidAction(null); NPC_HAND_ANIMATIONS.remove(owner); }
         else if (currentAction(owner) == null) NPC_HAND_ANIMATIONS.remove(owner);
      }
      var message = new DiarmuidActionAnimationMessage(owner.getUUID(),
         completed ? DiarmuidActionAnimationMessage.FINISH : -1, hand == null ? -1 : hand.ordinal());
      PacketDistributor.sendToPlayersTrackingEntity(owner, message);
      if (owner instanceof ServerPlayer player && player.connection != null) ModNetwork.sendToPlayer(player, message);
   }
}
