package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaThrownWeaponEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardLancelotBerserkerSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotBerserkerCombatAi;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;

@GameTestHolder("typemoonworld_lancelot")
@PrefixGameTestTemplate(false)
public final class LancelotDestructionGameTests {
   private LancelotDestructionGameTests() { }

   @GameTest(template = "ancient_temple", timeoutTicks = 30, batch = "lancelot_sprint_breakthrough")
   public static void lowSpeedSprintClearsAFullBodyWall(GameTestHelper h) {
      var origin = h.absolutePos(new BlockPos(6, 6, 6));
      for (int x = -3; x <= 3; x++) {
         h.getLevel().setBlock(origin.offset(x, -1, 0), Blocks.STONE.defaultBlockState(), 2);
         for (int z = 1; z <= 4; z++) {
            for (int y = 0; y < 4; y++) h.getLevel().setBlock(origin.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
         }
      }
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      player.teleportTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
      player.setYRot(0);
      player.setOnGround(true);
      player.setSprinting(true);
      h.assertTrue(ServantCardTransformManager.transform(player, "lancelot_berserker"), "Lancelot transformation failed");
      ServantCardLancelotBerserkerSkills.tick(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
      h.runAfterDelay(12, () -> {
         for (int x = -1; x <= 1; x++) for (int z = 1; z <= 3; z++) for (int y = 0; y < 2; y++) {
            h.assertTrue(h.getLevel().getBlockState(origin.offset(x, y, z)).isAir(), "sprint wall remained at " + x + "," + y + "," + z);
         }
         h.assertTrue(h.getLevel().getBlockState(origin.below()).is(Blocks.STONE), "sprint break removed footing");
         h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "lancelot_pursuit")
   public static void npcSprintsDuringTargetPursuit(GameTestHelper h) {
      LancelotBerserkerEntity lancelot = ModEntities.LANCELOT_BERSERKER.get().create(h.getLevel());
      var target = EntityType.HUSK.create(h.getLevel());
      lancelot.setPos(h.absolutePos(new BlockPos(4, 5, 5)).getX() + 0.5, 5, h.absolutePos(new BlockPos(4, 5, 5)).getZ() + 0.5);
      target.setPos(lancelot.getX(), lancelot.getY(), lancelot.getZ() + 10);
      target.setNoAi(true);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
      target.setHealth(1000);
      h.getLevel().addFreshEntity(lancelot);
      h.getLevel().addFreshEntity(target);
      lancelot.setTarget(target);
      h.assertTrue(LancelotBerserkerCombatAi.tick(lancelot, h.getLevel()), "Lancelot did not take ownership of combat");
      h.assertTrue(lancelot.isSprinting(), "Lancelot did not sprint after its target");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30, batch = "lancelot_throw_depth")
   public static void knightOfOwnerThrowKeepsBlocksMoreThanTwoBelowLaunch(GameTestHelper h) {
      var origin = h.absolutePos(new BlockPos(6, 12, 6));
      for (int dy = -10; dy <= 0; dy++) {
         for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            h.getLevel().setBlock(origin.offset(x, dy, z), Blocks.STONE.defaultBlockState(), 2);
         }
      }
      var owner = ModEntities.LANCELOT_BERSERKER.get().create(h.getLevel());
      owner.setPos(origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5);
      h.getLevel().addFreshEntity(owner);
      ItemStack weapon = LancelotCombatHelper.knightOfOwnerStack(new ItemStack(Items.IRON_SWORD), owner);
      EmiyaThrownWeaponEntity projectile = new EmiyaThrownWeaponEntity(h.getLevel(), owner, weapon);
      projectile.setPos(origin.getX() + 0.5, origin.getY() + 6.5, origin.getZ() + 0.5);
      projectile.setNoGravity(true);
      projectile.setPiercingImpact(true);
      projectile.shoot(0, -1, 0, 2.4F, 0.0F);
      h.getLevel().addFreshEntity(projectile);
      h.runAfterDelay(15, () -> {
         h.assertTrue(h.getLevel().getBlockState(origin.below(3)).is(Blocks.STONE), "Knight of Owner projectile dug more than two blocks below launch");
         h.succeed();
      });
   }
}
