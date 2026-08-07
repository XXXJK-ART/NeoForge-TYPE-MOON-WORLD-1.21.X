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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.MagicResearchTableBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModMenus;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class MagicResearchTableMenu extends AbstractContainerMenu {
   private final MagicResearchTableBlockEntity blockEntity; private final ItemStackHandler items; private final ContainerLevelAccess access;
   public MagicResearchTableMenu(int id, Inventory inv, FriendlyByteBuf buf) { this(id, inv, resolve(inv.player.level(), buf)); }
   private MagicResearchTableMenu(int id, Inventory inv, MagicResearchTableBlockEntity be) { this(id,inv,be,be.getBlockPos()); }
   public MagicResearchTableMenu(int id, Inventory inv, MagicResearchTableBlockEntity be, BlockPos pos) {
      super((MenuType)TypeMoonWorldModMenus.MAGIC_RESEARCH_TABLE.get(), id); blockEntity=be; items=be.getItems(); access=ContainerLevelAccess.create(inv.player.level(),pos);
      addSlot(new SlotItemHandler(items,0,26,44) {
         @Override public boolean mayPlace(ItemStack stack) {
            if (stack.is(ModItems.UNKNOWN_MAGIC_PAGE.get())) return true;
            var key=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            return "typemoonworld".equals(key.getNamespace()) && MagicLearningStrategy.isConcretePagePath(key.getPath());
         }
      });
      for(int r=0;r<3;r++) for(int c=0;c<9;c++) addSlot(new Slot(inv,c+r*9+9,13+c*18,136+r*18));
      for(int c=0;c<9;c++) addSlot(new Slot(inv,c,13+c*18,194));
   }
   private static MagicResearchTableBlockEntity resolve(Level level,FriendlyByteBuf buf){ BlockPos p=buf==null?BlockPos.ZERO:buf.readBlockPos(); return level.getBlockEntity(p) instanceof MagicResearchTableBlockEntity be?be:new MagicResearchTableBlockEntity(p,ModBlocks.MAGIC_RESEARCH_TABLE.get().defaultBlockState()); }
   public boolean stillValid(Player p){ return stillValid(access,p,(Block)ModBlocks.MAGIC_RESEARCH_TABLE.get()); }
   @Override public ItemStack quickMoveStack(Player player,int index){
      Slot slot=getSlot(index); if(!slot.hasItem()) return ItemStack.EMPTY;
      ItemStack source=slot.getItem(),copy=source.copy();
      if(index==0){if(!moveItemStackTo(source,1,37,true))return ItemStack.EMPTY;}
      else if(getSlot(0).mayPlace(source)){if(!moveItemStackTo(source,0,1,false))return ItemStack.EMPTY;}
      else if(index<28){if(!moveItemStackTo(source,28,37,false))return ItemStack.EMPTY;}
      else if(!moveItemStackTo(source,1,28,false))return ItemStack.EMPTY;
      if(source.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
      if(source.getCount()==copy.getCount())return ItemStack.EMPTY;slot.onTake(player,source);return copy;
   }
   public void tryResearch(ServerPlayer player,String id){
      var vars=player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES); if(id==null||id.isBlank()||!MagicLearningStrategy.canResearch(id)||!vars.learned_magics.contains(id)){ player.displayClientMessage(Component.translatable("message.typemoonworld.magic.research_locked"),true); return; }
      ItemStack in=items.getStackInSlot(0); boolean analysis="magic_analysis".equals(id); if(analysis){ if(!in.is(ModItems.UNKNOWN_MAGIC_PAGE.get())){player.displayClientMessage(Component.translatable("message.typemoonworld.magic.research_need_unknown"),true);return;} } else if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(in.getItem()).getPath().equals(MagicLearningStrategy.pageItemPath(id))){player.displayClientMessage(Component.translatable("message.typemoonworld.magic.research_need_page"),true);return;}
      double proficiency=MagicProficiencyService.get(vars,id); double cost=MagicLearningStrategy.researchManaCost(id,proficiency); if(vars.player_mana<cost){player.displayClientMessage(Component.translatable("message.typemoonworld.magic.insufficient_mana"),true);return;} int ticks=MagicLearningStrategy.researchTicks(id,proficiency);if(!blockEntity.start(id,player.getUUID(),ticks)){player.displayClientMessage(Component.translatable("message.typemoonworld.magic.research_busy"),true);return;}vars.player_mana-=cost;in.shrink(1);vars.syncPlayerVariables(player);blockEntity.setChanged();broadcastChanges();
   }
}
