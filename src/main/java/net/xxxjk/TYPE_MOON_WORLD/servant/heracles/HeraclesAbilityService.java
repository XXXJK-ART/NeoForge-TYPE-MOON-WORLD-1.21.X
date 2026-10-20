package net.xxxjk.TYPE_MOON_WORLD.servant.heracles;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTargetingService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantCommandMode;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.BindingService;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;

/** Server executor shared by player input and NPC selection. No weapon requirement or free NPC skills. */
@EventBusSubscriber(modid="typemoonworld")
public final class HeraclesAbilityService {
   public record Spec(String id, String name, int mana, int cooldown, int windup, int recovery) {}
   private static final Spec[] SPECS={
      new Spec("heracles_big_jump","大跳·坠击",18,120,1,4),
      new Spec("heracles_ground_slam","重击·砸地",32,180,6,6),
      new Spec("heracles_shoulder","蛮力·肩撞",22,120,1,4),
      new Spec("heracles_uppercut","崩击·上挑",20,140,2,4),
      new Spec("heracles_sweep","回身·横扫",26,160,3,5),
      new Spec("heracles_throw","追袭·背摔",35,240,3,5),
      new Spec("heracles_stomp","震脚·践踏",18,100,1,4),
      new Spec("heracles_flurry","狂战·连打",30,180,2,4),
      new Spec("heracles_pounce","飞身·扑杀",38,220,3,6),
      new Spec("god_hand_status","十二试炼",0,20,0,0)};
   private static final Map<LivingEntity,Action> RUNNING=new WeakHashMap<>();
   private static final Map<LivingEntity,Thrown> THROWN=new WeakHashMap<>();
   private static final String GRAB="HeraclesGrabbedBy";
   private static final class Action {
      final int slot; final LivingEntity target; final Vec3 direction, destination; final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
      final Set<UUID> hit=new HashSet<>();
      int age, finishAt=Integer.MAX_VALUE, queuedSlot=-1; boolean airborne, grabbed; Vec3 anchor, held;
      Action(int slot,LivingEntity target,Vec3 direction,Vec3 destination,LivingEntity caster) {
         this.slot=slot;this.target=target;this.direction=direction;this.destination=destination;this.dimension=caster.level().dimension();
      }
   }
   private static final class Thrown {
      final LivingEntity owner; final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
      int ticks; boolean airborne; Vec3 previous, lastMotion;
      Thrown(LivingEntity owner,LivingEntity target) { this.owner=owner;dimension=target.level().dimension();previous=target.position();lastMotion=target.getDeltaMovement(); }
   }
   private HeraclesAbilityService() {}
   public static Spec spec(int slot) { return slot>=0&&slot<SPECS.length?SPECS[slot]:null; }
   public static boolean isHeracles(LivingEntity e) { var def=ServantIdentityHelper.definitionOf(e);return def!=null&&"heracles".equals(def.id()); }
   public static boolean busy(LivingEntity e) { return RUNNING.containsKey(e); }
   public static boolean held(LivingEntity e) { return e.getPersistentData().hasUUID(GRAB); }
   public static boolean motionLocked(LivingEntity e) { Action a=RUNNING.get(e);return held(e)||a!=null&&a.grabbed; }
   public static boolean controlled(LivingEntity e) {
      return e.hasEffect(ModMobEffects.PETRIFIED) || e.hasEffect(ModMobEffects.BINDING) || BindingService.isBound(e.getUUID())
         || e.getPersistentData().getLong(ServantCombatSystem.TAG_STUN_UNTIL)>e.level().getGameTime()
         || e.getPersistentData().getLong("ServantCardCombatStunUntil")>e.level().getGameTime();
   }
   public static boolean enemy(LivingEntity owner,LivingEntity target) {
      return target!=null&&target!=owner&&target.isAlive()&&target.level()==owner.level()
         && !EntityUtils.isImmunePlayerTarget(target) && !EntityUtils.isUntargetableServantTransition(target)
         && !owner.isAlliedTo(target)&&!target.isAlliedTo(owner)&&!ServantMasterProtection.isProtectedMaster(owner,target);
   }
   private static boolean visible(LivingEntity owner,LivingEntity target) { return owner.hasLineOfSight(target); }
   private static Vec3 forward(LivingEntity e) { Vec3 v=e.getLookAngle().multiply(1,0,1);return v.lengthSqr()<1e-6?new Vec3(0,0,1):v.normalize(); }
   private static boolean inArc(LivingEntity owner,LivingEntity target,Vec3 facing,double radius,double degrees) {
      Vec3 delta=target.position().subtract(owner.position());
      return delta.horizontalDistanceSqr()<=radius*radius && Math.abs(delta.y)<=2.5
         && (delta.horizontalDistanceSqr()<1e-6 || facing.dot(delta.multiply(1,0,1).normalize())>=Math.cos(Math.toRadians(degrees/2))) && visible(owner,target);
   }
   private static LivingEntity select(LivingEntity caster,int slot) {
      double range=slot==5?12:slot==8?16:slot==2?6:3;
      Vec3 facing=forward(caster);
      if(caster instanceof ServantEntity npc) {
         var target=npc.getTarget();
         return enemy(caster,target)&&caster.distanceTo(target)<=range&&visible(caster,target)
            && (slot!=3 || inArc(caster,target,facing,3,90))?target:null;
      }
      return caster.level().getEntitiesOfClass(LivingEntity.class,caster.getBoundingBox().inflate(range),t->enemy(caster,t)
         &&caster.distanceTo(t)<=range&&visible(caster,t)&&inArc(caster,t,facing,range,slot==3?90:45))
         .stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
   }
   /** Validate a full actor box and a real supporting collision face, including slabs/stairs. */
   private static Vec3 behind(LivingEntity caster,LivingEntity target) {
      Vec3 back=forward(target).scale(-1);
      for(double angle:new double[]{0,30,-30,60,-60}) {
         Vec3 offset=back.yRot((float)Math.toRadians(angle)).scale(1.6), candidate=target.position().add(offset);
         var floor=caster.level().clip(new ClipContext(candidate.add(0,1,0),candidate.add(0,-1.6,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster));
         if(floor.getType()==HitResult.Type.MISS) continue;
         candidate=new Vec3(candidate.x,floor.getLocation().y,candidate.z);
         AABB box=caster.getBoundingBox().move(candidate.subtract(caster.position()));
         if(caster.level().noCollision(caster,box) && !box.intersects(target.getBoundingBox())) return candidate;
      }
      return null;
   }
   private static boolean grabbable(LivingEntity t) {
      return t.getBbWidth()<=2&&t.getBbHeight()<=4&&!held(t)&&!controlled(t)&&!t.isPassenger()
         && !t.getPersistentData().getBoolean("ImmuneToDisplacement") && !t.getPersistentData().getBoolean("TypeMoonDisplacementImmune");
   }
   public static boolean start(LivingEntity caster,int slot) {
      Spec spec=spec(slot);
      if(spec==null||!(caster.level() instanceof ServerLevel)||!isHeracles(caster)||!caster.isAlive()||held(caster)||controlled(caster)
         ||ServantCombatSystem.skillsSuppressed(caster)) return false;
      long now=caster.level().getGameTime();
      if(caster.getPersistentData().getLong("HeraclesCooldown:"+slot)>now) return false;
      Action running=RUNNING.get(caster);
      if(running!=null) {
         // One input may be buffered in the final 0.2 seconds of recovery.
         // It is validated and paid for only when the next action actually starts.
         if(caster instanceof ServerPlayer&&slot<9&&running.finishAt!=Integer.MAX_VALUE&&running.finishAt-running.age<=4) {
            running.queuedSlot=slot;return true;
         }
         return false;
      }
      LivingEntity target=null; Vec3 destination=null;
      if(slot==2||slot==3||slot==5||slot==7||slot==8) {
         target=select(caster,slot);if(target==null)return false;
      }
      if(slot==5) { if(!grabbable(target)||(destination=behind(caster,target))==null)return false; }
      if(slot==8) destination=target.position();
      if(slot==0&&!caster.onGround())return false;
      // Every validation above is read-only; resource payment is the commit point.
      if(caster instanceof ServerPlayer player) {
         var vars=player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if(!ServantCardManaService.consume(player,vars,spec.mana()))return false;
         ServantCardTransformManager.setSkillCooldownUntil(player,vars,slot,now+spec.cooldown());
      } else if(caster instanceof ServantEntity npc) {
         if(npc.getCurrentMp()<spec.mana())return false;
         npc.setCurrentMp(npc.getCurrentMp()-spec.mana());
      }
      caster.getPersistentData().putLong("HeraclesCooldown:"+slot,now+spec.cooldown());
      if(slot==9) { status(caster);return true; }
      Vec3 direction=target==null?forward(caster):target.position().subtract(caster.position()).multiply(1,0,1).normalize();
      RUNNING.put(caster,new Action(slot,target,direction,destination,caster));
      if (caster instanceof ServerPlayer p) {
         animation(p,slot);
         ServantCardVoiceHelper.tryPlaySkill(p,spec.id());
      }
      if(caster instanceof ServerPlayer p)ServantCardActionTimeline.begin(p,spec.id(),spec.windup(),130,spec.recovery());
      if(caster instanceof ServantEntity npc) {
         npc.getNavigation().stop();
         npc.triggerNamedActionAnimation("martial_"+slot);
      }
      return true;
   }
   public static void status(LivingEntity caster) {
      if(caster instanceof ServerPlayer player)player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.god_hand_lives",Math.max(1,GodHandService.read(caster).lives()+1)),true);
      if(caster.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,caster.getX(),caster.getY()+1,caster.getZ(),18,.5,.55,.5,.035);
         level.playSound(null,caster.blockPosition(),SoundEvents.BEACON_AMBIENT,SoundSource.PLAYERS,.55F,.72F);
      }
   }
   public static boolean npcCombat(ServantEntity npc) {
      if(!isHeracles(npc))return false;
      if(busy(npc)||held(npc)) { npc.getNavigation().stop();return true; }
      long now=npc.level().getGameTime();
      var target=ServantTargetingService.refreshOrRecover(npc,now);
      if(!enemy(npc,target)) { npc.setSprinting(false);return false; }
      if(npc.getEntityMaster()!=null&&npc.getCommandMode()==ServantCommandMode.STAY
         &&target.distanceToSqr(Vec3.atCenterOf(npc.getStayAnchor()))>144) {
         npc.setTarget(null);ServantTargetingService.forget(npc);npc.setSprinting(false);return false;
      }
      if(controlled(npc)||ServantCombatSystem.cannotAct(npc)) { npc.getNavigation().stop();return true; }
      npc.getLookControl().setLookAt(target,75,75);
      npc.faceToward(target.position());
      double distance=npc.distanceTo(target);
      boolean sight=visible(npc,target);
      // Select attacks for the current geometry; never jump or slam in place
      // just because every useful gap closer is cooling down.
      if(npc.tickCount%4==0&&sight&&distance<=16) {
         int[] choices=Math.abs(target.getY()-npc.getY())>2?new int[]{8,0}
            :distance<=3?new int[]{7,3,4,6,1}:distance<=5.4?new int[]{2,1,4,5,8}:new int[]{2,5,8};
         int offset=npc.getRandom().nextInt(choices.length);
         for(int i=0;i<choices.length;i++)if(start(npc,choices[(i+offset)%choices.length]))return true;
      }
      // Own the entire combat tick, including low mana and cooldown gaps, so
      // generic duel pacing cannot insert probing, retreats or orbiting.
      double contact=2.4;
      npc.setSprinting(distance>contact);
      if(distance>contact||!sight) {
         ServantNavigationHelper.moveToTargetThrottled(npc,target,1.55,now,5,.12,"HeraclesPursuit");
         if(npc.horizontalCollision||!sight&&npc.getNavigation().isDone()) {
            if(now>=npc.getPersistentData().getLong("HeraclesNextWallBreak")) {
               npc.getPersistentData().putLong("HeraclesNextWallBreak",now+12);
               wall(npc,target.position().subtract(npc.position()));
            }
         }
      } else {
         npc.getNavigation().stop();
         if(now>=npc.getPersistentData().getLong("HeraclesNextBasic")) {
            npc.getPersistentData().putLong("HeraclesNextBasic",now+10);
            npc.doBasicHurtTarget(target);
            npc.triggerBasicAttackAnimation();
         }
      }
      return true;
   }
   public static void cancel(LivingEntity caster) {
      Action a=RUNNING.remove(caster);if(a!=null&&a.target!=null&&a.grabbed)release(a.target);
      if (a != null && caster instanceof ServantEntity npc) npc.triggerNamedActionAnimation("martial_end");
      if (a != null && (a.slot == 0 || a.slot == 8)) resetAir(caster);
      if(caster instanceof ServerPlayer p) {
         ServantCardActionTimeline.clear(p);
         if (a != null) animation(p,-1);
      }
   }
   private static void animation(ServerPlayer player,int slot) {
      var message=new net.xxxjk.TYPE_MOON_WORLD.network.HeraclesActionAnimationMessage(player.getUUID(),slot);
      for(var observer:player.serverLevel().players()) {
         if(observer==player||observer.distanceToSqr(player)<=128*128)
            net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork.sendToPlayer(observer,message);
      }
   }
   private static void release(LivingEntity target) { target.getPersistentData().remove(GRAB); }
   private static void teleport(LivingEntity entity,Vec3 pos) { entity.teleportTo(pos.x,pos.y,pos.z);entity.setDeltaMovement(Vec3.ZERO);entity.hurtMarked=true; }
   private static boolean hit(LivingEntity caster,LivingEntity target,int slot,float damage,double horizontal,double vertical,boolean multi) {
      if(!enemy(caster,target)||!visible(caster,target))return false;
      int old=target.invulnerableTime;
      if(multi)target.invulnerableTime=0;
      var base=caster instanceof ServerPlayer p?caster.damageSources().playerAttack(p):caster.damageSources().mobAttack(caster);
      boolean result;
      try { result=target.hurt(new CombatDamageSource(base,AttackOrigin.skill("heracles",slot<0?"landing":spec(slot).id())),damage); }
      finally { if(multi)target.invulnerableTime=old; }
      if(result && (horizontal!=0||vertical!=0)) {
         Vec3 away=target.position().subtract(caster.position()).multiply(1,0,1);if(away.lengthSqr()<1e-6)away=forward(caster);else away=away.normalize();
         target.setDeltaMovement(away.scale(horizontal).add(0,vertical,0));target.hurtMarked=true;
      }
      return result;
   }
   private static void area(LivingEntity caster,int slot,double radius,double arc,float damage,double horizontal,double vertical) {
      for(var t:caster.level().getEntitiesOfClass(LivingEntity.class,caster.getBoundingBox().inflate(radius,2.5,radius),t->enemy(caster,t))) {
         if(!inArc(caster,t,forward(caster),radius,arc))continue;
         boolean hit=hit(caster,t,slot,damage,horizontal,vertical,false);
         if(hit&&slot==6) {
            if(t instanceof ServantEntity npc)net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPlannedActionExecutor.interrupt(npc,4,caster.level().getGameTime());
            if(busy(t))cancel(t);
            if(t instanceof ServerPlayer p && p.getPersistentData().getBoolean(ServantCardActionTimeline.INTERRUPTIBLE_TAG))ServantCardActionTimeline.clear(p);
         }
      }
   }
   private static void ground(LivingEntity caster,int slot,double radius,float damage,double horizontal,double vertical) {
      area(caster,slot,radius,360,damage,horizontal,vertical);
      if(caster.level() instanceof ServerLevel level) {
         var tier=slot==1?TerrainImpactProfile.Tier.HEAVY:TerrainImpactProfile.Tier.MEDIUM;
         var base=TerrainImpactProfile.of(tier);
         var profile=new TerrainImpactProfile(tier,Math.min(radius,slot==1?3.4:2.4),base.maximumHardness(),64,96);
         TerrainImpactService.impactHeraclesGround(level,caster,caster.position(),profile,caster instanceof ServerPlayer?TerrainImpactService.Permission.PLAYER:TerrainImpactService.Permission.NPC,slot==1||slot==8?2:1);
         impactFx(level,caster.position(),slot==1?1F:slot==6?.55F:.75F);
      }
   }
   private static void recover(LivingEntity e,Action a) {
      if(a.finishAt==Integer.MAX_VALUE)a.finishAt=a.age+spec(a.slot).recovery();
      if(e instanceof ServerPlayer p)ServantCardActionTimeline.recovery(p);
   }
   private static void advance(LivingEntity e,Action a) {
      if(!e.isAlive()||!isHeracles(e)||e.level().dimension()!=a.dimension||controlled(e)||EntityUtils.isUntargetableServantTransition(e)) { cancel(e);return; }
      a.age++; int windup=spec(a.slot).windup();
      if(a.age>=a.finishAt) {
         int queued=a.queuedSlot;
         cancel(e);
         if(queued>=0&&e instanceof ServerPlayer)start(e,queued);
         return;
      }
      if(a.finishAt!=Integer.MAX_VALUE)return;
      if(a.age<windup)return;
      if(e instanceof ServerPlayer p)ServantCardActionTimeline.active(p);
      switch(a.slot) {
         case 0 -> {
            if(a.age==windup) { e.setDeltaMovement(a.direction.scale(1.05).add(0,1.52,0));e.hurtMarked=true;e.fallDistance=0; }
            if(!e.onGround())a.airborne=true;
            if(a.age>240) {cancel(e);break;}
            if(a.airborne&&e.onGround()) { ground(e,0,3.2,26,1.15,.42);recover(e,a);resetAir(e); }
         }
         case 1 -> { ground(e,1,5.4,58,1.85,.72);recover(e,a); }
         case 2 -> {
            if(a.age>=windup+8) { recover(e,a);break; }
            Vec3 before=e.position();e.setDeltaMovement(Vec3.ZERO);e.move(MoverType.SELF,a.direction.scale(.75));e.hurtMarked=true;
            if(a.age%2==0&&e.level() instanceof ServerLevel level) {
               BlockPos feet=BlockPos.containing(before.add(0,-.1,0));
               var state=level.getBlockState(feet);
               if(!state.isAir())level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),before.x,before.y+.1,before.z,10,.45,.12,.45,.16);
               level.sendParticles(ParticleTypes.SMOKE,before.x,before.y+.15,before.z,3,.3,.08,.3,.035);
            }
            AABB path=e.getBoundingBox().minmax(e.getBoundingBox().move(before.subtract(e.position()))).inflate(.5,0,.5);
            for(var t:e.level().getEntitiesOfClass(LivingEntity.class,path,t->enemy(e,t)))if(a.hit.add(t.getUUID()))hit(e,t,2,32,1.25,.26,false);
            if(e.horizontalCollision||e.position().distanceToSqr(before)<.01) { wall(e,a.direction);recover(e,a); }
         }
         case 3 -> { if(enemy(e,a.target)&&inArc(e,a.target,a.direction,3,90))hit(e,a.target,3,36,.55,1.05,false);recover(e,a); }
         case 4 -> { area(e,4,4.5,240,40,1.1,.2);recover(e,a); }
         case 5 -> {
            if(a.age==windup) {
               if(!enemy(e,a.target)||!grabbable(a.target)||!visible(e,a.target)||e.distanceTo(a.target)>12) { recover(e,a);break; }
               Vec3 pos=behind(e,a.target);if(pos==null) {recover(e,a);break;}
               teleport(e,pos);
               if(!hit(e,a.target,5,8,0,0,true)) {recover(e,a);break;}
               a.grabbed=true;a.anchor=e.position();a.held=a.target.position();
               a.target.getPersistentData().putUUID(GRAB,e.getUUID());
            }
            if(a.grabbed) {
               if(!enemy(e,a.target)||a.target.level().dimension()!=a.dimension) { cancel(e);break; }
               if(a.age<windup+6) { teleport(e,a.anchor);teleport(a.target,a.held); }
               else {
                  release(a.target);a.grabbed=false;
                  if(hit(e,a.target,5,20,1.6,.65,true)&&a.target.isAlive())THROWN.put(a.target,new Thrown(e,a.target));
                  recover(e,a);
               }
            }
         }
         case 6 -> { area(e,6,3.5,360,24,.25,.12);recover(e,a); }
         case 7 -> {
            if(!enemy(e,a.target)||e.distanceTo(a.target)>3||!visible(e,a.target)) {recover(e,a);break;}
            int finalHit=windup+6;
            if(a.age==windup||a.age==windup+3||a.age==finalHit)hit(e,a.target,7,a.age==finalHit?30:18,a.age==finalHit?1.2:0,a.age==finalHit?.35:0,true);
            if(a.age>=finalHit)recover(e,a);
         }
         case 8 -> {
            if(a.age==windup)a.anchor=e.position();
            int t=a.age-windup+1;
            double ratio=Math.min(1,t/16.0);
            Vec3 point=a.anchor.lerp(a.destination,ratio).add(0,Math.sin(Math.PI*ratio)*3,0);
            Vec3 before=e.position(),step=point.subtract(before);
            e.setDeltaMovement(Vec3.ZERO);e.move(MoverType.SELF,step);e.hurtMarked=true;e.fallDistance=0;
            if(t>=16||e.position().distanceToSqr(point)>.02||t>3&&e.onGround()) { ground(e,8,4,46,1.2,.35);recover(e,a);resetAir(e); }
         }
      }
   }
   private static void wall(LivingEntity caster,Vec3 direction) {
      if(caster.level() instanceof ServerLevel level) {
         TerrainImpactService.impactWallTunnel(level,caster,caster.position(),direction,new TerrainImpactProfile(TerrainImpactProfile.Tier.MEDIUM,2.4,6,48,80),3,3,(int)Math.ceil(caster.getBbHeight()));
      }
   }
   private static void impactFx(ServerLevel level,Vec3 position,float scale) {
      VFXServerEffects.spawnScaled(level,"servant_heracles_slam",position,96,scale);
      level.playSound(null,position.x,position.y,position.z,SoundEvents.GENERIC_EXPLODE.value(),SoundSource.HOSTILE,1.0F*scale,.58F);
      level.playSound(null,position.x,position.y,position.z,SoundEvents.STONE_BREAK,SoundSource.HOSTILE,1.4F*scale,.68F);
   }
   private static void resetAir(LivingEntity e) { var d=e.getPersistentData();d.remove("HeraclesAirTicks");d.remove("HeraclesAirTop");d.putBoolean("HeraclesLandingConsumed",true); }
   private static void detach(LivingEntity e) {
      cancel(e);release(e);THROWN.remove(e);
      for(var entry:new ArrayList<>(RUNNING.entrySet()))if(entry.getValue().target==e&&entry.getValue().grabbed)cancel(entry.getKey());
      THROWN.entrySet().removeIf(entry->entry.getValue().owner==e);
   }
   @SubscribeEvent public static void leave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
      if(!event.getLevel().isClientSide()&&event.getEntity() instanceof LivingEntity e)detach(e);
   }
   @SubscribeEvent public static void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) { if(!event.isCanceled())detach(event.getEntity()); }
   @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) { detach(event.getEntity()); }
   private static void passiveLanding(LivingEntity e) {
      var d=e.getPersistentData();
      if(!e.onGround()) {
         d.putInt("HeraclesAirTicks",d.getInt("HeraclesAirTicks")+1);
         d.putDouble("HeraclesAirTop",d.contains("HeraclesAirTop")?Math.max(d.getDouble("HeraclesAirTop"),e.getY()):e.getY());
      } else {
         if(!d.getBoolean("HeraclesLandingConsumed")&&!busy(e)&&d.getInt("HeraclesAirTicks")>=8&&d.getDouble("HeraclesAirTop")-e.getY()>=5)ground(e,-1,3.2,26,1.15,.42);
         d.remove("HeraclesAirTicks");d.remove("HeraclesAirTop");d.remove("HeraclesLandingConsumed");
      }
   }
   @SubscribeEvent
   public static void tick(EntityTickEvent.Post event) {
      if(!(event.getEntity() instanceof LivingEntity e)||e.level().isClientSide())return;
      Action action=RUNNING.get(e);if(action!=null)advance(e,action);
      if(isHeracles(e))passiveLanding(e);
      if(held(e)) {
         boolean exists=RUNNING.values().stream().anyMatch(a->a.grabbed&&a.target==e);
         if(!exists||!e.isAlive())release(e);
      }
      Thrown thrown=THROWN.get(e);
      if(thrown!=null) {
         if(!e.isAlive()||!thrown.owner.isAlive()||thrown.owner.isRemoved()||!isHeracles(thrown.owner)||thrown.owner.level()!=e.level()||e.level().dimension()!=thrown.dimension||++thrown.ticks>40) { THROWN.remove(e);return; }
         if(!e.onGround())thrown.airborne=true;
         boolean entityCollision = !e.level().getEntitiesOfClass(LivingEntity.class,e.getBoundingBox().minmax(e.getBoundingBox().move(thrown.previous.subtract(e.position()))), t -> t != e && t != thrown.owner && t.isAlive()).isEmpty();
         Vec3 traveled=e.position().subtract(thrown.previous);
         if(traveled.horizontalDistanceSqr()>.001)thrown.lastMotion=traveled;
         thrown.previous=e.position();
         if(e.horizontalCollision||entityCollision||thrown.airborne&&e.onGround()) {
            THROWN.remove(e);hit(thrown.owner,e,5,22,0,0,true);
            if(e.level() instanceof ServerLevel level) {
               var profile=new TerrainImpactProfile(TerrainImpactProfile.Tier.MEDIUM,2.2,6,48,80);
               var permission=thrown.owner instanceof ServerPlayer?TerrainImpactService.Permission.PLAYER:TerrainImpactService.Permission.NPC;
               if(e.horizontalCollision)TerrainImpactService.impactHeraclesWallAt(level,thrown.owner,e.position(),e.getBbHeight(),thrown.lastMotion,profile,permission);
               else if(e.onGround())TerrainImpactService.impactHeraclesGround(level,thrown.owner,e.position(),profile,permission,2);
               impactFx(level,e.position(),.65F);
            }
         }
      }
   }
}
