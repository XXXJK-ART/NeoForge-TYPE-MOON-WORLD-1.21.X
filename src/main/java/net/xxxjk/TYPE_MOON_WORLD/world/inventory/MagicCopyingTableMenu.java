package net.xxxjk.TYPE_MOON_WORLD.world.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.MagicCopyingTableBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModMenus;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class MagicCopyingTableMenu extends AbstractContainerMenu {
   public static final String SELF_GEAS_COPY_ID = "self_geas_scroll";
   private final MagicCopyingTableBlockEntity blockEntity; private final ItemStackHandler items; private final ContainerLevelAccess access;
   public MagicCopyingTableMenu(int id,Inventory inv,FriendlyByteBuf buf){this(id,inv,resolve(inv.player.level(),buf));}
   private MagicCopyingTableMenu(int id,Inventory inv,MagicCopyingTableBlockEntity be){this(id,inv,be,be.getBlockPos());}
   public MagicCopyingTableMenu(int id,Inventory inv,MagicCopyingTableBlockEntity be,BlockPos pos){super((MenuType)TypeMoonWorldModMenus.MAGIC_COPYING_TABLE.get(),id);blockEntity=be;items=be.getItems();access=ContainerLevelAccess.create(inv.player.level(),pos);addSlot(new SlotItemHandler(items,0,26,44){@Override public boolean mayPlace(ItemStack stack){return stack.is(Items.PAPER);}});addSlot(new SlotItemHandler(items,1,26,75){@Override public boolean mayPlace(ItemStack stack){return stack.is(Items.INK_SAC);}});for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,c+r*9+9,13+c*18,136+r*18));for(int c=0;c<9;c++)addSlot(new Slot(inv,c,13+c*18,194));}
   private static MagicCopyingTableBlockEntity resolve(Level l,FriendlyByteBuf b){BlockPos p=b==null?BlockPos.ZERO:b.readBlockPos();return l.getBlockEntity(p) instanceof MagicCopyingTableBlockEntity be?be:new MagicCopyingTableBlockEntity(p,ModBlocks.MAGIC_COPYING_TABLE.get().defaultBlockState());}
   public boolean stillValid(Player p){return stillValid(access,p,(Block)ModBlocks.MAGIC_COPYING_TABLE.get());}
   @Override public ItemStack quickMoveStack(Player player,int index){Slot slot=getSlot(index);if(!slot.hasItem())return ItemStack.EMPTY;ItemStack source=slot.getItem(),copy=source.copy();if(index<2){if(!moveItemStackTo(source,2,38,true))return ItemStack.EMPTY;}else if(source.is(Items.PAPER)){if(!moveItemStackTo(source,0,1,false))return ItemStack.EMPTY;}else if(source.is(Items.INK_SAC)){if(!moveItemStackTo(source,1,2,false))return ItemStack.EMPTY;}else if(index<29){if(!moveItemStackTo(source,29,38,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(source,2,29,false))return ItemStack.EMPTY;if(source.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(source.getCount()==copy.getCount())return ItemStack.EMPTY;slot.onTake(player,source);return copy;}
   public void tryCopy(ServerPlayer player, String id) {
      tryCopy(player, id, 0);
   }

   public void tryCopy(ServerPlayer player, String id, int force) {
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (SELF_GEAS_COPY_ID.equals(id)) {
         tryCopySelfGeas(player, force);
         return;
      }
      boolean contract = "contract_magecraft".equals(id);
      boolean learned = MagicLearningStrategy.isLearned(vars, id);
      boolean copyable = MagicLearningStrategy.canCopy(id);
      boolean requirementsMet = contract || MagicLearningStrategy.materialAllowed(vars, id);
      if (!learned || (!contract && MagicProficiencyService.get(vars, id) < 100) || !copyable || !requirementsMet) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.copy_locked"), true);
         return;
      }

      ItemStack paper = items.getStackInSlot(0), ink = items.getStackInSlot(1);
      if (!paper.is(Items.PAPER) || !ink.is(Items.INK_SAC)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.copy_need_material"), true);
         return;
      }

      var pageItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
         net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("typemoonworld", MagicLearningStrategy.pageItemPath(id))
      );
      if (pageItem == Items.AIR) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.copy_locked"), true);
         return;
      }
      ItemStack result = new ItemStack(pageItem);

      paper.shrink(1);
      ink.shrink(1);
      if (!player.addItem(result)) player.drop(result, false);
      blockEntity.setChanged();
      broadcastChanges();
   }

   private void tryCopySelfGeas(ServerPlayer player, int requestedForce) {
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!MagicLearningStrategy.isLearned(vars, "contract_magecraft")) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.copy_locked"), true);
         return;
      }

      ItemStack paper = items.getStackInSlot(0), ink = items.getStackInSlot(1);
      if (!paper.is(Items.PAPER) || !ink.is(Items.INK_SAC)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.copy_need_material"), true);
         return;
      }

      int maxForce = Math.max(0, Math.min(100, (int)Math.floor(MagicProficiencyService.get(vars, "contract_magecraft"))));
      int force = Math.max(0, Math.min(maxForce, requestedForce));
      ItemStack result = net.xxxjk.TYPE_MOON_WORLD.item.custom.SelfGeasScrollItem.create(player, force);
      paper.shrink(1);
      ink.shrink(1);
      if (!player.addItem(result)) player.drop(result, false);
      blockEntity.setChanged();
      broadcastChanges();
   }
}
