package net.xxxjk.TYPE_MOON_WORLD.servant.sasaki;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantDefenseTraits;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantSkillDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroRules.Technique;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

@EventBusSubscriber(modid = "typemoonworld")
public final class SasakiKojiroCombatService {
   public static final String CONCEALED = "SasakiKojiroConcealed";
   public static final String CONCEALMENT_UNTIL = "SasakiKojiroConcealmentUntil";
   public static final String COUNTER_UNTIL = "SasakiKojiroCounterUntil";
   private static final String NPC_COOLDOWN = "SasakiKojiroCooldown:";
   private static final String NPC_STEALTH_READY = "SasakiKojiroNpcStealthReady";
   private static final int PRESENCE_CONCEALMENT_COST = 18;
   private static final double TRANSPARENCY_RANGE = 8.0;
   private static final int TRANSPARENCY_COOLDOWN = 600;

   private SasakiKojiroCombatService() { }

   public static Technique techniqueForSlot(int slot) {
      return Technique.forSlot(slot);
   }

   public static boolean isSasakiOwner(LivingEntity entity) {
      if (entity instanceof SasakiKojiroEntity) return true;
      return entity instanceof ServerPlayer player && isSasakiCard(player);
   }

   public static double souwaBonus(LivingEntity attacker) {
      return SasakiKojiroRules.souwaBonus(souwaRank(attacker));
   }

   public static boolean rollDefenseBypass(DamageSource source) {
      if (source == null || !(source.getEntity() instanceof LivingEntity attacker)) return false;
      double chance = SasakiKojiroRules.souwaDefenseBypassChance(souwaRank(attacker));
      return chance > 0.0 && attacker.getRandom().nextFloat() < chance;
   }

   private static String souwaRank(LivingEntity attacker) {
      if (attacker == null) return "";
      ServantDefinition definition = ServantDefenseTraits.definition(attacker);
      if (definition == null) return "";
      for (String id : definition.skillIds()) {
         if (!skillKey(id).startsWith("souwa_expertise_")) continue;
         var skill = ServantSkillDataRegistry.get(id);
         String rank = skill == null ? id.substring(id.lastIndexOf('_') + 1) : skill.rank();
         return rank;
      }
      return "";
   }

   public static boolean isTsubameGaeshi(DamageSource source) {
      return source != null && "tsubame_gaeshi".equals(AttackOrigin.of(source).skill());
   }

   public static boolean isTechniqueDamage(DamageSource source) {
      AttackOrigin origin = source == null ? null : AttackOrigin.of(source);
      return isTsubameGaeshi(source) || origin != null && "sasaki_kojiro".equals(origin.servant())
         && Technique.forEffect(origin.skill()) != null;
   }

   public static boolean isConcealed(LivingEntity entity) {
      if (entity == null || !entity.getPersistentData().getBoolean(CONCEALED)) return false;
      long until = entity.getPersistentData().getLong(CONCEALMENT_UNTIL);
      return until == 0L || entity.level().getGameTime() < until;
   }

   public static boolean setConcealed(LivingEntity entity, boolean active) {
      if (entity == null || entity.level().isClientSide() || !isSasakiOwner(entity)) return false;
      CompoundTag data = entity.getPersistentData();
      if (!active) {
         data.remove(CONCEALED);
         data.remove(CONCEALMENT_UNTIL);
         entity.removeEffect(MobEffects.INVISIBILITY);
         return true;
      }
      if (isConcealed(entity)) return false;
      data.putBoolean(CONCEALED, true);
      data.putLong(CONCEALMENT_UNTIL, 0L);
      maintainInvisibility(entity, Integer.MAX_VALUE);
      clearHostileAggro(entity);
      return true;
   }

   public static void tick(LivingEntity entity) {
      if (entity == null || entity.level().isClientSide() || !isSasakiOwner(entity)) return;
      CompoundTag data = entity.getPersistentData();
      data.remove("MindseyeActive");
      data.remove("MindseyeDodgeChance");
      data.remove("MindseyeBlockChance");
      data.remove("StealthPassiveActive");
      if (!data.getBoolean(CONCEALED)) return;
      long now = entity.level().getGameTime();
      long until = data.getLong(CONCEALMENT_UNTIL);
      if (until > 0L && now >= until) {
         setConcealed(entity, false);
         return;
      }
      maintainInvisibility(entity, until == 0L ? Integer.MAX_VALUE : (int)Math.max(1L, until - now));
      if (now % 5L == 0L) clearHostileAggro(entity);
      String rank = skillRank(entity, "stealth_");
      int interval = SasakiKojiroRules.concealmentParticleInterval(rank);
      int count = SasakiKojiroRules.concealmentParticles(rank);
      if (count > 0 && now % interval == 0L && entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5,
            entity.getZ(), count, 0.34, Math.max(0.25, entity.getBbHeight() * 0.35), 0.34, 0.015);
      }
   }

   public static boolean performTransparency(LivingEntity entity) {
      if (entity == null || !(entity.level() instanceof ServerLevel level) || !isSasakiOwner(entity)) return false;
      LivingEntity preferred = entity instanceof ServerPlayer player ? findLookTarget(player, TRANSPARENCY_RANGE)
         : entity instanceof Mob mob ? mob.getTarget() : null;
      var targets = new java.util.ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
         entity.getBoundingBox().inflate(TRANSPARENCY_RANGE), target -> canDamageTarget(entity, target, TRANSPARENCY_RANGE)
            && entity.hasLineOfSight(target)));
      targets.sort(Comparator.comparingDouble(entity::distanceToSqr));
      if (targets.remove(preferred)) targets.addFirst(preferred);
      for (LivingEntity target : targets) {
         Vec3 destination = transparencyDestination(entity, target);
         if (destination == null) continue;
         float yaw = (float)Math.toDegrees(Math.atan2(target.getZ() - destination.z, target.getX() - destination.x)) - 90.0F;
         entity.stopRiding();
         if (entity instanceof ServerPlayer player) {
            player.connection.teleport(destination.x, destination.y, destination.z, yaw, 0.0F);
         } else {
            entity.teleportTo(destination.x, destination.y, destination.z);
            entity.setYRot(yaw);
            entity.setXRot(0.0F);
         }
         entity.setYHeadRot(yaw);
         entity.yBodyRot = yaw;
         entity.setDeltaMovement(Vec3.ZERO);
         entity.fallDistance = 0.0F;
         entity.hurtMarked = true;
         if (entity instanceof Mob mob) mob.getNavigation().stop();
         cleanseHarmfulEffects(entity);
         SasakiKojiroActionAnimationService.play(entity, SasakiKojiroAction.TRANSPARENCY);
         return true;
      }
      return false;
   }

   private static Vec3 transparencyDestination(LivingEntity caster, LivingEntity target) {
      ServerLevel level = (ServerLevel)caster.level();
      Vec3 behind = horizontalLook(target).scale(-1.0);
      double separation = Math.max(1.35, (caster.getBbWidth() + target.getBbWidth()) * 0.5 + 0.6);
      for (double distance : new double[]{separation, separation + 0.6}) {
         for (int angle : new int[]{0, 30, -30, 60, -60}) {
            Vec3 candidate = target.position().add(behind.yRot((float)Math.toRadians(angle)).scale(distance));
            for (int dy : new int[]{0, 1, -1}) {
               BlockPos feet = BlockPos.containing(candidate).offset(0, dy, 0);
               if (!level.hasChunkAt(feet) || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) continue;
               Vec3 destination = new Vec3(candidate.x, feet.getY(), candidate.z);
               AABB bounds = caster.getBoundingBox().move(destination.subtract(caster.position()));
               if (level.getWorldBorder().isWithinBounds(bounds) && level.noCollision(caster, bounds)
                  && !level.containsAnyLiquid(bounds)) return destination;
            }
         }
      }
      return null;
   }

   private static void cleanseHarmfulEffects(LivingEntity entity) {
      for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) entity.removeEffect(effect.getEffect());
      }
   }

   public static boolean canPerform(ServerPlayer player, Technique technique) {
      return player != null && technique != null && canPerform((LivingEntity)player, technique, findLookTarget(player, technique.range()));
   }

   public static boolean perform(ServerPlayer player, Technique technique) {
      if (player == null || technique == null || !(player.level() instanceof ServerLevel)) return false;
      LivingEntity target = technique == Technique.COUNTER_STANCE ? null : findLookTarget(player, technique.range());
      if (!canPerform(player, technique, target)) return false;
      return execute(player, target, technique);
   }

   public static boolean performTsubameGaeshi(LivingEntity attacker, LivingEntity target) {
      if (!canDamageTarget(attacker, target, 8.0) || !attacker.hasLineOfSight(target)) return false;
      playTsubamePresentation(attacker);
      applyTsubameDamage(attacker, target);
      return true;
   }

   public static void playTsubamePresentation(LivingEntity attacker) {
      if (attacker == null || attacker.level().isClientSide()) return;
      breakConcealment(attacker);
      SasakiKojiroActionAnimationService.play(attacker, SasakiKojiroAction.TSUBAME_GAESHI);
      if (attacker.level() instanceof ServerLevel level) {
         Vec3 origin = attacker.position().add(0.0, attacker.getBbHeight() * 0.54, 0.0)
            .add(horizontalLook(attacker).scale(0.9));
         VFXServerEffects.spawnOriented(level, "sasaki_tsubame", origin, horizontalLook(attacker), 96.0);
      }
   }

   public static boolean applyTsubameDamage(LivingEntity attacker, LivingEntity target) {
      if (!canDamageTarget(attacker, target, 8.0) || !attacker.hasLineOfSight(target)) return false;
      breakConcealment(attacker);
      DamageSource base = physicalAttackSource(attacker);
      DamageSource source = new CombatDamageSource(base, AttackOrigin.skill("sasaki_kojiro", "tsubame_gaeshi"));
      return target.hurt(source, 450.0F);
   }

   public static boolean tickNpc(SasakiKojiroEntity entity, LivingEntity target) {
      if (entity == null || target == null || !canDamageTarget(entity, target, 16.0)) return false;
      long now = entity.level().getGameTime();
      if (entity.isSasakiActionActive()) return true;
      double distance = entity.distanceTo(target);

      if (!isConcealed(entity) && distance >= 6.0 && entity.getCurrentMp() >= PRESENCE_CONCEALMENT_COST
         && now >= entity.getPersistentData().getLong(NPC_STEALTH_READY)) {
         entity.setCurrentMp(entity.getCurrentMp() - PRESENCE_CONCEALMENT_COST);
         entity.getPersistentData().putLong(NPC_STEALTH_READY, now + 120L);
         setConcealed(entity, true);
         SasakiKojiroActionAnimationService.play(entity, SasakiKojiroAction.PRESENCE_CONCEALMENT);
         return true;
      }

      if (needsTransparency(entity) && canNpcUse(entity, TRANSPARENCY_COST(), TRANSPARENCY_COOLDOWN, "transparency")) {
         if (performTransparency(entity)) {
            spendNpcCost(entity, TRANSPARENCY_COST(), TRANSPARENCY_COOLDOWN, "transparency");
            return true;
         }
      }

      if (distance <= Technique.TSUBAME_GAESHI.range()
         && canNpcUse(entity, Technique.TSUBAME_GAESHI.mpCost(), Technique.TSUBAME_GAESHI.cooldownTicks(), Technique.TSUBAME_GAESHI.effectId())
         && entity.getRandom().nextFloat() < 0.08F) {
         executeNpc(entity, target, Technique.TSUBAME_GAESHI);
         return true;
      }

      int nearby = nearbyTargets(entity, 4.0).size();
      if (nearby >= 2 && canNpcUse(entity, Technique.SPINNING_SLASH.mpCost(), Technique.SPINNING_SLASH.cooldownTicks(), Technique.SPINNING_SLASH.effectId())
         && entity.getRandom().nextFloat() < 0.22F && canPerform(entity, Technique.SPINNING_SLASH, target)) {
         executeNpc(entity, target, Technique.SPINNING_SLASH);
         return true;
      }

      if (distance <= Technique.COUNTER_STANCE.range()
         && canNpcUse(entity, Technique.COUNTER_STANCE.mpCost(), Technique.COUNTER_STANCE.cooldownTicks(), Technique.COUNTER_STANCE.effectId())
         && entity.getRandom().nextFloat() < 0.1F) {
         executeNpc(entity, target, Technique.COUNTER_STANCE);
         return true;
      }

      Technique technique = chooseNpcTechnique(entity, target, distance);
      if (technique != null && canNpcUse(entity, technique.mpCost(), technique.cooldownTicks(), technique.effectId())
         && canPerform(entity, technique, target)) {
         executeNpc(entity, target, technique);
         return true;
      }
      return false;
   }

   public static void startCounter(LivingEntity defender) {
      if (defender == null || defender.level().isClientSide()) return;
      defender.getPersistentData().putLong(COUNTER_UNTIL, defender.level().getGameTime() + 10L);
      SasakiKojiroActionAnimationService.play(defender, SasakiKojiroAction.COUNTER_STANCE);
      if (defender.level() instanceof ServerLevel level) {
         Vec3 origin = defender.position().add(0.0, defender.getBbHeight() * 0.58, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z, 8, 0.24, 0.32, 0.24, 0.015);
      }
   }

   public static void onSuccessfulGuard(LivingEntity defender, Entity attacker) {
      if (attacker instanceof LivingEntity target && defender != null
         && defender.getPersistentData().getLong(COUNTER_UNTIL) > defender.level().getGameTime()
         && canDamageTarget(defender, target, Technique.COUNTER_STANCE.range())) {
         defender.getPersistentData().remove(COUNTER_UNTIL);
         hit(defender, target, Technique.COUNTER_STANCE.damage(), Technique.COUNTER_STANCE.effectId());
      }
   }

   private static Technique chooseNpcTechnique(SasakiKojiroEntity entity, LivingEntity target, double distance) {
      if (distance > 4.5 || distance < 1.0) return null;
      Technique[] close = {Technique.VERTICAL_SLASH, Technique.DOWNWARD_SLASH, Technique.UPWARD_SLASH, Technique.HORIZONTAL_SLASH};
      if (distance <= 3.5) return close[entity.getRandom().nextInt(close.length)];
      if (distance <= 4.5) return Technique.THRUST;
      return null;
   }

   private static boolean executeNpc(SasakiKojiroEntity entity, LivingEntity target, Technique technique) {
      if (!canPerform(entity, technique, target)) return false;
      if (!execute(entity, target, technique)) return false;
      spendNpcCost(entity, technique.mpCost(), technique.cooldownTicks(), technique.effectId());
      return true;
   }

   private static boolean canNpcUse(SasakiKojiroEntity entity, double cost, int cooldown, String id) {
      return entity.getCurrentMp() + 1.0E-6 >= cost
         && entity.level().getGameTime() >= entity.getPersistentData().getLong(NPC_COOLDOWN + id);
   }

   private static void spendNpcCost(SasakiKojiroEntity entity, double cost, int cooldown, String id) {
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - cost));
      entity.getPersistentData().putLong(NPC_COOLDOWN + id, entity.level().getGameTime() + cooldown);
   }

   private static boolean canPerform(LivingEntity caster, Technique technique, LivingEntity target) {
      if (caster == null || technique == null || caster.level().isClientSide() || !caster.isAlive()) return false;
      if (technique == Technique.COUNTER_STANCE) return true;
      if (technique == Technique.SPINNING_SLASH) return !nearbyTargets(caster, technique.range()).isEmpty();
      if (!validTarget(caster, target)) return false;
      if (caster.distanceTo(target) > technique.range() || !caster.hasLineOfSight(target)) return false;
      return technique != Technique.THRUST || clearThrustDistance(caster, technique.range()) > 0.4;
   }

   private static boolean execute(LivingEntity caster, LivingEntity target, Technique technique) {
      if (technique == Technique.COUNTER_STANCE) {
         startCounter(caster);
         return true;
      }
      breakConcealment(caster);
      switch (technique) {
         case CROUCH_CUTBACK -> {
            SasakiKojiroActionAnimationService.play(caster, SasakiKojiroAction.CROUCH_CUTBACK);
            slashVfx(caster, 1.05);
            hitTwice(caster, target, technique.damage(), technique.effectId());
         }
         case VERTICAL_SLASH -> singleSlash(caster, target, technique, SasakiKojiroAction.VERTICAL_SLASH, 1.0);
         case DOWNWARD_SLASH -> singleSlash(caster, target, technique, SasakiKojiroAction.DOWNWARD_SLASH, 0.85);
         case UPWARD_SLASH -> {
            if (singleSlash(caster, target, technique, SasakiKojiroAction.UPWARD_SLASH, 0.9)) {
               target.push(0.0, 0.55, 0.0);
               target.hurtMarked = true;
            }
         }
         case HORIZONTAL_SLASH -> singleSlash(caster, target, technique, SasakiKojiroAction.HORIZONTAL_SLASH, 1.0);
         case THRUST -> {
            SasakiKojiroActionAnimationService.play(caster, SasakiKojiroAction.THRUST);
            slashVfx(caster, 1.15);
            Vec3 forward = horizontalLook(caster);
            double travel = Math.min(technique.range(), clearThrustDistance(caster, technique.range()));
            double distance = caster.distanceTo(target);
            if (distance <= travel + 0.35) hit(caster, target, technique.damage(), technique.effectId());
            caster.setDeltaMovement(forward.scale(Math.min(1.35, travel * 0.3)).add(0.0, caster.getDeltaMovement().y, 0.0));
            caster.hurtMarked = true;
         }
         case SPINNING_SLASH -> {
            SasakiKojiroActionAnimationService.play(caster, SasakiKojiroAction.SPINNING_SLASH);
            slashVfx(caster, 1.3);
            for (LivingEntity victim : nearbyTargets(caster, technique.range()).stream().limit(3).toList()) {
               hit(caster, victim, technique.damage(), technique.effectId());
            }
         }
         case TSUBAME_GAESHI -> {
            playTsubamePresentation(caster);
            applyTsubameDamage(caster, target);
         }
         case COUNTER_STANCE -> startCounter(caster);
      }
      return true;
   }

   private static boolean singleSlash(LivingEntity caster, LivingEntity target, Technique technique,
                                   SasakiKojiroAction action, double scale) {
      SasakiKojiroActionAnimationService.play(caster, action);
      slashVfx(caster, scale);
      return hit(caster, target, technique.damage(), technique.effectId());
   }

   private static void hitTwice(LivingEntity caster, LivingEntity target, float damage, String skill) {
      if (!validTarget(caster, target)) return;
      beforeAttack(caster);
      target.invulnerableTime = 0;
      target.hurt(damageSource(caster, skill), damage);
      target.invulnerableTime = 0;
      target.hurt(damageSource(caster, skill), damage);
   }

   private static boolean hit(LivingEntity caster, LivingEntity target, float damage, String skill) {
      if (!validTarget(caster, target)) return false;
      beforeAttack(caster);
      return target.hurt(damageSource(caster, skill), damage);
   }

   private static DamageSource damageSource(LivingEntity caster, String skill) {
      return new CombatDamageSource(physicalAttackSource(caster), AttackOrigin.skill("sasaki_kojiro", skill));
   }

   private static DamageSource physicalAttackSource(LivingEntity caster) {
      return caster instanceof ServerPlayer player ? player.damageSources().playerAttack(player) : caster.damageSources().mobAttack(caster);
   }

   private static LivingEntity findLookTarget(ServerPlayer player, double range) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, range, 1.0);
      return validTarget(player, target) && player.hasLineOfSight(target) ? target : null;
   }

   private static boolean validTarget(LivingEntity attacker, LivingEntity target) {
      return canDamageTarget(attacker, target, Double.MAX_VALUE);
   }

   private static boolean canDamageTarget(LivingEntity attacker, LivingEntity target, double range) {
      if (attacker == null || target == null || attacker == target || !target.isAlive()
         || attacker.level() != target.level() || EntityUtils.isImmunePlayerTarget(target)
         || EntityUtils.isUntargetableServantTransition(target) || attacker.distanceTo(target) > range
         || attacker.isAlliedTo(target) || target.isAlliedTo(attacker)) return false;
      if (target instanceof Player targetPlayer && attacker instanceof ServerPlayer sourcePlayer
         && !sourcePlayer.canHarmPlayer(targetPlayer)) return false;
      if (attacker instanceof ServerPlayer player
         && net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting.isContractMaster(player, target)) return false;
      return !(attacker instanceof ServantEntity servant
         && net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection.isProtectedMaster(servant, target));
   }

   private static List<LivingEntity> nearbyTargets(LivingEntity caster, double range) {
      Vec3 forward = horizontalLook(caster);
      Vec3 origin = caster.position().add(0.0, caster.getBbHeight() * 0.5, 0.0);
      AABB bounds = caster.getBoundingBox().inflate(range, 2.5, range);
      return caster.level().getEntitiesOfClass(LivingEntity.class, bounds, target -> validTarget(caster, target)
            && caster.distanceTo(target) <= range && caster.hasLineOfSight(target))
         .stream()
         .filter(target -> {
            Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(origin);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            return flat.lengthSqr() < 1.0E-5 || flat.normalize().dot(forward) >= 0.45;
         })
         .sorted(Comparator.comparingDouble(caster::distanceToSqr))
         .toList();
   }

   private static double clearThrustDistance(LivingEntity caster, double maxRange) {
      Vec3 start = caster.position().add(0.0, caster.getBbHeight() * 0.5, 0.0);
      Vec3 end = start.add(horizontalLook(caster).scale(maxRange));
      var hit = caster.level().clip(new net.minecraft.world.level.ClipContext(start, end,
         net.minecraft.world.level.ClipContext.Block.COLLIDER,
         net.minecraft.world.level.ClipContext.Fluid.NONE, caster));
      if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return maxRange;
      return Math.max(0.0, start.distanceTo(hit.getLocation()) - 0.35);
   }

   private static Vec3 horizontalLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      if (horizontal.lengthSqr() < 1.0E-5) return Vec3.directionFromRotation(0.0F, entity.getYRot()).normalize();
      return horizontal.normalize();
   }

   private static void slashVfx(LivingEntity caster, double scale) {
      if (!(caster.level() instanceof ServerLevel level)) return;
      Vec3 direction = horizontalLook(caster);
      Vec3 origin = caster.position().add(0.0, caster.getBbHeight() * 0.56, 0.0).add(direction.scale(0.65));
      VFXServerEffects.spawnOrientedScaled(level, "sasaki_slash", origin, direction, 64.0, (float)scale);
   }

   private static boolean needsTransparency(LivingEntity entity) {
      return entity.getHealth() <= entity.getMaxHealth() * 0.38
         || entity.getActiveEffects().stream().anyMatch(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
   }

   private static double TRANSPARENCY_COST() { return 20.0; }

   private static void breakConcealment(LivingEntity entity) {
      if (isConcealed(entity)) setConcealed(entity, false);
   }

   private static void beforeAttack(LivingEntity attacker) {
      breakConcealment(attacker);
      if (attacker != null) attacker.getPersistentData().remove(COUNTER_UNTIL);
   }

   private static void maintainInvisibility(LivingEntity entity, int duration) {
      MobEffectInstance effect = entity.getEffect(MobEffects.INVISIBILITY);
      int safeDuration = Math.max(1, duration);
      if (effect == null || effect.getDuration() < 10) {
         entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, safeDuration, 0, false, false, false));
      }
   }

   private static void clearHostileAggro(LivingEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      for (Mob mob : level.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(48.0),
         mob -> mob.isAlive() && mob.getTarget() == entity)) {
         mob.setTarget(null);
         mob.getNavigation().stop();
      }
   }

   private static String skillRank(LivingEntity entity, String skillId) {
      ServantDefinition definition = ServantDefenseTraits.definition(entity);
      if (definition == null) return "";
      for (String id : definition.skillIds()) {
         if (!skillKey(id).startsWith(skillId)) continue;
         var skill = ServantSkillDataRegistry.get(id);
         return skill == null ? "" : skill.rank();
      }
      return "";
   }

   private static String skillKey(String id) {
      int separator = id == null ? -1 : id.lastIndexOf(':');
      return separator < 0 ? (id == null ? "" : id) : id.substring(separator + 1);
   }

   private static boolean isSasakiCard(ServerPlayer player) {
      var card = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      return card.transformed() && "sasaki_kojiro".equals(card.id());
   }

   @SubscribeEvent
   public static void closeOnPlayerAttack(AttackEntityEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && isSasakiCard(player)) breakConcealment(player);
   }

   @SubscribeEvent
   public static void closeOnActualDamage(LivingDamageEvent.Post event) {
      if (event.getNewDamage() > 0.0F && isConcealed(event.getEntity())) setConcealed(event.getEntity(), false);
   }
}
