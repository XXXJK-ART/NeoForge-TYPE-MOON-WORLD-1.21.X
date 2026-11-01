package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes;
import net.xxxjk.typemoonworld.api.MagecraftDamageSource;
import net.xxxjk.typemoonworld.api.MagicComplexity;

@GameTestHolder("typemoonworld_palerider")
@PrefixGameTestTemplate(false)
public final class PaleRiderResetGameTests {
   private PaleRiderResetGameTests() {}

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void identityAndAttributesSurviveReloadWithoutHealing(GameTestHelper helper) {
      PaleRiderEntity rider = spawnRider(helper);
      var params = rider.getDefinition().parameters();
      close(helper, rider.getMaxHealth(), 500.0, "endurance A");
      close(helper, rider.getAttributeValue(Attributes.MOVEMENT_SPEED), params.movementSpeed(), "agility B");
      close(helper, rider.getAttributeValue(Attributes.ATTACK_DAMAGE), params.attackDamage(), "strength E");
      close(helper, rider.getAttributeValue(Attributes.ARMOR), params.armor(), "shared armor formula");
      helper.assertTrue(rider.getDefinition().faction() == ServantFaction.HEAVEN, "wrong faction");
      helper.assertTrue("EX".equals(params.noblePhantasmRank()), "missing EX parameter");
      helper.assertTrue(ServantIdentityHelper.hasTrait(rider, ServantTraitTag.RIDING), "missing Riding trait");
      helper.assertTrue(ServantIdentityHelper.hasTrait(rider, ServantTraitTag.HEAVEN_OR_EARTH_SERVANT), "missing Heaven/Earth trait");
      helper.assertTrue(!ServantIdentityHelper.hasTrait(rider, ServantTraitTag.MOUNTED), "permanent mounted state");
      rider.setHealth(123.0F);
      rider.setCurrentMp(321.0);
      CompoundTag saved = new CompoundTag();
      rider.addAdditionalSaveData(saved);
      PaleRiderEntity loaded = ModEntities.PALE_RIDER.get().create(helper.getLevel());
      helper.assertTrue(loaded != null, "reload entity missing");
      loaded.readAdditionalSaveData(saved);
      close(helper, loaded.getMaxHealth(), params.maxHealth(), "reload maximum health");
      close(helper, loaded.getHealth(), 123.0, "reload healed rider");
      close(helper, loaded.getCurrentMp(), 321.0, "reload refilled MP");
      close(helper, loaded.getAttributeValue(Attributes.ARMOR), params.armor(), "reload restored legacy armor");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardAndNpcUseOneMagecraftReduction(GameTestHelper helper) {
      PaleRiderEntity rider = spawnRider(helper);
      var player = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(ServantCardTransformManager.transform(player, "pale_rider"), "card transform failed");
      close(helper, player.getMaxHealth(), rider.getMaxHealth(), "card/NPC health differs");
      var base = rider.damageSources().indirectMagic(null, null);
      var high = MagecraftDamageSource.of(base, "pale_reset_test", MagicComplexity.HIGH_THAUMATURGY, null);
      float expected = MagicResistanceHelper.applyMagicDamageReduction(player, high, 100.0F);
      close(helper, expected, 78.0, "shared C resistance not applied");
      close(helper, MagicResistanceHelper.applyMagicDamageReduction(rider, high, 100.0F), expected, "card/NPC magic reduction differs");
      float[] incoming = {Float.NaN};
      java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> capture =
         event -> { if (event.getEntity() == rider) incoming[0] = event.getOriginalAmount(); };
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
         net.neoforged.bus.api.EventPriority.HIGHEST, true,
         net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent.class, capture);
      try {
         rider.hurt(high, 100.0F);
         close(helper, incoming[0], 100.0, "NPC applied resistance before the shared damage event");
      } finally {
         net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(capture);
      }
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void fourCalamitiesPreservesDamageTypeOwnerAndNpClassification(GameTestHelper helper) {
      PaleRiderEntity rider = spawnRider(helper);
      for (var type : java.util.List.of(PaleRiderDamageTypes.FAMINE, PaleRiderDamageTypes.CONCEPT_DEATH,
         PaleRiderDamageTypes.CONCEPT_SWORD)) {
         var source = PaleRiderDamageTypes.noblePhantasm(rider.damageSources().source(type, rider));
         helper.assertTrue(source.is(type) && source.getEntity() == rider, "damage type or owner lost");
         helper.assertTrue(NoblePhantasmDamageClassifier.isNoblePhantasmDamage(source, 5.0F), "unclassified NP hit");
         helper.assertTrue(NoblePhantasmDamageClassifier.typeOf(source) == ServantNoblePhantasmDefinition.NpType.ARMY,
            "not an anti-army NP");
         helper.assertTrue(MagecraftDamageSource.find(source) == null, "NP incorrectly marked as magecraft");
      }
      helper.assertTrue(!NoblePhantasmDamageClassifier.isNoblePhantasmDamage(
         rider.damageSources().source(PaleRiderDamageTypes.INFECTION, rider), 20.0F), "ordinary infection became an NP");
      var action = ServantCardSkillLayout.actionFor("pale_rider", 9, false);
      var definition = net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantNoblePhantasmDataRegistry.get(PaleRiderCombatRules.NOBLE_PHANTASM_ID);
      close(helper, definition.mpCost(), action.mpCost(), "NP data mana differs");
      close(helper, definition.range(), 25.0, "NP data range differs");
      close(helper, action.mpCost(), PaleRiderCombatRules.CALAMITY_CAST_MP_COST, "NP mana differs");
      helper.assertTrue(action.cooldownTicks() == PaleRiderCombatRules.CALAMITY_COOLDOWN_TICKS, "NP cooldown differs");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void calamityEndsWhenManaSupplyFails(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(ServantCardTransformManager.transform(player, "pale_rider"), "card transform failed");
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servantCardState().setMana(0.0);
      helper.assertTrue(ServantCardPaleRiderSkills.toggleCalamity(player), "domain did not open");
      player.getPersistentData().putLong("PaleRiderCardLastCalamityTick", helper.getLevel().getGameTime() - 20L);
      ServantCardPaleRiderSkills.tick(player, vars);
      helper.assertTrue(!ServantCardPaleRiderSkills.isCalamityActive(player), "empty supply left domain active");
      ServantCardPaleRiderSkills.clear(player);
      helper.succeed();
   }

   private static PaleRiderEntity spawnRider(GameTestHelper helper) {
      PaleRiderEntity rider = helper.spawn(ModEntities.PALE_RIDER.get(), new BlockPos(3, 8, 3));
      rider.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(rider.blockPosition()),
         net.minecraft.world.entity.MobSpawnType.COMMAND, null);
      rider.setNoAi(true);
      return rider;
   }

   private static void close(GameTestHelper helper, double actual, double expected, String message) {
      helper.assertTrue(Math.abs(actual - expected) < 0.001, message + ": " + actual + " != " + expected);
   }
}
