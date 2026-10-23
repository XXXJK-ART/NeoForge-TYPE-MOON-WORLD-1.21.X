package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import static net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.*;

/** Keeps unobserved NPC battles in their source level, so no empty dimension needs to keep ticking. */
@EventBusSubscriber(modid = "typemoonworld")
public final class OdaHajunDuelService {
   public static final String PREVIOUS_NO_GRAVITY = "OdaHajunOffscreenPrevNoGravity";
   public static final String LAST_OUTCOME = "OdaHajunLastDuelOutcome";
   private static final Map<UUID, Duel> DUELS = new LinkedHashMap<>();
   private OdaHajunDuelService() {}

   private record Participant(LivingEntity entity, Vec3 position) {}
   private static final class Duel {
      final OdaNobunagaEntity owner;
      final ServerLevel level;
      final List<Participant> participants;
      int remaining;
      Duel(OdaNobunagaEntity owner, List<LivingEntity> targets) {
         this.owner = owner; this.level = (ServerLevel) owner.level();
         participants = new ArrayList<>();
         participants.add(new Participant(owner, owner.position()));
         targets.forEach(target -> participants.add(new Participant(target, target.position())));
         remaining = OdaHajunDuelRules.MIN_DURATION
            + owner.getRandom().nextInt(OdaHajunDuelRules.MAX_DURATION - OdaHajunDuelRules.MIN_DURATION + 1);
      }
   }

   public static boolean active(LivingEntity entity) {
      return entity.getPersistentData().getBoolean(TAG_HAJUN_OFFSCREEN_DUEL);
   }

   public static boolean start(OdaNobunagaEntity owner, List<LivingEntity> targets) {
      if (!(owner.level() instanceof ServerLevel) || !owner.isAlive() || active(owner) || targets.isEmpty()
         || targets.stream().anyMatch(target -> target == owner || target instanceof Player || !target.isAlive()
            || target.isRemoved() || target.level() != owner.level() || active(target))) return false;
      OdaAbilityService.clear(owner);
      var duel = new Duel(owner, targets);
      DUELS.put(owner.getUUID(), duel);
      for (var participant : duel.participants) hide(participant.entity());
      effect(duel);
      return true;
   }

   private static void hide(LivingEntity living) {
      living.stopRiding();
      var data = living.getPersistentData();
      data.putBoolean(TAG_HAJUN_OFFSCREEN_DUEL, true);
      data.putBoolean(TAG_HAJUN_OFFSCREEN_PREVIOUS_INVISIBLE, living.isInvisible());
      data.putBoolean(TAG_HAJUN_OFFSCREEN_PREVIOUS_INVULNERABLE, living.isInvulnerable());
      data.putBoolean(PREVIOUS_NO_GRAVITY, living.isNoGravity());
      if (living instanceof Mob mob) {
         data.putBoolean(TAG_HAJUN_OFFSCREEN_PREVIOUS_NO_AI, mob.isNoAi());
         mob.setTarget(null); mob.setNoAi(true); mob.getNavigation().stop();
      }
      living.setInvisible(true); living.setInvulnerable(true); living.setNoGravity(true);
      living.setDeltaMovement(Vec3.ZERO); living.hurtMarked = true;
   }

   @SubscribeEvent
   public static void freeze(EntityTickEvent.Pre event) {
      if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof LivingEntity living && active(living))
         event.setCanceled(true);
   }

   @SubscribeEvent
   public static void tick(ServerTickEvent.Post event) {
      for (var duel : new ArrayList<>(DUELS.values())) {
         if (duel.participants.stream().anyMatch(p -> !p.entity().isAlive() || p.entity().isRemoved()
            || p.entity().level() != duel.level || !active(p.entity()))) {
            cancel(duel.owner);
            continue;
         }
         for (var participant : duel.participants) {
            participant.entity().setPos(participant.position());
            participant.entity().setDeltaMovement(Vec3.ZERO);
         }
         if (--duel.remaining > 0) continue;
         DUELS.remove(duel.owner.getUUID());
         // Restore everyone before applying any death hooks or loot in the source world.
         duel.participants.forEach(p -> OdaNobunagaCombatHelper.restoreLegacyDuelState(p.entity()));
         effect(duel);
         var targets = duel.participants.stream().skip(1).map(Participant::entity).toList();
         resolve(duel.owner, targets, weights(duel.owner, targets).roll(duel.owner.getRandom().nextDouble()));
      }
   }

   public static OdaHajunDuelRules.Weights weights(OdaNobunagaEntity owner, List<LivingEntity> targets) {
      double opposition = 0;
      for (var target : targets) {
         opposition += score(target) / OdaHajunDuelRules.matchupMultiplier(OdaCombatEvents.divinity(target), OdaCombatEvents.era(target));
      }
      return OdaHajunDuelRules.weights(score(owner), opposition);
   }

   private static double score(LivingEntity entity) {
      var definition = ServantIdentityHelper.definitionOf(entity);
      double panel;
      if (definition != null) panel = OdaHajunDuelRules.panelScore(definition.parameters());
      else {
         var attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
         var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
         panel = entity.getMaxHealth() / 10 * .25 + (attack == null ? 1 : attack.getBaseValue()) * 2 * .25
            + (speed == null ? 0 : Math.max(0, (speed.getBaseValue() - .16) / .004)) * .20;
      }
      return Math.max(1, panel) * OdaHajunDuelRules.healthFactor(entity.getHealth() / Math.max(1, entity.getMaxHealth()));
   }

   /** Applies a terminal battle result once; damage multipliers and evasion must not roll a second battle. */
   public static void resolve(OdaNobunagaEntity owner, List<LivingEntity> targets, OdaHajunDuelRules.Outcome outcome) {
      if (!(owner.level() instanceof ServerLevel) || !owner.isAlive() || targets.isEmpty()
         || targets.stream().anyMatch(target -> target == owner || target instanceof Player
         || !target.isAlive() || target.level() != owner.level())) return;
      owner.getPersistentData().putString(LAST_OUTCOME, outcome.name());
      targets.forEach(target -> target.getPersistentData().putString(LAST_OUTCOME, outcome.name()));
      switch (outcome) {
         case ODA_WINS -> targets.forEach(target -> defeat(target, owner));
         case OPPONENTS_WIN -> defeat(owner, targets.getFirst());
         case BOTH_DIE -> {
            targets.forEach(target -> defeat(target, owner));
            defeat(owner, targets.getFirst());
         }
         case NEITHER_WINS -> {
            owner.setHealth(Math.max(1, owner.getHealth() * .7F));
            targets.forEach(target -> target.setHealth(Math.max(1, target.getHealth() * .7F)));
         }
      }
      if (owner.isAlive()) {
         var survivor = targets.stream().filter(LivingEntity::isAlive).findFirst().orElse(null);
         owner.setTarget(survivor);
         if (survivor instanceof Mob mob) mob.setTarget(owner);
      }
   }

   private static void defeat(LivingEntity loser, LivingEntity winner) {
      var source = loser.damageSources().mobAttack(winner);
      loser.setLastHurtByMob(winner);
      loser.getCombatTracker().recordDamage(source, loser.getHealth());
      loser.setHealth(0);
      loser.die(source);
   }

   public static void cancel(LivingEntity participant) {
      for (var duel : new ArrayList<>(DUELS.values())) {
         if (duel.participants.stream().noneMatch(p -> p.entity() == participant)) continue;
         DUELS.remove(duel.owner.getUUID());
         duel.participants.forEach(p -> OdaNobunagaCombatHelper.restoreLegacyDuelState(p.entity()));
      }
   }

   @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
      if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) cancel(living);
   }
   @SubscribeEvent public static void travel(EntityTravelToDimensionEvent event) {
      if (event.getEntity() instanceof LivingEntity living) cancel(living);
   }
   @SubscribeEvent public static void stopping(ServerStoppingEvent event) {
      for (var duel : new ArrayList<>(DUELS.values())) cancel(duel.owner);
   }

   private static void effect(Duel duel) {
      var owner = duel.owner;
      duel.level.sendParticles(ParticleTypes.SMOKE, owner.getX(), owner.getY() + .5, owner.getZ(), 32, 2, .4, 2, .03);
      duel.level.playSound(null, owner.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1, .7F);
   }
}
