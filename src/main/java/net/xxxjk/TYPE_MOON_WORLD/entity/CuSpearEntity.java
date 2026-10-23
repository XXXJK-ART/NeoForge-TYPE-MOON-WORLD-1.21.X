package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.cu.*;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.*;
import net.xxxjk.typemoonworld.api.event.TerrainImpactBlockEvent;
import org.joml.Vector3f;

/** Physical carrier. Only the ledger can return its original item. */
public final class CuSpearEntity extends Entity {
   private static final EntityDataAccessor<Integer> STATE=SynchedEntityData.defineId(CuSpearEntity.class,EntityDataSerializers.INT);
   private static final EntityDataAccessor<ItemStack> APPEARANCE=SynchedEntityData.defineId(CuSpearEntity.class,EntityDataSerializers.ITEM_STACK);
   private static final DustParticleOptions RED=new DustParticleOptions(new Vector3f(.85F,.015F,.05F),1.2F);
   private UUID owner,ticket,target;
   private boolean army,attacked,falling;
   private float damage;
   private double travelled,maxFlightDistance=CuChulainnRules.THROW_RANGE;
   private Vec3 waypoint;
   private Vec3 returnOrigin,returnAxis,returnSide;
   private int returnLeg;
   private final Set<UUID> hit=new HashSet<>();
   public final List<Vec3> trail=new ArrayList<>();
   public CuSpearEntity(EntityType<? extends CuSpearEntity> type,Level level) {super(type,level);}
   @Override protected void defineSynchedData(SynchedEntityData.Builder b) {b.define(STATE,0);b.define(APPEARANCE,ItemStack.EMPTY);}
   public UUID ownerId(){return owner;}
   public UUID ticket(){return ticket;}
   public int state(){return entityData.get(STATE);}
   /** Let the outbound attack reach its target before the NPC requests the paid recall skill. */
   public boolean readyForNpcRecall(){return state()==1 || state()==0 && (attacked || tickCount>=30);}
   public ItemStack appearance(){return entityData.get(APPEARANCE);}
   public void configure(LivingEntity owner,UUID ticket,ItemStack item,boolean army,float damage,LivingEntity target) {
      configure(owner,ticket,item,army,damage,target,null);
   }
   public void configure(LivingEntity owner,UUID ticket,ItemStack item,boolean army,float damage,LivingEntity target,Vec3 fallbackAim) {
      this.owner=owner.getUUID();this.ticket=ticket;this.army=army;this.damage=damage;this.target=target==null?null:target.getUUID();
      if(!army && owner.level() instanceof ServerLevel level)maxFlightDistance=maxFlightDistance(level,owner);
      entityData.set(APPEARANCE,item.copy());
      setPos(owner.getEyePosition()); Vec3 aim=target!=null?target.getBoundingBox().getCenter():fallbackAim!=null?fallbackAim:owner.getEyePosition().add(owner.getLookAngle().scale(48));
      setDeltaMovement(aim.subtract(position()).normalize().scale(army?2.65:2.4));
   }
   private static double maxFlightDistance(ServerLevel level,LivingEntity owner) {
      int viewChunks=level.getServer().getPlayerList().getViewDistance();
      if(owner instanceof ServerPlayer player && player.clientInformation().viewDistance()>0)
         viewChunks=Math.min(viewChunks,player.clientInformation().viewDistance());
      return Math.max(1,viewChunks)*16.0;
   }
   public void beginReturn() {
      if(state()==2 || !CuSpearLedger.valid(this)) return;
      entityData.set(STATE,2); waypoint=null;hit.clear();returnLeg=0;returnOrigin=position();
      returnAxis=null;CuSpearLedger.state(this,CuSpearLedger.State.RETURNING);
   }
   private void embed(Vec3 direction) {
      setYRot((float)Math.toDegrees(Math.atan2(direction.x,direction.z)));
      setXRot((float)Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
      entityData.set(STATE,1);setDeltaMovement(Vec3.ZERO);refreshDimensions();updateEmbeddedBounds();
      CuSpearLedger.state(this,CuSpearLedger.State.EMBEDDED);
   }
   private void updateEmbeddedBounds() {
      Vec3 direction=Vec3.directionFromRotation(-getXRot(),-getYRot());
      setBoundingBox(new AABB(position(),position().subtract(direction.scale(2.2))).inflate(.4));
   }
   @Override public boolean isPickable(){return true;}
   @Override public EntityDimensions getDimensions(Pose pose){return state()==1?EntityDimensions.fixed(.8F,2.2F):super.getDimensions(pose);}
   @Override public void onSyncedDataUpdated(EntityDataAccessor<?> data){super.onSyncedDataUpdated(data);if(STATE.equals(data))refreshDimensions();}
   @Override public InteractionResult interactAt(Player player,Vec3 at,InteractionHand hand){return interact(player,hand);}
   @Override public InteractionResult interact(Player player,InteractionHand hand) {
      if(level().isClientSide()) return InteractionResult.SUCCESS;
      if(!CuSpearLedger.valid(this)) {discard();return InteractionResult.CONSUME;}
      if(player.getUUID().equals(owner) && distanceTo(player)<=3 && state()!=2) CuSpearLedger.deliver(this,player);
      else beginReturn();
      return InteractionResult.CONSUME;
   }
   @Override public void tick() {
      super.tick();
      if(state()==1)updateEmbeddedBounds();
      if(level().isClientSide()) {
         trail.add(position());while(trail.size()>28) trail.removeFirst();return;
      }
      if(!(level() instanceof ServerLevel level)) return;
      if(!CuSpearLedger.valid(this)){discard();return;}
      Entity entity=level.getEntity(owner);
      if(!(entity instanceof LivingEntity wielder) || !wielder.isAlive()) {CuSpearLedger.pending(this);discard();return;}
      if(state()==1) return;
      Vec3 from=position(),next;
      if(state()==2) {
         Vec3 goal=wielder.getEyePosition().add(0,-.35,0);double remaining=from.distanceTo(goal);
         if(remaining<1.2){CuSpearLedger.deliver(this,wielder);return;}
         if(remaining<=3) waypoint=goal;
         else if(waypoint==null || from.distanceTo(waypoint)<.3) {
            // Anchor the zigzag to one axis so each leg crosses the full width.
            if(returnAxis==null){returnOrigin=from;returnAxis=goal.subtract(from).normalize();returnSide=new Vec3(-returnAxis.z,0,returnAxis.x).normalize();}
            double length=returnOrigin.distanceTo(goal),advance=Math.min((returnLeg+1)*8,length-3);
            if(returnLeg*8>=length-3)waypoint=goal;
            else {
               double width=Math.min(6,length*.3);
               waypoint=returnOrigin.add(returnAxis.scale(advance)).add(returnSide.scale((returnLeg++%2==0?1:-1)*width));
            }
         }
         Vec3 move=waypoint.subtract(from);next=from.add(move.normalize().scale(Math.min(2.4,move.length())));
         scan(wielder,from,next,true);breakPath(level,wielder,from,next);redTrail(level,from,next);
      } else {
         if(army && !attacked && target!=null && level.getEntity(target) instanceof LivingEntity living && CuChulainnAbilityService.enemy(wielder,living)) {
            Vec3 direction=living.getBoundingBox().getCenter().subtract(from);
            setDeltaMovement(getDeltaMovement().scale(.28).add(direction.normalize().scale(2.65*.72)));
            if(direction.length()<=5){split(level,wielder,living);attacked=true;falling=true;setDeltaMovement(0,-.2,0);}
         }
         if(army && falling) setDeltaMovement(getDeltaMovement().add(0,-.06,0));
         else if(!army) setDeltaMovement(getDeltaMovement().add(0,-.005,0));
         Vec3 motion=getDeltaMovement();
         double rangeLimit=army?CuChulainnRules.THROW_RANGE:maxFlightDistance;
         if(!falling && travelled+motion.length()>rangeLimit)motion=motion.normalize().scale(Math.max(0,rangeLimit-travelled));
         next=from.add(motion);travelled+=from.distanceTo(next);
         var block=level.clip(new ClipContext(from,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
         Vec3 collisionEnd=block.getType()==HitResult.Type.BLOCK?block.getLocation():next;
         if(!army&&!attacked && scan(wielder,from,collisionEnd,false)) attacked=true;
         if(block.getType()==HitResult.Type.BLOCK) {
            setPos(block.getLocation()); if(army&&!attacked){split(level,wielder,null);attacked=true;}
            Vec3 direction=next.subtract(from);
            if(direction.lengthSqr()<1.0E-8)direction=Vec3.atLowerCornerOf(block.getDirection().getOpposite().getNormal());
            embed(direction);return;
         }
         if(army && (travelled>=CuChulainnRules.THROW_RANGE || tickCount>=100)) {
            if(!attacked){split(level,wielder,null);attacked=true;}
            falling=true;setDeltaMovement(0,-.4,0);
         } else if(!army && travelled>=maxFlightDistance) {
            setPos(next);beginReturn();return;
         }
         if(next.y<level.getMinBuildHeight()) {
            if(army){CuSpearLedger.pending(this);discard();}
            else {setPos(next);beginReturn();}
            return;
         }
      }
      if(state()==2)setDeltaMovement(next.subtract(from));
      setPos(next);
      Vec3 d=next.subtract(from);setYRot((float)Math.toDegrees(Math.atan2(d.x,d.z)));setXRot((float)Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));
   }
   private boolean scan(LivingEntity wielder,Vec3 from,Vec3 to,boolean returning) {
      var targets=level().getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(.45),e->CuChulainnAbilityService.enemy(wielder,e)
         && !hit.contains(e.getUUID()) && e.getBoundingBox().inflate(.3).clip(from,to).isPresent());
      targets.sort(Comparator.comparingDouble(e->e.distanceToSqr(from)));
      for(var living:targets) {
         if(!hit.add(living.getUUID()))continue;
         CuChulainnAbilityService.hit(wielder,living,24,returning?"cu_recall":"cu_throw",false);
         if(!returning)return true;
      }
      return false;
   }
   private void split(ServerLevel level,LivingEntity wielder,LivingEntity target) {
      // Retain the existing ten-way attack, tracking, range and per-cast damage budget.
      var attack=new GaeBulgArmyProjectileEntity(level,wielder);
      attack.setArmyDamage(damage);attack.setTrackedTarget(target);attack.setPos(position());
      attack.setDeltaMovement(getDeltaMovement());attack.releasePhysicalCarrierSplits(level,target);
   }
   private void redTrail(ServerLevel level,Vec3 from,Vec3 to){for(int i=0;i<=12;i++){Vec3 p=from.lerp(to,i/12.0);level.sendParticles(RED,p.x,p.y,p.z,1,0,0,0,0);}}
   private void breakPath(ServerLevel level,LivingEntity owner,Vec3 from,Vec3 to) {
      if(!Config.terrainDestructionEnabled || owner instanceof Player && !Config.playerTerrainDestructionEnabled
         || !(owner instanceof Player) && (!Config.npcTerrainDestructionEnabled || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)))return;
      Set<BlockPos> scanned=new HashSet<>();
      for(double t=0;t<=1;t+=.08) {
         BlockPos pos=BlockPos.containing(from.lerp(to,t));if(!scanned.add(pos)||!level.hasChunkAt(pos))continue;
         var state=level.getBlockState(pos);float hardness=state.getDestroySpeed(level,pos);
         if(state.isAir()||hardness<0||level.getBlockEntity(pos)!=null||!CombatTerrainProtection.canRemove(level,pos))continue;
         if(NeoForge.EVENT_BUS.post(new TerrainImpactBlockEvent(level,owner,pos,state,new TerrainImpactProfile(TerrainImpactProfile.Tier.CHIP,.45F,Float.MAX_VALUE,0,0),"CU_RECALL")).isCanceled())continue;
         if(owner instanceof net.minecraft.server.level.ServerPlayer player && (!level.mayInteract(player,pos)
            || NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level,pos,state,player)).isCanceled()))continue;
         level.destroyBlock(pos,false,owner);
      }
   }
   @Override protected void readAdditionalSaveData(CompoundTag tag) {
      owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;ticket=tag.hasUUID("Ticket")?tag.getUUID("Ticket"):null;
      target=tag.hasUUID("Target")?tag.getUUID("Target"):null;entityData.set(STATE,tag.getInt("State"));
      entityData.set(APPEARANCE,ItemStack.parseOptional(registryAccess(),tag.getCompound("Appearance")));
      army=tag.getBoolean("Army");attacked=tag.getBoolean("Attacked");falling=tag.getBoolean("Falling");damage=tag.getFloat("Damage");travelled=tag.getDouble("Travelled");
      maxFlightDistance=tag.contains("MaxFlightDistance")?Math.max(1,tag.getDouble("MaxFlightDistance")):CuChulainnRules.THROW_RANGE;
   }
   @Override protected void addAdditionalSaveData(CompoundTag tag) {
      if(owner!=null)tag.putUUID("Owner",owner);if(ticket!=null)tag.putUUID("Ticket",ticket);if(target!=null)tag.putUUID("Target",target);
      tag.putInt("State",state());tag.put("Appearance",appearance().saveOptional(registryAccess()));
      tag.putBoolean("Army",army);tag.putBoolean("Attacked",attacked);tag.putBoolean("Falling",falling);tag.putFloat("Damage",damage);tag.putDouble("Travelled",travelled);
      tag.putDouble("MaxFlightDistance",maxFlightDistance);
   }
}
