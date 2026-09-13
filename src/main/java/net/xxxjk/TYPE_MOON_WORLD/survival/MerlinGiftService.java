package net.xxxjk.TYPE_MOON_WORLD.survival;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;

public final class MerlinGiftService {
   public static final String HERO="TypeMoonHeroCreation", KING="TypeMoonKingQualification";
   public static final double HERO_CHANCE=.10;
   private record Blessing(Holder<MobEffect> effect,int amplifier) {}
   // Highest levels obtainable from vanilla potions, beacons or raids; not arbitrary command amplifiers.
   private static final List<Blessing> BLESSINGS=List.of(new Blessing(MobEffects.REGENERATION,1),new Blessing(MobEffects.MOVEMENT_SPEED,1),
      new Blessing(MobEffects.DAMAGE_BOOST,1),new Blessing(MobEffects.DIG_SPEED,1),new Blessing(MobEffects.JUMP,1),
      new Blessing(MobEffects.DAMAGE_RESISTANCE,3),new Blessing(MobEffects.FIRE_RESISTANCE,0),new Blessing(MobEffects.WATER_BREATHING,0),
      new Blessing(MobEffects.NIGHT_VISION,0),new Blessing(MobEffects.INVISIBILITY,0),new Blessing(MobEffects.SLOW_FALLING,0),
      new Blessing(MobEffects.ABSORPTION,3),new Blessing(MobEffects.LUCK,0),new Blessing(MobEffects.HERO_OF_THE_VILLAGE,4));
   private MerlinGiftService() {}
   public static boolean isKing(Player p){return p.getPersistentData().getBoolean(KING);}
   public static boolean offer(ServerPlayer player,ItemStack flowers){
      if(flowers.getCount()<10 || !flowers.is(ItemTags.FLOWERS)) return false;
      flowers.shrink(10);
      if(player.getRandom().nextDouble()<HERO_CHANCE){
         player.getPersistentData().putBoolean(HERO,true); player.getPersistentData().putBoolean(KING,true);
         PassiveService.reconcileAttributes(player,player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
         player.displayClientMessage(Component.translatable("message.typemoonworld.merlin.hero_creation"),false);
      } else {
         int a=player.getRandom().nextInt(BLESSINGS.size()), b=(a+1+player.getRandom().nextInt(BLESSINGS.size()-1))%BLESSINGS.size();
         for(int index:new int[]{a,b}) { Blessing gift=BLESSINGS.get(index); player.addEffect(new MobEffectInstance(gift.effect(),3600,gift.amplifier())); }
         player.displayClientMessage(Component.translatable("message.typemoonworld.merlin.blessing"),false);
      }
      return true;
   }
   public static void reconcile(ServerPlayer p, boolean suppressed){
      boolean enabled=p.getPersistentData().getBoolean(HERO) && !suppressed;
      modifier(p,Attributes.MAX_HEALTH,"hero_creation_health",enabled?20:0,AttributeModifier.Operation.ADD_VALUE);
      modifier(p,Attributes.MOVEMENT_SPEED,"hero_creation_speed",enabled?.4:0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      modifier(p,Attributes.JUMP_STRENGTH,"hero_creation_jump",enabled?.1:0,AttributeModifier.Operation.ADD_VALUE);
   }
   private static void modifier(ServerPlayer p,Holder<Attribute> attr,String key,double amount,AttributeModifier.Operation operation){
      AttributeInstance attribute=p.getAttribute(attr); if(attribute==null)return;
      ResourceLocation id=ResourceLocation.fromNamespaceAndPath("typemoonworld",key);
      AttributeModifier current=attribute.getModifier(id);
      if(current!=null && current.amount()==amount && current.operation()==operation)return;
      attribute.removeModifier(id);
      if(amount!=0)attribute.addTransientModifier(new AttributeModifier(id,amount,operation));
   }
}
