package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

@GameTestHolder("typemoonworld_basic_duels")
@PrefixGameTestTemplate(false)
public final class CuDiarmuidBasicDuelGameTests {
   private static final class Duel {
      final long seed;
      final BlockPos center;
      final ServantEntity cu, diarmuid;
      int basics;
      float maximumBasic;
      boolean inflated;
      String death = "none";

      Duel(GameTestHelper h, int index, long seed) {
         this.seed = seed;
         center = h.absolutePos(new BlockPos(512 + index * 192, 160, 512));
         for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            h.getLevel().setChunkForced((center.getX() >> 4) + x, (center.getZ() >> 4) + z, true);
         for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z++)
            for (int y = -2; y <= 0; y++) h.getLevel().setBlock(center.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
         cu = spawn(h, "cu_chulainn", center.offset(-2, 1, 0), seed);
         diarmuid = spawn(h, "diarmuid_ua_duibhne", center.offset(2, 1, 0), seed + 1);
         cu.setTarget(diarmuid);
         diarmuid.setTarget(cu);
      }
   }

   private static ServantEntity spawn(GameTestHelper h, String id, BlockPos pos, long seed) {
      var entity = BuiltinServantEntityFactory.create(h.getLevel(), ResourceLocation.fromNamespaceAndPath("typemoonworld", id));
      entity.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
      entity.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, null);
      entity.getRandom().setSeed(seed);
      entity.setPersistenceRequired();
      h.getLevel().addFreshEntity(entity);
      return entity;
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 650)
   public static void threeNativeDuelsKeepBasicDamageWithinItsOrdinaryBudget(GameTestHelper h) {
      List<Duel> duels = new ArrayList<>();
      long[] seeds = {42, 2026, 1337};
      for (int i = 0; i < seeds.length; i++) duels.add(new Duel(h, i, seeds[i]));
      Consumer<LivingDamageEvent.Post> record = event -> {
         for (Duel duel : duels) {
            var attacker = event.getSource().getEntity();
            if ((attacker == duel.cu && event.getEntity() == duel.diarmuid
               || attacker == duel.diarmuid && event.getEntity() == duel.cu)
               && "basic".equals(AttackOrigin.of(event.getSource()).skill()) && event.getNewDamage() > 0
               && !NoblePhantasmDamageClassifier.isNoblePhantasmDamage(event.getSource(), event.getNewDamage())) {
               duel.basics++;
               duel.maximumBasic = Math.max(duel.maximumBasic, event.getNewDamage());
               var servant = (ServantEntity)attacker;
               duel.inflated |= event.getNewDamage() > servant.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.31;
            }
         }
      };
      Consumer<LivingDeathEvent> recordDeath = event -> {
         for (Duel duel : duels) if (event.getEntity() == duel.cu || event.getEntity() == duel.diarmuid) {
            var source = event.getSource();
            duel.death = (event.getEntity() == duel.cu ? "cu:" : "diarmuid:")
               + source.typeHolder().unwrapKey().map(key -> key.location().toString()).orElse(source.getMsgId())
               + ":" + AttackOrigin.of(source).key()
               + ":np=" + NoblePhantasmDamageClassifier.isNoblePhantasmDamage(source, 0);
         }
      };
      NeoForge.EVENT_BUS.addListener(record);
      NeoForge.EVENT_BUS.addListener(recordDeath);
      // Manifestation clears combat targets. Reapply the intended duel after spawning.
      for (int tick = 80; tick < 600; tick += 20) h.runAfterDelay(tick, () -> {
         for (Duel duel : duels) if (duel.cu.isAlive() && duel.diarmuid.isAlive()) {
            duel.cu.setTarget(duel.diarmuid);
            duel.diarmuid.setTarget(duel.cu);
         }
      });
      h.runAfterDelay(600, () -> {
         NeoForge.EVENT_BUS.unregister(record);
         NeoForge.EVENT_BUS.unregister(recordDeath);
         int basics = 0;
         boolean inflated = false;
         for (Duel duel : duels) {
            basics += duel.basics;
            inflated |= duel.inflated;
            TYPE_MOON_WORLD.LOGGER.info("TMW_BASIC_DUEL seed={} seconds=30 hpCu={} hpDiarmuid={} ordinaryHits={} maximumOrdinaryDamage={} inflated={} ticksCu={} ticksDiarmuid={} death={}",
               duel.seed, duel.cu.getHealth(), duel.diarmuid.getHealth(), duel.basics, duel.maximumBasic, duel.inflated,
               duel.cu.tickCount, duel.diarmuid.tickCount, duel.death);
            for (var entity : h.getLevel().getEntities((net.minecraft.world.entity.Entity)null,
               new AABB(duel.center).inflate(80), entity -> !(entity instanceof net.minecraft.world.entity.player.Player))) entity.discard();
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
               h.getLevel().setChunkForced((duel.center.getX() >> 4) + x, (duel.center.getZ() >> 4) + z, false);
         }
         h.assertTrue(basics > 0, "Native AI duels never landed an ordinary attack");
         h.assertTrue(!inflated, "An ordinary hit exceeded its attack attribute and existing Cu passive");
         h.succeed();
      });
   }
}
