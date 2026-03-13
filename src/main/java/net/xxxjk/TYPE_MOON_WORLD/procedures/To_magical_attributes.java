package net.xxxjk.TYPE_MOON_WORLD.procedures;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.LevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicalattributesMenu;
import org.jetbrains.annotations.NotNull;

public class To_magical_attributes {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, final int pageMode) {
      if (entity != null) {
         if (entity instanceof ServerPlayer _ent) {
            final BlockPos _bpos = BlockPos.containing(x, y, z);
            _ent.openMenu(new MenuProvider() {
               @NotNull
               public Component getDisplayName() {
                  return Component.literal("Magicalattributes");
               }

               public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                  return false;
               }

               public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
                  return new MagicalattributesMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_bpos).writeInt(pageMode));
               }
            }, buf -> {
               buf.writeBlockPos(_bpos);
               buf.writeInt(pageMode);
            });
         }
      }
   }
}
