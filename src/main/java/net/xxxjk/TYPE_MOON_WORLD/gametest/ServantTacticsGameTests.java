package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPhaseService;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatTempoService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightCombatService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

@GameTestHolder("typemoonworld_tactics")
@PrefixGameTestTemplate(false)
public final class ServantTacticsGameTests {
   private static final AtomicInteger ARENAS = new AtomicInteger();

   @GameTest(template = "ancient_temple", templateNamespace = "typemoonworld_tactics", timeoutTicks = 20)
   public static void phasesAdvanceAtRangedDistanceAndNeverRegressAfterHealing(GameTestHelper h) {
      BlockPos center = arena(h);
      var a = spawn(h, "artoria_pendragon", center.above(), true);
      var b = spawn(h, "artoria_pendragon", center.offset(40, 1, 0), true);
      a.setTarget(b);
      long now = h.getLevel().getGameTime();
      a.getPersistentData().putLong("TypeMoonAiCombatStartedAt", now - 600L);
      a.getPersistentData().putLong("TypeMoonAiLastCombatAt", now);
      ServantCombatSystem.tickBeforeAi(a);
      h.assertTrue(ServantCombatSystem.getPhase(a) == ServantCombatPhase.NORMAL, "30-second ranged probing did not end");
      a.getPersistentData().putLong("TypeMoonAiCombatStartedAt", now - 1200L);
      ServantCombatSystem.tickBeforeAi(a);
      h.assertTrue(ServantCombatSystem.canUseNoblePhantasm(a), "full-health stalemate never unlocked NP");
      a.setHealth(a.getMaxHealth());
      h.assertTrue(ServantPhaseService.tick(a, b, null, now).phase().ordinal() >= 2, "healing downgraded combat phase");
      a.discard(); b.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", templateNamespace = "typemoonworld_tactics", timeoutTicks = 20)
   public static void overwhelmingOffenseSkipsProbingAtEqualHealth(GameTestHelper h) {
      BlockPos center = arena(h);
      var a = spawn(h, "artoria_pendragon", center.above(), true);
      var b = spawn(h, "artoria_pendragon", center.offset(12, 1, 0), true);
      b.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(500);
      a.setTarget(b); ServantCombatSystem.tickBeforeAi(a);
      h.assertTrue(ServantCombatSystem.canUseNoblePhantasm(a), "offense was ignored at equal maximum health");
      a.discard(); b.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", templateNamespace = "typemoonworld_tactics", timeoutTicks = 20)
   public static void rangedFlightRetreatsAfterNoContactTimeout(GameTestHelper h) {
      BlockPos center = arena(h);
      var gil = (GilgameshEntity)spawn(h, "gilgamesh", center.above(5), true);
      var enemy = spawn(h, "emiya_archer", center.offset(4, 1, 0), true);
      gil.setTarget(enemy);
      long now = h.getLevel().getGameTime();
      ServantCombatTempoService.tick(gil, enemy, now - 300L);
      gil.setDeltaMovement(Vec3.ZERO); GilgameshCombatHelper.tickFlight(gil); ServantFlightCombatService.tick(gil);
      h.assertTrue(gil.isFlyingMode() && gil.isNoGravity(), "flight disabled against Emiya or by no-contact timeout");
      h.assertTrue(gil.getDeltaMovement().x < 0, "flight safety replaced retreat with melee pursuit");
      h.assertTrue(!ServantFlightCombatService.forceMeleeApproach(gil, enemy, now), "ranged flyer accepted forced melee");
      gil.discard(); enemy.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", templateNamespace = "typemoonworld_tactics", timeoutTicks = 180)
   public static void gilgameshKeepsFlyingAndFiringNativeTreasuryVolleys(GameTestHelper h) {
      BlockPos center = arena(h);
      var gil = (GilgameshEntity)spawn(h, "gilgamesh", center.above(), false);
      var enemy = spawn(h, "sasaki_kojiro", center.offset(16, 1, 0), true);
      enemy.setInvulnerable(true); gil.setTarget(enemy); gil.getRandom().setSeed(173L);
      h.runAfterDelay(150L, () -> {
         h.assertTrue(gil.isFlyingMode() && gil.getY() > center.getY() + 3, "generic AI prevented sustained flight, y=" + gil.getY());
         h.assertTrue(gil.getPersistentData().contains("GilgameshLastGate"), "native treasury was starved by generic attacks");
         gil.discard(); enemy.discard(); h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", templateNamespace = "typemoonworld_tactics", timeoutTicks = 20)
   public static void smallVolleyKeepsAllProjectilesAndEffects(GameTestHelper h) {
      BlockPos center = arena(h);
      var gil = (GilgameshEntity)spawn(h, "gilgamesh", center.above(5), true);
      var enemy = spawn(h, "sasaki_kojiro", center.offset(16, 1, 0), true);
      GilgameshCombatHelper.fireGateVolley(gil, h.getLevel(), enemy, 5, 18);
      h.runAfterDelay(3L, () -> {
         var gates = h.getLevel().getEntitiesOfClass(GilgameshGateWeaponProjectileEntity.class,
            new AABB(center).inflate(50), p -> p.getOwnerEntity() == gil);
         h.assertTrue(gates.size() == 5, "small volley lost projectiles");
         h.assertTrue(gates.stream().allMatch(GilgameshGateWeaponProjectileEntity::preservesFullEffects), "small volley sampled effects");
         for (var gate : gates) gate.discard();
         gil.discard(); enemy.discard(); h.succeed();
      });
   }

   private static BlockPos arena(GameTestHelper h) {
      BlockPos center = new BlockPos(1600 + ARENAS.getAndIncrement() * 160, 100, 1600);
      for (int x = -2; x <= 3; x++) for (int z = -2; z <= 2; z++) h.getLevel().setChunkForced((center.getX() >> 4) + x, (center.getZ() >> 4) + z, true);
      for (int x = -24; x <= 48; x++) for (int z = -24; z <= 24; z++) h.getLevel().setBlock(center.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
      return center;
   }

   private static ServantEntity spawn(GameTestHelper h, String id, BlockPos pos, boolean noAi) {
      var entity = BuiltinServantEntityFactory.create(h.getLevel(), ResourceLocation.fromNamespaceAndPath("typemoonworld", id));
      h.assertTrue(entity != null, "failed to create " + id);
      entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
      // Deterministic fixtures start after manifestation; the client smoke
      // separately exercises the normal summon animation and live AI.
      var params = entity.getDefinition().parameters();
      entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(params.maxHealth());
      entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(params.attackDamage());
      entity.getAttribute(Attributes.ARMOR).setBaseValue(params.armor());
      entity.setHealth(entity.getMaxHealth()); entity.setCurrentMp(entity.getMaxMp());
      entity.setNoAi(noAi); h.getLevel().addFreshEntity(entity);
      return entity;
   }
}
