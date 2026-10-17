package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiBrain;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiControl;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiIntent;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantActionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPlannedActionExecutor;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor.Tag;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatTempoService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantDuelPacing;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantEngagementService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantManeuverService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshDuelState;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;

/** Ordinary NPC exchanges. All movement and damage remain server authoritative. */
public final class ServantCombatExchangeService {
   public enum State { APPROACH, CLASH, BREAK, PURSUIT, RECOVERY }
   public static final String LAST_ACTIVITY = "TypeMoonCombatLastActivityTick";
   public static final String MAX_IDLE = "TypeMoonCombatMaxIdleTicks";
   public static final String EXCHANGES = "TypeMoonCombatExchangeCount";
   public static final String DASHES = "TypeMoonCombatDashCount";
   public static final String CLASHES = "TypeMoonCombatClashCount";
   public static final String LAUNCHES = "TypeMoonCombatLaunchCount";
   public static final String PURSUITS = "TypeMoonCombatPursuitCount";
   public static final String FOOTWORK = "TypeMoonCombatFootworkCount";
   private static final String P = "TypeMoonExchange";
   private static final ResourceLocation INTENT = ResourceLocation.fromNamespaceAndPath("typemoonworld", "ai/combat_exchange");
   private static final ResourceLocation BASIC = ResourceLocation.fromNamespaceAndPath("typemoonworld", "ai/exchange_basic");
   private static final String[] SESSION_KEYS = {"Target", "State", "Combo", "Windup", "WindupTarget", "NextDash", "DashUntil",
      "LastLaunch", "LastHit", "LastContact", "LastReactiveDodge", "LastRetreat", "BasicPoseUntil", "DashX", "DashZ",
      "DashControlTick", "DashForwardX", "DashForwardZ", "DashSide", "DashSpeed", "DashStart", "DashSeparate", "DashPursuit",
      "FootworkTick", "FootworkUntil", "FootworkX", "FootworkZ", "FootworkRetreat", "GuardPressure", "X", "Y", "Z"};

   private ServantCombatExchangeService() { }

   public static boolean scripted(ServantEntity actor, LivingEntity target) {
      return specialState(actor) || target != null && specialState(target)
         || target instanceof ServantEntity other && isScriptedPair(actor.getServantId(), other.getServantId());
   }

   public static boolean isScriptedPair(String left, String right) {
      return "gilgamesh".equals(left) && "enkidu".equals(right)
         || "enkidu".equals(left) && "gilgamesh".equals(right);
   }

   private static boolean specialState(LivingEntity entity) {
      CompoundTag d = entity.getPersistentData();
      return GilgameshDuelState.isActive(entity) || d.getBoolean("IskandarIonioiActive")
         || d.getBoolean("IskandarIonioiOffscreenDuel") || d.getBoolean("EmiyaUbwOffscreenDuel")
         || d.getBoolean("OdaHajunOffscreenDuel") || entity.isPassenger();
   }

   public static boolean ordinary(ServantEntity actor, LivingEntity target) {
      return actor != null && target instanceof ServantEntity && actor != target && actor.isAlive() && target.isAlive()
         && actor.level() == target.level() && !actor.isAlliedTo(target) && !scripted(actor, target);
   }

   /** Runs even when stun, a cast or a character's helper owns this tick. */
   public static void observe(ServantEntity actor) {
      if (!(actor.level() instanceof ServerLevel)) return;
      LivingEntity target = actor.getTarget();
      CompoundTag d = actor.getPersistentData();
      long now = actor.level().getGameTime();
      if (!(target instanceof ServantEntity) || !target.isAlive() || actor.level() != target.level()) {
         clearSession(actor);
         return;
      }
      if (!d.hasUUID(P + "Target") || !d.getUUID(P + "Target").equals(target.getUUID())) {
         clearSession(actor);
         d.putUUID(P + "Target", target.getUUID());
         d.putLong(LAST_ACTIVITY, now);
         d.putLong(P + "LastContact", now);
         storePosition(actor);
      }
      d.putBoolean(P + "Scripted", scripted(actor, target));
      Vec3 previous = new Vec3(d.getDouble(P + "X"), d.getDouble(P + "Y"), d.getDouble(P + "Z"));
      boolean moving = actor.position().distanceToSqr(previous) >= 0.0025;
      storePosition(actor);
      boolean recovery = ServantCombatMotionService.state(actor) != ServantCombatMotionService.MotionState.NONE
         || ServantCombatSystem.cannotAct(actor) || ServantCombatMotionService.isRecovering(actor);
      setState(actor, selectState(recovery, ServantCombatMotionService.canPursue(actor, target),
         actor.distanceTo(target), now < d.getLong(P + "DashUntil")));
      if (recovery || protectedAction(actor) || moving || ServantCombatDefenseService.isGuarding(actor)
         || target instanceof ServantEntity opponent && (opponent.isAttackSwinging() || opponent.isPerformingAction())
         || ServantCombatMotionService.isLaunched(target)) {
         activity(actor, now);
      }
      // Only visible, unobstructed ground exchanges have a half-second deadline.
      if (ordinary(actor, target) && actor.onGround() && target.onGround() && actor.distanceTo(target) <= 8
         && actor.hasLineOfSight(target)) {
         d.putInt(MAX_IDLE, Math.max(d.getInt(MAX_IDLE), (int)idleTicks(actor, now)));
      }
      if (recovery || scripted(actor, target)) cancelWindup(actor);
   }

   public static State selectState(boolean recovery, boolean pursuit, double distance, boolean breaking) {
      return recovery ? State.RECOVERY : pursuit ? State.PURSUIT : breaking ? State.BREAK
         : distance <= 4.35 ? State.CLASH : State.APPROACH;
   }

   public static State state(ServantEntity actor) {
      try { return State.valueOf(actor.getPersistentData().getString(P + "State")); }
      catch (IllegalArgumentException ignored) { return State.APPROACH; }
   }

   private static void setState(ServantEntity actor, State state) {
      actor.getPersistentData().putString(P + "State", state.name());
   }

   private static void storePosition(ServantEntity actor) {
      CompoundTag d = actor.getPersistentData();
      d.putDouble(P + "X", actor.getX()); d.putDouble(P + "Y", actor.getY()); d.putDouble(P + "Z", actor.getZ());
   }

   public static void clearSession(ServantEntity actor) {
      cancelWindup(actor);
      CompoundTag d = actor.getPersistentData();
      for (String key : SESSION_KEYS) d.remove(P + key);
      d.remove(P + "Scripted");
      d.remove(LAST_ACTIVITY);
   }

   public static long idleTicks(ServantEntity actor, long now) {
      CompoundTag d = actor.getPersistentData();
      return d.contains(LAST_ACTIVITY) ? Math.max(0, now - d.getLong(LAST_ACTIVITY)) : 0;
   }

   public static void activity(LivingEntity actor, long now) {
      if (actor instanceof ServantEntity) actor.getPersistentData().putLong(LAST_ACTIVITY, now);
   }

   public static boolean protectedAction(ServantEntity actor) {
      return actor.isPerformingAction() || actor.isAttackSwinging() || actor.isRoaring() || actor.isSlamming()
         || NoblePhantasmCastService.chanting(actor) || ServantDuelPacing.nativeNpActive(actor)
         || hasWindup(actor) || ServantPlannedActionExecutor.isActive(actor)
            && ServantPlannedActionExecutor.stage(actor) != ServantPlannedActionExecutor.Stage.APPROACH;
   }

   public static void submit(ServantEntity actor, AiBrain brain, long now) {
      LivingEntity target = actor.getTarget();
      if (!ordinary(actor, target)) return;
      // Master commands, lethal defense and NP/phase transitions retain priority.
      if (protectedAction(actor) && !hasWindup(actor) && !ServantPlannedActionExecutor.isActive(actor)
         && !ServantDuelPacing.nativeNpActive(actor)) {
         brain.submit(AiIntent.of(INTENT, AiIntent.PRIORITY_POSITION + 8, 100, 1, true,
            () -> {
               actor.getLookControl().setLookAt(target, 65, 55);
               if (now < actor.getPersistentData().getLong(P + "BasicPoseUntil")) continueBasicFootwork(actor, target, now);
            }, AiControl.ATTACK, AiControl.MOVE, AiControl.LOOK));
         return;
      }
      if (hasWindup(actor) || !protectedAction(actor)) {
         int priority = hasWindup(actor) || idleTicks(actor, now) >= 6 || state(actor) == State.PURSUIT
            ? AiIntent.PRIORITY_ATTACK + 8 : AiIntent.PRIORITY_POSITION + 8;
         if (!hasWindup(actor) && ServantDuelPacing.skillDecisionAllowed(actor)) priority = AiIntent.PRIORITY_POSITION + 8;
         boolean attacking = hasWindup(actor) || !ServantDuelPacing.skillDecisionAllowed(actor)
            && ServantDuelPacing.basicReady(actor, now) && ServantCombatTempoService.canAttemptBasicAttack(actor, target);
         brain.submit(AiIntent.attempt(INTENT, priority, 150 + idleTicks(actor, now), 1, true,
            () -> act(actor, target, now), attacking ? AiControl.ATTACK : AiControl.MOVE, AiControl.MOVE, AiControl.LOOK));
      }
   }

   public static boolean act(ServantEntity actor, LivingEntity target, long now) {
      if (!ordinary(actor, target) || ServantCombatSystem.cannotAct(actor)) return false;
      if (ServantCombatDefenseService.isDodgeActive(actor, now)) return true;
      if (dashing(actor, now)) return ServantManeuverService.continueExchangeDash(actor, now);
      if (hasWindup(actor)) return tickWindup(actor, target, now);
      if (protectedAction(actor) || !actor.onGround()) return false;
      double distance = actor.distanceTo(target);
      boolean ranged = ServantEngagementService.role(actor) == ServantEngagementService.CombatRole.RANGED;
      if (ServantCombatMotionService.isLaunched(target) && !ServantCombatMotionService.canPursue(actor, target)) {
         return ServantManeuverService.exchangeFootwork(actor, target, now, side(actor, target, now), true, 0.65);
      }

      // A telegraphed swing is a visual cue for a fast defensive beat.  The
      // reaction is intentionally throttled so the pair does not jitter every
      // tick, but it gives close combat a visible backstep instead of two
      // bodies sliding through one another.
      if (!ranged && target instanceof ServantEntity opponent
         && (opponent.isAttackSwinging() || opponent.isPerformingAction())
         && distance <= 4.8
         && now - actor.getPersistentData().getLong(P + "LastReactiveDodge") >= 18L
         && Math.floorMod(now + actor.getId(), 5) == 0) {
         int escapeSide = side(actor, target, now);
         if (ServantCombatDefenseService.requestDodge(actor, -0.95F, escapeSide * 0.85F)) {
            actor.getPersistentData().putLong(P + "LastReactiveDodge", now);
            ServantCombatSpectacleService.exchangeFx(actor, actor, true, false);
            activity(actor, now);
            return true;
         }
      }
      if (distance < 4 && ServantCombatDefenseService.broken(actor) && hasWindup((ServantEntity)target)
         && ServantManeuverService.safeExchangeRoute(actor,
            ServantManeuverService.sideForwardDirection(target.position().subtract(actor.position()), 55 * side(actor, target, now)).scale(3))
         && ServantCombatDefenseService.requestDodge(actor, 0.65F, side(actor, target, now))) return true;
      if (state(actor) == State.PURSUIT) {
         if (ServantCombatTempoService.canAttemptBasicAttack(actor, target) && ServantDuelPacing.basicReady(actor, now))
            return beginBasic(actor, target, now);
         if (ServantManeuverService.tryExchangeDash(actor, target, now, side(actor, target, now), true)) {
            increment(actor, PURSUITS); return true;
         }
      }
      if (!ranged && !ServantDuelPacing.skillDecisionAllowed(actor) && idleTicks(actor, now) >= 6 && ServantDuelPacing.basicReady(actor, now)
         && ServantCombatTempoService.canAttemptBasicAttack(actor, target)) return beginBasic(actor, target, now);
      if (ranged) {
         // Casters and archers keep their projectile cadence and preferred range.
         return distance <= 18 && ServantManeuverService.exchangeFootwork(actor, target, now, side(actor, target, now), true);
      }
      if (distance > 3.8 || distance < 1.25 || idleTicks(actor, now) >= 10) {
         if (ServantManeuverService.tryExchangeDash(actor, target, now, side(actor, target, now), false)) return true;
      }
      return distance <= 14 && ServantManeuverService.exchangeFootwork(actor, target, now, side(actor, target, now), false);
   }

   /** Alternates every 12-18 ticks; both actors use the same absolute phase. */
   public static int side(ServantEntity actor, LivingEntity target, long now) {
      int side = side(actor.getUUID(), target.getUUID(), now);
      actor.getPersistentData().putInt(P + "Side", side);
      return side;
   }

   public static int side(UUID actor, UUID target, long now) {
      long hash = actor.getLeastSignificantBits() ^ target.getLeastSignificantBits();
      int period = 12 + (int)Math.floorMod(hash, 7L);
      return (actor.compareTo(target) < 0 ? 1 : -1) * (Math.floorMod(now / period, 2) == 0 ? 1 : -1);
   }

   public static boolean hasWindup(ServantEntity actor) {
      return actor.getPersistentData().contains(P + "Windup");
   }

   public static boolean beginBasic(ServantEntity actor, LivingEntity target, long now) {
      if (!ordinary(actor, target) || protectedAction(actor) || !ServantDuelPacing.basicReady(actor, now)
         || !ServantCombatTempoService.canAttemptBasicAttack(actor, target)) return false;
      CompoundTag d = actor.getPersistentData();
      d.putLong(P + "Windup", now + 4);
      d.putLong(P + "BasicPoseUntil", now + 10);
      d.remove(P + "DashUntil");
      d.putUUID(P + "WindupTarget", target.getUUID());
      actor.getNavigation().stop(); actor.faceToward(target.position());
      actor.triggerBasicAttackAnimation();
      if (actor.onGround() && target.onGround() && d.getInt("TypeMoonDuelBasicAttacks") % 12 == 11
         && actor.distanceTo(target) <= 3.5) {
         Vec3 motion = actor.getDeltaMovement();
         actor.setDeltaMovement(motion.x, Math.max(motion.y, 0.42), motion.z);
         actor.triggerJumpAttackAnimation();
         actor.hasImpulse = true;
         actor.hurtMarked = true;
         d.putLong(P + "BasicPoseUntil", now + 20);
      }
      if (actor.level() instanceof ServerLevel level) {
         CombatThreatService.publish(level, new CombatThreat(BASIC, actor.getUUID(), target.getUUID(),
            actor.getEyePosition(), target.getEyePosition().subtract(actor.getEyePosition()), CombatThreat.Shape.SPHERE,
            ServantCombatTempoService.basicAttackReach(actor, target), 0, 1, now, now + 4, now + 4, true, false, true));
      }
      activity(actor, now);
      return true;
   }

   private static boolean tickWindup(ServantEntity actor, LivingEntity target, long now) {
      CompoundTag d = actor.getPersistentData();
      if (!d.hasUUID(P + "WindupTarget") || !d.getUUID(P + "WindupTarget").equals(target.getUUID())) {
         cancelWindup(actor); return false;
      }
      if (now - d.getLong(P + "Windup") > 2) { cancelWindup(actor); return false; }
      actor.faceToward(target.position()); actor.getNavigation().stop();
      ServantManeuverService.exchangeFootwork(actor, target, now, side(actor, target, now), false, 0.45);
      activity(actor, now);
      if (now < d.getLong(P + "Windup")) return true;
      cancelWindup(actor);
      // This is the only damage call. The animation started four ticks earlier.
      if (ServantCombatTempoService.resolveTelegraphedBasic(actor, target, now)) ServantDuelPacing.noteBasic(actor, now);
      else ServantDuelPacing.noteBasicMiss(actor, now);
      return true;
   }

   private static void continueBasicFootwork(ServantEntity actor, LivingEntity target, long now) {
      if (dashing(actor, now)) { ServantManeuverService.continueExchangeDash(actor, now); return; }
      if (ServantCombatMotionService.isLaunched(target) && !ServantCombatMotionService.canPursue(actor, target)) {
         ServantManeuverService.exchangeFootwork(actor, target, now, side(actor, target, now), true, 0.65);
         return;
      }
      if (ServantCombatMotionService.canPursue(actor, target) && actor.distanceTo(target) > 4
         && ServantManeuverService.tryExchangeDash(actor, target, now, side(actor, target, now), true)) {
         increment(actor, PURSUITS); return;
      }
      ServantManeuverService.exchangeFootwork(actor, target, now, side(actor, target, now), false, 0.65);
   }

   public static void cancelWindup(ServantEntity actor) {
      if (!hasWindup(actor)) return;
      actor.getPersistentData().remove(P + "Windup"); actor.getPersistentData().remove(P + "WindupTarget");
      if (actor.level() instanceof ServerLevel level) CombatThreatService.clearForSource(level, actor.getUUID());
   }

   public static void dashStarted(ServantEntity actor, long now, boolean pursuit) {
      CompoundTag d = actor.getPersistentData();
      d.putLong(P + "NextDash", now + (pursuit ? 10 : 18));
      d.putLong(P + "DashUntil", now + 6);
      setState(actor, pursuit ? State.PURSUIT : State.BREAK);
      increment(actor, DASHES); activity(actor, now);
      ServantCombatSpectacleService.exchangeFx(actor, actor, true, false);
   }

   public static boolean dashReady(ServantEntity actor, long now) { return now >= actor.getPersistentData().getLong(P + "NextDash"); }
   public static boolean dashing(ServantEntity actor, long now) { return now < actor.getPersistentData().getLong(P + "DashUntil"); }

   public static void recordContact(ServantEntity actor, LivingEntity target, long now) {
      activity(actor, now); activity(target, now);
      increment(actor, EXCHANGES);
      actor.getPersistentData().putLong(P + "LastContact", now);
   }

   /** Called only after actual, unguarded melee damage; never adds another damage hit. */
   public static void meleeHit(ServantEntity actor, LivingEntity target) {
      if (!ordinary(actor, target) || !target.isAlive() || ServantPlannedActionExecutor.isActive(actor)
         || ServantCombatMotionService.isLaunched(target) || ServantCombatMotionService.isRecovering(target)) return;
      long now = actor.level().getGameTime();
      CompoundTag d = actor.getPersistentData();
      if (d.contains(P + "LastHit") && d.getLong(P + "LastHit") == now) return;
      d.putLong(P + "LastHit", now);
      int combo = d.getInt(P + "Combo") + 1; d.putInt(P + "Combo", combo);
      increment(actor, CLASHES);
      ServantCombatSpectacleService.exchangeFx(actor, target, false, false);
      var profile = ServantActionRegistry.get(actor.getServantId());
      boolean launcher = profile != null && profile.actions().stream().anyMatch(a -> a.tags().contains(Tag.LAUNCHER));
      Vec3 direction = target.position().subtract(actor.position()).multiply(1, 0, 1).normalize();
      // Every third or fourth clean hit can become a launcher.  The window is
      // short enough to read as a combo finisher, while still allowing a long
      // duel to show repeated knockback and wall impacts.
      if (combo % 3 == 0 && now - d.getLong(P + "LastLaunch") >= 48) {
         d.putLong(P + "LastLaunch", now);
         ServantCombatMotionService.launch(actor, target, direction, 1.35, launcher ? 0.78 : 0.36,
            TerrainImpactProfile.Tier.SMALL, 32);
      } else {
         double force = combo % 3 == 0 ? 0.34 : 0.18;
         target.push(direction.x * force, 0, direction.z * force);
         target.hurtMarked = true;
      }
   }

   public static void increment(ServantEntity actor, String key) {
      CompoundTag d = actor.getPersistentData(); d.putInt(key, d.getInt(key) + 1);
   }
}
