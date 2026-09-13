package net.xxxjk.TYPE_MOON_WORLD.survival;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.*;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.church.ChurchExecutorEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;

@EventBusSubscriber(modid="typemoonworld")
public final class SurvivalEvents {
   public static final float BOW_BREAK_CHANCE=.10F, RIN_NECKLACE_CHANCE=.30F, MERCURY_CHANCE=.20F, DRAGON_FANG_CHANCE=.30F;
   private SurvivalEvents(){}
   @SubscribeEvent(priority=EventPriority.LOWEST)
   public static void bowLoose(ArrowLooseEvent e){
      ItemStack bow=e.getBow();
      if(e.getLevel().isClientSide || !e.hasAmmo() || BowItem.getPowerForTime(e.getCharge())<.1F
         || !bow.isDamageableItem() || bow.getMaxDamage()-bow.getDamageValue()>=10 || bow.getMaxDamage()-bow.getDamageValue()<=0) return;
      if(e.getEntity().getRandom().nextFloat()>=BOW_BREAK_CHANCE)return;
      // Cancel the release because the string snapped. Never reward canceled or uncharged releases.
      e.setCanceled(true); bow.shrink(1);
      ItemStack string=new ItemStack(ModItems.BROKEN_BOWSTRING.get());if(!e.getEntity().addItem(string))e.getEntity().drop(string,false);
      e.getEntity().playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK);
   }
   @SubscribeEvent public static void clone(PlayerEvent.Clone e){
      for(String key:new String[]{MerlinGiftService.HERO,MerlinGiftService.KING,"TypeMoonBizenTsubaClaimed"})
         if(e.getOriginal().getPersistentData().getBoolean(key))e.getEntity().getPersistentData().putBoolean(key,true);
   }
   @SubscribeEvent(priority=EventPriority.HIGH) public static void trade(PlayerInteractEvent.EntityInteract e){
      if(!(e.getEntity() instanceof ServerPlayer player) || !(e.getTarget() instanceof Villager v) || !MerlinGiftService.isKing(player))return;
      var gossip=v.getGossips(); var id=player.getUUID();
      gossip.remove(id,GossipType.MAJOR_NEGATIVE);gossip.remove(id,GossipType.MINOR_NEGATIVE);
      gossip.add(id,GossipType.MAJOR_POSITIVE,100);gossip.add(id,GossipType.MINOR_POSITIVE,200);gossip.add(id,GossipType.TRADING,25);
   }
   @SubscribeEvent public static void drops(LivingDropsEvent e){
      LivingEntity mob=e.getEntity();if(mob.level().isClientSide)return;
      String namespace=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace();
      if(mob instanceof Villager || namespace.equals("typemoonworld")){
         int min=1,max=3;
         if(mob instanceof SeaBeastEntity){min=5;max=20;}
         else if(mob instanceof ServantEntity){min=10;max=40;}
         else if(mob instanceof DeadApostleEntity)max=10;
         drop(e,ModItems.MAGIC_FRAGMENTS.get(),min+mob.getRandom().nextInt(max-min+1));
      }
      if(mob instanceof SeaBeastEntity){drop(e,ModItems.EVIL_BONE.get(),1);drop(e,Items.BEEF,1+mob.getRandom().nextInt(3));}
      if(mob instanceof DragonfangSoldierEntity)chance(e,ModItems.DRAGON_FANG.get(),DRAGON_FANG_CHANCE);
      if(mob instanceof TohsakaRinEntity)chance(e,ModItems.GEM_NECKLACE.get(),RIN_NECKLACE_CHANCE);
      if(mob instanceof TohsakaRinEntity || mob instanceof MysticMagicianEntity)chance(e,ModItems.MERCURY_SWORD.get(),MERCURY_CHANCE);
      if(mob instanceof KendoMasterEntity k && k.school()==KendoSchool.TENNEN)chance(e,ModItems.RELIC_OKITA_KATANA.get(),.50F);
      if(mob instanceof ChurchExecutorEntity){chance(e,ModItems.RELIC_GILGAMESH_ASH_URN.get(),.10F);chance(e,ModItems.RELIC_ROUND_TABLE_FRAGMENT.get(),.05F);chance(e,ModItems.RELIC_APOCALYPSE_PAGE.get(),.10F);}
   }
   private static void chance(LivingDropsEvent e,Item i,float chance){if(e.getEntity().getRandom().nextFloat()<chance)drop(e,i,1);}
   private static void drop(LivingDropsEvent e,Item i,int count){var m=e.getEntity();e.getDrops().add(new ItemEntity(m.level(),m.getX(),m.getY(),m.getZ(),new ItemStack(i,count)));}
}
