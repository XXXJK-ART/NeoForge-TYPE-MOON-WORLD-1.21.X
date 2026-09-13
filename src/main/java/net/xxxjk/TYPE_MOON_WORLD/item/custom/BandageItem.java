package net.xxxjk.TYPE_MOON_WORLD.item.custom;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public class BandageItem extends Item {
   public BandageItem(Properties p){super(p.stacksTo(16));}
   @Override public UseAnim getUseAnimation(ItemStack s){return UseAnim.BOW;}
   @Override public int getUseDuration(ItemStack s,LivingEntity e){return 32;}
   @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
      if(p.getHealth()>=p.getMaxHealth())return InteractionResultHolder.fail(p.getItemInHand(h));
      p.startUsingItem(h);return InteractionResultHolder.consume(p.getItemInHand(h));
   }
   @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity e){
      if(!l.isClientSide && e instanceof Player p && p.isAlive()){p.heal(5);if(!p.getAbilities().instabuild)s.shrink(1);}return s;
   }
}