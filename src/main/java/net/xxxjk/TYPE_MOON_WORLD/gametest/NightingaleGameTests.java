package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.entity.NightingaleBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NightingaleGunItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.NightingaleEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardNightingaleSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleSupportService;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanDamageTypes;

@GameTestHolder("typemoonworld")
@PrefixGameTestTemplate(false)
public final class NightingaleGameTests {
   private static final BlockPos NIGHTINGALE_POS = new BlockPos(3, 8, 3);

   private NightingaleGameTests() {}

   @GameTest(batch = "nightingale", template = "ancient_temple", timeoutTicks = 40)
   public static void steelNursingHealsAndCleanses(GameTestHelper helper) {
      NightingaleEntity nightingale = nightingale(helper);
      nightingale.setHealth(nightingale.getMaxHealth() - 200.0F);
      nightingale.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
      float before = nightingale.getHealth();
      NightingaleSupportService.applyHealing(nightingale, nightingale, NightingaleRules.STEEL_NURSING_AMOUNT);
      helper.assertTrue(Math.abs(nightingale.getHealth() - (before + 150.0F)) < 0.01F, "Steel Nursing did not heal exactly 150 health");
      helper.assertTrue(!nightingale.hasEffect(MobEffects.POISON), "Steel Nursing did not cleanse poison");
      helper.succeed();
   }

   @GameTest(batch = "nightingale", template = "ancient_temple", timeoutTicks = 40)
   public static void paleRiderNpcReceivesExactHealingReversal(GameTestHelper helper) {
      NightingaleEntity nightingale = nightingale(helper);
      var paleRider = helper.spawn(ModEntities.PALE_RIDER.get(), new BlockPos(6, 8, 3));
      paleRider.setNoAi(true);
      paleRider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0);
      paleRider.setHealth(500.0F);
      NightingaleSupportService.applyHealing(nightingale, paleRider, 150.0F);
      helper.assertTrue(Math.abs(paleRider.getHealth() - 350.0F) < 0.01F, "Pale Rider NPC did not receive exact 150 reversal damage");
      helper.succeed();
   }

   @GameTest(batch = "nightingale", template = "ancient_temple", timeoutTicks = 40)
   public static void paleRiderCardPlayerReceivesExactHealingReversal(GameTestHelper helper) {
      NightingaleEntity nightingale = nightingale(helper);
      var player = helper.makeMockServerPlayerInLevel();
      player.getAbilities().invulnerable = false;
      player.setInvulnerable(false);
      player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
      player.setHealth(400.0F);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true;
      vars.servant_card_id = "pale_rider";
      NightingaleSupportService.applyHealing(nightingale, player, 150.0F);
      helper.assertTrue(Math.abs(player.getHealth() - 250.0F) < 0.01F, "Pale Rider card player did not receive exact 150 reversal damage");
      helper.succeed();
   }

   @GameTest(batch = "nightingale", template = "ancient_temple", timeoutTicks = 430)
   public static void angelCryExpiresAfterFourHundredTicks(GameTestHelper helper) {
      NightingaleEntity nightingale = nightingale(helper);
      var ally = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 8, 3));
      ally.setNoAi(true);
      long started = helper.getLevel().getGameTime();
      NightingaleSupportService.applyAngelCry(nightingale, ally, started);
      helper.assertTrue(NightingaleSupportService.hasAngelCry(ally, started), "Angel's Cry was not applied");
      helper.runAfterDelay(401, () -> {
         long now = helper.getLevel().getGameTime();
         NightingaleSupportService.tickBuffs(ally, now);
         helper.assertTrue(!NightingaleSupportService.hasAngelCry(ally, now), "Angel's Cry remained active after 400 ticks");
         helper.succeed();
      });
   }

   @GameTest(batch = "nightingale", template = "ancient_temple", timeoutTicks = 60)
   public static void safetyCircleRejectsDamageEffectsKnockbackAndInfection(GameTestHelper helper) {
      NightingaleEntity nightingale = nightingale(helper);
      NightingaleSupportService.createSafetyCircle(nightingale, helper.getLevel(), nightingale.position(), helper.getLevel().getGameTime());
      float health = nightingale.getHealth();
      nightingale.hurt(helper.getLevel().damageSources().generic(), 80.0F);
      nightingale.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
      nightingale.setDeltaMovement(Vec3.ZERO);
      nightingale.knockback(2.0, 1.0, 0.0);
      var paleRider = helper.spawn(ModEntities.PALE_RIDER.get(), new BlockPos(7, 8, 3));
      paleRider.setNoAi(true);
      PaleRiderInfectionService.infect(nightingale, paleRider, 2);
      helper.runAfterDelay(12, () -> {
         helper.assertTrue(Math.abs(nightingale.getHealth() - health) < 0.01F, "Safety circle did not cancel damage");
         helper.assertTrue(!nightingale.hasEffect(MobEffects.POISON), "Safety circle accepted a harmful effect");
         helper.assertTrue(nightingale.getDeltaMovement().horizontalDistanceSqr() < 0.0001, "Safety circle accepted knockback");
         helper.assertTrue(!nightingale.getPersistentData().contains(PaleRiderInfectionService.TAG_LEVEL), "Safety circle did not cleanse Pale Rider infection");
         helper.succeed();
      });
   }

   @GameTest(batch = "nightingale", template = "ancient_temple", timeoutTicks = 40)
   public static void noblePhantasmPenaltyDoesNotReduceOrdinaryDamage(GameTestHelper helper) {
      var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 8, 3));
      var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 8, 3));
      attacker.setNoAi(true); target.setNoAi(true);
      target.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0); target.setHealth(200.0F);
      attacker.getPersistentData().putLong(NightingaleSupportService.NP_POWER_DOWN_UNTIL, helper.getLevel().getGameTime() + 600L);
      var holder = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
         .getHolderOrThrow(ShadowHassanDamageTypes.MEDITATIVE_SENSITIVITY);
      target.hurt(new DamageSource(holder, attacker), 20.0F);
      helper.assertTrue(Math.abs(target.getHealth() - 190.0F) < 0.01F, "NP penalty did not halve Noble Phantasm damage");
      target.invulnerableTime = 0;
      target.hurt(helper.getLevel().damageSources().mobAttack(attacker), 20.0F);
      helper.assertTrue(Math.abs(target.getHealth() - 170.0F) < 0.01F, "NP penalty incorrectly reduced ordinary damage");
      helper.succeed();
   }

   @GameTest(batch = "nightingale", template = "ancient_temple", timeoutTicks = 40)
   public static void gunDeclaresFifteenBaseDamageAndFiltersOwner(GameTestHelper helper) {
      NightingaleEntity nightingale = nightingale(helper);
      NightingaleBulletEntity bullet = new NightingaleBulletEntity(helper.getLevel(), nightingale);
      helper.assertTrue(NightingaleBulletEntity.BASE_DAMAGE == 15.0F, "Nightingale gun base damage is not 15");
      helper.assertTrue(!bullet.canHitTarget(nightingale), "Nightingale bullet can hit its owner");
      helper.succeed();
   }

   @GameTest(batch = "nightingale", template = "ancient_temple", timeoutTicks = 40)
   public static void servantCardNightingaleDoesNotRequireGunAmmo(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(NightingaleGunItem.requiresAmmunition(player), "Ordinary gun user incorrectly received infinite ammunition");
      helper.assertTrue(ServantCardTransformManager.transform(player, "nightingale"), "Nightingale card transform failed");
      helper.assertTrue(!NightingaleGunItem.requiresAmmunition(player), "Nightingale card still requires bullets");
      helper.succeed();
   }

   @GameTest(batch = "nightingale", template = "ancient_temple", timeoutTicks = 40)
   public static void servantCardAppliesStatsLoadoutAndSelfNursing(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      player.getAbilities().invulnerable = false;
      player.setInvulnerable(false);
      helper.assertTrue(ServantCardTransformManager.transform(player, "nightingale"), "Nightingale card transform failed");
      helper.assertTrue(Math.abs(player.getMaxHealth() - 1000.0F) < 0.01F, "Nightingale card did not apply 1000 health");
      helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 40.0) < 0.01, "Nightingale card did not apply 40 attack");
      helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 30.0) < 0.01, "Nightingale card did not apply 30 armor");
      helper.assertTrue(player.getMainHandItem().is(ModItems.NIGHTINGALE_GUN.get()), "Nightingale gun was not equipped");

      player.setHealth(700.0F);
      player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
      helper.assertTrue(ServantCardNightingaleSkills.performSteelNursing(player), "Card Steel Nursing failed");
      helper.assertTrue(Math.abs(player.getHealth() - 850.0F) < 0.01F, "Card Steel Nursing did not heal exactly 150");
      helper.assertTrue(!player.hasEffect(MobEffects.POISON), "Card Steel Nursing did not cleanse poison");
      helper.succeed();
   }

   @GameTest(batch = "nightingale", template = "ancient_temple", timeoutTicks = 40)
   public static void cardSafetyCircleHealsAndRestoresServantCardMana(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(ServantCardTransformManager.transform(player, "nightingale"), "Nightingale card transform failed");
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      player.setHealth(600.0F);
      vars.servant_card_mana = 0.0;
      NightingaleSupportService.createSafetyCircle(player, helper.getLevel(), player.position(), helper.getLevel().getGameTime());
      helper.assertTrue(Math.abs(player.getHealth() - 800.0F) < 0.01F, "Card safety circle did not heal exactly 200");
      helper.assertTrue(Math.abs(vars.servant_card_mana - 50.0) < 0.01, "Card safety circle restored the wrong mana pool");
      helper.succeed();
   }

   private static NightingaleEntity nightingale(GameTestHelper helper) {
      NightingaleEntity nightingale = helper.spawn(ModEntities.NIGHTINGALE.get(), NIGHTINGALE_POS);
      nightingale.setNoAi(true);
      nightingale.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
      nightingale.setHealth(1000.0F);
      nightingale.setCurrentMp(0.0);
      return nightingale;
   }
}
