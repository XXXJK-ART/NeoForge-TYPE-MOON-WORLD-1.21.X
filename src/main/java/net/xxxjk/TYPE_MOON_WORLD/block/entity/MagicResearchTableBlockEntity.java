package net.xxxjk.TYPE_MOON_WORLD.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicResearchTableMenu;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MagicResearchTableBlockEntity extends BlockEntity implements MenuProvider {
   private final ItemStackHandler items = new ItemStackHandler(1) { @Override protected void onContentsChanged(int slot) { setChanged(); } };
   private String researchMagicId = "";
   private UUID researcher;
   private int remainingTicks;
   private int totalTicks;
   public MagicResearchTableBlockEntity(BlockPos pos, BlockState state) { super((BlockEntityType)ModBlockEntities.MAGIC_RESEARCH_TABLE.get(), pos, state); }
   public ItemStackHandler getItems() { return items; }
   public boolean isRunning() { return remainingTicks > 0; }
   public boolean start(String magicId, UUID playerId, int ticks) { if (isRunning()) return false; researchMagicId=magicId; researcher=playerId; remainingTicks=totalTicks=Math.max(1,ticks); setChanged(); return true; }
   public static void tick(Level level, BlockPos pos, BlockState state, MagicResearchTableBlockEntity be) {
      if (level.isClientSide || !be.isRunning()) return;
      if (--be.remainingTicks > 0) { if (be.remainingTicks % 20 == 0) be.setChanged(); return; }
      if (level instanceof ServerLevel server && be.researcher != null) {
         ServerPlayer player=server.getServer().getPlayerList().getPlayer(be.researcher);
         if (player != null) {
            var vars=player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if ("magic_analysis".equals(be.researchMagicId)) {
               ItemStack out;
               if (player.getRandom().nextDouble() < .10) out=new ItemStack(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.MAGIC_WASTE_PAPER.get());
               else {
                  java.util.List<net.minecraft.world.item.Item> pages=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(item->{var key=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);return "typemoonworld".equals(key.getNamespace())&&MagicLearningStrategy.isConcretePagePath(key.getPath());}).toList();
                  out=pages.isEmpty()?new ItemStack(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.MAGIC_PAGE_PROJECTION.get()):new ItemStack(pages.get(player.getRandom().nextInt(pages.size())));
               }
               if(!player.addItem(out)) player.drop(out,false);
            } else net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.add(vars,be.researchMagicId,Math.max(.1,5.0-net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy.complexity(be.researchMagicId)*.03));
            vars.syncPlayerVariables(player);
         }
      }
      be.researchMagicId=""; be.researcher=null; be.totalTicks=0; be.setChanged();
   }
   @Override protected void saveAdditional(@NotNull CompoundTag tag, @NotNull Provider provider) { super.saveAdditional(tag, provider); tag.put("Items", items.serializeNBT(provider)); tag.putString("ResearchMagic",researchMagicId); if(researcher!=null)tag.putUUID("Researcher",researcher); tag.putInt("Remaining",remainingTicks);tag.putInt("Total",totalTicks); }
   @Override protected void loadAdditional(@NotNull CompoundTag tag, @NotNull Provider provider) { super.loadAdditional(tag, provider); if (tag.contains("Items")) items.deserializeNBT(provider, tag.getCompound("Items")); researchMagicId=tag.getString("ResearchMagic"); researcher=tag.hasUUID("Researcher")?tag.getUUID("Researcher"):null;remainingTicks=tag.getInt("Remaining");totalTicks=tag.getInt("Total"); }
   @Override public Component getDisplayName() { return Component.translatable("block.typemoonworld.magic_research_table"); }
   @Nullable @Override public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) { return new MagicResearchTableMenu(id, inv, this, worldPosition); }
}
