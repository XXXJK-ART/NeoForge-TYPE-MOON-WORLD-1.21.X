package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import org.junit.jupiter.api.Test;

class ServantCardBalanceFixesTest {
   @Test
   void gawainRankPlusStatsAreCalculatedBeforeTheTripleMultiplier() {
      ServantParams params = ServantParams.of("B", true, "B", true, "B", false, "A", false, "A", false);

      assertEquals(800.0, params.maxHealth(), 0.001);
      assertEquals(40.0, params.attackDamage(), 0.001);
      assertEquals(0.32, params.movementSpeed(), 0.001);
      assertEquals(24.0, params.armor(), 0.001);
      assertEquals(1000.0, params.manaPool(), 0.001);
      assertEquals(10.0, params.critRatePercent(), 0.001);

      assertEquals(2400.0, params.maxHealth() * 3.0, 0.001);
      assertEquals(120.0, params.attackDamage() * 3.0, 0.001);
      assertEquals(0.96, params.movementSpeed() * 3.0, 0.001);
      assertEquals(72.0, params.armor() * 3.0, 0.001);
   }

   @Test
   void maxHealthCapMixinIsRegisteredForGawainTripleHealth() throws IOException {
      String mixins = Files.readString(Path.of("src/main/resources/typemoonworld.mixins.json"));
      String source = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/mixin/AttributesMixin.java"));

      assertTrue(mixins.contains("\"AttributesMixin\""));
      assertTrue(source.contains("TYPEMOONWORLD_MAX_HEALTH_CAP = 1000000.0"));
      assertTrue(source.contains("TYPEMOONWORLD_ARMOR_CAP = 10000.0"));
      assertTrue(source.contains("TYPEMOONWORLD_ARMOR_TOUGHNESS_CAP = 10000.0"));
      assertTrue(source.contains("stringValue=generic.max_health"));
      assertTrue(source.contains("stringValue=generic.armor"));
      assertTrue(source.contains("stringValue=generic.armor_toughness"));
   }

   @Test
   void everyServantDefinitionKeepsRankPlusStatsWithinRaisedCaps() throws IOException {
      Path definitions = Path.of("src/main/resources/data/typemoonworld/servant/definitions");
      try (Stream<Path> paths = Files.list(definitions)) {
         for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonObject p = root.getAsJsonObject("parameters");
            ServantParams params = ServantParams.of(
               p.get("endurance").getAsString(), getBooleanOrDefault(p, "endurance_plus"),
               p.get("strength").getAsString(), getBooleanOrDefault(p, "strength_plus"),
               p.get("agility").getAsString(), getBooleanOrDefault(p, "agility_plus"),
               p.get("magic").getAsString(), getBooleanOrDefault(p, "magic_plus"),
               p.get("luck").getAsString(), getBooleanOrDefault(p, "luck_plus")
            );

            assertTrue(params.maxHealth() * 3.0 < 1000000.0, root.get("id").getAsString() + " health cap");
            assertTrue(params.armor() * 3.0 < 10000.0, root.get("id").getAsString() + " armor cap");
            assertTrue(params.attackDamage() * 3.0 < 2048.0, root.get("id").getAsString() + " attack cap");
            assertTrue(params.movementSpeed() * 3.0 < 1024.0, root.get("id").getAsString() + " speed cap");
         }
      }
   }

   @Test
   void servantEntitiesAndCardsBothUseSharedServantParams() throws IOException {
      String entity = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/ServantEntity.java"));
      String card = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardTransformManager.java"));

      assertTrue(entity.contains("setBaseValue(params.maxHealth())"));
      assertTrue(entity.contains("setBaseValue(params.attackDamage())"));
      assertTrue(entity.contains("setBaseValue(params.movementSpeed())"));
      assertTrue(entity.contains("setBaseValue(params.armor())"));
      assertTrue(card.contains("params.maxHealth() - player.getAttributeBaseValue(Attributes.MAX_HEALTH)"));
      assertTrue(card.contains("params.attackDamage() - player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)"));
      assertTrue(card.contains("params.movementSpeed() - player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)"));
      assertTrue(card.contains("addOrReplace(player.getAttribute(Attributes.ARMOR), ARMOR_ID, params.armor())"));
   }

   @Test
   void odaMatchlockTargetsExcludeContractMasters() throws IOException {
      String gun = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity/OdaMatchlockGunEntity.java"));
      String bullet = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity/OdaMatchlockBulletEntity.java"));
      String card = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardOdaNobunagaSkills.java"));

      assertTrue(gun.contains("ServantMasterTargeting.isContractMaster(owner, target)"));
      assertTrue(bullet.contains("ServantMasterTargeting.isContractMaster(owner, living)"));
      assertTrue(card.contains("private static boolean isOdaCardTarget"));
      assertTrue(card.contains("ServantMasterTargeting.isContractMaster(player, target)"));
   }

   @Test
   void odaMountFlightAvoidsPerTickSyncAndParticleSpam() throws IOException {
      String gun = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity/OdaMatchlockGunEntity.java"));
      String card = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardOdaNobunagaSkills.java"));

      assertTrue(gun.contains("MOUNT_INPUT_SYNC_EPSILON"));
      assertTrue(gun.contains("AIM_SYNC_EPSILON_DEGREES"));
      assertTrue(gun.contains("MOUNT_TRAIL_PARTICLE_INTERVAL = 12"));
      assertTrue(gun.contains("FOOT_SUPPORT_TRAIL_PARTICLE_INTERVAL = 12"));
      assertTrue(gun.contains("MOUNT_TRAIL_MIN_SPEED_SQR"));
      assertTrue(gun.contains("setSynchedFloatIfChanged(MOVE_FORWARD"));
      assertTrue(gun.contains("setSynchedAngleIfChanged(AIM_YAW"));
      assertTrue(gun.contains("velocity.lengthSqr() >= MOUNT_TRAIL_MIN_SPEED_SQR"));
      assertTrue(gun.contains("owner.getDeltaMovement().lengthSqr() >= 0.0004"));
      assertTrue(gun.contains("private static final RawAnimation IDLE_ANIMATION"));
      assertTrue(card.contains("discardFloatingMatchlocks(level, player)"));
      assertTrue(card.contains("gun -> gun.isFloatingFor(player.getUUID())"));
      assertTrue(card.contains("gun.discardSilently()"));
   }

   @Test
   void gilgameshBabIluMarksEaSummonOnTheItemStack() throws IOException {
      String source = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/item/custom/GilgameshNoblePhantasmItem.java"));

      assertTrue(source.contains("BAB_ILU_EA_SUMMONED_TAG"));
      assertTrue(source.contains("tryMarkBabIluEaSummoned(stack)"));
      assertTrue(source.contains("giveEaIfMissing(player)"));
   }

   @Test
   void servantCardsReuseNpcCriticalEffectsForMuramasaAndLiShuwen() throws IOException {
      String muramasaCard = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardSenkoMuramasaSkills.java"));
      String muramasaHelper = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/MuramasaCombatHelper.java"));
      String liCard = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardLiShuwenSkills.java"));
      String liHelper = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/LiShuwenCombatHelper.java"));
      String guaranteedHits = Files.readString(Path.of("src/main/resources/data/typemoonworld/tags/damage_type/fanatic_guaranteed_hits.json"));
      String bypassesDefenses = Files.readString(Path.of("src/main/resources/data/typemoonworld/tags/damage_type/fanatic_bypasses_defenses.json"));
      String noblePhantasmDamage = Files.readString(Path.of("src/main/resources/data/typemoonworld/tags/damage_type/noble_phantasm_damage.json"));
      String bypassesArmor = Files.readString(Path.of("src/main/resources/data/minecraft/tags/damage_type/bypasses_armor.json"));
      String bypassesShield = Files.readString(Path.of("src/main/resources/data/minecraft/tags/damage_type/bypasses_shield.json"));
      String bypassesEnchantments = Files.readString(Path.of("src/main/resources/data/minecraft/tags/damage_type/bypasses_enchantments.json"));
      String bypassesResistance = Files.readString(Path.of("src/main/resources/data/minecraft/tags/damage_type/bypasses_resistance.json"));

      assertTrue(muramasaHelper.contains("public static void applyNoDefenseDamage(LivingEntity attacker"));
      assertTrue(muramasaHelper.contains("public static double precisionStrikeDamage(LivingEntity attacker"));
      assertTrue(muramasaCard.contains("tryBypassDefenseAttack"));
      assertTrue(muramasaCard.contains("MuramasaCombatHelper.applyNoDefenseDamage(player, target"));
      assertTrue(muramasaCard.contains("MuramasaCombatHelper.precisionStrikeDamage"));

      assertTrue(liHelper.contains("public static void resolveWuErDa(LivingEntity attacker"));
      assertTrue(liCard.contains("LiShuwenCombatHelper.resolveWuErDa(player, target"));
      assertTrue(liCard.contains("findWuErDaTarget"));
      assertTrue(liCard.contains("WU_ER_DA_LOCK_RANGE = 7.0"));
      assertTrue(liCard.contains("WU_ER_DA_LOCK_MIN_DOT = 0.35"));
      assertTrue(liCard.contains("findLookTarget(player, WU_ER_DA_LOCK_RANGE, 2.4"));
      assertTrue(liHelper.contains("public static final ResourceKey<DamageType> WU_ER_DA"));
      assertTrue(liHelper.contains("wuErDaDamage(attacker)"));
      assertTrue(liHelper.contains("source(WU_ER_DA, attacker)"));
      assertTrue(Files.exists(Path.of("src/main/resources/data/typemoonworld/damage_type/wu_er_da.json")));
      assertTrue(guaranteedHits.contains("typemoonworld:wu_er_da"));
      assertTrue(bypassesDefenses.contains("typemoonworld:wu_er_da"));
      assertTrue(noblePhantasmDamage.contains("typemoonworld:wu_er_da"));
      assertTrue(bypassesArmor.contains("typemoonworld:wu_er_da"));
      assertTrue(bypassesShield.contains("typemoonworld:wu_er_da"));
      assertTrue(bypassesEnchantments.contains("typemoonworld:wu_er_da"));
      assertTrue(bypassesResistance.contains("typemoonworld:wu_er_da"));
   }

   private static boolean getBooleanOrDefault(JsonObject json, String key) {
      return json.has(key) && json.get(key).getAsBoolean();
   }
}
