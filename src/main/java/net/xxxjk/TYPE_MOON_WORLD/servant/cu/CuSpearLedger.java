package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.xxxjk.TYPE_MOON_WORLD.entity.CuSpearEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;

/** Overworld authority. A visual entity never owns a redeemable item. */
public final class CuSpearLedger extends SavedData {
   public enum State { IN_HAND, FLYING, EMBEDDED, RETURNING, PENDING }
   public static final class Entry {
      public final UUID owner, ticket;
      public UUID entity;
      public final ItemStack item;
      public final InteractionHand hand;
      public State state;
      public String dimension;
      Entry(UUID owner,UUID ticket,UUID entity,ItemStack item,InteractionHand hand,State state,String dimension) {
         this.owner=owner; this.ticket=ticket; this.entity=entity; this.item=item; this.hand=hand; this.state=state; this.dimension=dimension;
      }
   }
   private final Map<UUID,Entry> entries=new HashMap<>();
   public CuSpearLedger() { }
   public static CuSpearLedger load(CompoundTag tag,HolderLookup.Provider lookup) {
      var ledger=new CuSpearLedger();
      for(var element:tag.getList("Spears",Tag.TAG_COMPOUND)) {
         var row=(CompoundTag)element;
         ItemStack item=ItemStack.parseOptional(lookup,row.getCompound("Item"));
         if(item.isEmpty()) continue;
         var entry=new Entry(row.getUUID("Owner"),row.getUUID("Ticket"),null,item,
            row.getBoolean("Offhand")?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,State.PENDING,row.getString("Dimension"));
         ledger.entries.put(entry.owner,entry);
      }
      ledger.setDirty(); return ledger;
   }
   public static CuSpearLedger get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(CuSpearLedger::new,CuSpearLedger::load),"cu_unique_spears");
   }
   @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup) {
      ListTag rows=new ListTag();
      for(Entry e:entries.values()) {
         if(e.state==State.IN_HAND || e.item.isEmpty()) continue;
         CompoundTag row=new CompoundTag(); row.putUUID("Owner",e.owner); row.putUUID("Ticket",e.ticket);
         if(e.entity!=null) row.putUUID("Entity",e.entity);
         row.put("Item",e.item.save(lookup)); row.putBoolean("Offhand",e.hand==InteractionHand.OFF_HAND);
         row.putString("State",e.state.name()); row.putString("Dimension",e.dimension); rows.add(row);
      }
      tag.put("Spears",rows); return tag;
   }
   public static Entry entry(LivingEntity owner) {
      return owner.level() instanceof ServerLevel level ? get(level).entries.get(owner.getUUID()) : null;
   }
   public static boolean outstanding(LivingEntity owner) { Entry e=entry(owner); return e!=null && e.state!=State.IN_HAND; }
   public static boolean recallable(LivingEntity owner) { Entry e=entry(owner); return e!=null && e.entity!=null && e.state!=State.RETURNING && e.state!=State.PENDING; }
   public static boolean launch(LivingEntity owner,boolean army,float damage,LivingEntity target) {
      if(!(owner.level() instanceof ServerLevel level) || outstanding(owner)) return false;
      InteractionHand hand=CuChulainnAbilityService.spearHand(owner); if(hand==null) return false;
      var spear=ModEntities.CU_PERSISTENT_SPEAR.get().create(level); if(spear==null) return false;
      ItemStack item=owner.getItemInHand(hand).copyWithCount(1);
      Entry entry=new Entry(owner.getUUID(),UUID.randomUUID(),spear.getUUID(),item,hand,State.FLYING,level.dimension().location().toString());
      spear.configure(owner,entry.ticket,item,army,damage,target);
      var ledger=get(level); ledger.entries.put(owner.getUUID(),entry); ledger.setDirty();
      owner.getItemInHand(hand).shrink(1);
      if(!level.addFreshEntity(spear)) {
         entry.entity=null; entry.state=State.PENDING; ledger.setDirty(); deliverPending(owner); return false;
      }
      return true;
   }
   public static boolean valid(CuSpearEntity spear) {
      if(!(spear.level() instanceof ServerLevel level)) return true;
      Entry e=get(level).entries.get(spear.ownerId());
      return e!=null && e.ticket.equals(spear.ticket()) && spear.getUUID().equals(e.entity)
         && e.state!=State.PENDING && e.state!=State.IN_HAND;
   }
   public static void state(CuSpearEntity spear,State state) {
      if(!valid(spear)) return;
      var ledger=get((ServerLevel)spear.level()); ledger.entries.get(spear.ownerId()).state=state; ledger.setDirty();
   }
   public static void pending(CuSpearEntity spear) {
      if(!valid(spear)) return;
      var ledger=get((ServerLevel)spear.level()); var e=ledger.entries.get(spear.ownerId());
      e.state=State.PENDING; e.entity=null; ledger.setDirty();
   }
   public static void recall(LivingEntity owner) {
      Entry e=entry(owner); if(e==null || e.entity==null) return;
      var entity=((ServerLevel)owner.level()).getEntity(e.entity);
      if(entity instanceof CuSpearEntity spear && valid(spear)) spear.beginReturn();
      else {e.state=State.PENDING;e.entity=null;get((ServerLevel)owner.level()).setDirty();}
   }
   public static void deliver(CuSpearEntity spear,LivingEntity owner) {
      pending(spear); spear.discard(); deliverPending(owner);
   }
   public static void deliverPending(LivingEntity owner) {
      if(!(owner.level() instanceof ServerLevel level)) return;
      Entry e=entry(owner); if(e==null) return;
      if(e.state!=State.PENDING && !e.dimension.equals(level.dimension().location().toString())) {
         e.state=State.PENDING; e.entity=null; get(level).setDirty();
      }
      if(e.state!=State.PENDING || !owner.isAlive() || owner.isRemoved() || owner instanceof ServerPlayer p && p.hasDisconnected()) return;
      if(ServantCardTransformManager.shouldDeleteBoundDrop(e.item) && !CuChulainnAbilityService.isOwner(owner)) { get(level).entries.remove(e.owner);get(level).setDirty();return; }
      ItemStack copy=e.item.copy();
      if(owner.getItemInHand(e.hand).isEmpty()) { owner.setItemInHand(e.hand,copy); copy=ItemStack.EMPTY; }
      else if(owner instanceof ServerPlayer p) {
         // Inventory.add discards overflow in creative mode; a unique spear must stay pending.
         int slot=p.getInventory().getFreeSlot();
         if(slot>=0) {p.getInventory().setItem(slot,copy);copy=ItemStack.EMPTY;}
      }
      if(copy.isEmpty()) { get(level).entries.remove(e.owner); get(level).setDirty(); }
   }
   public static void ownerLeft(LivingEntity owner) {
      Entry e=entry(owner); if(e!=null) {e.state=State.PENDING;e.entity=null;get((ServerLevel)owner.level()).setDirty();}
   }
   public static void releaseForm(LivingEntity owner) {
      Entry e=entry(owner); if(e==null) return;
      if(ServantCardTransformManager.shouldDeleteBoundDrop(e.item)) {get((ServerLevel)owner.level()).entries.remove(e.owner);get((ServerLevel)owner.level()).setDirty();}
      else ownerLeft(owner);
   }
}
