package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantDuelPacing;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantManeuverService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalController;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatExchangeService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

@GameTestHolder("typemoonworld_combat")
@PrefixGameTestTemplate(false)
public final class ServantExchangeGameTests {
   private static final AtomicInteger ARENAS = new AtomicInteger();
   private static final long RUN_SALT = UUID.randomUUID().getLeastSignificantBits();

   @GameTest(template = "ancient_temple", timeoutTicks = 5000)
   public static void meleeExchangeStaysActive(GameTestHelper h) { rounds(h, "artoria_pendragon", "cu_chulainn", true); }
   @GameTest(template = "ancient_temple", timeoutTicks = 5000)
   public static void meleePursuesRangedOpponent(GameTestHelper h) { rounds(h, "sasaki_kojiro", "medea", false); }
   @GameTest(template = "ancient_temple", timeoutTicks = 5000)
   public static void rangedExchangeKeepsSpacingAndPressure(GameTestHelper h) { rounds(h, "medea", "gilles_de_rais_caster", false); }
   @GameTest(template = "ancient_temple", timeoutTicks = 5000)
   public static void closeRangedDefenderDoesNotStall(GameTestHelper h) { rounds(h, "gilgamesh", "sasaki_kojiro", false); }

   @GameTest(template = "ancient_temple", timeoutTicks = 35)
   public static void basicCooldownYieldsAndTelegraphDamagesOnlyOnce(GameTestHelper h) {
      BlockPos center = arena(h);
      ServantEntity a = fixture(h.getLevel(), "artoria_pendragon", center.offset(-1, 1, 0), 442);
      ServantEntity b = fixture(h.getLevel(), "cu_chulainn", center.offset(1, 1, 0), 443);
      a.setNoAi(true); b.setNoAi(true); a.setTarget(b); b.setTarget(a);
      a.getPersistentData().putLong("TypeMoonDuelNextBasic", h.getLevel().getGameTime() + 100);
      // Initialize the pacing target before setting its cooldown.
      ServantDuelPacing.tick(a);
      ServantCombatExchangeService.cancelWindup(a);
      a.getPersistentData().putLong("TypeMoonDuelNextBasic", h.getLevel().getGameTime() + 100);
      h.assertTrue(!ServantDuelPacing.tick(a), "cooldown consumed the AI tick");
      a.getPersistentData().remove("TypeMoonDuelNextBasic");
      b.getPersistentData().putDouble(ServantCombatDefenseService.TAG_STAMINA, 0);
      b.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES, 0);
      b.getPersistentData().putLong(ServantCombatDefenseService.TAG_DODGE_COOLDOWN, h.getLevel().getGameTime() + 100);
      float hp = b.getHealth();
      h.assertTrue(ServantCombatExchangeService.beginBasic(a, b, h.getLevel().getGameTime()), "basic windup failed");
      h.assertTrue(b.getHealth() == hp, "telegraph dealt damage before impact");
      h.runAfterDelay(4, () -> {
         ServantCombatExchangeService.act(a, b, h.getLevel().getGameTime());
         float after = b.getHealth();
         h.assertTrue(after < hp, "telegraphed strike did not hit");
         ServantCombatExchangeService.act(a, b, h.getLevel().getGameTime());
         h.assertTrue(b.getHealth() == after, "windup dealt a duplicate hit");
         cleanup(h.getLevel(), center); h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 25)
   public static void dashRejectsWallsAndUnsupportedGround(GameTestHelper h) {
      BlockPos center = arena(h);
      ServantEntity a = fixture(h.getLevel(), "artoria_pendragon", center.above(), 142);
      a.setNoAi(true); a.setOnGround(true);
      h.assertTrue(ServantManeuverService.safeExchangeRoute(a, new Vec3(4, 0, 0)), "clear ground route rejected");
      for (int y = 1; y <= 4; y++) h.getLevel().setBlockAndUpdate(center.offset(2, y, 0), Blocks.BARRIER.defaultBlockState());
      h.assertTrue(!ServantManeuverService.safeExchangeRoute(a, new Vec3(4, 0, 0)), "dash can cross a wall");
      for (int x = -2; x <= 2; x++) for (int y = -1; y <= 0; y++)
         h.getLevel().setBlockAndUpdate(center.offset(x, y, 2), Blocks.AIR.defaultBlockState());
      h.assertTrue(!ServantManeuverService.safeExchangeRoute(a, new Vec3(0, 0, 4)), "dash can cross an unsupported gap");
      cleanup(h.getLevel(), center); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 25)
   public static void footworkSlidesAlongABlockedForwardLane(GameTestHelper h) {
      BlockPos center = arena(h);
      ServantEntity a = fixture(h.getLevel(), "artoria_pendragon", center.above(), 302);
      ServantEntity b = fixture(h.getLevel(), "cu_chulainn", center.offset(8, 1, 0), 303);
      a.setNoAi(true); b.setNoAi(true); a.setOnGround(true);
      a.setPos(center.getX() + 1 - a.getBbWidth() * 0.5 - 0.05, center.getY() + 1, center.getZ() + 0.5);
      for (int y = 1; y <= 4; y++) h.getLevel().setBlockAndUpdate(center.offset(1, y, 0), Blocks.BARRIER.defaultBlockState());
      h.assertTrue(ServantManeuverService.exchangeFootwork(a, b, h.getLevel().getGameTime(), 1, false), "wall left the servant idle");
      h.assertTrue(Math.abs(a.getDeltaMovement().z) >= 0.09 && Math.abs(a.getDeltaMovement().x) < 0.01,
         "footwork did not remove the blocked forward component");
      cleanup(h.getLevel(), center); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void dashChangesSideTwiceWithoutReplanning(GameTestHelper h) {
      BlockPos center = arena(h);
      ServantEntity a = fixture(h.getLevel(), "artoria_pendragon", center.above(), 702);
      ServantEntity b = fixture(h.getLevel(), "cu_chulainn", center.offset(12, 1, 0), 703);
      a.setNoAi(true); b.setNoAi(true); a.setOnGround(true);
      long started = h.getLevel().getGameTime();
      h.assertTrue(ServantManeuverService.tryExchangeDash(a, b, started, 1, false), "dash did not start");
      double firstSide = Math.signum(a.getDeltaMovement().z);
      h.runAfterDelay(2, () -> {
         h.assertTrue(ServantManeuverService.continueExchangeDash(a, h.getLevel().getGameTime()), "middle dash leg failed");
         h.assertTrue(a.getDeltaMovement().x > 0 && Math.signum(a.getDeltaMovement().z) == -firstSide,
            "dash did not cross to its opposite diagonal");
         Vec3 once = a.getDeltaMovement();
         ServantManeuverService.continueExchangeDash(a, h.getLevel().getGameTime());
         h.assertTrue(once.equals(a.getDeltaMovement()), "duplicate AI entry applied the dash twice");
         h.runAfterDelay(2, () -> {
            ServantManeuverService.continueExchangeDash(a, h.getLevel().getGameTime());
            h.assertTrue(a.getDeltaMovement().x > 0 && Math.signum(a.getDeltaMovement().z) == firstSide,
               "dash did not return to its original diagonal");
            cleanup(h.getLevel(), center); h.succeed();
         });
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 25)
   public static void backstepContinuesAfterLeavingMeleeRange(GameTestHelper h) {
      BlockPos center = arena(h);
      ServantEntity a = fixture(h.getLevel(), "artoria_pendragon", center.above(), 712);
      ServantEntity b = fixture(h.getLevel(), "cu_chulainn", center.offset(3, 1, 0), 713);
      a.setNoAi(true); b.setNoAi(true); a.setOnGround(true);
      long beat = h.getLevel().getGameTime();
      beat += Math.floorMod(-beat - a.getId() * 3L, 48L);
      h.assertTrue(ServantManeuverService.exchangeFootwork(a, b, beat, 1, false), "backstep did not start");
      h.assertTrue(a.getDeltaMovement().x < -0.4, "backstep was only a sideways shuffle");
      a.setPos(a.getX() - 3, a.getY(), a.getZ()); a.setDeltaMovement(Vec3.ZERO); a.setOnGround(true);
      ServantManeuverService.exchangeFootwork(a, b, beat + 2, 1, false);
      h.assertTrue(a.getDeltaMovement().x < -0.4, "crossing the range threshold reversed the backstep");
      cleanup(h.getLevel(), center); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void flightPreservesLaunchAndPursuitWaitsEightTicks(GameTestHelper h) {
      BlockPos center = arena(h);
      ServantEntity a = fixture(h.getLevel(), "artoria_pendragon", center.above(), 722);
      var b = (net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaEntity)
         fixture(h.getLevel(), "medea", center.offset(2, 1, 0), 723);
      a.setNoAi(true); b.setNoAi(true); a.setTarget(b); b.setTarget(a);
      b.setFlyingMode(true);
      var launch = ServantCombatMotionService.launch(a, b, new Vec3(1, 0, 0), 1.35, 0.78,
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.SMALL, 32);
      h.assertTrue(launch.applied(), "flight launch rejected");
      Vec3 motion = b.getDeltaMovement();
      net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightCombatService.tick(b);
      h.assertTrue(b.getDeltaMovement().equals(motion) && !b.isNoGravity(), "hover controller erased launch physics");
      h.assertTrue(!ServantCombatMotionService.canPursue(a, b), "pursuit began before separation was visible");
      h.runAfterDelay(7, () -> {
         h.assertTrue(!ServantCombatMotionService.canPursue(a, b), "pursuit started before its eight-tick beat");
         h.runAfterDelay(1, () -> {
            h.assertTrue(ServantCombatMotionService.canPursue(a, b), "pursuit window never opened");
            cleanup(h.getLevel(), center); h.succeed();
         });
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 25)
   public static void repeatedGuardPressureCreatesKnockbackWithoutExtraDamage(GameTestHelper h) {
      BlockPos center = arena(h);
      ServantEntity a = fixture(h.getLevel(), "artoria_pendragon", center.above(), 732);
      ServantEntity b = fixture(h.getLevel(), "cu_chulainn", center.offset(2, 1, 0), 733);
      a.setNoAi(true); b.setNoAi(true); a.setTarget(b); b.setTarget(a);
      ServantCombatDefenseService.initialize(b);
      b.getPersistentData().putDouble(ServantCombatDefenseService.TAG_STAMINA, 100);
      float health = b.getHealth();
      for (int hit = 0; hit < 3; hit++) {
         h.assertTrue(ServantCombatDefenseService.applyGuardHit(b, a.damageSources().mobAttack(a), 4) != null,
            "guard did not resolve a pressure hit");
      }
      h.assertTrue(ServantCombatMotionService.isLaunched(b) && b.getDeltaMovement().x > 0.5,
         "repeated guard pressure did not push the defender away");
      h.assertTrue(b.getHealth() == health, "guard motion applied an additional damage transaction");
      cleanup(h.getLevel(), center); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 45)
   public static void launchCreatesAtLeastFiveBlocksOfRealSeparation(GameTestHelper h) {
      BlockPos center = arena(h);
      ServantEntity attacker = fixture(h.getLevel(), "artoria_pendragon", center.above(), 602);
      attacker.setNoAi(true);
      ServantEntity target = fixture(h.getLevel(), "cu_chulainn", center.offset(2, 1, 0), 603);
      Vec3 origin = target.position();
      ServantCombatMotionService.launch(attacker, target, new Vec3(1, 0, 0), 1.35, 0.78,
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.SMALL, 32);
      h.runAfterDelay(12, () -> {
         double distance = target.position().subtract(origin).horizontalDistance();
         h.assertTrue(!target.isRemoved(), "launch fixture was removed before its trajectory completed");
         TYPE_MOON_WORLD.LOGGER.info("TMW_EXCHANGE_LAUNCH horizontalTravel={} ticks={} origin={} position={} motion={} alive={}",
            distance, target.tickCount, origin, target.position(), target.getDeltaMovement(), target.isAlive());
         h.assertTrue(distance >= 5, "launch did not create five blocks of separation: " + distance);
         h.assertTrue(target.getY() < center.getY() + 15, "launcher produced excessive height");
         cleanup(h.getLevel(), center); h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 25)
   public static void spectatorDoesNotEmitConcealmentLeakParticles(GameTestHelper h) {
      // The stock GameTest fake player hardcodes isSpectator() to false.
      var player = new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
         new com.mojang.authlib.GameProfile(UUID.randomUUID(), "SpectatorQA")) {
         @Override public boolean isSpectator() { return true; }
      };
      player.setInvisible(true);
      player.getPersistentData().putLong("TypeMoonConcealmentNextParticleTick", h.getLevel().getGameTime());
      player.getPersistentData().putBoolean("TypeMoonConcealmentManagedSilence", true);
      player.getPersistentData().putBoolean("TypeMoonConcealmentPreviousSilence", false);
      player.setSilent(true);
      net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment.tick(player);
      h.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment.isFullyConcealed(player), "spectator treated as presence-concealed");
      h.assertTrue(!player.getPersistentData().contains("TypeMoonConcealmentNextParticleTick"), "spectator retained its black-particle timer");
      h.assertTrue(player.isInvisible() && !player.isSilent(), "spectator invisibility or previous silence was changed incorrectly");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 35)
   public static void guardTracksOnlyRelevantThreatAndExpires(GameTestHelper h) {
      BlockPos center = arena(h);
      ServantEntity defender = fixture(h.getLevel(), "cu_chulainn", center.above(), 502);
      ServantEntity source = fixture(h.getLevel(), "artoria_pendragon", center.offset(8, 1, 0), 503);
      ServantEntity other = fixture(h.getLevel(), "medea", center.offset(12, 1, 0), 504);
      defender.setNoAi(true); source.setNoAi(true); other.setNoAi(true);
      h.runAfterDelay(5, () -> {
         h.assertTrue(h.getLevel().getEntity(source.getUUID()) == source, "threat source is not registered in the level");
         var action = ResourceLocation.fromNamespaceAndPath("typemoonworld", "test/guard_threat");
         net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService.publishWindup(source, other, action, 4, false, 1);
         ServantCombatDefenseService.tick(defender);
         h.assertTrue(!ServantCombatDefenseService.isGuarding(defender), "unrelated threat activated guard");
         net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService.publishWindup(source, defender, action, 4, false, 1);
         ServantCombatDefenseService.tick(defender);
         h.assertTrue(ServantCombatDefenseService.isGuarding(defender), "targeted threat did not activate guard");
         h.runAfterDelay(14, () -> {
            ServantCombatDefenseService.tick(defender);
            h.assertTrue(!ServantCombatDefenseService.isGuarding(defender), "expired threat left guard active");
            cleanup(h.getLevel(), center); h.succeed();
         });
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void specialDuelRetainsItsController(GameTestHelper h) {
      BlockPos center = arena(h);
      ServantEntity a = fixture(h.getLevel(), "gilgamesh", center.offset(-2, 1, 0), 52);
      ServantEntity b = fixture(h.getLevel(), "enkidu", center.offset(2, 1, 0), 53);
      a.setNoAi(true); b.setNoAi(true); a.setTarget(b); b.setTarget(a);
      a.getPersistentData().putBoolean("GilEnkiduDuelActive", true);
      b.getPersistentData().putBoolean("GilEnkiduDuelActive", true);
      a.setDeltaMovement(new Vec3(0.1, 0, 0.1)); Vec3 motion = a.getDeltaMovement();
      ServantCombatExchangeService.observe(a);
      h.assertTrue(!ServantDuelPacing.tick(a) && !ServantTacticalController.tick(a), "generic AI claimed the scripted duel");
      h.assertTrue(!ServantCombatExchangeService.act(a, b, h.getLevel().getGameTime()), "exchange director claimed the scripted duel");
      h.assertTrue(a.getDeltaMovement().equals(motion), "exchange director changed scripted movement");
      h.assertTrue(ServantDuelPacing.skillDecisionAllowed(a), "pacing blocked the original character helper");
      b.getPersistentData().putBoolean("IskandarIonioiOffscreenDuel", true);
      h.assertTrue(ServantCombatExchangeService.scripted(b, a), "offscreen script is not protected");
      cleanup(h.getLevel(), center); h.succeed();
   }

   private static void rounds(GameTestHelper h, String left, String right, boolean launchRequired) {
      BlockPos center = arena(h);
      start(h, center, left, right, 42, launchRequired,
         () -> start(h, center, left, right, 2026, launchRequired, () -> { cleanup(h.getLevel(), center); h.succeed(); }));
   }

   private static void start(GameTestHelper h, BlockPos center, String left, String right, long seed,
                             boolean launchRequired, Runnable done) {
      for (int x = -29; x <= 29; x++) for (int z = -29; z <= 29; z++) {
         h.getLevel().setBlock(center.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
         h.getLevel().setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
      }
      ServantEntity a = spawn(h.getLevel(), left, center.offset(-6, 1, 0), seed);
      ServantEntity b = spawn(h.getLevel(), right, center.offset(6, 1, 0), seed + 1);
      a.setTarget(b); b.setTarget(a);
      sample(h, center, a, b, seed, h.getLevel().getGameTime(), launchRequired, new Observation(), done);
   }

   private static final class Observation {
      int overlap, maxOverlap, idle, maxIdle;
      int basicA, basicB, followups, skillStarts;
      long basicTickA = Long.MIN_VALUE, basicTickB = Long.MIN_VALUE;
      boolean skillA, skillB;
      double travel, maxSeparationAfterContact;
      boolean contacted, mountedA, mountedB;
      Vec3 lastA, lastB;
      float lastHpA, lastHpB;
   }

   private static void sample(GameTestHelper h, BlockPos center, ServantEntity a, ServantEntity b, long seed,
                              long started, boolean launchRequired, Observation o, Runnable done) {
      h.runAfterDelay(1, () -> {
         long now = h.getLevel().getGameTime(), elapsed = now - started;
         int basicA = a.getPersistentData().getInt("TypeMoonDuelBasicAttacks");
         int basicB = b.getPersistentData().getInt("TypeMoonDuelBasicAttacks");
         if (basicA > o.basicA) {
            if (o.basicTickA != Long.MIN_VALUE && now - o.basicTickA <= 12) o.followups++;
            o.basicTickA = now; o.basicA = basicA;
         }
         if (basicB > o.basicB) {
            if (o.basicTickB != Long.MIN_VALUE && now - o.basicTickB <= 12) o.followups++;
            o.basicTickB = now; o.basicB = basicB;
         }
         boolean skillA = net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPlannedActionExecutor.isActive(a)
            || ServantDuelPacing.nativeNpActive(a);
         boolean skillB = net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPlannedActionExecutor.isActive(b)
            || ServantDuelPacing.nativeNpActive(b);
         if (skillA && !o.skillA) o.skillStarts++;
         if (skillB && !o.skillB) o.skillStarts++;
         o.skillA = skillA; o.skillB = skillB;
         if (a.isAlive() && b.isAlive()) { a.setTarget(b); b.setTarget(a); }
         boolean near = a.onGround() && b.onGround() && a.distanceTo(b) <= 8 && a.hasLineOfSight(b);
         if (o.lastA != null) o.travel += a.position().distanceTo(o.lastA) + b.position().distanceTo(o.lastB);
         if (a.distanceTo(b) < 4.35) o.contacted = true;
         if (o.contacted) o.maxSeparationAfterContact = Math.max(o.maxSeparationAfterContact, a.distanceTo(b));
         boolean moved = o.lastA == null || a.position().distanceToSqr(o.lastA) + b.position().distanceToSqr(o.lastB) > 0.0025;
         boolean busy = a.isPerformingAction() || b.isPerformingAction() || a.isAttackSwinging() || b.isAttackSwinging()
            || ServantCombatExchangeService.protectedAction(a) || ServantCombatExchangeService.protectedAction(b)
            || ServantCombatSystem.cannotAct(a) || ServantCombatSystem.cannotAct(b)
            || ServantCombatDefenseService.isGuarding(a) || ServantCombatDefenseService.isGuarding(b)
            || a.getHealth() != o.lastHpA || b.getHealth() != o.lastHpB;
         o.idle = near && !moved && !busy ? o.idle + 1 : 0; o.maxIdle = Math.max(o.maxIdle, o.idle);
         o.overlap = near && a.distanceTo(b) < 0.8 ? o.overlap + 1 : 0; o.maxOverlap = Math.max(o.maxOverlap, o.overlap);
         o.lastA = a.position(); o.lastB = b.position(); o.lastHpA = a.getHealth(); o.lastHpB = b.getHealth();
         // A giant mount can eject its rider onto its 64-block-high body.
         // Resume the ordinary launch bound after that rider has landed.
         o.mountedA = a.isPassenger() || o.mountedA && !a.onGround();
         o.mountedB = b.isPassenger() || o.mountedB && !b.onGround();
         h.assertTrue((o.mountedA || a.getY() < center.getY() + 40) && (o.mountedB || b.getY() < center.getY() + 40),
            "unbounded launch height: " + a.getServantId() + "=" + a.position() + ", " + b.getServantId() + "=" + b.position()
               + ", ground=" + net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightHelper.groundY(b)
               + ", motion=" + b.getDeltaMovement() + ", riding=" + b.isPassenger());
         if (a.isAlive() && b.isAlive() && elapsed < 2400) {
            sample(h, center, a, b, seed, started, launchRequired, o, done); return;
         }
         int basic = metric(a, b, "TypeMoonDuelBasicAttacks"), dashes = metric(a, b, ServantCombatExchangeService.DASHES);
         int launches = metric(a, b, ServantCombatExchangeService.LAUNCHES), pursuits = metric(a, b, ServantCombatExchangeService.PURSUITS);
         TYPE_MOON_WORLD.LOGGER.info("TMW_EXCHANGE left={} right={} seed={} seconds={} hpA={} hpB={} basic={} exchanges={} footwork={} dash={} clash={} launch={} pursuit={} guard={} maxIdle={} maxOverlap={} totalTravel={} maxSeparationAfterContact={} maxLaunchTravel={}",
            a.getServantId(), b.getServantId(), seed, elapsed / 20.0, a.getHealth(), b.getHealth(), basic,
            metric(a, b, ServantCombatExchangeService.EXCHANGES), metric(a, b, ServantCombatExchangeService.FOOTWORK), dashes,
            metric(a, b, ServantCombatExchangeService.CLASHES), launches, pursuits,
            metric(a, b, "TypeMoonDefenseGuardCount"), o.maxIdle, o.maxOverlap, o.travel, o.maxSeparationAfterContact,
            Math.max(a.getPersistentData().getDouble("TypeMoonCombatMaxLaunchTravel"), b.getPersistentData().getDouble("TypeMoonCombatMaxLaunchTravel")));
         TYPE_MOON_WORLD.LOGGER.info("TMW_EXCHANGE_TEMPO left={} right={} seed={} fastFollowups={} skillStarts={}",
            a.getServantId(), b.getServantId(), seed, o.followups, o.skillStarts);
         h.assertTrue(o.maxIdle <= 10, "visible inactivity exceeded ten ticks: " + o.maxIdle);
         h.assertTrue(o.maxOverlap <= 10, "sustained servant overlap: " + o.maxOverlap);
         h.assertTrue(basic > 0 && metric(a, b, ServantCombatExchangeService.EXCHANGES) > 0, "no real attack/defense exchange");
         h.assertTrue(metric(a, b, ServantCombatExchangeService.FOOTWORK) > 0, "no visible footwork");
         if (launchRequired) {
            h.assertTrue(o.followups > 0 && o.skillStarts > 0, "missing fast combo followups or actual skill timelines");
            h.assertTrue(dashes > 0 && launches > 0 && pursuits > 0, "missing dash, launch or pursuit");
            h.assertTrue(elapsed >= 1200, "ordinary melee duel ended before sixty seconds");
            h.assertTrue(o.travel > 60 && o.maxSeparationAfterContact >= 8, "melee exchange did not become a moving battle");
         }
         discardArenaEntities(h.getLevel(), center); done.run();
      });
   }

   private static int metric(ServantEntity a, ServantEntity b, String key) {
      return a.getPersistentData().getInt(key) + b.getPersistentData().getInt(key);
   }

   private static ServantEntity fixture(ServerLevel level, String id, BlockPos pos, long seed) {
      return spawn(level, id, pos, seed, false);
   }

   private static ServantEntity spawn(ServerLevel level, String id, BlockPos pos, long seed) {
      return spawn(level, id, pos, seed, true);
   }

   private static ServantEntity spawn(ServerLevel level, String id, BlockPos pos, long seed, boolean manifest) {
      ServantEntity entity = BuiltinServantEntityFactory.create(level, ResourceLocation.fromNamespaceAndPath("typemoonworld", id));
      if (entity == null) throw new IllegalStateException("Missing servant " + id);
      UUID stable = UUID.nameUUIDFromBytes((id + ":exchange:" + seed).getBytes(StandardCharsets.UTF_8));
      // A run salt avoids stale entities from a failed prior run; XOR cancels in pair footwork phases.
      entity.setUUID(new UUID(stable.getMostSignificantBits(), stable.getLeastSignificantBits() ^ RUN_SALT));
      entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
      if (manifest) entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, null);
      entity.getRandom().setSeed(seed); entity.setPersistenceRequired();
      if (!level.addFreshEntity(entity)) throw new IllegalStateException("Could not add duel participant " + id);
      return entity;
   }

   private static BlockPos arena(GameTestHelper h) {
      BlockPos center = new BlockPos(2048 + ARENAS.getAndIncrement() * 160, 100, 2048);
      force(h.getLevel(), center, true);
      for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z++) {
         for (int y = 1; y <= 31; y++) h.getLevel().setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
         h.getLevel().setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
         h.getLevel().setBlock(center.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
         if (Math.abs(x) == 30 || Math.abs(z) == 30)
            for (int y = 1; y <= 30; y++) h.getLevel().setBlock(center.offset(x, y, z), Blocks.BARRIER.defaultBlockState(), 2);
      }
      discardArenaEntities(h.getLevel(), center);
      return center;
   }

   private static void force(ServerLevel level, BlockPos center, boolean forced) {
      for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
         level.setChunkForced((center.getX() >> 4) + x, (center.getZ() >> 4) + z, forced);
   }

   private static void discardArenaEntities(ServerLevel level, BlockPos center) {
      for (var e : level.getEntities((net.minecraft.world.entity.Entity)null, new AABB(center).inflate(64),
         e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
   }

   private static void cleanup(ServerLevel level, BlockPos center) { discardArenaEntities(level, center); force(level, center, false); }
}
