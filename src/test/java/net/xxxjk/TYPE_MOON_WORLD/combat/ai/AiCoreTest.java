package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamClashManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatTempoService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactBypass;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactCondition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactType;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

class AiCoreTest {
   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", path);
   }

   @Test
   void commitmentOnlyYieldsWhenMarkedInterruptibleAndReplacementIsHigherPriority() {
      AiBlackboard board = new AiBlackboard();
      AiIntent attack = AiIntent.of(id("attack"), AiIntent.PRIORITY_ATTACK, 1.0, 20, true, () -> { }, AiControl.ATTACK);
      board.commit(attack, 100L);
      AiIntent movement = AiIntent.of(id("move"), AiIntent.PRIORITY_POSITION, 100.0, 1, true, () -> { }, AiControl.MOVE);
      AiIntent defense = AiIntent.of(id("defense"), AiIntent.PRIORITY_LETHAL_DEFENSE, 1.0, 1, true, () -> { }, AiControl.DEFEND);
      assertTrue(board.commitmentBlocks(movement, 105L));
      assertFalse(board.commitmentBlocks(defense, 105L));

      board.commit(AiIntent.of(id("locked"), AiIntent.PRIORITY_ATTACK, 1.0, 20, false, () -> { }, AiControl.CAST), 200L);
      assertTrue(board.commitmentBlocks(defense, 205L));
   }

   @Test
   void memoryIsCappedAndExpires() {
      AiBlackboard board = new AiBlackboard();
      UUID retained = null;
      for (int i = 0; i < 10; i++) {
         UUID opponent = new UUID(0L, i + 1L);
         retained = opponent;
         for (int action = 0; action < 10; action++) board.observe(opponent, id("action_" + action), 4.0, 1.0, action % 2 == 0, i);
      }
      assertEquals(8, board.opponent(retained).observedActions());
      assertEquals(AiBlackboard.OpponentSnapshot.EMPTY, board.opponent(new UUID(0L, 1L)));
      board.beginTick(700L);
      assertEquals(AiBlackboard.OpponentSnapshot.EMPTY, board.opponent(retained));
   }

   @Test
   void threatLineGeometryUsesRadiusAndLength() {
      CombatThreat threat = new CombatThreat(id("beam"), UUID.randomUUID(), null, Vec3.ZERO, new Vec3(1, 0, 0),
         CombatThreat.Shape.LINE, 1.0, 10.0, 4, 0, 5, 10, false, true, true);
      assertTrue(threat.threatens(new Vec3(6, 0.8, 0), 0.1));
      assertFalse(threat.threatens(new Vec3(12, 0, 0), 0.1));
      assertFalse(threat.threatens(new Vec3(6, 3, 0), 0.1));
   }

   @Test
   void threatAreaGeometryMatchesTelegraphedShapes() {
      UUID source = UUID.randomUUID();
      CombatThreat sphere = new CombatThreat(id("sphere"), source, null, Vec3.ZERO, new Vec3(1, 0, 0),
         CombatThreat.Shape.SPHERE, 3.0, 0.0, 2, 0, 3, 5, true, true, true);
      assertTrue(sphere.threatens(new Vec3(2.9, 0, 0), 0.0));
      assertFalse(sphere.threatens(new Vec3(3.1, 0, 0), 0.0));

      CombatThreat cone = new CombatThreat(id("cone"), source, null, Vec3.ZERO, new Vec3(1, 0, 0),
         CombatThreat.Shape.CONE, 4.0, 10.0, 3, 0, 3, 5, true, true, true);
      assertTrue(cone.threatens(new Vec3(5.0, 1.9, 0), 0.0));
      assertFalse(cone.threatens(new Vec3(5.0, 2.1, 0), 0.0));
      assertFalse(cone.threatens(new Vec3(-0.2, 0, 0), 0.0));

      CombatThreat hemisphere = new CombatThreat(id("hemisphere"), source, null, Vec3.ZERO, new Vec3(0, 1, 0),
         CombatThreat.Shape.HEMISPHERE, 4.0, 0.0, 3, 0, 3, 5, true, true, true);
      assertTrue(hemisphere.threatens(new Vec3(0, 3.9, 0), 0.0));
      assertFalse(hemisphere.threatens(new Vec3(0, -0.1, 0), 0.0));
   }

   @Test
   void failedPrimaryFallsBackWithoutCreatingCommitment() {
      List<String> executed = new ArrayList<>();
      AiIntent unavailable = AiIntent.attempt(id("unavailable"), AiIntent.PRIORITY_ATTACK, 50.0, 20, true,
         () -> false, AiControl.ATTACK);
      AiIntent fallback = AiIntent.attempt(id("fallback"), AiIntent.PRIORITY_POSITION, 10.0, 3, true, () -> {
         executed.add("fallback");
         return true;
      }, AiControl.MOVE);
      AiBlackboard board = new AiBlackboard();
      AiBrain.Resolution resolution = AiBrain.resolve(List.of(unavailable, fallback), board, 100L);
      assertTrue(resolution.executed());
      assertEquals(fallback, resolution.intent());
      assertEquals(List.of("fallback"), executed);
      assertTrue(board.hasActiveCommitment(101L));
   }

   @Test
   void beamClashRequiresOpposedIntersectingBeams() {
      assertTrue(BeamClashManager.canClashGeometry(new Vec3(0, 1, 0), new Vec3(100, 1, 0), 4.0,
         new Vec3(100, 1, 0), new Vec3(0, 1, 0), 4.0));
      assertFalse(BeamClashManager.canClashGeometry(new Vec3(0, 1, 0), new Vec3(100, 1, 0), 4.0,
         new Vec3(0, 1, 12), new Vec3(100, 1, 12), 4.0));
      assertFalse(BeamClashManager.canClashGeometry(new Vec3(0, 1, 0), new Vec3(100, 1, 0), 4.0,
         new Vec3(100, 1, 12), new Vec3(0, 1, 12), 4.0));
      assertFalse(BeamClashManager.canClashGeometry(new Vec3(0, 1, 0), new Vec3(20, 1, 0), 4.0,
         new Vec3(100, 1, 0), new Vec3(80, 1, 0), 4.0));
   }

   @Test
   void beamClashPressureUsesPowerAndManaAndLeavesBoundedResidual() {
      float fullEa = BeamClashManager.effectiveStrength(1.18F, 1.0F);
      float exhaustedEa = BeamClashManager.effectiveStrength(1.18F, 0.1F);
      float fullExcalibur = BeamClashManager.effectiveStrength(1.0F, 1.0F);
      assertTrue(BeamClashManager.pressureDelta(fullEa, fullExcalibur) > 0.0F);
      assertTrue(BeamClashManager.pressureDelta(exhaustedEa, fullExcalibur) < 0.0F);
      assertTrue(BeamClashManager.residualScale(fullEa, fullExcalibur, 0.4F) >= 0.25F);
      assertTrue(BeamClashManager.residualScale(100.0F, 0.1F, 1.0F) <= 0.9F);
   }

   @Test
   void jumpQualificationMatchesAgilityRules() {
      assertEquals(EvasionMovementService.JumpCapability.BACKSTEP, EvasionMovementService.jumpCapability(2, false, false, false));
      assertEquals(EvasionMovementService.JumpCapability.SINGLE_JUMP, EvasionMovementService.jumpCapability(3, false, false, false));
      assertEquals(EvasionMovementService.JumpCapability.DOUBLE_JUMP, EvasionMovementService.jumpCapability(5, false, true, false));
      assertEquals(EvasionMovementService.JumpCapability.DOUBLE_JUMP, EvasionMovementService.jumpCapability(3, true, true, false));
      assertEquals(EvasionMovementService.JumpCapability.NONE, EvasionMovementService.jumpCapability(5, false, true, true));
      assertEquals(32, ProjectileThreatSensor.scanLimit());
   }

   @Test
   void rangedPressureEvasionsAlwaysMoveSidewaysAndTowardTheShooter() {
      Vec3 toward = new Vec3(0.8, 0.0, 0.6).normalize();
      Vec3 side = new Vec3(-toward.z, 0.0, toward.x);
      List<Vec3> candidates = EvasionMovementService.forwardEvasionDirections(toward);
      assertEquals(4, candidates.size());
      for (Vec3 candidate : candidates) {
         assertTrue(candidate.dot(toward) > 0.3, "candidate must retain a forward component");
         assertTrue(Math.abs(candidate.dot(side)) > 0.65, "candidate must leave the projectile line laterally");
      }
   }

   @Test
   void combatTempoEscalatesAtExplicitNoContactDeadlines() {
      assertEquals(0, ServantCombatTempoService.stageForDisconnectedTicks(39));
      assertEquals(1, ServantCombatTempoService.stageForDisconnectedTicks(40));
      assertEquals(2, ServantCombatTempoService.stageForDisconnectedTicks(100));
      assertEquals(3, ServantCombatTempoService.stageForDisconnectedTicks(160));
      assertEquals(4, ServantCombatTempoService.stageForDisconnectedTicks(200));
   }

   @Test
   void combatFlightBandCannotClimbWithRoofsAndDescendsWhenDisconnected() {
      double engaged = ServantFlightHelper.desiredCombatY(64.0, 64.0, 1.8, 0L);
      double pressured = ServantFlightHelper.desiredCombatY(64.0, 64.0, 1.8, 40L);
      double forcedDown = ServantFlightHelper.desiredCombatY(64.0, 64.0, 1.8, 100L);
      assertTrue(engaged <= 70.0);
      assertTrue(pressured < engaged);
      assertTrue(forcedDown < pressured);
      assertEquals(70.0, ServantFlightHelper.desiredCombatY(64.0, 200.0, 2.0, 0L));
   }

   @Test
   void detailedNavigationOnlyTreatsActualMovementAsAccepted() {
      assertTrue(ServantNavigationHelper.NavigationResult.MOVED.accepted());
      assertFalse(ServantNavigationHelper.NavigationResult.NO_PATH.accepted());
      assertFalse(ServantNavigationHelper.NavigationResult.BLOCKED.accepted());
      assertFalse(ServantNavigationHelper.NavigationResult.NO_PROGRESS.accepted());
      assertFalse(ServantNavigationHelper.NavigationResult.UNSAFE.accepted());
   }

   @Test
   void arbitrationSelectsOnePrimaryAndOnlyDisjointAuxiliaries() {
      List<String> executed = new ArrayList<>();
      AiIntent move = AiIntent.of(id("move"), AiIntent.PRIORITY_POSITION, 10.0, 1, true,
         () -> executed.add("move"), AiControl.MOVE, AiControl.LOOK);
      AiIntent attack = AiIntent.of(id("attack"), AiIntent.PRIORITY_ATTACK, 10.0, 5, true,
         () -> executed.add("attack"), AiControl.ATTACK, AiControl.LOOK);
      AiIntent defend = AiIntent.of(id("defend"), AiIntent.PRIORITY_LETHAL_DEFENSE, 10.0, 3, false,
         () -> executed.add("defend"), AiControl.DEFEND, AiControl.MOVE);
      AiIntent cast = AiIntent.of(id("cast"), AiIntent.PRIORITY_ATTACK, 5.0, 2, true,
         () -> executed.add("cast"), AiControl.CAST);

      AiIntentArbitrator.Selection result = AiIntentArbitrator.select(List.of(move, attack, defend, cast), ignored -> false);
      assertEquals(defend, result.primary());
      assertEquals(List.of(attack, cast), result.auxiliaries());
   }

   @Test
   void phaseThresholdsAreStableAndExplicit() {
      assertEquals(ServantCombatPhase.PROBING, ServantPhaseService.desiredPhase(1.0F, 59L, 1.0F));
      assertEquals(ServantCombatPhase.NORMAL, ServantPhaseService.desiredPhase(1.0F, 60L, 1.0F));
      assertEquals(ServantCombatPhase.DECISIVE, ServantPhaseService.desiredPhase(0.55F, 1L, 1.0F));
      assertEquals(ServantCombatPhase.LAST_STAND, ServantPhaseService.desiredPhase(0.25F, 1L, 3.0F));
      assertEquals("CAUTIOUS", DeadApostleTacticalState.determinePhase(true, 1.0F, 0.0F, 1.0F));
      assertEquals("HUNTING", DeadApostleTacticalState.determinePhase(false, 1.0F, 0.45F, 1.0F));
      assertEquals("FERAL", DeadApostleTacticalState.determinePhase(false, 0.3F, 0.0F, 1.0F));
   }

   @Test
   void maneuverActionsRemainOptionalAndDecodeWhenDeclared() {
      String legacy = "{\"id\":\"typemoonworld:test/legacy\",\"tags\":[\"melee\"]}";
      AiActionDescriptor legacyAction = AiActionDescriptor.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(legacy))
         .result().orElseThrow();
      assertEquals(AiActionDescriptor.ManeuverSpec.NONE, legacyAction.maneuver());

      String advanced = "{\"id\":\"typemoonworld:test/launcher\",\"tags\":[\"melee\",\"launcher\",\"pursuit\"],"
         + "\"maneuver\":{\"movement\":\"pursuit\",\"control\":\"launcher\",\"pursuit_window\":24,"
         + "\"interrupt_level\":3,\"horizontal_force\":1.2,\"vertical_force\":0.5,"
         + "\"approach_range\":18,\"damage_scale\":0.8}}";
      AiActionDescriptor advancedAction = AiActionDescriptor.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(advanced))
         .result().orElseThrow();
      assertTrue(advancedAction.tags().contains(AiActionDescriptor.Tag.LAUNCHER));
      assertEquals(24, advancedAction.maneuver().pursuitWindowTicks());
      assertEquals(1.2, advancedAction.maneuver().horizontalForce());
      assertEquals(18.0, advancedAction.maneuver().approachRange());
      assertEquals(0.8, advancedAction.maneuver().damageScale());
      assertEquals(3, advancedAction.maneuver().interruptResistance());
   }

   @Test
   void antiAirQualificationRejectsFreshUnsettledGroundTargets() {
      assertFalse(ServantPlannedActionExecutor.qualifiesAntiAirTarget(false, false, 0.0, 0.0, 0.0F));
      assertFalse(ServantPlannedActionExecutor.qualifiesAntiAirTarget(true, false, 0.0, 0.0, 0.0F));
      assertTrue(ServantPlannedActionExecutor.qualifiesAntiAirTarget(false, true, 0.0, 0.0, 0.0F));
      assertTrue(ServantPlannedActionExecutor.qualifiesAntiAirTarget(false, false, 0.0, 0.2, 0.0F));
      assertTrue(ServantPlannedActionExecutor.qualifiesAntiAirTarget(false, false, 2.0, 0.0, 0.0F));
      assertTrue(ServantPlannedActionExecutor.qualifiesAntiAirTarget(false, false, 0.0, 0.0, 1.0F));
   }

   @Test
   void servantSkillAiFactsAreOptionalAndOldConstructorRemainsCompatible() {
      String legacy = "{\"id\":\"legacy\",\"type\":\"passive\",\"effects\":[]}";
      ServantSkillDefinition oldJson = ServantSkillDefinition.CODEC.parse(JsonOps.INSTANCE,
         JsonParser.parseString(legacy)).result().orElseThrow();
      assertTrue(oldJson.ai().facts().isEmpty());

      ServantSkillDefinition oldJava = new ServantSkillDefinition("legacy", "Legacy", "Legacy",
         ServantSkillDefinition.SkillType.PASSIVE, 0, 0, 0, List.of());
      assertTrue(oldJava.ai().facts().isEmpty());

      String aware = "{\"id\":\"aware\",\"ai\":{\"facts\":[{\"type\":\"projectile_negation\","
         + "\"strength\":1.0,\"requires\":[\"mobile\"],\"bypassed_by\":[\"explosion\",\"piercing\"]}]}}";
      var fact = ServantSkillDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(aware))
         .result().orElseThrow().ai().facts().getFirst();
      assertEquals(FactType.PROJECTILE_NEGATION, fact.type());
      assertEquals(List.of(FactCondition.MOBILE), fact.conditions());
      assertEquals(List.of(FactBypass.EXPLOSION, FactBypass.PIERCING), fact.bypassedBy());
   }

   @Test
   void learnedFactsAreBoundedAndExpireWithOpponentMemory() {
      AiBlackboard board = new AiBlackboard();
      UUID opponent = UUID.randomUUID();
      for (FactType type : FactType.values()) board.revealFact(opponent, type, 0.8, 100L);
      assertTrue(board.opponent(opponent).knownFacts().size() <= AiBlackboard.MAX_FACTS_PER_OPPONENT);
      assertTrue(board.opponent(opponent).knows(FactType.PROJECTILE_NEGATION));
      board.beginTick(701L);
      assertEquals(AiBlackboard.OpponentSnapshot.EMPTY, board.opponent(opponent));
   }

   @Test
   void learnedProjectileNegationChangesChannelsButControlRestoresShooting() {
      String projectileJson = "{\"id\":\"typemoonworld:test/shot\",\"tags\":[\"projectile\"]}";
      String controlJson = "{\"id\":\"typemoonworld:test/bind\",\"tags\":[\"control\",\"area\"]}";
      AiActionDescriptor projectile = AiActionDescriptor.CODEC.parse(JsonOps.INSTANCE,
         JsonParser.parseString(projectileJson)).result().orElseThrow();
      AiActionDescriptor control = AiActionDescriptor.CODEC.parse(JsonOps.INSTANCE,
         JsonParser.parseString(controlJson)).result().orElseThrow();
      AiBlackboard.OpponentSnapshot known = new AiBlackboard.OpponentSnapshot(
         1L, 8.0, 0.0, 1, 1, 1, Map.of(FactType.PROJECTILE_NEGATION, 1.0));

      double mobileShot = CombatMatchupEvaluator.learnedDefenseMultiplier(projectile, known, true);
      double bind = CombatMatchupEvaluator.learnedDefenseMultiplier(control, known, true);
      double immobilizedShot = CombatMatchupEvaluator.learnedDefenseMultiplier(projectile, known, false);
      assertTrue(mobileShot < 0.5);
      assertTrue(bind > 1.0);
      assertTrue(immobilizedShot > 1.0);
      assertTrue(mobileShot >= CombatMatchupEvaluator.MIN_ACTION_MULTIPLIER);
      assertTrue(bind <= CombatMatchupEvaluator.MAX_ACTION_MULTIPLIER);
   }

   @Test
   void launcherTerrainBreakWaitsForAnImpactShape() {
      String launcherJson = "{\"id\":\"typemoonworld:test/launcher_break\",\"tags\":[\"melee\",\"launcher\",\"terrain_break\"],"
         + "\"threat\":{\"shape\":\"cone\",\"radius\":2,\"length\":5},"
         + "\"maneuver\":{\"movement\":\"gap_closer\",\"control\":\"launcher\"}}";
      String slamJson = "{\"id\":\"typemoonworld:test/slam_break\",\"tags\":[\"melee\",\"launcher\",\"terrain_break\"],"
         + "\"threat\":{\"shape\":\"hemisphere\",\"radius\":4,\"length\":0},"
         + "\"maneuver\":{\"movement\":\"gap_closer\",\"control\":\"slam\"}}";
      AiActionDescriptor launcher = AiActionDescriptor.CODEC.parse(JsonOps.INSTANCE,
         JsonParser.parseString(launcherJson)).result().orElseThrow();
      AiActionDescriptor slam = AiActionDescriptor.CODEC.parse(JsonOps.INSTANCE,
         JsonParser.parseString(slamJson)).result().orElseThrow();
      assertFalse(ServantPlannedActionExecutor.triggersTerrainAtHit(launcher));
      assertTrue(ServantPlannedActionExecutor.triggersTerrainAtHit(slam));
   }
}
