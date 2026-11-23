package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;
import net.xxxjk.typemoonworld.api.MagecraftDamageSource;
import net.xxxjk.typemoonworld.api.MagicComplexity;

@Mod(value = "typemoonworld_magecraft_smoke", dist = Dist.CLIENT)
public final class MagecraftClientSmoke {
   private static final String WORLD = "Magecraft-QA";
   private boolean opened, submitted, done;
   private int ticks;
   private volatile boolean passed;
   private volatile Throwable failure;

   public MagecraftClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (failure != null) throw new AssertionError("integrated server checks failed", failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true; mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(3); mc.options.simulationDistance().set(4);
            if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile())
               mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA save failed"); });
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,
               new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
               new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                  .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         if (++ticks > 400) throw new AssertionError("integrated server checks timed out");
         if (ticks >= 20 && !submitted) {
            submitted = true;
            mc.getSingleplayerServer().execute(() -> {
               try {
                  var server = mc.getSingleplayerServer(); var level = server.overworld();
                  level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                  var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                  var target = EntityType.SHEEP.create(level);
                  target.setNoAi(true); target.setNoGravity(true); target.setPos(player.position().add(4, 1, 0));
                  target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
                  var vars = target.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                  vars.is_magus = false; vars.magicState().setMaxMana(0);
                  level.addFreshEntity(target);
                  MagicResistanceHelper.setMagicResistance(target, MagicResistanceRank.B, 0.35F, 0.55F);
                  target.hurt(target.damageSources().magic(), 10);
                  close(target.getHealth(), 190, "ordinary magic");
                  target.invulnerableTime = 0;
                  target.hurt(MagecraftDamageSource.of(target.damageSources().magic(), "qa_spell", MagicComplexity.HIGH_THAUMATURGY, player), 10);
                  close(target.getHealth(), 183.5F, "one magecraft reduction");
                  target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
                  require(target.getEffect(MobEffects.WEAKNESS).getDuration() == 200, "ordinary potion changed");
                  require(ServantCardTransformManager.transform(player, "artoria_pendragon"), "card transform failed");
                  require(MagicResistanceHelper.getMagicResistanceRank(player) == MagicResistanceRank.A, "card rank lost");
                  close(MagicResistanceHelper.applyMagicDamageReduction(player,
                     MagecraftDamageSource.of(target.damageSources().magic(), "qa_spell", MagicComplexity.HIGH_THAUMATURGY, target), 10), 0, "card immunity");
                  close(MagicResistanceHelper.applyMagicDamageReduction(player, target.damageSources().magic(), 10), 10, "card ordinary magic");
                  target.discard(); passed = true;
               } catch (Throwable error) { failure = error; }
            });
         }
         if (passed && ticks >= 60) {
            TYPE_MOON_WORLD.LOGGER.info("MAGECRAFT_CLIENT_SMOKE_PASS ordinary_magic=true single_reduction=true potion=true card_rank_a=true");
            done = true; mc.stop();
         }
      } catch (Throwable error) {
         done = true; TYPE_MOON_WORLD.LOGGER.error("MAGECRAFT_CLIENT_SMOKE_FAIL", error); mc.stop();
      }
   }

   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
   private static void close(float actual, float expected, String message) { require(Math.abs(actual - expected) < 0.001F, message + ": " + actual); }
}
