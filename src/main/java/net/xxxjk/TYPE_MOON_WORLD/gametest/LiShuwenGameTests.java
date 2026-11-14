package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDefenseHandler;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardLiShuwenSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenCombatHelper;

@GameTestHolder("typemoonworld_li_shuwen")
@PrefixGameTestTemplate(false)
public final class LiShuwenGameTests {
   private LiShuwenGameTests() { }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void npcSeasonedAppliesOnceAndPreservesMentalStability(GameTestHelper h) {
      var npc = h.spawn(ModEntities.LI_SHUWEN.get(), new BlockPos(3, 5, 3));
      // GameTestHelper.spawn skips finalizeSpawn; reload follows the existing-world initialization path.
      npc.readAdditionalSaveData(npc.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
      h.assertTrue(npc.getPersistentData().getBoolean("LiShuwenSeasonedActive"), "Seasoned passive was not registered during NPC initialization");
      npc.setNoAi(true);
      npc.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
      h.runAfterDelay(2, () -> {
         npc.tick();
         npc.tick();
         close(h, npc.getAttributeValue(Attributes.MOVEMENT_SPEED), npc.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 1.05, "NPC speed stacked or missing");
         close(h, npc.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 1.0, "NPC capped knockback resistance changed");
         npc.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.1);
         close(h, npc.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.3, "NPC knockback bonus missing below cap");
         close(h, LiShuwenCombatHelper.applyIncomingDamageModifiers(npc, npc.damageSources().generic(), 100), 95, "NPC Seasoned damage reduction");
         close(h, LiShuwenCombatHelper.applyIncomingDamageModifiers(npc, npc.damageSources().fellOutOfWorld(), 100), 100, "Seasoned reduced bypass damage");
         close(h, LiShuwenCombatHelper.applyIncomingDamageModifiers(npc, npc.damageSources().starve(), 100), 100, "Seasoned reduced resistance-bypass damage");
         h.assertTrue(!npc.hasEffect(MobEffects.BLINDNESS), "Mental stability was lost");
         h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void cardSeasonedAppliesOnceAndClearsOnFormChange(GameTestHelper h) {
      var player = h.makeMockServerPlayerInLevel();
      net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(player.connection.getConnection());
      double beforeTransformSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
      double beforeTransformKnockback = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      h.assertTrue(ServantCardTransformManager.transform(player, "li_shuwen"), "Li Shuwen transformation failed");
      player.tickCount = 10;
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double originalSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
      double originalKnockback = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      ServantCardLiShuwenSkills.tick(player, vars);
      ServantCardLiShuwenSkills.tick(player, vars);
      close(h, player.getAttributeValue(Attributes.MOVEMENT_SPEED), originalSpeed * 1.05, "Card speed stacked or missing");
      close(h, player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), originalKnockback + 0.20, "Card knockback resistance");
      var hit = new LivingIncomingDamageEvent(player,
         new net.neoforged.neoforge.common.damagesource.DamageContainer(player.damageSources().generic(), 100));
      ServantCardDefenseHandler.handleIncomingDamage(player, vars, hit);
      close(h, hit.getAmount(), 95, "Card Seasoned damage reduction");
      h.assertTrue(ServantCardTransformManager.release(player, false), "Card release failed");
      close(h, player.getAttributeValue(Attributes.MOVEMENT_SPEED), beforeTransformSpeed, "Speed leaked after revert");
      close(h, player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), beforeTransformKnockback, "Knockback resistance leaked after revert");
      h.assertTrue(ServantCardTransformManager.transform(player, "li_shuwen"), "Second transformation failed");
      ServantCardLiShuwenSkills.tick(player, vars);
      h.assertTrue(ServantCardTransformManager.transform(player, "sasaki_kojiro"), "Changing servant failed");
      var modifierId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("typemoonworld", "li_shuwen_seasoned");
      h.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(modifierId) == null
         && player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(modifierId) == null, "Seasoned leaked after changing servant");
      h.succeed();
   }

   private static void close(GameTestHelper h, double actual, double expected, String message) {
      h.assertTrue(Math.abs(actual - expected) < 0.0001, message + ": " + actual + " != " + expected);
   }
}
