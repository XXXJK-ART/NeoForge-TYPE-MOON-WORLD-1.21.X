package net.xxxjk.TYPE_MOON_WORLD.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.BloodstainEffectEntity;
import org.joml.Vector3f;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class BloodBottleInteractionService {
   private static final DustParticleOptions BLOOD_DUST = new DustParticleOptions(new Vector3f(0.43F, 0.015F, 0.02F), 0.72F);

   private BloodBottleInteractionService() {
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      Player player = event.getEntity();
      if (event.getHand() != InteractionHand.MAIN_HAND
         || event.getFace() != Direction.UP
         || !player.getMainHandItem().isEmpty()
         || !player.getOffhandItem().is(ModItems.BLOOD_BOTTLE.get())) {
         return;
      }

      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
      if (!(player instanceof ServerPlayer serverPlayer) || !(event.getLevel() instanceof ServerLevel level)) {
         return;
      }
      if (serverPlayer.getCooldowns().isOnCooldown(ModItems.BLOOD_BOTTLE.get())) {
         return;
      }

      drawBloodstain(level, event.getPos());
      consumeBottle(serverPlayer);
      serverPlayer.getCooldowns().addCooldown(ModItems.BLOOD_BOTTLE.get(), 8);
      serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.blood_bottle.draw_bloodstain"), true);
   }

   private static void drawBloodstain(ServerLevel level, BlockPos pos) {
      double centerX = pos.getX() + 0.5;
      double centerY = pos.getY() + 1.012;
      double centerZ = pos.getZ() + 0.5;
      float rotation = level.random.nextFloat() * 360.0F;
      float scale = 0.46F + level.random.nextFloat() * 0.18F;
      level.addFreshEntity(new BloodstainEffectEntity(level, centerX, centerY, centerZ, scale, 600, rotation));

      for (int i = 0; i < 18; i++) {
         double angle = level.random.nextDouble() * Math.PI * 2.0;
         double radius = Math.sqrt(level.random.nextDouble()) * scale * 0.92;
         level.sendParticles(BLOOD_DUST, centerX + Math.cos(angle) * radius, centerY + 0.012, centerZ + Math.sin(angle) * radius,
            1, 0.0, 0.0, 0.0, 0.0);
      }
      level.sendParticles(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, centerX, centerY + 0.04, centerZ, 4, 0.18, 0.015, 0.18, 0.0);
      level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.42F, 0.72F + level.random.nextFloat() * 0.08F);
   }

   private static void consumeBottle(ServerPlayer player) {
      if (player.getAbilities().instabuild) {
         return;
      }

      ItemStack offhand = player.getOffhandItem();
      offhand.shrink(1);
      ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
      if (offhand.isEmpty()) {
         player.setItemInHand(InteractionHand.OFF_HAND, emptyBottle);
      } else if (!player.getInventory().add(emptyBottle)) {
         player.drop(emptyBottle, false);
      }
   }
}
