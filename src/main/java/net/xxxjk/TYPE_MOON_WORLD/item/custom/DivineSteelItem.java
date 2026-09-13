package net.xxxjk.TYPE_MOON_WORLD.item.custom;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
public class DivineSteelItem extends Item {
   public DivineSteelItem(Properties p){super(p.stacksTo(1));}
   public static int progress(ItemStack s){return Math.max(1,Math.min(64,s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("progress")));}
   public static boolean isComplete(ItemStack s){return s.is(ModItems.DIVINE_STEEL.get()) && progress(s)==64;}
   public static void setProgress(ItemStack s,int n){CompoundTag t=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();t.putInt("progress",Math.max(1,Math.min(64,n)));s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
   @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){
      ItemStack s=p.getItemInHand(hand);
      ItemStack mana=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
      if(progress(s)>=64 || !mana.is(ModItems.MAGIC_FRAGMENTS.get()))return InteractionResultHolder.fail(s);
      if(!l.isClientSide){mana.shrink(1);setProgress(s,progress(s)+1);}
      return InteractionResultHolder.sidedSuccess(s,l.isClientSide);
   }
   @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> t,TooltipFlag f){
      t.add(Component.translatable("tooltip.typemoonworld.divine_steel.progress",progress(s)));t.add(Component.translatable("tooltip.typemoonworld.divine_steel.infuse"));
   }
}
