package net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid;

import java.util.Comparator;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
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
   private static final String PREFIX = "DiarmuidAction:";
   private static final String LAST_INPUT = PREFIX + "lastInput";
   private static final String FALL_PROTECTION = PREFIX + "fallProtection";
   private static final String FALL_DIMENSION = PREFIX + "fallDimension";

   private static final class Cast {
      final DiarmuidAction action;
      final long start;
      final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
      final Vec3 origin;
      final Vec3 forward;
      final Vec3 landing;
      final Set<String> hits = new HashSet<>();
      final InteractionHand basicHand;
      int processed = -1;
      boolean selected;
      UUID target;
      long counterAt = -1;
      long ends;

      Cast(LivingEntity owner, DiarmuidAction action, Vec3 landing, InteractionHand basicHand) {
         this.action = action;
         start = owner.level().getGameTime();
         ends = start + action.duration();
         dimension = owner.level().dimension();
         origin = owner.position();
         forward = horizontalLook(owner);
         this.landing = landing;
         this.basicHand = basicHand;
      }
   }

   private DiarmuidAbilityService() { }

   public static boolean isOwner(LivingEntity owner) {
      return owner instanceof DiarmuidUaDuibhneEntity
         || owner instanceof ServerPlayer player && ServantCardDiarmuidSkills.isActiveCard(player);
   }

   public static boolean active(LivingEntity owner) { return CASTS.containsKey(owner); }

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
      if (action == null || action == DiarmuidAction.GUARD_COUNTER || !(owner.level() instanceof ServerLevel) || !isOwner(owner)
         || !canAct(owner) || active(owner) || owner instanceof ServerPlayer player && ServantCardActionTimeline.isActive(player)) return false;
      var data = owner.getPersistentData();
      long now = owner.level().getGameTime();
      if (data.contains(LAST_INPUT) && data.getLong(LAST_INPUT) == now || cooldown(owner, action, hand) > now) return false;
      if (action.requiresRed() && !hasSpear(owner, true) || action.requiresYellow() && !hasSpear(owner, false)) return false;
      if (hand != null && handDisabled(owner, hand)) return false;
      Vec3 landing = action == DiarmuidAction.MANA_LEAP ? findLanding(owner) : null;
      if (action == DiarmuidAction.MANA_LEAP && landing == null) return false;
      if (owner instanceof ServerPlayer player) {
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!ServantCardManaService.consume(player, vars, action.mana())) return false;
      } else if (owner instanceof ServantEntity servant) {
         if (servant.getCurrentMp() < action.mana()) return false;
         servant.setCurrentMp(servant.getCurrentMp() - action.mana());
         servant.getNavigation().stop();
      }
      DiarmuidAction animation = randomBasicAnimation && owner.getRandom().nextInt(6) == 0
         ? DiarmuidAction.ROSE_DANCE : action;
      data.putLong(LAST_INPUT, now);
      data.putLong(cooldownKey(action, hand), now + action.cooldown());
      CASTS.put(owner, new Cast(owner, action, landing, hand));
      publishThreats(owner, action, now);
      ServantCombatDefenseService.initialize(owner);
      ServantCombatDefenseService.setGuarding(owner, false);
      if (owner instanceof ServerPlayer player) {
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (action.slot() >= 0 && action.slot() <= 9) {
            ServantCardTransformManager.setSkillCooldownUntil(player, vars, action.slot(), now + action.cooldown());
         }
         vars.syncPlayerVariables(player);
         if (!action.requiresYellow() || !DiarmuidVoiceService.yellowReady(owner)) ServantCardVoiceHelper.tryPlayAttack(player);
      } else if (owner instanceof DiarmuidUaDuibhneEntity npc
         && (!action.requiresYellow() || !DiarmuidVoiceService.yellowReady(owner))) ServantVoiceHelper.tryPlayAttack(npc);
      play(owner, animation);
      return true;
   }

   private static void publishThreats(LivingEntity owner, DiarmuidAction action, long now) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      for (var hit : action.hits()) {
         var shape = action.angle() >= 240 ? net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat.Shape.SPHERE
            : net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat.Shape.CONE;
         net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService.publish(level,
            new net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat(net.minecraft.resources.ResourceLocation.parse(action.actionId()),
               owner.getUUID(), null, owner.getEyePosition(), horizontalLook(owner), shape,
               action.angle() >= 180 ? hit.range() : hit.range() * Math.tan(Math.toRadians(action.angle() / 2.0)),
               hit.range(), action == DiarmuidAction.ROSE_DANCE ? 4 : 3, now, now + hit.tick(), now + hit.tick(), true, true, true));
      }
   }

   private static String cooldownKey(DiarmuidAction action, InteractionHand hand) {
      return PREFIX + (hand == null ? action.path() : "hand_" + hand.name());
   }

   public static long cooldown(LivingEntity owner, DiarmuidAction action) { return cooldown(owner, action, null); }
   private static long cooldown(LivingEntity owner, DiarmuidAction action, InteractionHand hand) {
      return owner.getPersistentData().getLong(cooldownKey(action, hand));
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
      Cast cast = CASTS.get(owner);
      if (cast == null) return;
      long now = level.getGameTime();
      if (!isOwner(owner) || cast.dimension != level.dimension()) { cancel(owner); return; }
      if (!canAct(owner)) { finish(owner); return; }
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
      if (!move(owner, cast, age)) return;
      for (int i = 0; i < cast.action.hits().size(); i++) {
         var strike = cast.action.hits().get(i);
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
         InteractionHand hand = spearHand(owner, strike.red());
         if (hand != null) owner.swing(hand, true);
         level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.45F, strike.red() ? 1.0F : 1.5F);
      }
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
      if (hand == null || cast.basicHand != null && hand != cast.basicHand) return;
      ItemStack stack = owner.getItemInHand(hand);
      var source = new CombatDamageSource(owner instanceof ServerPlayer player ? owner.damageSources().playerAttack(player)
         : owner.damageSources().mobAttack(owner), AttackOrigin.skill("diarmuid_ua_duibhne", cast.action.skillId()));
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

   private static Vec3 findLanding(LivingEntity owner) {
      if (!owner.onGround()) return null;
      Vec3 forward = horizontalLook(owner);
      for (double length = 6; length >= 1; length -= 0.5) {
         Vec3 landing = owner.position().add(forward.scale(length));
         var box = owner.getBoundingBox().move(landing.subtract(owner.position()));
         if (!owner.level().hasChunkAt(BlockPos.containing(landing)) || !owner.level().getWorldBorder().isWithinBounds(box)
            || !owner.level().noCollision(owner, box) || owner.level().noCollision(owner, box.move(0, -0.15, 0))) continue;
         boolean clear = true;
         for (int tick = 1; tick <= 16; tick++) {
            Vec3 point = leapPoint(owner.position(), landing, tick / 16.0);
            if (!owner.level().noCollision(owner, owner.getBoundingBox().move(point.subtract(owner.position())))) { clear = false; break; }
         }
         if (clear) return landing;
      }
      return null;
   }

   public static Vec3 leapPoint(Vec3 origin, Vec3 landing, double progress) {
      double t = Mth.clamp(progress, 0, 1);
      return origin.lerp(landing, t).add(0, 8 * t * (1 - t), 0);
   }

   private static boolean move(LivingEntity owner, Cast cast, int age) {
      double step = cast.action == DiarmuidAction.ADVANCING_THRUST && age >= 1 && age <= 5 ? 1.2 / 5
         : cast.action == DiarmuidAction.RETREATING_THRUST && age >= 1 && age <= 6 ? -2.0 / 6
         : cast.action == DiarmuidAction.ROSE_DANCE && age >= 22 && age <= 26 ? 1.2 / 5 : 0;
      if (step != 0) {
         Vec3 desired = cast.forward.scale(step);
         Vec3 before = owner.position();
         owner.move(MoverType.SELF, desired);
         syncMovement(owner);
         owner.setDeltaMovement(0, owner.getDeltaMovement().y, 0);
         owner.hurtMarked = true;
         if (owner.position().distanceToSqr(before) > 0.001 && owner.level() instanceof ServerLevel level)
            level.sendParticles(ParticleTypes.CLOUD, owner.getX(), owner.getY() + 0.04, owner.getZ(), 2, 0.12, 0.01, 0.12, 0.01);
      }
      if (cast.action != DiarmuidAction.MANA_LEAP || age < 4 || age > 20) return true;
      if (age == 4) {
         if (owner.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, owner.getX(), owner.getY() + 0.1, owner.getZ(), 12, 0.3, 0.1, 0.3, 0.03);
            level.playSound(null, owner.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.6F, 1.3F);
         }
      }
      if (age == 4) return true;
      if (age == 5) {
         owner.getPersistentData().putBoolean(FALL_PROTECTION, true);
         owner.getPersistentData().putString(FALL_DIMENSION, owner.level().dimension().location().toString());
      }
      Vec3 destination = leapPoint(cast.origin, cast.landing, (age - 4) / 16.0);
      Vec3 movement = destination.subtract(owner.position());
      owner.move(MoverType.SELF, movement);
      syncMovement(owner);
      owner.setDeltaMovement(Vec3.ZERO);
      owner.fallDistance = 0;
      owner.hurtMarked = true;
      if (owner.position().distanceToSqr(destination) > 0.0025) { finish(owner); return false; }
      if (age == 20) clearFallProtection(owner);
      return true;
   }

   private static void syncMovement(LivingEntity owner) {
      if (owner instanceof ServerPlayer player && player.connection != null)
         player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
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
      stopAnimation(owner);
   }

   public static void cancel(LivingEntity owner) {
      finish(owner);
      clearFallProtection(owner);
   }

   private static void play(LivingEntity owner, DiarmuidAction action) {
      if (owner instanceof DiarmuidUaDuibhneEntity npc) npc.triggerDiarmuidAction(action);
      var message = new DiarmuidActionAnimationMessage(owner.getUUID(), action.networkId());
      PacketDistributor.sendToPlayersTrackingEntity(owner, message);
      if (owner instanceof ServerPlayer player && player.connection != null) ModNetwork.sendToPlayer(player, message);
   }

   private static void stopAnimation(LivingEntity owner) {
      if (owner.level().isClientSide()) return;
      if (owner instanceof DiarmuidUaDuibhneEntity npc) npc.triggerDiarmuidAction(null);
      var message = new DiarmuidActionAnimationMessage(owner.getUUID(), -1);
      PacketDistributor.sendToPlayersTrackingEntity(owner, message);
      if (owner instanceof ServerPlayer player && player.connection != null) ModNetwork.sendToPlayer(player, message);
   }
}
