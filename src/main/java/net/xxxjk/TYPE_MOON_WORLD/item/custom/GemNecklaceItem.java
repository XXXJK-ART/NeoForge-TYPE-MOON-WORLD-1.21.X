package net.xxxjk.TYPE_MOON_WORLD.item.custom;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
public class GemNecklaceItem extends Item {
   public static final int CAPACITY=10000, TRANSFER=100;
   public GemNecklaceItem(Properties p){super(p.stacksTo(1));}
   public static int stored(ItemStack s){return Math.max(0,Math.min(CAPACITY,s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("mana")));}
   public static boolean transfer(ServerPlayer p,ItemStack s,boolean deposit){
      if(!p.isAlive() || !s.is(ModItems.GEM_NECKLACE.get()) || s!=p.getMainHandItem() && s!=p.getOffhandItem())return false;
      var v=p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);int n=stored(s);
      double mana=v.servant_card_transformed?v.servant_card_mana:v.player_mana;
      double cap=v.servant_card_transformed?v.servant_card_max_mana:v.player_max_mana;
      if(!Double.isFinite(mana) || !Double.isFinite(cap))return false;
      if(deposit ? mana<TRANSFER || n>CAPACITY-TRANSFER : n<TRANSFER || mana+TRANSFER>cap)return false;
      int delta=deposit?TRANSFER:-TRANSFER;
      if(v.servant_card_transformed)v.servant_card_mana-=delta;else v.player_mana-=delta;
      CompoundTag tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt("mana",n+delta);
      s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));v.syncMana(p);p.getInventory().setChanged();return true;
   }
   @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
      ItemStack s=p.getItemInHand(h);
      if(p instanceof ServerPlayer sp && !transfer(sp,s,false))return InteractionResultHolder.fail(s);
      return InteractionResultHolder.sidedSuccess(s,l.isClientSide);
   }
   @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> l,TooltipFlag f){l.add(Component.translatable("tooltip.typemoonworld.necklace.mana",stored(s),CAPACITY));}
}