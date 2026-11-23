package net.xxxjk.TYPE_MOON_WORLD.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicBinding;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicSuggestion;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantNoblePhantasmDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;
import net.xxxjk.typemoonworld.api.MagecraftDamageSource;
import net.xxxjk.typemoonworld.api.MagicComplexity;

@GameTestHolder("typemoonworld_magecraft")
@PrefixGameTestTemplate(false)
public final class MagecraftResistanceGameTests {
   private MagecraftResistanceGameTests() { }

   private static LivingEntity target(GameTestHelper h) {
      var target = h.spawn(EntityType.SHEEP, new BlockPos(2, 3, 2));
      target.setNoAi(true);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
      target.setHealth(200);
      return target;
   }

   private static void rank(LivingEntity target, MagicResistanceRank rank) {
      var vars = target.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.is_magus = false;
      vars.magicState().setMaxMana(0);
      MagicResistanceHelper.setMagicResistance(target, rank, MagicResistanceHelper.damageReductionForRank(rank),
         MagicResistanceHelper.debuffResistanceForRank(rank));
   }

   private static void close(GameTestHelper h, float actual, float expected, String message) {
      h.assertTrue(Math.abs(actual - expected) < 0.001F, message + ": " + actual + " != " + expected);
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void fullMatrixMatchesForPlayerAndNpc(GameTestHelper h) {
      var npc = target(h);
      var player = h.makeMockServerPlayerInLevel();
      for (var target : new LivingEntity[]{npc, player}) {
         for (var rank : MagicResistanceRank.values()) {
            rank(target, rank);
            for (var complexity : MagicComplexity.values()) {
               boolean immune = rank != MagicResistanceRank.NONE && complexity.level() < rank.level();
               var source = MagecraftDamageSource.of(target.damageSources().magic(), "test_spell", complexity, null);
               float expected = immune ? 0 : 100 * (1 - MagicResistanceHelper.damageReductionForRank(rank));
               close(h, MagicResistanceHelper.applyMagicDamageReduction(target, source, 100), expected, rank + "/" + complexity);
               close(h, MagicResistanceHelper.applyMagicDamageReduction(target, source, 1), expected / 100, "no small-hit threshold");
            }
         }
      }
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void realDamageBusOnlyReducesExplicitMagecraftOnce(GameTestHelper h) {
      var target = target(h);
      rank(target, MagicResistanceRank.B);
      float before = target.getHealth();
      target.hurt(target.damageSources().magic(), 10);
      close(h, before - target.getHealth(), 10, "ordinary MAGIC");
      var caster = target(h);
      for (var ordinary : new DamageSource[]{target.damageSources().indirectMagic(caster, caster), caster.damageSources().mobAttack(caster)}) {
         target.invulnerableTime = 0;
         before = target.getHealth();
         target.hurt(ordinary, 10);
         close(h, before - target.getHealth(), 10, "ordinary indirect magic or physical hit");
      }
      target.invulnerableTime = 0;
      before = target.getHealth();
      var source = MagecraftDamageSource.of(target.damageSources().magic(), "test_spell", MagicComplexity.HIGH_THAUMATURGY, null);
      target.hurt(new CombatDamageSource(source, AttackOrigin.skill("test", "spell")), 10);
      close(h, before - target.getHealth(), 6.5F, "single 35% reduction through decorators");
      target.invulnerableTime = 0;
      before = target.getHealth();
      target.hurt(MagecraftDamageSource.of(target.damageSources().magic(), "test_spell", MagicComplexity.THREE_VERSE, null), 100);
      close(h, target.getHealth(), before, "immune spell");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void noblePhantasmsRequireExplicitMetadata(GameTestHelper h) {
      var target = target(h);
      rank(target, MagicResistanceRank.A);
      var old = ServantNoblePhantasmDataRegistry.all();
      var definitions = new LinkedHashMap<>(old);
      var definition = ServantNoblePhantasmDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
         "{\"id\":\"magecraft_test_np\",\"type\":\"army\",\"magic_damage\":true,\"magic_complexity\":\"high_thaumaturgy\"}"))
         .result().orElseThrow();
      definitions.put("magecraft_test_np", definition);
      ServantNoblePhantasmDataRegistry.reload(definitions);
      try {
         DamageSource ordinary = NoblePhantasmDamageClassifier.withNoblePhantasm(target.damageSources().magic(), "ordinary_test_np");
         close(h, MagicResistanceHelper.applyMagicDamageReduction(target, ordinary, 100), 100, "ordinary NP");
         target.hurt(ordinary, 10);
         close(h, target.getHealth(), 190, "ordinary NP on damage bus");
         var magic = NoblePhantasmDamageClassifier.withNoblePhantasm(target.damageSources().magic(), "magecraft_test_np");
         h.assertTrue(NoblePhantasmDamageClassifier.isNoblePhantasmDamage(magic, 1), "NP provenance lost");
         h.assertTrue(NoblePhantasmDamageClassifier.typeOf(magic) == ServantNoblePhantasmDefinition.NpType.ARMY, "NP type lost");
         close(h, MagicResistanceHelper.applyMagicDamageReduction(target, magic, 100), 0, "explicit magic NP");
         target.invulnerableTime = 0;
         target.hurt(magic, 10);
         close(h, target.getHealth(), 190, "explicit magic NP on damage bus");
         var nested = NoblePhantasmDamageClassifier.withNoblePhantasm(
            MagecraftDamageSource.of(target.damageSources().magic(), "test_spell", MagicComplexity.SIMPLE_ACTION, null), "ordinary_test_np");
         close(h, MagicResistanceHelper.applyMagicDamageReduction(target, nested, 100), 100, "ordinary NP overrides inner magic");
      } finally {
         ServantNoblePhantasmDataRegistry.reload(old);
      }
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void potionsRemainWhileMagecraftControlsAreResisted(GameTestHelper h) {
      var target = target(h);
      var caster = target(h);
      rank(target, MagicResistanceRank.A);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
      h.assertTrue(target.hasEffect(MobEffects.WEAKNESS), "ordinary potion was removed");
      h.assertTrue(target.getEffect(MobEffects.WEAKNESS).getDuration() == 200, "ordinary potion duration changed");
      h.assertTrue(!MagicBinding.applyBinding(caster, target, 40), "binding bypassed immunity");
      h.assertTrue(!MagicSuggestion.applySuggestion(caster, target, 40), "suggestion bypassed immunity");
      h.assertTrue(!target.hasEffect(ModMobEffects.BINDING) && !target.hasEffect(ModMobEffects.SUGGESTION), "magic control was installed");
      MagecraftEffects.addHarmful(target, new MobEffectInstance(MobEffects.JUMP, 100, 128), MagicComplexity.TWO_VERSE);
      h.assertTrue(!target.hasEffect(MobEffects.JUMP), "magic jump suppression bypassed immunity");
      net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravityEffectHandler.applyGravityState(
         target, 1, h.getLevel().getGameTime() + 100, caster);
      h.assertTrue(!target.getPersistentData().contains("TypeMoonGravityUntil"), "gravity bypassed immunity");
      rank(target, MagicResistanceRank.E);
      MagecraftEffects.add(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100), MagicComplexity.TWO_VERSE);
      h.assertTrue(target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() == 94, "effect was not shortened exactly once");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void definitionsAndProficiencyAreUsedAtImpact(GameTestHelper h) {
      var target = target(h);
      var caster = target(h);
      rank(target, MagicResistanceRank.NONE);
      for (var definition : MagicDefinitionRegistry.all().values()) {
         var source = MagecraftDamageSource.of(target.damageSources().magic(), definition.id().toString(), caster);
         h.assertTrue(source.complexity() == definition.resolvedResistanceComplexity(), "wrong tier for " + definition.id());
      }
      target.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).magic_proficiencies.put("magic_bullet", 100.0);
      var source = MagecraftDamageSource.of(target.damageSources().magic(), "magic_bullet", caster, 20);
      close(h, MagicResistanceHelper.applyMagicDamageReduction(target, source, 100), 0, "proficiency contest lost");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void casterProjectilesCarryMagicButMeleeAndNpDoNot(GameTestHelper h) throws Exception {
      var victim = target(h);
      var caster = target(h);
      rank(victim, MagicResistanceRank.C);
      var bolt = new net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity(h.getLevel(), caster);
      bolt.setMagicDamage(10);
      var impact = bolt.getClass().getDeclaredMethod("onHitEntity", net.minecraft.world.phys.EntityHitResult.class);
      impact.setAccessible(true);
      float before = victim.getHealth();
      impact.invoke(bolt, new net.minecraft.world.phys.EntityHitResult(victim));
      close(h, victim.getHealth(), before, "Medea bolt bypassed immunity");
      var spirit = net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.PARACELSUS_SPIRIT_CANNON.get().create(h.getLevel());
      var shot = spirit.getClass().getDeclaredMethod("fireShot", net.minecraft.server.level.ServerLevel.class, LivingEntity.class, LivingEntity.class);
      shot.setAccessible(true);
      shot.invoke(spirit, h.getLevel(), caster, victim);
      close(h, victim.getHealth(), before, "Paracelsus spirit bypassed immunity");
      h.assertTrue(!victim.isOnFire() && !victim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "immune spirit applied an elemental effect");
      h.assertTrue(!MagicResistanceHelper.isMagicDamage(caster.damageSources().mobAttack(caster)), "melee was marked as magic");
      var cannon = caster.damageSources().mobProjectile(bolt, caster);
      h.assertTrue(!MagicResistanceHelper.isMagicDamage(cannon), "unmarked Caster projectile was magic");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void gilgameshSlateAndRoyalCannonHaveDifferentProvenance(GameTestHelper h) throws Exception {
      var victim = target(h);
      rank(victim, MagicResistanceRank.C);
      var caster = target(h);
      var shot = new net.xxxjk.TYPE_MOON_WORLD.entity.RoyalCannonProjectileEntity(
         h.getLevel(), caster, caster.position(), new net.minecraft.world.phys.Vec3(1, 0, 0), 10);
      var impact = shot.getClass().getDeclaredMethod("hurtVictim", LivingEntity.class, LivingEntity.class, float.class);
      impact.setAccessible(true);
      impact.invoke(shot, caster, victim, 10F);
      close(h, victim.getHealth(), 200, "slate spell bypassed immunity");
      shot.configureRoyalCannon(3);
      impact.invoke(shot, caster, victim, 10F);
      close(h, victim.getHealth(), 190, "Royal Cannon incorrectly resisted as magecraft");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void immuneElementalMagicBulletCannotIgnite(GameTestHelper h) throws Exception {
      var victim = target(h);
      rank(victim, MagicResistanceRank.E);
      var caster = target(h);
      var bullet = new net.xxxjk.TYPE_MOON_WORLD.entity.MagicBulletProjectileEntity(h.getLevel(), caster);
      bullet.configure(10, 0, 24, net.xxxjk.TYPE_MOON_WORLD.entity.MagicBulletProjectileEntity.ELEMENT_FIRE, 1);
      var impact = bullet.getClass().getDeclaredMethod("onHitEntity", net.minecraft.world.phys.EntityHitResult.class);
      impact.setAccessible(true);
      impact.invoke(bullet, new net.minecraft.world.phys.EntityHitResult(victim));
      close(h, victim.getHealth(), 200, "simple bullet bypassed rank E immunity");
      h.assertTrue(!victim.isOnFire(), "immune bullet ignited its target");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void jewelAndRuneExplosionsKeepMagecraftClassification(GameTestHelper h) throws Exception {
      var victim = target(h);
      rank(victim, MagicResistanceRank.E);
      var caster = target(h);
      rank(caster, MagicResistanceRank.E);
      var ruby = new net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity(h.getLevel(), caster);
      ruby.setPos(victim.position().add(0, 0.5, 0));
      var explode = ruby.getClass().getDeclaredMethod("explodeWithoutAffectingOwner", float.class, boolean.class,
         net.minecraft.world.level.Level.ExplosionInteraction.class);
      explode.setAccessible(true);
      explode.invoke(ruby, 1F, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
      close(h, victim.getHealth(), 200, "ruby explosion lost its magic ID");
      var bullet = new net.xxxjk.TYPE_MOON_WORLD.entity.MagicBulletProjectileEntity(h.getLevel(), caster);
      bullet.setRuneExplosionRadius(1);
      var runeExplosion = bullet.getClass().getDeclaredMethod("explodeRune", net.minecraft.world.phys.Vec3.class);
      runeExplosion.setAccessible(true);
      runeExplosion.invoke(bullet, victim.position().add(0, 0.5, 0));
      close(h, victim.getHealth(), 200, "rune explosion lost its magic ID");
      victim.invulnerableTime = 0;
      h.getLevel().explode(null, victim.getX(), victim.getY() + 0.5, victim.getZ(), 1,
         net.minecraft.world.level.Level.ExplosionInteraction.NONE);
      h.assertTrue(victim.getHealth() < 200, "ordinary explosion was resisted or explosion fixture was ineffective");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void definitionOverridesApplyToDamageAndBinding(GameTestHelper h) {
      var victim = target(h);
      var caster = target(h);
      rank(victim, MagicResistanceRank.D);
      var old = MagicDefinitionRegistry.all();
      var definitions = new LinkedHashMap<>(old);
      for (String id : new String[]{"magecraft_test:graded_spell", "typemoonworld:binding_magic"}) {
         var definition = net.xxxjk.typemoonworld.api.MagicDefinitionData.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString("{\"id\":\"" + id + "\",\"resistance_complexity\":\"high_thaumaturgy\"}"))
            .result().orElseThrow();
         definitions.put(id, definition);
      }
      MagicDefinitionRegistry.reload(definitions);
      try {
         var source = MagecraftDamageSource.of(victim.damageSources().magic(), "magecraft_test:graded_spell", caster);
         h.assertTrue(source.complexity() == MagicComplexity.HIGH_THAUMATURGY, "addon definition tier was ignored");
         victim.hurt(source, 100);
         close(h, victim.getHealth(), 112, "configured high magecraft reduced exactly once");
         h.assertTrue(MagicBinding.applyBinding(caster, victim, 40), "binding ignored configured complexity");
         h.assertTrue(victim.hasEffect(ModMobEffects.BINDING), "configured binding effect was missing");
         victim.removeEffect(ModMobEffects.BINDING);
         rank(victim, MagicResistanceRank.C);
         h.assertTrue(MagicBinding.applyArea(caster, victim.position(), 40, 2), "area binding ignored configured complexity");
      } finally {
         MagicDefinitionRegistry.reload(old);
      }
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void characterResistanceMatchesCardsAndNpc(GameTestHelper h) {
      var player = h.makeMockServerPlayerInLevel();
      for (String id : new String[]{"arash", "pale_rider"}) {
         var npc = net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory.create(h.getLevel(),
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("typemoonworld", id));
         npc.setNoAi(true); npc.setNoGravity(true);
         npc.setPos(h.absolutePos(new BlockPos(4, 3, 4)).getCenter());
         h.getLevel().addFreshEntity(npc);
         h.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(player, id), "card transform failed: " + id);
         float expected = "arash".equals(id) ? 0.20F : MagicResistanceHelper.damageReductionForRank(MagicResistanceRank.C);
         close(h, MagicResistanceHelper.getDamageReduction(player), expected, id + " card resistance differs");
         close(h, MagicResistanceHelper.getDamageReduction(npc), expected, id + " NPC resistance differs");
         close(h, MagicResistanceHelper.getDebuffResistance(player), MagicResistanceHelper.getDebuffResistance(npc), id + " effect resistance differs");
         npc.discard();
      }
      h.succeed();
   }
}
