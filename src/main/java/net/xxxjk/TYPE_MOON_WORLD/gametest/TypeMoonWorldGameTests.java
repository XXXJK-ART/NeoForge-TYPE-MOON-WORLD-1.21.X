package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantSkillDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.DefinitionSnapshotService;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicPresetRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.MagicAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

@GameTestHolder("typemoonworld")
@PrefixGameTestTemplate(false)
public final class TypeMoonWorldGameTests {
   private TypeMoonWorldGameTests() { }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void codecDefinitionsLoad(GameTestHelper helper) {
      helper.assertTrue(ServantDataRegistry.size() > 0, "servant definitions were not loaded");
      helper.assertTrue(ServantSkillDataRegistry.all().size() >= 80, "skill definitions were not loaded");
      helper.assertTrue(ServantAiDefinitionRegistry.all().size() >= 10, "AI definitions were not loaded");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void magicDefinitionsLoad(GameTestHelper helper) {
      helper.assertTrue(!MagicDefinitionRegistry.ids().isEmpty(), "magic definitions were not loaded");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void genericServantSummons(GameTestHelper helper) {
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_pendragon");
      var entity = TypeMoonWorldApi.addon("typemoonworld").servants().summon(helper.getLevel(), id, helper.absolutePos(new BlockPos(2, 2, 2)));
      helper.assertTrue(entity != null && entity.isAlive(), "generic servant summon failed");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void ushiwakamaruRiderSummonInitializesDedicatedEntity(GameTestHelper helper) {
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "ushiwakamaru_rider");
      var entity = TypeMoonWorldApi.addon("typemoonworld").servants().summon(helper.getLevel(), id, helper.absolutePos(new BlockPos(2, 2, 2)));
      helper.assertTrue(entity instanceof UshiwakamaruRiderEntity, "Rider summon did not use dedicated entity");
      var rider = (UshiwakamaruRiderEntity) entity;
      helper.assertTrue(rider.getMainHandItem().is(ModItems.SPIDER_CUTTER.get()), "Rider did not equip Spider Cutter");
      helper.assertTrue(rider.getCombatPhase() == 1 && rider.getMaxHealth() > 0.0F, "Rider attributes were not initialized");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 80)
   public static void ushiwakamaruEightBoatClonesMatchOwnerAndAcquireTargets(GameTestHelper helper) {
      var level = helper.getLevel();
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "ushiwakamaru_rider");
      var summoned = TypeMoonWorldApi.addon("typemoonworld").servants().summon(level, id, helper.absolutePos(new BlockPos(2, 2, 2)));
      helper.assertTrue(summoned instanceof UshiwakamaruRiderEntity, "Rider summon did not use dedicated entity");
      var rider = (UshiwakamaruRiderEntity)summoned;
      rider.setCombatPhase(3);
      rider.setCurrentMp(100.0);

      var firstTarget = EntityType.ZOMBIE.create(level);
      var secondTarget = EntityType.ZOMBIE.create(level);
      helper.assertTrue(firstTarget != null && secondTarget != null, "Could not create clone targets");
      firstTarget.setNoAi(true);
      secondTarget.setNoAi(true);
      firstTarget.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
      secondTarget.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
      firstTarget.setHealth(1000.0F);
      secondTarget.setHealth(1000.0F);
      firstTarget.moveTo(rider.getX() + 10.0, rider.getY(), rider.getZ(), 0.0F, 0.0F);
      secondTarget.moveTo(rider.getX() - 10.0, rider.getY(), rider.getZ(), 0.0F, 0.0F);
      level.addFreshEntity(firstTarget);
      level.addFreshEntity(secondTarget);
      rider.setTarget(firstTarget);

      helper.runAfterDelay(10, () -> {
         helper.assertTrue(rider.getPersistentData().hasUUID(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET),
            "Eight-Boat Leap did not retain its original target UUID; mp=" + rider.getCurrentMp()
               + ", phase=" + rider.getCombatPhase() + ", target=" + rider.getTarget()
               + ", until=" + rider.getPersistentData().getLong("UshiwakamaruEightBoatUntil")
               + ", last=" + rider.getPersistentData().getLong("UshiwakamaruLastEightBoat"));
         helper.assertTrue(firstTarget.isAlive(), "Eight-Boat original target died before the cleanup check");
         helper.assertTrue(level.getEntity(firstTarget.getUUID()) == firstTarget,
            "Eight-Boat original target was missing from the level UUID index");
         var clones = level.getEntitiesOfClass(UshiwakamaruRiderEntity.class,
            new AABB(rider.blockPosition()).inflate(32.0), UshiwakamaruRiderEntity::isClone);
         helper.assertTrue(clones.size() == 7, "Eight-Boat Leap did not create exactly seven clones; found " + clones.size());
         var targetIds = new java.util.HashSet<java.util.UUID>();
         for (var clone : clones) {
            if (clone.getTarget() != null) targetIds.add(clone.getTarget().getUUID());
         }
         helper.assertTrue(targetIds.size() >= 2, "Clones did not acquire targets independently; locked targets: " + targetIds.size());
         for (var clone : clones) {
            helper.assertTrue(Math.abs(clone.getMaxHealth() - rider.getMaxHealth()) < 0.001F,
               "Clone max health does not match the owner");
            helper.assertTrue(Math.abs(clone.getAttributeValue(Attributes.ATTACK_DAMAGE)
               - rider.getAttributeValue(Attributes.ATTACK_DAMAGE)) < 0.001,
               "Clone attack damage does not match the owner");
            helper.assertTrue(Math.abs(clone.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()
               - rider.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()) < 0.001,
               "Clone base movement speed does not match the owner");
            helper.assertTrue(clone.getAttributeValue(Attributes.MOVEMENT_SPEED)
               > clone.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * 2.5,
               "Clone did not retain Riding and Eight-Boat Leap movement bonuses: "
                  + clone.getAttributeValue(Attributes.MOVEMENT_SPEED) + " / "
                  + clone.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers());
            helper.assertTrue(Math.abs(clone.getCurrentMp() - 80.0) < 0.001,
               "Clone consumed MP by entering the active-skill AI path");
            if (clone.getTarget() != null) targetIds.add(clone.getTarget().getUUID());
         }
         firstTarget.kill();
      });
      helper.runAfterDelay(16, () -> {
         var clones = level.getEntitiesOfClass(UshiwakamaruRiderEntity.class,
            new AABB(rider.blockPosition()).inflate(32.0), UshiwakamaruRiderEntity::isClone);
         helper.assertTrue(clones.isEmpty(), "Eight-Boat clones remained after the original target died");
         helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void serverSnapshotContainsAllSections(GameTestHelper helper) {
      String snapshot = DefinitionSnapshotService.build();
      helper.assertTrue(snapshot.contains("\"magic\"") && snapshot.contains("\"cards\"") && snapshot.contains("\"skills\"")
         && snapshot.contains("\"noble_phantasms\"") && snapshot.contains("\"ai\""), "definition snapshot is incomplete");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void transformAndMasterContract(GameTestHelper helper) {
      var masterPlayer = helper.makeMockServerPlayerInLevel();
      var servantPlayer = helper.makeMockServerPlayerInLevel();
      var servant = TypeMoonWorldApi.servantForm(servantPlayer);
      var master = TypeMoonWorldApi.master(masterPlayer);
      helper.assertTrue(servant.transform(ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_pendragon")), "servant transform failed");
      helper.assertTrue(master.activate(), "master activation failed");
      helper.assertTrue(master.bind(servantPlayer), "master contract failed");
      helper.assertTrue(master.boundServant() != null && servant.transformed(), "contract state was not persisted");
      helper.assertTrue(servant.release(), "servant release failed");
      helper.assertTrue(master.release(), "master release failed");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void magicProgressAndPresetValidation(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "magic_bullet");
      var magic = TypeMoonWorldApi.addon("typemoonworld").magics();
      helper.assertTrue(magic.knowledge(player).learn(id), "magic learning failed");
      magic.knowledge(player).setProficiency(id, 250);
      helper.assertTrue(magic.knowledge(player).proficiency(id) == 100.0, "proficiency was not clamped");
      CompoundTag hostile = new CompoundTag();
      for (int i = 0; i < 80; i++) hostile.putString("field_" + i, "x".repeat(200));
      helper.assertTrue(MagicPresetRegistry.normalize(id.toString(), hostile).payload().isEmpty(), "oversized preset was accepted");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void imaginaryAttributeFacadeReadsExistingState(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.player_magic_attributes_imaginary_number = true;
      var attributes = TypeMoonWorldApi.magicAttributes(player);
      helper.assertTrue(attributes.has(MagicAttributes.IMAGINARY_NUMBER), "imaginary-number attribute was not exposed");
      helper.assertTrue(attributes.attributes().contains(MagicAttributes.IMAGINARY_NUMBER), "attribute snapshot was incomplete");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 60)
   public static void heraclesCardPrimaryAttackDealsDamage(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(TypeMoonWorldApi.servantForm(player).transform(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "heracles")), "Heracles transform failed");
      helper.assertTrue(player.getMainHandItem().is(ModItems.TEMPLE_STONE_SWORD_AXE.get()), "Heracles weapon was not equipped");
      BlockPos playerPos = helper.absolutePos(new BlockPos(1, 2, 2));
      player.teleportTo(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
      player.setYRot(-90.0F);
      var target = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 2));
      target.setHealth(target.getMaxHealth());
      float before = target.getHealth();
      helper.runAfterDelay(1, () -> {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHeraclesSkills.performBasicSweep(player);
         float afterFirst = target.getHealth();
         helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) > 0.0,
            "Heracles attack attribute was not positive");
         helper.assertTrue(afterFirst < before,
            "Heracles primary sweep dealt no damage; attack=" + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHeraclesSkills.performBasicSweep(player);
         helper.assertTrue(target.getHealth() == afterFirst,
            "Heracles primary sweep ignored its 8-tick cooldown");
         helper.succeed();
      });
   }
}
