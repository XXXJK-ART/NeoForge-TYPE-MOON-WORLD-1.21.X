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
import net.xxxjk.TYPE_MOON_WORLD.api.MagicPresetRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.MagicAttributes;

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
}
