package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTraitService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantTrueSweepService;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;

@GameTestHolder("typemoonworld_basic_balance")
@PrefixGameTestTemplate(false)
public final class ServantBasicAttackBalanceGameTests {
   private static CuChulainnEntity cu(GameTestHelper h) {
      for (int x = 1; x <= 12; x++) for (int z = 1; z <= 12; z++) {
         h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
         for (int y = 2; y <= 8; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
      }
      var owner = ModEntities.CU_CHULAINN.get().create(h.getLevel());
      owner.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5, 2, 3))));
      owner.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(owner.blockPosition()), MobSpawnType.COMMAND, null);
      stationary(owner);
      owner.setYRot(0);
      h.getLevel().addFreshEntity(owner);
      return owner;
   }

   private static void stationary(Mob entity) {
      entity.setNoAi(true);
      entity.setNoGravity(true);
      entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
   }

   private static LivingEntity husk(GameTestHelper h, LivingEntity owner) {
      var target = EntityType.HUSK.create(h.getLevel());
      target.setPos(owner.position().add(0, 0, 2));
      stationary(target);
      target.getAttribute(Attributes.ARMOR).setBaseValue(0);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
      target.setHealth(1000);
      h.getLevel().addFreshEntity(target);
      return target;
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cuBasicCannotOneShotNativeDiarmuid(GameTestHelper h) {
      var owner = cu(h);
      var target = ModEntities.DIARMUID_UA_DUIBHNE.get().create(h.getLevel());
      target.setPos(owner.position().add(0, 0, 2));
      target.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(target.blockPosition()), MobSpawnType.COMMAND, null);
      stationary(target);
      h.getLevel().addFreshEntity(target);
      // Isolate a clean contact from reactive dodges without changing HP, armor or the damage source.
      target.getPersistentData().putLong("TypeMoonCombatSuppressedUntil", h.getLevel().getGameTime() + 100);
      float before = target.getHealth();
      double calculated = ServantCombatService.calculateDamage(owner, target, 1, false, null, 1);
      boolean hit = owner.doBasicHurtTarget(target);
      TYPE_MOON_WORLD.LOGGER.info("TMW_BASIC_BALANCE cu_vs_diarmuid attack={} sharedDamage={} before={} after={} hit={}",
         owner.getAttributeValue(Attributes.ATTACK_DAMAGE), calculated, before, target.getHealth(), hit);
      h.assertTrue(calculated <= owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.11,
         "Identity traits became implicit special attacks: " + calculated);
      h.assertTrue(hit && target.isAlive() && before - target.getHealth() > 0 && before - target.getHealth() < 30,
         "One ordinary NPC attack dealt lethal or inflated damage: " + (before - target.getHealth()));
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 50)
   public static void playerSweepUsesOrdinaryArmorAndResetsCharge(GameTestHelper h) {
      var anchor = cu(h);
      Vec3 pos = anchor.position();
      anchor.discard();
      var player = h.makeMockServerPlayerInLevel();
      player.teleportTo(pos.x, pos.y, pos.z);
      player.setYRot(0);
      player.setXRot(0);
      h.assertTrue(ServantCardTransformManager.transform(player, "cu_chulainn"), "Cu transformation rejected");
      var target = EntityType.WARDEN.create(h.getLevel());
      target.setPos(pos.add(0, 0, 2));
      stationary(target);
      // Vanilla wardens have no armor; add armor to exercise the reduction path.
      target.getAttribute(Attributes.ARMOR).setBaseValue(20);
      h.getLevel().addFreshEntity(target);
      h.runAfterDelay(22, () -> {
         for (int tick = 0; tick < 20; tick++) player.doTick();
         h.assertTrue(player.getAttackStrengthScale(0) == 1, "Test player never charged its ordinary attack");
         float before = target.getHealth();
         target.hurt(player.damageSources().playerAttack(player), (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE));
         float ordinary = before - target.getHealth();
         target.setHealth(before);
         target.invulnerableTime = 0;
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         h.assertTrue(ServantTrueSweepService.triggerPlayerAttack(player, vars), "Charged sweep rejected");
         float sweep = before - target.getHealth();
         TYPE_MOON_WORLD.LOGGER.info("TMW_BASIC_BALANCE player_vs_warden attack={} ordinary={} sweep={} chargeAfter={}",
            player.getAttributeValue(Attributes.ATTACK_DAMAGE), ordinary, sweep, player.getAttackStrengthScale(0));
         h.assertTrue(Math.abs(sweep - ordinary) < .05, "Sweep bypassed ordinary armor: normal=" + ordinary + " sweep=" + sweep);
         h.assertTrue(player.getAttackStrengthScale(0) == 0, "Sweep did not consume the player's attack charge");
         h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void sweepPreservesExistingInvulnerability(GameTestHelper h) {
      var owner = cu(h);
      var target = husk(h, owner);
      target.hurt(target.damageSources().generic(), 100);
      float before = target.getHealth();
      int invulnerability = target.invulnerableTime;
      owner.doBasicHurtTarget(target);
      h.assertTrue(target.getHealth() == before, "Ordinary sweep erased another hit's invulnerability");
      h.assertTrue(target.invulnerableTime == invulnerability, "Sweep changed existing invulnerability");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void blockedSweepDoesNotRetryAsAnOrdinaryAttack(GameTestHelper h) {
      var owner = cu(h);
      var target = husk(h, owner);
      AtomicInteger attempts = new AtomicInteger();
      Consumer<LivingIncomingDamageEvent> cancel = event -> {
         if (event.getEntity() == target && event.getSource().getEntity() == owner) {
            attempts.incrementAndGet();
            event.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(cancel);
      try {
         long now = h.getLevel().getGameTime();
         owner.doBasicHurtTarget(target);
         owner.doBasicHurtTarget(target);
         ServantTrueSweepService.tryNpcSweep(owner, target, now);
         h.assertTrue(attempts.get() == 1, "Blocked/cooldown sweep retried damage " + attempts.get() + " times");
         h.assertTrue(target.getHealth() == 1000, "Cancelled hit dealt damage");
      } finally {
         NeoForge.EVENT_BUS.unregister(cancel);
      }
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 35)
   public static void repeatedInputDoesNotPostponePlayerSweepRecovery(GameTestHelper h) {
      var anchor = cu(h);
      Vec3 pos = anchor.position();
      anchor.discard();
      var player = h.makeMockServerPlayerInLevel();
      player.teleportTo(pos.x, pos.y, pos.z);
      player.setYRot(0);
      h.assertTrue(ServantCardTransformManager.transform(player, "cu_chulainn"), "Cu transformation rejected");
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      h.assertTrue(ServantTrueSweepService.triggerPlayerAttack(player, vars), "First sweep rejected");
      int interval = ServantTrueSweepService.intervalTicks(player.getAttributeValue(Attributes.ATTACK_SPEED));
      for (int tick = 1; tick < interval; tick++) h.runAfterDelay(tick, () ->
         h.assertTrue(!ServantTrueSweepService.triggerPlayerAttack(player, vars), "Cooldown accepted a sweep"));
      h.runAfterDelay(interval, () -> {
         h.assertTrue(ServantTrueSweepService.triggerPlayerAttack(player, vars), "Rejected inputs postponed the attack window");
         h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void sweepHitsFrontAndSideOnceAndKeepsTheNormalOrigin(GameTestHelper h) {
      var owner = cu(h);
      var front = husk(h, owner);
      var side = husk(h, owner);
      side.setPos(owner.position().add(2, 0, 0));
      var behind = husk(h, owner);
      behind.setPos(owner.position().add(0, 0, -2));
      h.assertTrue(owner.doBasicHurtTarget(front), "Front attack missed");
      float damage = 1000 - front.getHealth();
      h.assertTrue(damage > 0 && Math.abs(damage - owner.getAttributeValue(Attributes.ATTACK_DAMAGE)) < .05,
         "Sweep did not use one ordinary hit: " + damage);
      h.assertTrue(Math.abs(side.getHealth() - front.getHealth()) < .05, "Side target received different damage");
      h.assertTrue(behind.getHealth() == 1000, "Sweep hit behind its arc");
      h.assertTrue(front.invulnerableTime > 0, "Sweep erased its own invulnerability");
      owner.doBasicHurtTarget(front);
      h.assertTrue(front.getHealth() == 1000 - damage && side.getHealth() == 1000 - damage,
         "The same basic attack resolved twice");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 45)
   public static void fourFastSweepsCannotStackFourFullHitsOnAWarden(GameTestHelper h) {
      var anchor = cu(h);
      Vec3 pos = anchor.position();
      anchor.discard();
      var player = h.makeMockServerPlayerInLevel();
      player.teleportTo(pos.x, pos.y, pos.z);
      player.setYRot(0);
      player.setXRot(0);
      h.assertTrue(ServantCardTransformManager.transform(player, "cu_chulainn"), "Cu transformation rejected");
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var target = EntityType.WARDEN.create(h.getLevel());
      target.setPos(pos.add(0, 0, 2));
      stationary(target);
      h.getLevel().addFreshEntity(target);
      float before = target.getHealth();
      for (int tick = 0; tick < 20; tick++) player.doTick();
      double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      for (int tick : new int[] {1, 6, 11, 16}) h.runAfterDelay(tick, () -> {
         // Keep every attempted strike fully charged to isolate victim invulnerability.
         for (int charge = 0; charge < 20; charge++) player.doTick();
         target.setPos(pos.add(0, 0, 2));
         target.setDeltaMovement(Vec3.ZERO);
         ServantTrueSweepService.triggerPlayerAttack(player, vars);
      });
      h.runAfterDelay(18, () -> {
         float damage = before - target.getHealth();
         TYPE_MOON_WORLD.LOGGER.info("TMW_BASIC_BALANCE four_sweeps_warden attack={} totalDamage={}", attack, damage);
         h.assertTrue(damage > 0 && damage <= attack * 2 + .05, "Four quick inputs stacked full hits: " + damage);
         h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void everyBuiltinNpcRequiresExplicitSpecialAttackConditions(GameTestHelper h) {
      var anchor = cu(h);
      var target = husk(h, anchor);
      anchor.discard();
      for (ServantTraitTag trait : ServantTraitTag.values()) target.getPersistentData().putBoolean("TypeMoonTrait_" + trait.key(), true);
      int checked = 0;
      for (var definition : ServantDataRegistry.getAll().values()) {
         ResourceLocation id = definition.id().contains(":") ? ResourceLocation.parse(definition.id())
            : ResourceLocation.fromNamespaceAndPath("typemoonworld", definition.id());
         var owner = BuiltinServantEntityFactory.create(h.getLevel(), id);
         h.assertTrue(owner != null, "Missing NPC " + id);
         owner.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(20);
         target.getPersistentData().putString("ServantCardFaction", definition.faction().key());
         double ordinary = ServantCombatService.calculateDamage(owner, target, 1, false, null, 1);
         double explicit = ServantCombatService.calculateDamage(owner, target, 1, false, List.of(ServantTraitTag.MALE), 1);
         h.assertTrue(Math.abs(ordinary - 20) < .001, id + " inferred a special attack from identity: " + ordinary);
         h.assertTrue(Math.abs(explicit - 30) < .001 || "oda_nobunaga".equals(definition.id()),
            id + " lost explicitly supplied special attack conditions");
         checked++;
      }
      TYPE_MOON_WORLD.LOGGER.info("TMW_TRAIT_AUDIT npcDefinitions={} implicitBonus=false explicitConditions=preserved", checked);
      h.assertTrue(checked >= 29, "Builtin definitions were not loaded");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void everyPlayerCardKeepsIdentityOutOfOutgoingAndSharedDamage(GameTestHelper h) {
      var anchor = cu(h);
      var target = husk(h, anchor);
      anchor.discard();
      for (ServantTraitTag trait : ServantTraitTag.values()) target.getPersistentData().putBoolean("TypeMoonTrait_" + trait.key(), true);
      var player = h.makeMockServerPlayerInLevel();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(20);
      int checked = 0;
      try {
         // Set only identity, isolating it from separately defined active/passive buffs.
         vars.servant_card_transformed = true;
         for (var definition : ServantDataRegistry.getAll().values()) {
            vars.servant_card_id = definition.id();
            target.getPersistentData().putString("ServantCardFaction", definition.faction().key());
            double outgoing = ServantCardTraitService.applyOutgoingDamage(player, target, 20);
            double shared = ServantCombatService.calculateDamage(player, target, 1, false, null, 1);
            double explicit = ServantCombatService.calculateDamage(player, target, 1, false, List.of(ServantTraitTag.MALE), 1);
            h.assertTrue(Math.abs(outgoing - 20) < .001, definition.id() + " added a hidden player-card special attack: " + outgoing);
            h.assertTrue(Math.abs(shared - 20) < .001, definition.id() + " shared damage used identity as special attack: " + shared);
            h.assertTrue(Math.abs(explicit - 30) < .001 || "oda_nobunaga".equals(definition.id()),
               definition.id() + " lost explicitly supplied player special attack conditions");
            checked++;
         }
      } finally {
         vars.servant_card_transformed = false;
      }
      TYPE_MOON_WORLD.LOGGER.info("TMW_TRAIT_AUDIT playerDefinitions={} implicitBonus=false explicitConditions=preserved", checked);
      h.assertTrue(checked >= 29, "Player-card definitions were not loaded");
      h.succeed();
   }
}
