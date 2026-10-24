package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.DefenseRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantDefenseTraits;

@GameTestHolder("typemoonworld_guard")
@PrefixGameTestTemplate(false)
public final class ServantCardDefenseGameTests {
   private ServantCardDefenseGameTests() { }

   private static ServerPlayer card(GameTestHelper h) {
      var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
         new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "guard-test"), false);
      var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
      var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
      new io.netty.channel.embedded.EmbeddedChannel(connection);
      h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
      player.setGameMode(GameType.SURVIVAL);
      player.setPos(h.absolutePos(new BlockPos(4, 3, 4)).getCenter());
      // Vanilla gives new server players sixty ticks of spawn protection.
      // The standard GameTest mock also hardcodes creative mode, so use a real player.
      for (int tick = 0; tick < 60; tick++) player.tick();
      h.assertTrue(ServantCardTransformManager.transform(player, "oda_nobunaga"), "card transform failed");
      player.setNoGravity(true);
      player.getAttribute(Attributes.ARMOR).setBaseValue(10);
      player.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
      ServantCombatDefenseService.initialize(player);
      player.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES, 0);
      player.getPersistentData().putLong(ServantCombatDefenseService.AUTO_RESTORE, Long.MAX_VALUE);
      return player;
   }

   private static LivingEntity attacker(GameTestHelper h) {
      var attacker = h.spawn(EntityType.HUSK, new BlockPos(4, 3, 6));
      attacker.setNoAi(true);
      attacker.setNoGravity(true);
      return attacker;
   }

   private static float afterArmor(ServerPlayer player, DamageSource source, float damage) {
      return CombatRules.getDamageAfterAbsorb(player, damage, source,
         (float)player.getAttributeValue(Attributes.ARMOR), (float)player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
   }

   private static void close(GameTestHelper h, double expected, double actual, String message) {
      h.assertTrue(Math.abs(expected - actual) < 0.002, message + ": expected=" + expected + ", actual=" + actual);
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void passiveGuardSpendsOnlyPostArmorDamageDividedByFive(GameTestHelper h) {
      var player = card(h); var attacker = attacker(h); var source = attacker.damageSources().mobAttack(attacker);
      double stamina = player.getPersistentData().getDouble("ServantCardCombatStamina");
      float health = player.getHealth(), damage = afterArmor(player, source, 50);
      player.hurt(source, 50);
      close(h, damage * 0.5, health - player.getHealth(), "passive guard damage was not settled immediately after armor");
      close(h, DefenseRules.guardCost(damage, ServantDefenseTraits.resolve(player).guardBonus(), false, false),
         stamina - player.getPersistentData().getDouble("ServantCardCombatStamina"), "passive guard used raw damage or a flat cost");
      player.discard(); attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void activeGuardReducesPostArmorDamageWithoutReducingItsOwnCost(GameTestHelper h) {
      var player = card(h); var attacker = attacker(h); var source = attacker.damageSources().mobAttack(attacker);
      h.assertTrue(ServantCombatDefenseService.setGuarding(player, true), "guard rejected");
      player.getPersistentData().putLong(ServantCombatDefenseService.TAG_GUARD_START, h.getLevel().getGameTime() - 20);
      double stamina = player.getPersistentData().getDouble("ServantCardCombatStamina");
      float health = player.getHealth(), damage = afterArmor(player, source, 50);
      player.hurt(source, 50);
      close(h, damage * 0.2, health - player.getHealth(), "active guard did not reduce damage immediately");
      close(h, DefenseRules.guardCost(damage, ServantDefenseTraits.resolve(player).guardBonus(), true, false),
         stamina - player.getPersistentData().getDouble("ServantCardCombatStamina"), "active guard charged raw or already guarded damage");
      player.discard(); attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void exhaustedPoiseUsesTheSamePostArmorDamageBeforeAbsorption(GameTestHelper h) {
      var player = card(h); var attacker = attacker(h); var source = attacker.damageSources().mobAttack(attacker);
      player.getPersistentData().putDouble("ServantCardCombatStamina", 0);
      player.setAbsorptionAmount(2);
      double poise = player.getPersistentData().getDouble("ServantCardCombatPoise");
      float damage = afterArmor(player, source, 50);
      player.hurt(source, 50);
      close(h, damage / 5.0, poise - player.getPersistentData().getDouble("ServantCardCombatPoise"),
         "poise used raw damage, minimum loss, or a different basis from guard");
      player.discard(); attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void baseDefenseAbsorbedHitsNeverTriggerGuardOrConsumeEvasion(GameTestHelper h) {
      var player = card(h); var attacker = attacker(h);
      h.assertTrue(ServantCombatDefenseService.setGuarding(player, true), "guard rejected");
      player.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES, 1);
      double stamina = player.getPersistentData().getDouble("ServantCardCombatStamina");
      float health = player.getHealth();
      Consumer<LivingIncomingDamageEvent> armor = event -> {
         if (event.getEntity() == player) event.addReductionModifier(DamageContainer.Reduction.ARMOR,
            (container, reduction) -> container.getNewDamage());
      };
      NeoForge.EVENT_BUS.addListener(armor);
      try { player.hurt(attacker.damageSources().mobAttack(attacker), 50); }
      finally { NeoForge.EVENT_BUS.unregister(armor); }
      close(h, health, player.getHealth(), "base defense failed to absorb hit");
      close(h, stamina, player.getPersistentData().getDouble("ServantCardCombatStamina"), "absorbed hit spent guard");
      h.assertTrue(player.getPersistentData().getInt("TypeMoonDefenseGuardCount") == 0, "absorbed hit triggered a guard");
      h.assertTrue(player.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES) == 1, "absorbed hit spent evasion");
      player.discard(); attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void stigmaIsSuppressedThroughoutCardDamageSettlement(GameTestHelper h) {
      var player = card(h); var attacker = attacker(h); var source = attacker.damageSources().mobAttack(attacker);
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).learned_magics.add("stigma");
      float health = player.getHealth(), damage = afterArmor(player, source, 50);
      player.hurt(source, 50);
      float settledHealth = player.getHealth();
      close(h, damage * 0.5, health - settledHealth, "stigma deferred servant damage");
      h.runAfterDelay(25, () -> {
         close(h, settledHealth, player.getHealth(), "guarded damage was applied again later");
         player.discard(); attacker.discard(); h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void releaseCooldownLastsTwentyTicksAndRepeatedReleaseDoesNotExtendIt(GameTestHelper h) {
      var player = card(h);
      h.assertTrue(ServantCombatDefenseService.setGuarding(player, true), "guard rejected");
      ServantCombatDefenseService.setGuarding(player, false);
      long cooldown = player.getPersistentData().getLong(ServantCombatDefenseService.TAG_GUARD_COOLDOWN);
      h.assertTrue(cooldown == h.getLevel().getGameTime() + 20, "release cooldown is not one second");
      h.assertTrue(!ServantCombatDefenseService.setGuarding(player, true), "rapid tap restarted guard");
      h.runAfterDelay(19, () -> {
         ServantCombatDefenseService.setGuarding(player, false);
         h.assertTrue(player.getPersistentData().getLong(ServantCombatDefenseService.TAG_GUARD_COOLDOWN) == cooldown,
            "repeated release extended cooldown");
         h.assertTrue(!ServantCombatDefenseService.setGuarding(player, true), "cooldown ended before twenty ticks");
      });
      h.runAfterDelay(20, () -> {
         h.assertTrue(ServantCombatDefenseService.setGuarding(player, true), "guard failed to recover at twenty ticks");
         player.discard(); h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void halfSecondPerfectWindowAndImmediatePerfectRestart(GameTestHelper h) {
      var player = card(h); var attacker = attacker(h); var source = attacker.damageSources().mobAttack(attacker);
      h.assertTrue(ServantCombatDefenseService.setGuarding(player, true), "guard rejected");
      var params = ServantDefenseTraits.definition(player).parameters();
      long now = h.getLevel().getGameTime();
      player.getPersistentData().putLong(ServantCombatDefenseService.TAG_GUARD_START,
         now - ServantCombatFormulas.parryWindowTicks(params));
      double stamina = player.getPersistentData().getDouble("ServantCardCombatStamina");
      float health = player.getHealth();
      player.hurt(source, 50);
      close(h, health, player.getHealth(), "last tick of widened perfect window failed");
      close(h, stamina, player.getPersistentData().getDouble("ServantCardCombatStamina"), "perfect guard spent stamina");
      h.assertTrue(player.getPersistentData().getInt("TypeMoonDefenseParryCount") == 1, "perfect guard not recorded");
      ServantCombatDefenseService.setGuarding(player, false);
       h.assertTrue(ServantCombatDefenseService.setGuarding(player, true), "perfect guard inherited a release cooldown");
      player.invulnerableTime = 0;
      player.getPersistentData().putLong(ServantCombatDefenseService.TAG_GUARD_START,
         h.getLevel().getGameTime() - ServantCombatFormulas.parryWindowTicks(params) - 1);
      player.hurt(source, 50);
      h.assertTrue(player.getHealth() < health, "outside-window hit also received a perfect guard");
      h.assertTrue(player.getPersistentData().getInt("TypeMoonDefenseParryCount") == 1, "window extended beyond its boundary");
      ServantCombatDefenseService.setGuarding(player, false);
      player.discard(); attacker.discard(); h.succeed();
   }
}
