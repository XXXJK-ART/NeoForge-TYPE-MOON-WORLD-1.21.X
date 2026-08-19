package net.xxxjk.TYPE_MOON_WORLD.combat;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ChurchDeadApostleResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA_SOURCES = Path.of("src/main/java");

   @Test void contenderDefaultItemDoesNotReadSyncedDataDuringConstruction() throws IOException {
      String source = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/ContenderBulletEntity.java"));
      int methodStart = source.indexOf("protected Item getDefaultItem()");
      int methodEnd = source.indexOf("public boolean isOriginBullet()", methodStart);
      assertTrue(methodStart >= 0 && methodEnd > methodStart);
      String method = source.substring(methodStart, methodEnd);
      assertFalse(method.contains("isOriginBullet()"));
      assertTrue(method.contains("return ModItems.BULLET.get();"));
   }

   @Test void onlyPlayerThrownBlackKeysCanDropAsItems() throws IOException {
      String source = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/BlackKeyProjectileEntity.java"));
      assertTrue(source.contains("recoverable && getOwner() instanceof Player && !stack.isEmpty()"));
      String executor = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/church/ChurchExecutorEntity.java"));
      assertTrue(executor.contains("new BlackKeyProjectileEntity(level(), this, stack.copyWithCount(1), false)"));
   }

   @Test void currentDeadApostleScaleAndHighStageStatsAreRegistered() throws IOException {
      String apostle = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/deadapostle/DeadApostleEntity.java"));
      assertTrue(apostle.contains("MIN_BODY_SCALE = 0.6"));
      assertTrue(apostle.contains("MAX_BODY_SCALE = 1.0"));
      String attributes = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/init/ModEventBusEvents.java"));
      assertTrue(attributes.contains("attributes(100.0, 18.0, 12.0, 0.32)"));
      assertTrue(attributes.contains("attributes(150.0, 24.0, 16.0, 0.36)"));
   }

   @Test void churchAndDeadApostlesUseAdaptiveCombatGoals() throws IOException {
      String executor = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/church/ChurchExecutorEntity.java"));
      assertTrue(executor.contains("new ChurchCombatGoal(this)"));
      assertTrue(executor.contains("closeAssault = !closeAssault"));
      assertTrue(executor.contains("getMoveControl().strafe"));
      String apostle = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/deadapostle/DeadApostleEntity.java"));
      assertTrue(apostle.contains("new DeadApostleCombatGoal(this)"));
      assertTrue(apostle.contains("getMoveControl().strafe"));
   }

   @Test void highStageDeadApostlesDodgeAndUseMultipleApproaches() throws IOException {
      String apostle = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/deadapostle/DeadApostleEntity.java"));
      assertTrue(apostle.contains("tryDodgeIncomingProjectile()"));
      assertTrue(apostle.contains("impactTicks"));
      assertTrue(apostle.contains("trySpecialApproach(target, distance)"));
      assertTrue(apostle.contains("boolean leap"));
      assertTrue(apostle.contains("double dash"));
      assertTrue(apostle.contains("apostle instanceof NightKinEntity ? 10.0 : 8.0"));
   }

   @Test void churchExecutorsAlertShareTargetsAndRegroup() throws IOException {
      String executor = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/church/ChurchExecutorEntity.java"));
      assertTrue(executor.contains("new HurtByTargetGoal(this).setAlertOthers()"));
      assertTrue(executor.contains("coordinatePackTargets()"));
      assertTrue(executor.contains("ally.setTarget(sharedTarget)"));
      assertTrue(executor.contains("new ChurchPackRegroupGoal(this)"));
      assertTrue(executor.contains("candidate.getId() < selected.getId()"));
   }

   @Test void highStageDeadApostlesUseTheMagicianChineseNamePools() throws IOException {
      String names = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/deadapostle/DeadApostleNameGenerator.java"));
      assertTrue(names.contains("MysticMagicianEntity.generateCulturalNameChinese"));
      assertFalse(names.contains("Anderson"));
      assertFalse(names.contains("Haruto"));
      String magician = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/MysticMagicianEntity.java"));
      assertTrue(magician.contains("EUROPEAN_GIVEN_NAMES_ZH[firstNameIndex]"));
      assertTrue(magician.contains("generateRandomJapaneseName(random, femaleVariant).chinese()"));
   }

   @Test void churchExecutorsCastTheRealReinforcementEffects() throws IOException {
      String executor = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/church/ChurchExecutorEntity.java"));
      assertTrue(executor.contains("tryCastReinforcement()"));
      assertTrue(executor.contains("REINFORCEMENT_SELF_DEFENSE"));
      assertTrue(executor.contains("REINFORCEMENT_SELF_STRENGTH"));
      assertTrue(executor.contains("REINFORCEMENT_SELF_AGILITY"));
      assertTrue(executor.contains("REINFORCEMENT_SELF_SIGHT"));
      assertTrue(executor.contains("(600 + proficiency * 10) * level"));
      assertFalse(executor.contains("hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)"));
   }

   @Test void churchExecutorReinforcementUsesTheMagicCircuitOverlay() throws IOException {
      String renderer = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/client/renderer/ChurchExecutorRenderer.java"));
      String layer = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/client/renderer/ChurchExecutorReinforcementLayer.java"));
      assertTrue(renderer.contains("new ChurchExecutorReinforcementLayer(this, context)"));
      for (String effect : Set.of("REINFORCEMENT_SELF_STRENGTH", "REINFORCEMENT_SELF_DEFENSE",
         "REINFORCEMENT_SELF_AGILITY", "REINFORCEMENT_SELF_SIGHT")) {
         assertTrue(layer.contains(effect));
      }
      assertTrue(layer.contains("ReinforcementRenderType.getSkinRenderType"));
      assertTrue(layer.contains("entity.isFemale() ? alexOverlayModel : steveOverlayModel"));
   }

   @Test void churchExecutorsRandomizeAndUseBasicMagecraft() throws IOException {
      String executor = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/church/ChurchExecutorEntity.java"));
      for (String magic : Set.of("Binding", "Suggestion", "Healing")) {
         assertTrue(executor.contains("Church" + magic + "Proficiency"));
         assertTrue(executor.contains("random.nextInt(101)"));
      }
      assertTrue(executor.contains("MagicBinding.applyBinding"));
      assertTrue(executor.contains("MagicSuggestion.applySuggestion"));
      assertTrue(executor.contains("MagicHealing.healDirect"));
      assertTrue(executor.contains("getEntitiesOfClass(ChurchExecutorEntity.class"));
      assertTrue(executor.contains("healthRatio < selectedRatio"));
   }

   @Test void blackKeyHasAllSixVisualStatesAndAssets() throws IOException {
      JsonObject animations = json("assets/typemoonworld/animations/black_key.animation.json")
         .getAsJsonObject("animations");
      Set<String> expected = Set.of("count_1_expanded", "count_1_folded", "count_2_expanded", "count_2_folded",
         "count_3_expanded", "count_3_folded");
      assertEquals(expected, animations.keySet());
      assertTrue(Files.isRegularFile(RESOURCES.resolve("assets/typemoonworld/geo/black_key.geo.json")));
      var texture = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/black_key.png").toFile());
      assertNotNull(texture);
      assertEquals(32, texture.getWidth());
      assertEquals(32, texture.getHeight());
   }

   @Test void allNpcSkinsAreStandardPlayerTextures() throws IOException {
      String[] names = {"the_dead", "ghoul", "living_dead", "night_kin", "church_executor_male_1",
         "church_executor_male_2", "church_executor_male_3", "church_executor_female"};
      for (String name : names) {
         var image = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/entity/" + name + ".png").toFile());
         assertNotNull(image, name);
         assertEquals(64, image.getWidth(), name);
         assertEquals(64, image.getHeight(), name);
      }
      assertFalse(Files.exists(RESOURCES.resolve("assets/typemoonworld/textures/entity/church_executor_male_4.png")));
   }

   @Test void churchExecutorBaptismUsesTheSharedPurificationRite() throws IOException {
      String executor = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/entity/church/ChurchExecutorEntity.java"));
      String rite = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/magic/church/BaptismRiteEventHandler.java"));
      assertTrue(executor.contains("tryStartBaptism(target)"));
      assertTrue(executor.contains("tickBaptismChant()"));
      assertTrue(executor.contains("BAPTISM_CHANT_TICKS"));
      assertTrue(executor.contains("interruptBaptismChant()"));
      assertTrue(executor.contains("BaptismRiteEventHandler.applyRite(this, target, proficiency)"));
      String events = Files.readString(JAVA_SOURCES.resolve(
         "net/xxxjk/TYPE_MOON_WORLD/event/ChurchDeadApostleEvents.java"));
      assertTrue(events.contains("damage > 0.0F"));
      assertTrue(events.contains("executor.interruptBaptismChant()"));
      assertTrue(events.contains("BaptismRiteEventHandler.interrupt(player)"));
      assertFalse(executor.contains("target.hurt(damageSources().magic(), 8.0F"));
      assertTrue(rite.contains("public static float applyRite(LivingEntity caster"));
      assertTrue(rite.contains("cleanse(target, proficiency)"));
      assertTrue(rite.contains("PaleRiderInfectionService.cleanse(target, true)"));
      assertTrue(rite.contains("caster.damageSources().source(BAPTISM_DAMAGE, caster)"));
   }

   @Test void cityContainsOnlyRequestedHostilesAndNoStructureTag() throws IOException {
      JsonObject city = json("data/typemoonworld/worldgen/biome/city.json");
      var monsters = city.getAsJsonObject("spawners").getAsJsonArray("monster");
      assertEquals(4, monsters.size());
      for (var element : monsters) assertTrue(element.getAsJsonObject().get("type").getAsString().startsWith("typemoonworld:"));
      assertTrue(Files.readString(RESOURCES.resolve("data/typemoonworld/worldgen/placed_feature/city_mega_spruce.json"))
         .contains("minecraft:mega_spruce"));
      Path structureTags = RESOURCES.resolve("data/typemoonworld/tags/worldgen/biome/has_structure");
      try (var files = Files.walk(structureTags)) {
         assertTrue(files.filter(Files::isRegularFile).allMatch(path -> {
            try { return !Files.readString(path).contains("typemoonworld:city"); }
            catch (IOException exception) { throw new RuntimeException(exception); }
         }));
      }
   }

   @Test void deadApostlesAreTaggedAsUndead() throws IOException {
      String tag = Files.readString(RESOURCES.resolve("data/minecraft/tags/entity_type/undead.json"));
      for (String id : Set.of("the_dead", "ghoul", "living_dead", "night_kin")) assertTrue(tag.contains("typemoonworld:" + id));
   }

   @Test void churchMagicDefinitionsMatchTheirKnowledgeBehavior() throws IOException {
      JsonObject stigma = json("data/typemoonworld/magic/definitions/stigma.json");
      assertEquals("typemoonworld:church", stigma.get("category").getAsString());
      assertFalse(stigma.get("wheel_selectable").getAsBoolean());
      assertTrue(stigma.get("knowledge_only").getAsBoolean());
   }

   private static JsonObject json(String relative) throws IOException {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative))).getAsJsonObject();
   }
}
