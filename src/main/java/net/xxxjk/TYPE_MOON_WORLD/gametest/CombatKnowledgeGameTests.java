package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiBrain;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatKnowledgeService;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatMatchupEvaluator;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ProjectileThreatClassifier;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactType;

@GameTestHolder("typemoonworld")
@PrefixGameTestTemplate(false)
public final class CombatKnowledgeGameTests {
   private CombatKnowledgeGameTests() { }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void protectionFromArrowsUsesLiveMobilityAndProjectileBypasses(GameTestHelper helper) {
      var level = helper.getLevel();
      var cu = ModEntities.CU_CHULAINN.get().create(level);
      var archer = EntityType.SKELETON.create(level);
      helper.assertTrue(cu != null && archer != null, "failed to create projectile matchup entities");
      cu.setPos(helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2)).getCenter());
      archer.setPos(helper.absolutePos(new net.minecraft.core.BlockPos(8, 1, 2)).getCenter());
      level.addFreshEntity(cu);
      level.addFreshEntity(archer);
      cu.getPersistentData().putBoolean(CuChulainnCombatHelper.PROTECTION_FROM_ARROWS_TAG, true);

      Arrow arrow = new Arrow(level, archer, Items.ARROW.getDefaultInstance(), Items.BOW.getDefaultInstance());
      var normal = level.damageSources().mobProjectile(arrow, archer);
      helper.assertTrue(CombatMatchupEvaluator.negatesProjectileDamage(cu, normal),
         "mobile protection from arrows should negate a normal projectile");

      CombatKnowledgeService.observeProjectileNegation(cu, arrow);
      helper.assertTrue(AiBrain.blackboard(archer).opponent(cu.getUUID()).knows(FactType.PROJECTILE_NEGATION),
         "projectile owner did not learn the observed negation");

      cu.addEffect(new MobEffectInstance(ModMobEffects.BINDING, 20));
      helper.assertTrue(!CombatMatchupEvaluator.negatesProjectileDamage(cu, normal),
         "binding should disable protection from arrows");
      cu.removeEffect(ModMobEffects.BINDING);

      arrow.getPersistentData().putBoolean(ProjectileThreatClassifier.EXPLOSIVE_TAG, true);
      helper.assertTrue(!CombatMatchupEvaluator.negatesProjectileDamage(cu, normal),
         "explosive projectiles should bypass protection from arrows");
      helper.succeed();
   }
}
