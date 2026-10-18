package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real NPCs, native HP/MP/skills and deterministic seeds; no health time gates. */
@GameTestHolder("typemoonworld_duels")
@PrefixGameTestTemplate(false)
public final class ServantDuelBalanceGameTests {
   private static final AtomicInteger NEXT_ARENA = new AtomicInteger();
   @GameTest(template = "ancient_temple", timeoutTicks = 5100)
   public static void gilgameshAgainstGilles(GameTestHelper h) { rounds(h, "gilgamesh", "gilles_de_rais_caster"); }
   @GameTest(template = "ancient_temple", timeoutTicks = 5100)
   public static void enkiduAgainstMedea(GameTestHelper h) { rounds(h, "enkidu", "medea"); }
   @GameTest(template = "ancient_temple", timeoutTicks = 5100)
   public static void gilgameshAgainstKojiro(GameTestHelper h) { rounds(h, "gilgamesh", "sasaki_kojiro"); }
   @GameTest(template = "ancient_temple", timeoutTicks = 5100)
   public static void artoriaAgainstCu(GameTestHelper h) { rounds(h, "artoria_pendragon", "cu_chulainn"); }
   @GameTest(template = "ancient_temple", timeoutTicks = 5100)
   public static void lancelotAgainstHeracles(GameTestHelper h) { rounds(h, "lancelot_berserker", "heracles"); }
   @GameTest(template = "ancient_temple", timeoutTicks = 5100)
   public static void emiyaAgainstNobunaga(GameTestHelper h) { rounds(h, "emiya_archer", "oda_nobunaga"); }
   @GameTest(template = "ancient_temple", timeoutTicks = 5100)
   public static void medeaAgainstGilles(GameTestHelper h) { rounds(h, "medea", "gilles_de_rais_caster"); }

   private static void rounds(GameTestHelper h, String left, String right) {
      int arena = NEXT_ARENA.getAndIncrement();
      BlockPos center = new BlockPos(512 + arena * 160, 100, 512);
      prepare(h.getLevel(), center);
      start(h, center, left, right, 42, () -> start(h, center, left, right, 2026, () -> {
         force(h.getLevel(), center, false); h.succeed();
      }));
   }
   private static void force(ServerLevel level, BlockPos center, boolean forced) {
      int cx = center.getX() >> 4, cz = center.getZ() >> 4;
      for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) level.setChunkForced(x, z, forced);
   }
   private static void prepare(ServerLevel level, BlockPos center) {
      force(level, center, true);
      for (int x = -28; x <= 28; x++) for (int z = -28; z <= 28; z++) {
         for (int y = -2; y <= 0; y++) level.setBlock(center.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
         if (Math.abs(x) == 28 || Math.abs(z) == 28)
            for (int y = 1; y <= 20; y++) level.setBlock(center.offset(x, y, z), Blocks.BARRIER.defaultBlockState(), 2);
      }
   }
   private static void start(GameTestHelper h, BlockPos center, String left, String right, long seed, Runnable done) {
      var level = h.getLevel();
      ServantEntity a = spawn(level, left, center.offset(-8, 1, 0), seed);
      ServantEntity b = spawn(level, right, center.offset(8, 1, 0), seed + 1);
      a.setTarget(b); b.setTarget(a);
      long started = level.getGameTime();
      poll(h, center, a, b, left, right, seed, started, done);
   }
   private static ServantEntity spawn(ServerLevel level, String id, BlockPos pos, long seed) {
      ServantEntity servant = BuiltinServantEntityFactory.create(level, ResourceLocation.fromNamespaceAndPath("typemoonworld", id));
      if (servant == null) throw new IllegalStateException("Missing servant " + id);
      servant.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
      servant.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), net.minecraft.world.entity.MobSpawnType.COMMAND, null);
      servant.getRandom().setSeed(seed); servant.setPersistenceRequired(); level.addFreshEntity(servant);
      return servant;
   }
   private static void poll(GameTestHelper h, BlockPos center, ServantEntity a, ServantEntity b,
                            String left, String right, long seed, long started, Runnable done) {
      h.runAfterDelay(20, () -> {
         long elapsed = h.getLevel().getGameTime() - started;
         boolean ended = !a.isAlive() || !b.isAlive();
         if (!ended && elapsed < 2400) {
            if (a.level() == b.level()) { a.setTarget(b); b.setTarget(a); }
            poll(h, center, a, b, left, right, seed, started, done); return;
         }
         int lost = 0;
         for (int x = -26; x <= 26; x++) for (int z = -26; z <= 26; z++)
            if (h.getLevel().getBlockState(center.offset(x, 0, z)).getCollisionShape(h.getLevel(), center.offset(x, 0, z)).isEmpty()) lost++;
         TYPE_MOON_WORLD.LOGGER.info("TMW_DUEL left={} right={} seed={} seconds={} ended={} hpA={} hpB={} basic={} skills={} guards={} parries={} breaks={} groundLost={} dash={} launch={} pursuit={} maxIdle={}",
            left, right, seed, elapsed / 20.0, ended, a.getHealth(), b.getHealth(),
            metric(a, b, "TypeMoonDuelBasicAttacks"), metric(a, b, "TypeMoonDuelSkillDecisions"),
            metric(a, b, "TypeMoonDefenseGuardCount"), metric(a, b, "TypeMoonDefenseParryCount"),
            metric(a, b, "TypeMoonDefenseBreakCount"), lost,
            metric(a, b, "TypeMoonCombatDashCount"), metric(a, b, "TypeMoonCombatLaunchCount"),
            metric(a, b, "TypeMoonCombatPursuitCount"), Math.max(a.getPersistentData().getInt("TypeMoonCombatMaxIdleTicks"), b.getPersistentData().getInt("TypeMoonCombatMaxIdleTicks")));
         for (var e : h.getLevel().getEntities((net.minecraft.world.entity.Entity)null, new AABB(center).inflate(60), e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
         a.discard(); b.discard();
         prepare(h.getLevel(), center);
         done.run();
      });
   }
   private static int metric(ServantEntity a, ServantEntity b, String key) {
      return a.getPersistentData().getInt(key) + b.getPersistentData().getInt(key);
   }
}
