package net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun;

import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.BindingService;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterDamageRouter;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ZhaoYunRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Skill 6: one cancellable single-spear timeline shared by the card and NPC. */
@EventBusSubscriber(modid = "typemoonworld")
public final class ZhaoYunSpearDanceService {
   public static final double MANA = 28;
   public static final int COOLDOWN = 280;
   public static final int DURATION = 300;
   public static final int FIRST_HIT = 6;
   public static final int HIT_INTERVAL = 4;
   public static final float DAMAGE = 10;
   public static final double RANGE = 4;
   public static final int TARGETS = 3;
   private static final String NPC_COOLDOWN = "ZhaoYunSevenProbeCooldown";
   private static final ResourceLocation ACTION_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "zhao_yun/seven_probe");
   private static final Map<LivingEntity, Cast> CASTS = new WeakHashMap<>();
   private record Cast(ServerLevel level, long start) { }
   private ZhaoYunSpearDanceService() { }

   public static boolean active(LivingEntity owner) { return CASTS.containsKey(owner); }

   /** The card action dispatcher owns the player's MP/cooldown transaction. */
   public static boolean start(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level) || active(owner) || !canDance(owner)) return false;
      long now = level.getGameTime();
      if (owner instanceof ZhaoYunRiderEntity npc) {
         if (now < npc.getPersistentData().getLong(NPC_COOLDOWN) || npc.getCurrentMp() < MANA) return false;
         npc.setCurrentMp(npc.getCurrentMp() - MANA);
         npc.getPersistentData().putLong(NPC_COOLDOWN, now + COOLDOWN);
         ServantVoiceHelper.tryPlayAttack(npc);
      }
      CASTS.put(owner, new Cast(level, now));
      publishThreat(owner, level, now, FIRST_HIT);
      ServantCombatDefenseService.broadcastBaseAttack(owner, ServantBaseAction.ZHAO_YUN_HELD_DANCE);
      return true;
   }

   public static void stop(LivingEntity owner, boolean recover) {
      if (CASTS.remove(owner) == null) return;
      if (owner.level() instanceof ServerLevel level) CombatThreatService.clearForSource(level, owner.getUUID());
      ServantCombatDefenseService.broadcastBaseAttack(owner,
         recover && owner.isAlive() && owner.getMainHandItem().is(ModItems.YAJIAO_QIANG.get())
            ? ServantBaseAction.ZHAO_YUN_RECOVERY : ServantBaseAction.STOP);
   }

   private static void publishThreat(LivingEntity owner, ServerLevel level, long now, int delay) {
      CombatThreatService.publish(level, new CombatThreat(ACTION_ID, owner.getUUID(), null,
         owner.getEyePosition(), owner.getLookAngle(), CombatThreat.Shape.SPHERE, RANGE, RANGE, 3,
         now, now + delay, now + delay, true, true, true));
   }

   private static boolean canDance(LivingEntity owner) {
      boolean identity = owner instanceof ZhaoYunRiderEntity;
      if (owner instanceof ServerPlayer player) {
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         identity = vars.servant_card_transformed && "zhao_yun_rider".equals(vars.servant_card_id)
            && !ServantMasterCarryService.isCarryingMaster(player);
      }
      long now = owner.level().getGameTime();
      return identity && owner.isAlive() && !owner.isRemoved() && !owner.isSleeping()
         && owner.getMainHandItem().is(ModItems.YAJIAO_QIANG.get())
         && !BindingService.isBound(owner.getUUID()) && !ServantCombatSystem.cannotAct(owner)
         && !ServantCombatSystem.skillsSuppressed(owner) && !ServantCombatMotionService.isLaunched(owner)
         && !owner.hasEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.PETRIFIED)
         && !ServantCombatDefenseService.isGuarding(owner) && !NoblePhantasmCastService.chanting(owner)
         && owner.getPersistentData().getLong("ServantCardCombatStunUntil") <= now
         && owner.getPersistentData().getLong("TypeMoonCombatStunUntil") <= now
         && owner.getPersistentData().getLong("TypeMoonCombatSuppressedUntil") <= now;
   }

   private static boolean canHit(LivingEntity owner, LivingEntity target) {
      if (!EntityUtils.isValidCombatTarget(owner, target) || target.level() != owner.level()
         || target == owner.getVehicle()
         || owner instanceof ZhaoYunRiderEntity npc && target == npc.getEntityMaster()
         || owner instanceof ServerPlayer player && ServantMasterTargeting.isContractMaster(player, target)) return false;
      Vec3 eye = owner.getEyePosition();
      var box = target.getBoundingBox();
      Vec3 point = new Vec3(Mth.clamp(eye.x, box.minX, box.maxX), Mth.clamp(eye.y, box.minY, box.maxY),
         Mth.clamp(eye.z, box.minZ, box.maxZ));
      return eye.distanceToSqr(point) <= RANGE * RANGE
         && owner.level().clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner))
            .getType() == HitResult.Type.MISS;
   }

   @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity owner)) return;
      Cast cast = CASTS.get(owner);
      if (cast == null) return;
      int age = (int)(owner.level().getGameTime() - cast.start);
      if (cast.level != owner.level() || !canDance(owner) || age < 0 || age >= DURATION) {
         stop(owner, age >= DURATION && canDance(owner)); return;
      }
      if (owner instanceof ZhaoYunRiderEntity npc && !npc.isNoAi()
         && (npc.getTarget() == null || !canHit(owner, npc.getTarget()))) {
         stop(owner, true); return;
      }
      if (age % 8 == 0) ServantCombatDefenseService.broadcastBaseAttack(owner, ServantBaseAction.ZHAO_YUN_HELD_DANCE);
      if (age >= FIRST_HIT && (age - FIRST_HIT) % HIT_INTERVAL == 0 && age + HIT_INTERVAL < DURATION)
         publishThreat(owner, cast.level, owner.level().getGameTime(), HIT_INTERVAL);
      if (age < FIRST_HIT || (age - FIRST_HIT) % HIT_INTERVAL != 0) return;
      var targets = cast.level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(RANGE + 1),
         target -> canHit(owner, target)).stream()
         .sorted(Comparator.comparingDouble((LivingEntity target) -> owner.distanceToSqr(target)).thenComparingInt(LivingEntity::getId))
         .limit(TARGETS).toList();
      try (var segment = ServantMasterDamageRouter.attackSegment()) {
         for (var target : targets) {
            var base = owner instanceof ServerPlayer player ? owner.damageSources().playerAttack(player) : owner.damageSources().mobAttack(owner);
            int invulnerable = target.invulnerableTime;
            try {
               target.invulnerableTime = 0;
               target.hurt(new CombatDamageSource(base, AttackOrigin.skill("zhao_yun_rider", "zhao_yun_seven_probe")), DAMAGE);
            } finally { target.invulnerableTime = Math.max(invulnerable, target.invulnerableTime); }
         }
      }
      cast.level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
         owner instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 0.4F, 1.15F);
   }

   @SubscribeEvent public static void attack(AttackEntityEvent event) {
      if (active(event.getEntity())) event.setCanceled(true);
   }
   @SubscribeEvent public static void death(LivingDeathEvent event) { stop(event.getEntity(), false); }
   @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
      if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity owner) stop(owner, false);
   }
}
