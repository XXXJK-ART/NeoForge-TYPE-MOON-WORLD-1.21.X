package net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ShadowHassanResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java");

   @Test
   void definitionHasExactParameters() throws IOException {
      JsonObject definition = json("data/typemoonworld/servant/definitions/shadow_hassan.json");
      JsonObject params = definition.getAsJsonObject("parameters");
      assertEquals("D", params.get("strength").getAsString());
      assertEquals("C", params.get("endurance").getAsString());
      assertEquals("C", params.get("agility").getAsString());
      assertEquals("D", params.get("magic").getAsString());
      assertEquals("E", params.get("luck").getAsString());
      assertEquals("meditative_sensitivity", definition.get("noble_phantasm").getAsString());
      assertEquals("shadow_hassan_ai", definition.get("ai_config").getAsString());
      assertEquals("ancient", definition.get("era").getAsString());
      assertEquals("human", definition.get("faction").getAsString());
      assertEquals("EX", params.get("noble_phantasm_rank").getAsString());
      var traits = definition.getAsJsonArray("traits").toString();
      for (String trait : List.of("servant", "humanoid", "assassin", "male", "lawful", "evil",
         "human_kind", "seven_knights", "ea_special")) assertTrue(traits.contains("\"" + trait + "\""), trait);
      for (String removed : List.of("conceptual_existence", "shadow_servant", "true_assassin", "old_man_of_the_mountain")) {
         assertFalse(traits.contains("\"" + removed + "\""), removed);
      }
      String actions = definition.getAsJsonObject("specialization").getAsJsonArray("combat_actions").toString();
      assertTrue(actions.contains("shadow_ambush"));
      assertTrue(actions.contains("shadow_flurry"));
      assertTrue(actions.contains("shadow_bind"));
      assertEquals("\u5e7d\u5f0b\u4e4b\u54c8\u6851", definition.get("display_name_zh").getAsString());
   }

   @Test
   void allRegistriesAndDataResourcesArePresent() throws IOException {
      String entities = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/init/ModEntities.java"));
      String items = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/item/ModItems.java"));
      assertTrue(entities.contains("\"shadow_hassan\""));
      assertTrue(entities.contains("\"shadow_hassan_death_shadow\""));
      assertTrue(items.contains("SHADOW_HASSAN_MASK"));
      assertTrue(items.contains("SERVANT_CARD_SHADOW_HASSAN"));
      assertTrue(items.contains("\"shadow_hassan_spawn_egg\""));

      for (String path : List.of(
         "data/typemoonworld/servant/ai/shadow_hassan_ai.json",
         "data/typemoonworld/servant/skills/presence_concealment_ex_shadow_hassan.json",
         "data/typemoonworld/servant/skills/shadow_lantern_a.json",
         "data/typemoonworld/servant/skills/shadow_wandering_a.json",
         "data/typemoonworld/servant/actions/shadow_hassan.json",
         "data/typemoonworld/damage_type/shadow_penetration.json",
         "data/typemoonworld/tags/damage_type/shadow_penetration.json",
         "data/typemoonworld/servant/noble_phantasms/meditative_sensitivity.json",
         "data/typemoonworld/damage_type/meditative_sensitivity.json",
         "data/typemoonworld/tags/entity_type/shadow_hassan_blade_immune.json",
         "data/typemoonworld/tags/mob_effect/presence_concealment_revealing.json",
         "assets/typemoonworld/models/item/shadow_hassan_mask.json",
         "assets/typemoonworld/models/item/shadow_hassan_spawn_egg.json",
         "assets/typemoonworld/models/item/servant_card_shadow_hassan.json",
         "assets/typemoonworld/models/item/servant_card_shadow_hassan_head.json",
         "assets/typemoonworld/textures/item/card_faces_3d/servant/shadow_hassan_card.png",
         "assets/typemoonworld/geo/servant_card_shadow_hassan.geo.json",
         "assets/typemoonworld/animations/servant_card_shadow_hassan.animation.json",
         "assets/typemoonworld/textures/models/armor/servant_card_shadow_hassan.png",
         "assets/typemoonworld/textures/item/servant_card_armor/shadow_hassan_mask.png"
      )) assertTrue(Files.isRegularFile(RESOURCES.resolve(path)), path);
   }

   @Test
   void servantCardUsesPlannedSlotsAndOptimizedTexture() throws IOException {
      String layout = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardSkillLayout.java"));
      String shadowLayout = layout.substring(layout.indexOf("case \"shadow_hassan\""), layout.indexOf("case \"emiya_archer\""));
      for (int slot : List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)) assertTrue(shadowLayout.contains("case " + slot + " ->"), "slot " + slot);
      var concealment = net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout.actionFor("shadow_hassan", 0, false);
      var lantern = net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout.actionFor("shadow_hassan", 1, false);
      var wandering = net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout.actionFor("shadow_hassan", 2, false);
      var possession = net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout.actionFor("shadow_hassan", 8, false);
      assertEquals("shadow_hassan_concealment", concealment.effectId());
      assertEquals(20.0, concealment.mpCost());
      assertEquals(200, concealment.cooldownTicks());
      assertEquals(100L, net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardShadowHassanSkills.ACTIVE_CONCEALMENT_DURATION_TICKS);
      assertEquals("shadow_hassan_lantern", lantern.effectId());
      assertEquals("shadow_hassan_wandering", wandering.effectId());
      assertEquals(15.0, wandering.mpCost());
      assertEquals(100, wandering.cooldownTicks());
      assertEquals("shadow_hassan_possession", possession.effectId());
      assertEquals(0.0, possession.mpCost());
      assertEquals(0, possession.cooldownTicks());

      String skills = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardShadowHassanSkills.java"));
      assertTrue(skills.contains("ShadowHassanCardConcealed"));
      assertTrue(skills.contains("performActiveConcealment"));
      assertTrue(skills.contains("ACTIVE_CONCEALMENT_DURATION_TICKS = 100L"));
      assertTrue(skills.contains("ACTIVE_CONCEALMENT_MANA_COST = 20"));
      assertTrue(skills.contains("ACTIVE_CONCEALMENT_COOLDOWN_TICKS = 200"));
      assertTrue(skills.contains("clearEnemyAggroForMaskOnly"));
      assertTrue(skills.contains("ShadowHassanCardBodyInvisibilityManaged"));
      assertTrue(skills.contains("COMPLETE_CONCEALMENT_AMPLIFIER"));
      assertTrue(skills.contains("revealForAttack"));
      assertTrue(skills.contains("performMeditativeSensitivity"));
      assertTrue(skills.contains("getRayTraceTarget(player, 32.0)"));
      assertTrue(skills.contains("hasMeditativeSensitivityTarget"));
      assertTrue(skills.contains("onPlayerDeath"));
      assertTrue(skills.contains("Player.PERSISTED_NBT_TAG"));
      assertTrue(skills.contains("TAG_POSSESSION_HOST"));
      assertTrue(skills.contains("TAG_POSSESSION_PREVIOUS_NO_GRAVITY"));
      assertTrue(skills.contains("possessionManaAfterInterval"));
      assertTrue(skills.contains("SHADOW_STEP_MAX_DISTANCE"));

      var actions = json("data/typemoonworld/servant/actions/shadow_hassan.json").getAsJsonArray("actions");
      assertEquals(12.0, actions.get(0).getAsJsonObject().get("maximum_range").getAsDouble());
      assertEquals(15.0, actions.get(0).getAsJsonObject().get("mana_cost").getAsDouble());
      assertEquals(18.0, actions.get(1).getAsJsonObject().get("maximum_range").getAsDouble());
      assertEquals(20.0, actions.get(2).getAsJsonObject().get("mana_cost").getAsDouble());
      assertEquals(18.0, actions.get(3).getAsJsonObject().get("mana_cost").getAsDouble());
      assertEquals(24.0, actions.get(4).getAsJsonObject().get("mana_cost").getAsDouble());
      assertEquals(30.0, actions.get(6).getAsJsonObject().get("mana_cost").getAsDouble());
      assertEquals(100, actions.get(6).getAsJsonObject().getAsJsonObject("timing").get("active").getAsInt());

      String transformManager = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardTransformManager.java"));
      assertTrue(transformManager.contains("ManaSnapshot manaBeforeAction"));
      assertTrue(transformManager.contains("ServantCardManaService.restore(player, vars, manaBeforeAction)"));
      assertTrue(transformManager.contains("ServantCardShadowHassanSkills.hasMeditativeSensitivityTarget(player)"));

      Path texture = RESOURCES.resolve("assets/typemoonworld/textures/item/card_faces_3d/servant/shadow_hassan_card.png");
      var image = ImageIO.read(texture.toFile());
      assertNotNull(image);
      assertEquals(292, image.getWidth());
      assertEquals(500, image.getHeight());
      assertTrue(Files.size(texture) < 200_000L);
      assertEquals("typemoonworld:item/card_faces_3d/servant/shadow_hassan_card",
         json("assets/typemoonworld/models/item/servant_card_shadow_hassan.json")
            .getAsJsonObject("textures").get("front").getAsString());
   }

   @Test
   void copiedMaskUsesIndependentResourcePaths() throws IOException {
      var armor = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/models/armor/servant_card_shadow_hassan.png").toFile());
      var icon = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/servant_card_armor/shadow_hassan_mask.png").toFile());
      assertNotNull(armor);
      assertNotNull(icon);
      assertEquals(128, armor.getWidth());
      assertEquals(128, armor.getHeight());
      assertTrue(json("assets/typemoonworld/models/item/shadow_hassan_mask.json")
         .getAsJsonObject("textures").get("layer0").getAsString().endsWith("shadow_hassan_mask"));
   }

   @Test
   void conceptDeathBypassesAllDefenseTags() throws IOException {
      for (String tag : List.of("armor", "shield", "resistance", "invulnerability", "enchantments", "effects", "cooldown")) {
         String content = Files.readString(RESOURCES.resolve("data/minecraft/tags/damage_type/bypasses_" + tag + ".json"));
         assertTrue(content.contains("typemoonworld:meditative_sensitivity"), tag);
      }
   }

   @Test
   void concealmentRendererSkipsEveryLayer() throws IOException {
      String renderer = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/client/renderer/ShadowHassanRenderer.java"));
      assertTrue(renderer.contains("if (entity.isPresenceConcealed()) return;"));
      assertTrue(renderer.contains("textures/entity/empty.png"));
      String entity = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/entity/ShadowHassanEntity.java"));
      assertTrue(entity.contains("builder.define(PRESENCE_CONCEALED, false)"));
      assertTrue(entity.contains("this.setPresenceConcealed(shadowed && !exposed)"));
      assertTrue(entity.contains("ShadowHassanRules.isUsableShadowZone"));
      assertTrue(entity.contains("tryStartPossession"));
      assertFalse(entity.contains("this.setPresenceConcealed(true)"));
      assertTrue(entity.contains("this.setInvisible(concealed)"));
      assertTrue(entity.contains("this.setSilent(concealed)"));
      assertTrue(entity.contains("false, false, false"));
      assertTrue(entity.contains("this.setGlowingTag(false)"));
      assertTrue(entity.contains("ShadowHassanRules.REVEALING_EFFECTS"));
      assertTrue(entity.contains("this.tickCount % 20 == 0"));
      assertTrue(entity.contains("this.setCustomNameVisible(!concealed)"));
      assertTrue(entity.contains("return this.isPresenceConcealed() || super.isInvisibleTo(player)"));
      assertTrue(entity.contains("return !this.isPresenceConcealed() && super.isCustomNameVisible()"));
      assertTrue(entity.contains("return !this.isPresenceConcealed() && super.isCurrentlyGlowing()"));
      assertTrue(entity.contains("source.is(DamageTypes.GENERIC_KILL)"));
      assertFalse(entity.contains("source.getDirectEntity() != attacker"));
      String events = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/event/ShadowHassanEvents.java"));
      assertTrue(events.contains("MobEffectEvent.Applicable.Result.DO_NOT_APPLY"));
      assertTrue(events.contains("ShadowHassanRules.REVEALING_EFFECTS"));
      assertTrue(events.contains("removePursuitsForTarget"));
      String pursuit = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/shadowhassan/ShadowHassanPursuitData.java"));
      assertTrue(pursuit.contains("if (target != null && !target.isAlive())"));
      assertTrue(pursuit.contains("discardProxy(level, pursuit)"));
      assertTrue(pursuit.contains("for (Pursuit pursuit : List.copyOf(this.pursuits.values()))"));
      int contactBranch = pursuit.indexOf("distance <= ShadowHassanRules.DEATH_SHADOW_CONTACT_DISTANCE");
      int removalBeforeKill = pursuit.indexOf("this.pursuits.remove(pursuit.id, pursuit);", contactBranch);
      int lethalHit = pursuit.indexOf("killTarget(level, target);", contactBranch);
      assertTrue(contactBranch >= 0 && removalBeforeKill > contactBranch && lethalHit > removalBeforeKill);
      String clientEvents = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/client/ShadowHassanClientEvents.java"));
      assertTrue(clientEvents.contains("RenderLivingEvent.Pre<?, ?>"));
      assertTrue(clientEvents.contains("event.setCanceled(true)"));
      assertTrue(clientEvents.contains("event.setCanRender(TriState.FALSE)"));
      assertTrue(clientEvents.contains("RenderGuiEvent.Pre"));
      assertTrue(clientEvents.contains("BlockHitResult.miss"));
      assertTrue(clientEvents.contains("minecraft.crosshairPickEntity = null"));
      assertTrue(clientEvents.contains("restoreConcealedHudTargetBeforeTick"));
      String playerRendererEvents = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/client/ServantCardClientEvents.java"));
      assertTrue(playerRendererEvents.contains("hideBaseModel"));
      assertTrue(playerRendererEvents.contains("onRenderPlayerPost"));
      String firstPersonItem = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/mixin/client/ItemInHandRendererMixin.java"));
      assertTrue(firstPersonItem.contains("isShadowHassanMaskOnly(player)"));
      String firstPersonArm = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/mixin/client/PlayerRendererMixin.java"));
      assertTrue(firstPersonArm.contains("isShadowHassanMaskOnly(player)"));
      String concealmentClient = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/client/ServantCardConcealmentClient.java"));
      assertTrue(concealmentClient.contains("isShadowHassanMaskOnly"));
      assertTrue(concealmentClient.contains("isShadowHassanStealthActive"));
      String armorRenderer = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/client/renderer/ServantCardArmorRenderer.java"));
      assertTrue(armorRenderer.contains("isShadowHassanStealthActive(player)"));
      String concealmentMessage = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/network/ConcealmentStateMessage.java"));
      assertTrue(concealmentMessage.contains("boolean concealed, boolean perfect"));
      String concealmentRules = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/concealment/ServantConcealment.java"));
      assertTrue(concealmentRules.contains("ConcealmentRank.A_PLUS"));
      assertTrue(concealmentRules.contains("isPassiveConcealed"));
   }

   @Test
   void combatHelperProvidesAmbushFlurryAndBinding() throws IOException {
      String combat = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/shadowhassan/ShadowHassanCombatHelper.java"));
      assertTrue(combat.contains("SLASH_STRIKE_COUNT = 200"));
      assertTrue(combat.contains("SLASH_DURATION_TICKS = 100L"));
      assertTrue(combat.contains("isFullyExposed()"));
      assertTrue(combat.contains("isNpcAssassinationTarget"));
      assertTrue(combat.contains("if (ambush)"));
      assertTrue(combat.contains("sequence % 3 == 0"));
      assertTrue(combat.contains("queueStrike(hassan, target, 4"));
      assertTrue(combat.contains("queueStrike(hassan, target, 8"));
      assertTrue(combat.contains("sequence % 5 == 0"));
      assertTrue(combat.contains("MobEffects.DARKNESS"));
   }

   @Test
   void allServantConcealmentUsesTheCompleteVisibilityStandard() throws IOException {
      String authority = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/concealment/ServantConcealment.java"));
      assertTrue(authority.contains("entity instanceof ServantEntity"));
      assertTrue(authority.contains("\"shadow_hassan\".equals(vars.servant_card_id)"));
      assertTrue(authority.contains("ServantCardShadowHassanSkills.isConcealed(serverPlayer)"));
      assertTrue(authority.contains("player.hasEffect(MobEffects.INVISIBILITY)"));
      assertTrue(authority.contains("BajiquanCombatService.isCircleRealmActive(player)"));
      assertTrue(authority.contains("manageVanillaInvisibility(entity, concealed)"));
      assertTrue(authority.contains("ConcealmentStateSync.update(entity, concealed)"));
      assertTrue(authority.contains("entity.setGlowingTag(false)"));
      assertTrue(authority.contains("mob.setTarget(null)"));
      assertTrue(authority.contains("entity.setSilent(true)"));

      String client = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/client/ServantCardConcealmentClient.java"));
      assertTrue(client.contains("ObserverConcealmentClient.isPerfectlyConcealed(player.getUUID())"));
      assertTrue(client.contains("ObserverConcealmentClient.isPerfectlyConcealed(entity.getUUID())"));
      String sync = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/concealment/ConcealmentStateSync.java"));
      assertTrue(sync.contains("PlayerEvent.StartTracking"));
      assertTrue(sync.contains("sendToPlayersTrackingEntityAndSelf"));
      String message = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/network/ConcealmentStateMessage.java"));
      assertTrue(message.contains("UUID entityId, boolean concealed"));
      String renderer = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/client/renderer/BaseServantRenderer.java"));
      assertTrue(renderer.contains("ServantCardConcealmentClient.isPerfectlyConcealed(entity)"));
      String dispatcher = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/mixin/client/EntityRenderDispatcherMixin.java"));
      assertTrue(dispatcher.contains("hideConcealedFire"));
      String mixins = Files.readString(RESOURCES.resolve("typemoonworld.mixins.json"));
      assertTrue(mixins.contains("client.EntityRenderDispatcherMixin"));
      assertTrue(mixins.contains("client.NeatHealthBarRendererMixin"));
      String neat = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/mixin/client/NeatHealthBarRendererMixin.java"));
      assertTrue(neat.contains("vazkii.neat.HealthBarRenderer"));
      assertTrue(neat.contains("ServantCardConcealmentClient.isPerfectlyConcealed(entity)"));
   }

   @Test
   void shadowHassanVoicesAreRegisteredAndValidOgg() throws IOException {
      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      for (String sound : List.of("shadow_hassan_voice_attack", "shadow_hassan_voice_command_attack",
         "shadow_hassan_voice_victory", "shadow_hassan_voice_meditative_sensitivity")) {
         assertTrue(sounds.has(sound), sound);
      }
      for (String file : List.of("attack1.ogg", "attack2.ogg", "command_attack.ogg", "victory.ogg", "meditative_sensitivity.ogg")) {
         Path path = RESOURCES.resolve("assets/typemoonworld/sounds/voice/shadow_hassan/" + file);
         assertTrue(Files.size(path) > 0, file);
         assertEquals("OggS", new String(Files.readAllBytes(path), 0, 4, StandardCharsets.US_ASCII), file);
      }
      String registrations = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/init/ModSounds.java"));
      assertTrue(registrations.contains("SHADOW_HASSAN_VOICE_MEDITATIVE_SENSITIVITY"));
      String voices = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/entity/ServantVoiceHelper.java"));
      assertTrue(voices.contains("tryPlayShadowHassanCommandAttack"));
      assertTrue(voices.contains("tryPlayShadowHassanMeditativeSensitivity"));
      String entity = Files.readString(JAVA.resolve("net/xxxjk/TYPE_MOON_WORLD/servant/entity/ShadowHassanEntity.java"));
      assertTrue(entity.contains("playDeathVoiceOnce"));
   }

   private static JsonObject json(String path) throws IOException {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(path))).getAsJsonObject();
   }
}
