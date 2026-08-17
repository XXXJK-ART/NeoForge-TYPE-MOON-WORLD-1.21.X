package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.BaobhanSithCurseOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.baobhan.BaobhanSithCurseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public final class ServantCardBaobhanSithSkills {
   public static final String SERVANT_ID = "baobhan_sith";
   private static final String TAG_GRIMALKIN_UNTIL = "ServantCardBaobhanSithGrimalkinUntil";
   private static final String TAG_BLESSED_UNTIL = "ServantCardBaobhanSithBlessedUntil";
   private static final String TAG_LAST_HOOF_FX = "ServantCardBaobhanSithLastHoofFx";
   private static final String TAG_LAST_HAIR_COLLECT = "ServantCardBaobhanSithLastHairCollect";
   private static final long HAIR_COLLECT_COOLDOWN_TICKS = 20L;
   private static final float HAIR_COLLECT_HEALTH_COST = 0.5F;
   private static final float NORMAL_ATTACK_MEDIUM_CHANCE = 0.55F;
   private static final double FETCH_FAILNAUGHT_MP_COST = 35.0;
   private static final int FETCH_FAILNAUGHT_COOLDOWN_TICKS = 300;
   private static final ResourceLocation GRIMALKIN_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "baobhan_sith_grimalkin_speed");
   private static final ResourceLocation GRIMALKIN_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "baobhan_sith_grimalkin_attack");
   private static final DustParticleOptions BLOOD_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.04F, 0.08F), 1.12F);
   private static final DustParticleOptions CURSE_DUST = new DustParticleOptions(new Vector3f(0.08F, 0.02F, 0.10F), 1.0F);
   private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(new Vector3f(0.44F, 0.06F, 0.62F), 1.0F);
   private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.18F), 1.0F);

   private ServantCardBaobhanSithSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!isActive(player)) {
         return;
      }
      BaobhanSithCurseService.tickOwnerCurses(player);
      long now = player.level().getGameTime();
      if (player.getPersistentData().getLong(TAG_GRIMALKIN_UNTIL) > now) {
         tickHoofTrail(player, now);
      } else {
         removeGrimalkin(player);
      }
      if (player.getPersistentData().getLong(TAG_BLESSED_UNTIL) > now && player.tickCount % 40 == 0) {
         int sealed = sealEnemies(player, now);
         if (sealed > 0) {
            vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + sealed * 5.0);
            vars.syncMana(player);
         }
      }
   }

   public static void clear(ServerPlayer player) {
      removeGrimalkin(player);
      player.getPersistentData().remove(TAG_GRIMALKIN_UNTIL);
      player.getPersistentData().remove(TAG_BLESSED_UNTIL);
      player.getPersistentData().remove(TAG_LAST_HOOF_FX);
      player.getPersistentData().remove(TAG_LAST_HAIR_COLLECT);
      BaobhanSithCurseService.clearPanelState(player);
   }

   public static boolean tryCollectHairMedium(ServerPlayer player, InteractionHand hand, Entity targetEntity) {
      if (!isActive(player)
         || hand != InteractionHand.MAIN_HAND
         || !player.isCrouching()
         || !player.getMainHandItem().isEmpty()
         || !player.getOffhandItem().isEmpty()
         || !(targetEntity instanceof LivingEntity target)
         || !validTarget(player, target)) {
         return false;
      }
      long now = player.level().getGameTime();
      long last = player.getPersistentData().getLong(TAG_LAST_HAIR_COLLECT);
      if (now - last < HAIR_COLLECT_COOLDOWN_TICKS) {
         return true;
      }
      if (!isVeryCloseTo(player, target)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.baobhan_sith.hair_collect_too_far"), true);
         return true;
      }
      BaobhanSithCurseService.addMedium(player, target, BaobhanSithCurseService.MEDIUM_HAIR, 1);
      BaobhanSithCurseService.setSelectedTarget(player, target.getUUID());
      quietlyRemoveHealth(player, target, HAIR_COLLECT_HEALTH_COST);
      player.getPersistentData().putLong(TAG_LAST_HAIR_COLLECT, now);
      if (player.level() instanceof ServerLevel level) {
         spawnHairCollectFx(level, player, target);
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.baobhan_sith.hair_collected"), true);
      return true;
   }

   public static void onNormalAttack(ServerPlayer player, LivingEntity target) {
      if (!isActive(player) || !validTarget(player, target) || player.getRandom().nextFloat() >= NORMAL_ATTACK_MEDIUM_CHANCE) {
         return;
      }
      String medium = randomAttackMedium(player, target);
      BaobhanSithCurseService.addMedium(player, target, medium, 1);
      BaobhanSithCurseService.setSelectedTarget(player, target.getUUID());
      if (player.level() instanceof ServerLevel level) {
         spawnAttackMediumFx(level, target, medium);
      }
   }

   public static boolean openCursePanel(ServerPlayer player) {
      if (!isActive(player)) {
         return false;
      }
      sendPanel(player, false);
      return true;
   }

   public static boolean openFetchFailnaughtMap(ServerPlayer player) {
      if (isActive(player)) {
         sendPanel(player, true);
         return true;
      }
      return false;
   }

   public static void refreshCursePanel(ServerPlayer player, boolean noblePhantasmMode) {
      if (isActive(player)) {
         sendPanel(player, noblePhantasmMode);
      }
   }

   public static boolean selectPanelTarget(ServerPlayer player, UUID targetId) {
      if (!isActive(player) || BaobhanSithCurseService.findCursedTarget(player, targetId) == null) {
         return false;
      }
      BaobhanSithCurseService.setSelectedTarget(player, targetId);
      sendPanel(player, false);
      return true;
   }

   public static boolean triggerPanelMedium(ServerPlayer player, UUID targetId, String medium) {
      if (!isActive(player) || !(player.level() instanceof ServerLevel level)) {
         return false;
      }
      long now = level.getGameTime();
      long last = player.getPersistentData().getLong(BaobhanSithCurseService.TAG_PANEL_LAST_TRIGGER);
      if (now - last < 10L) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.baobhan_sith.panel_wait"), true);
         return false;
      }
      LivingEntity target = BaobhanSithCurseService.findCursedTarget(player, targetId);
      if (target == null || !validTarget(player, target)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      String normalized = BaobhanSithCurseService.normalizeMedium(medium);
      if (BaobhanSithCurseService.mediumCount(player, target, normalized) <= 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.baobhan_sith.no_medium"), true);
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!ServantCardManaService.consume(player, vars, 4.0)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return false;
      }
      if (!BaobhanSithCurseService.consumeMedium(player, target, normalized, 1)) {
         ServantCardManaService.restore(player, vars, new ServantCardManaService.ManaSnapshot(vars.servant_card_mana + 4.0, null, 0.0));
         return false;
      }
      float damage = switch (normalized) {
         case BaobhanSithCurseService.MEDIUM_SKIN -> 8.0F;
         case BaobhanSithCurseService.MEDIUM_HAIR -> 6.0F;
         case BaobhanSithCurseService.MEDIUM_REMAINS -> 24.0F;
         default -> 10.0F;
      };
      BaobhanSithCurseService.hurtWithCurse(player, target, damage);
      BaobhanSithCurseService.applyCurse(player, target, BaobhanSithCurseService.curseForMedium(normalized), 1, 220, 1.0F);
      applyPanelMediumEffect(target, normalized);
      spawnMediumTriggerFx(level, player, target, normalized);
      BaobhanSithCurseService.setSelectedTarget(player, targetId);
      player.getPersistentData().putLong(BaobhanSithCurseService.TAG_PANEL_LAST_TRIGGER, now);
      player.displayClientMessage(Component.translatable("message.typemoonworld.baobhan_sith.triggered_medium",
         Component.translatable("screen.typemoonworld.baobhan_sith.medium." + normalized.toLowerCase(java.util.Locale.ROOT))), true);
      sendPanel(player, false);
      return true;
   }

   public static boolean performBloodSpike(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 18.0, 1.2);
      if (target == null) {
         return noTarget(player);
      }
      BaobhanSithCurseService.hurtWithCurse(player, target, 22.0F);
      BaobhanSithCurseService.addMedium(player, target, BaobhanSithCurseService.MEDIUM_BLOOD, 1);
      BaobhanSithCurseService.applyCurse(player, target, BaobhanSithCurseService.CURSE_BLOOD, 1, 260, 1.0F);
      if (player.level() instanceof ServerLevel level) {
         spawnBloodSpikeFx(level, target.position(), target.getBbHeight());
      }
      ServantCardVoiceHelper.tryPlayAttack(player);
      return true;
   }

   public static boolean performBloodThorns(ServerPlayer player) {
      LivingEntity primary = findLookTarget(player, 22.0, 1.6);
      if (primary == null || !(player.level() instanceof ServerLevel level)) {
         return noTarget(player);
      }
      AABB box = primary.getBoundingBox().inflate(3.5, 1.0, 3.5);
      int hits = 0;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, other -> validTarget(player, other))) {
         float damage = target == primary ? 18.0F : 14.0F;
         BaobhanSithCurseService.hurtWithCurse(player, target, damage);
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 4, false, true, true));
         target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 1, false, true, true));
         BaobhanSithCurseService.addMedium(player, target, BaobhanSithCurseService.MEDIUM_SKIN, 1);
         BaobhanSithCurseService.applyCurse(player, target, BaobhanSithCurseService.CURSE_SKIN, 1, 300, 1.0F);
         hits++;
      }
      if (hits <= 0) {
         return noTarget(player);
      }
      spawnBloodThornsFx(level, primary.position(), 3.5);
      ServantCardVoiceHelper.tryPlayAttack(player);
      return true;
   }

   public static boolean performCurseVolley(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 38.0, 1.6);
      if (target == null || !(player.level() instanceof ServerLevel level)) {
         return noTarget(player);
      }
      spawnCurseVolleyBackFx(level, player, 5);
      fireBackCurseVolley(level, player, target);
      ServantCardVoiceHelper.tryPlayAttack(player);
      return true;
   }

   public static boolean performFingertipDance(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 4.2, 1.0);
      if (target == null) {
         return noTarget(player);
      }
      for (int i = 0; i < 3; i++) {
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().playerAttack(player), 20.0F);
         target.invulnerableTime = 0;
         BaobhanSithCurseService.addMedium(player, target, BaobhanSithCurseService.MEDIUM_BLOOD, 1);
         if (player.getRandom().nextFloat() < 0.5F) {
            BaobhanSithCurseService.addMedium(player, target, BaobhanSithCurseService.preferredMissingMedium(player, target), 1);
            BaobhanSithCurseService.applyCurse(player, target, BaobhanSithCurseService.CURSE_SKIN, 1, 300, 1.0F);
         }
      }
      if (player.level() instanceof ServerLevel level) {
         spawnSlashFx(level, player, target);
      }
      ServantCardVoiceHelper.tryPlayAttack(player);
      return true;
   }

   public static boolean performNightFeast(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 4.2, 1.0);
      if (target == null) {
         return noTarget(player);
      }
      int layers = BaobhanSithCurseService.totalCurseLayers(player, target);
      float drain = 80.0F + layers * 20.0F;
      BaobhanSithCurseService.drainLife(player, target, drain);
      BaobhanSithCurseService.addMedium(player, target, BaobhanSithCurseService.MEDIUM_BLOOD, 1);
      spawnDrainFx(player, target);
      ServantCardVoiceHelper.tryPlayAttack(player);
      return true;
   }

   public static boolean performGrimalkin(ServerPlayer player) {
      long now = player.level().getGameTime();
      player.getPersistentData().putLong(TAG_GRIMALKIN_UNTIL, now + 300L);
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.JUMP, 300, 2, false, true, true));
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.MOVEMENT_SPEED), GRIMALKIN_SPEED_ID, 0.22);
      ServantCardSkillUtils.addOrReplaceMultiplied(player.getAttribute(Attributes.ATTACK_SPEED), GRIMALKIN_ATTACK_ID, 0.25);
      playSelfBuffFx(player);
      return true;
   }

   public static boolean performBlessedSuccessor(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      long now = level.getGameTime();
      player.getPersistentData().putLong(TAG_BLESSED_UNTIL, now + 200L);
      int sealed = sealEnemies(player, now);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + sealed * 5.0);
      vars.syncMana(player);
      playDominionFx(player);
      return true;
   }

   public static boolean performFairyVampirism(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 4.2, 1.0);
      if (target == null) {
         return noTarget(player);
      }
      BaobhanSithCurseService.drainLife(player, target, 60.0F);
      drainTargetMana(player, target, 20.0);
      BaobhanSithCurseService.addMedium(player, target, BaobhanSithCurseService.MEDIUM_BLOOD, 1);
      BaobhanSithCurseService.applyCurse(player, target, BaobhanSithCurseService.CURSE_BLOOD, 1, 220, 1.0F);
      if (BaobhanSithCurseService.totalCurseLayers(player, target) >= 5 && player.getRandom().nextFloat() < 0.2F) {
         BaobhanSithCurseService.hurtWithCurse(target, player, 50.0F);
      }
      spawnDrainFx(player, target);
      ServantCardVoiceHelper.tryPlayAttack(player);
      return true;
   }

   public static boolean performFetchFailnaught(ServerPlayer player) {
      return openFetchFailnaughtMap(player);
   }

   public static boolean detonateFetchFailnaught(ServerPlayer player, UUID targetId) {
      if (!isActive(player)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!checkFetchFailnaughtReady(player, vars)) {
         return false;
      }
      LivingEntity target = BaobhanSithCurseService.findCursedTarget(player, targetId);
      if (target == null || !validTarget(player, target)) {
         return noTarget(player);
      }
      if (!hasFetchPayload(player, target)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.baobhan_sith.no_curse_payload"), true);
         return false;
      }
      if (!ServantCardManaService.consumeNoblePhantasm(player, vars, FETCH_FAILNAUGHT_MP_COST)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      executeFetchFailnaught(player, target);
      BaobhanSithCurseService.consumeAllMediums(player, target);
      finishFetchFailnaught(player, vars, 1);
      return true;
   }

   public static boolean detonateAllFetchFailnaught(ServerPlayer player) {
      if (!isActive(player)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!checkFetchFailnaughtReady(player, vars)) {
         return false;
      }
      List<LivingEntity> targets = BaobhanSithCurseService.snapshots(player).stream()
         .map(snapshot -> BaobhanSithCurseService.findCursedTarget(player, snapshot.uuid()))
         .filter(target -> target != null && validTarget(player, target) && hasFetchPayload(player, target))
         .distinct()
         .toList();
      if (targets.isEmpty()) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.baobhan_sith.no_curse_payload"), true);
         return false;
      }
      if (!ServantCardManaService.consumeNoblePhantasm(player, vars, FETCH_FAILNAUGHT_MP_COST)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      int detonated = 0;
      for (LivingEntity target : targets) {
         if (executeFetchFailnaught(player, target)) {
            BaobhanSithCurseService.consumeAllMediums(player, target);
            detonated++;
         }
      }
      if (detonated <= 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.baobhan_sith.no_curse_payload"), true);
         return false;
      }
      finishFetchFailnaught(player, vars, detonated);
      player.displayClientMessage(Component.translatable("message.typemoonworld.baobhan_sith.fetch_all", detonated), true);
      return true;
   }

   private static boolean executeFetchFailnaught(ServerPlayer player, LivingEntity target) {
      if (target == null || !validTarget(player, target)) {
         return false;
      }
      int layers = BaobhanSithCurseService.totalCurseLayers(player, target);
      int mediums = BaobhanSithCurseService.totalMediumCount(player, target);
      if (layers <= 0 && mediums <= 0) {
         return false;
      }
      float damage = Math.min(180.0F, 30.0F + layers * 12.0F + mediums * 8.0F);
      boolean wasAlive = target.isAlive();
      appendExistingCurseTypes(player, target);
      BaobhanSithCurseService.triggerBurst(player, target);
      float burstDamage = fetchFailnaughtBurstDamage(player, target, layers, mediums);
      BaobhanSithCurseService.hurtWithCurse(player, target, damage + burstDamage);
      if (player.level() instanceof ServerLevel level) {
         spawnFetchFailnaughtFx(level, player, target);
      }
      if (wasAlive && !target.isAlive()) {
         BaobhanSithCurseService.addMedium(player, target, BaobhanSithCurseService.MEDIUM_REMAINS, 1);
         player.heal(100.0F);
      }
      BaobhanSithCurseService.setSelectedTarget(player, target.getUUID());
      return true;
   }

   private static boolean checkFetchFailnaughtReady(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!ServantCardUnlimitedMode.isEnabled(player) && vars.servant_card_np_cooldown > 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.cooldown",
            String.format(java.util.Locale.ROOT, "%.1f", vars.servant_card_np_cooldown / 20.0F)), true);
         return false;
      }
      return true;
   }

   private static boolean hasFetchPayload(ServerPlayer player, LivingEntity target) {
      return BaobhanSithCurseService.totalCurseLayers(player, target) > 0
         || BaobhanSithCurseService.totalMediumCount(player, target) > 0;
   }

   private static float fetchFailnaughtBurstDamage(ServerPlayer player, LivingEntity target, int originalLayers, int originalMediums) {
      int blood = BaobhanSithCurseService.curseCount(player, target, BaobhanSithCurseService.CURSE_BLOOD);
      int skin = BaobhanSithCurseService.curseCount(player, target, BaobhanSithCurseService.CURSE_SKIN);
      int hair = BaobhanSithCurseService.curseCount(player, target, BaobhanSithCurseService.CURSE_HAIR);
      int remains = BaobhanSithCurseService.curseCount(player, target, BaobhanSithCurseService.CURSE_REMAINS);
      float typedPower = blood * 5.0F + skin * 4.0F + hair * 3.0F + remains * 15.0F;
      if (typedPower <= 0.0F && originalLayers <= 0 && originalMediums <= 0) {
         return 0.0F;
      }
      return Math.min(120.0F, Math.max(18.0F, typedPower * 2.5F + originalLayers * 4.0F + originalMediums * 2.5F));
   }

   private static void finishFetchFailnaught(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int detonatedCount) {
      ServantCardTransformManager.setNoblePhantasmCooldown(player, vars, FETCH_FAILNAUGHT_COOLDOWN_TICKS);
      vars.syncPlayerVariables(player);
      if (detonatedCount == 1) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.skill_activated",
            Component.translatable("skill.typemoonworld.servant_card.baobhan_sith_fetch_failnaught")), true);
      }
      ServantCardVoiceHelper.tryPlaySkill(player, "baobhan_sith_fetch_failnaught");
   }

   private static void sendPanel(ServerPlayer player, boolean noblePhantasmMode) {
      List<BaobhanSithCurseService.Snapshot> snapshots = new ArrayList<>(BaobhanSithCurseService.snapshots(player));
      LivingEntity selectedTarget = BaobhanSithCurseService.selectedTarget(player);
      if (!snapshots.isEmpty() && selectedTarget == null) {
         BaobhanSithCurseService.setSelectedTarget(player, snapshots.get(0).uuid());
      } else if (selectedTarget != null) {
         UUID selectedId = selectedTarget.getUUID();
         snapshots.sort((first, second) -> Boolean.compare(!first.uuid().equals(selectedId), !second.uuid().equals(selectedId)));
      }
      List<BaobhanSithCurseOpenScreenMessage.Target> targets = snapshots.stream()
         .map(snapshot -> new BaobhanSithCurseOpenScreenMessage.Target(
            snapshot.entityId(),
            snapshot.uuid(),
            snapshot.name(),
            snapshot.dimension(),
            snapshot.x(),
            snapshot.y(),
            snapshot.z(),
            snapshot.health(),
            snapshot.maxHealth(),
            snapshot.layers(),
            snapshot.blood(),
            snapshot.skin(),
            snapshot.hair(),
            snapshot.remains(),
            snapshot.bloodCurse(),
            snapshot.skinCurse(),
            snapshot.hairCurse(),
            snapshot.remainsCurse()
         ))
         .toList();
      ModNetwork.sendToPlayer(player, new BaobhanSithCurseOpenScreenMessage(targets, noblePhantasmMode));
   }

   private static LivingEntity findLookTarget(ServerPlayer player, double range, double inflate) {
      LivingEntity target = ServantCardSkillUtils.findAutomaticLookTarget(player, range, inflate);
      return target != null && validTarget(player, target) ? target : null;
   }

   private static boolean validTarget(ServerPlayer player, LivingEntity target) {
      return target != null && target.isAlive() && target != player
         && !ServantMasterTargeting.isContractMaster(player, target)
         && !player.isAlliedTo(target) && !target.isAlliedTo(player)
         && !EntityUtils.isImmunePlayerTarget(target);
   }

   private static boolean isActive(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && SERVANT_ID.equals(vars.servant_card_id);
   }

   private static boolean isVeryCloseTo(ServerPlayer player, LivingEntity target) {
      return player.distanceToSqr(target) <= 2.25D
         || player.getBoundingBox().inflate(0.7D).intersects(target.getBoundingBox());
   }

   private static String randomAttackMedium(ServerPlayer player, LivingEntity target) {
      float roll = player.getRandom().nextFloat();
      if (roll < 0.50F) {
         return BaobhanSithCurseService.MEDIUM_BLOOD;
      }
      if (roll < 0.82F) {
         return BaobhanSithCurseService.MEDIUM_SKIN;
      }
      return BaobhanSithCurseService.mediumCount(player, target, BaobhanSithCurseService.MEDIUM_HAIR) <= 0
         ? BaobhanSithCurseService.MEDIUM_HAIR
         : BaobhanSithCurseService.preferredMissingMedium(player, target);
   }

   private static void quietlyRemoveHealth(ServerPlayer owner, LivingEntity target, float amount) {
      float before = target.getHealth();
      float after = before - amount;
      if (after <= 0.0F) {
         target.setHealth(0.0F);
         target.die(BaobhanSithCurseService.curseDamageSource(owner));
         if (before > 0.0F) {
            BaobhanSithCurseService.addMedium(owner, target, BaobhanSithCurseService.MEDIUM_REMAINS, 1);
         }
      } else {
         target.setHealth(after);
      }
   }

   private static boolean noTarget(ServerPlayer player) {
      player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
      return false;
   }

   private static void drainTargetMana(ServerPlayer player, LivingEntity target, double amount) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double drained = amount;
      if (target instanceof ServantEntity servant) {
         drained = Math.min(amount, servant.getCurrentMp());
         servant.setCurrentMp(Math.max(0.0, servant.getCurrentMp() - drained));
      } else if (target instanceof ServerPlayer targetPlayer) {
         TypeMoonWorldModVariables.PlayerVariables targetVars = targetPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (targetVars.servant_card_transformed) {
            drained = Math.min(amount, targetVars.servant_card_mana);
            targetVars.servant_card_mana = Math.max(0.0, targetVars.servant_card_mana - drained);
            targetVars.syncMana(targetPlayer);
         }
      }
      vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + drained);
      vars.syncMana(player);
   }

   private static int sealEnemies(ServerPlayer player, long now) {
      if (!(player.level() instanceof ServerLevel level)) {
         return 0;
      }
      int count = 0;
      for (ServantEntity servant : level.getEntitiesOfClass(ServantEntity.class, player.getBoundingBox().inflate(20.0), servant -> servant.isAlive() && !servant.isAlliedTo(player))) {
         servant.getPersistentData().putLong("TypeMoonCombatSuppressedUntil", now + 200L);
         count++;
      }
      for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(20.0), other -> other != player && validTarget(player, other))) {
         other.getPersistentData().putLong("TypeMoonCombatSuppressedUntil", now + 120L);
         count++;
      }
      return count;
   }

   private static void appendExistingCurseTypes(ServerPlayer player, LivingEntity target) {
      if (BaobhanSithCurseService.mediumCount(player, target, BaobhanSithCurseService.MEDIUM_BLOOD) > 0
         || BaobhanSithCurseService.curseCount(player, target, BaobhanSithCurseService.CURSE_BLOOD) > 0) {
         BaobhanSithCurseService.applyCurse(player, target, BaobhanSithCurseService.CURSE_BLOOD, 1, 260, 1.0F);
      }
      if (BaobhanSithCurseService.mediumCount(player, target, BaobhanSithCurseService.MEDIUM_SKIN) > 0
         || BaobhanSithCurseService.curseCount(player, target, BaobhanSithCurseService.CURSE_SKIN) > 0) {
         BaobhanSithCurseService.applyCurse(player, target, BaobhanSithCurseService.CURSE_SKIN, 1, 300, 1.0F);
      }
      if (BaobhanSithCurseService.mediumCount(player, target, BaobhanSithCurseService.MEDIUM_HAIR) > 0
         || BaobhanSithCurseService.curseCount(player, target, BaobhanSithCurseService.CURSE_HAIR) > 0) {
         BaobhanSithCurseService.applyCurse(player, target, BaobhanSithCurseService.CURSE_HAIR, 1, 160, 1.0F);
      }
      if (BaobhanSithCurseService.mediumCount(player, target, BaobhanSithCurseService.MEDIUM_REMAINS) > 0
         || BaobhanSithCurseService.curseCount(player, target, BaobhanSithCurseService.CURSE_REMAINS) > 0) {
         BaobhanSithCurseService.applyCurse(player, target, BaobhanSithCurseService.CURSE_REMAINS, 1, 240, 1.0F);
      }
   }

   private static void applyPanelMediumEffect(LivingEntity target, String medium) {
      switch (medium) {
         case BaobhanSithCurseService.MEDIUM_SKIN -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, true, true));
         case BaobhanSithCurseService.MEDIUM_HAIR -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 8, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, true, true));
         }
         case BaobhanSithCurseService.MEDIUM_REMAINS -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1, false, true, true));
         default -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true, true));
      }
   }

   private static void removeGrimalkin(ServerPlayer player) {
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED), GRIMALKIN_SPEED_ID);
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_SPEED), GRIMALKIN_ATTACK_ID);
   }

   private static void tickHoofTrail(ServerPlayer player, long now) {
      if (!(player.level() instanceof ServerLevel level) || now - player.getPersistentData().getLong(TAG_LAST_HOOF_FX) < 6L) {
         return;
      }
      player.getPersistentData().putLong(TAG_LAST_HOOF_FX, now);
      Vec3 center = player.position().add(0.0, 0.07, 0.0);
      double yaw = Math.toRadians(player.getYRot());
      Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw)).scale(0.18);
      spawnHoofPrint(level, center.add(right));
      spawnHoofPrint(level, center.subtract(right));
   }

   private static void fireBackCurseVolley(ServerLevel level, ServerPlayer player, LivingEntity target) {
      Vec3 toTarget = target.getEyePosition().subtract(player.getEyePosition());
      Vec3 forward = toTarget.lengthSqr() < 1.0E-5 ? player.getLookAngle() : toTarget.normalize();
      Vec3 flatForward = new Vec3(forward.x, 0.0, forward.z);
      flatForward = flatForward.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : flatForward.normalize();
      Vec3 side = new Vec3(-flatForward.z, 0.0, flatForward.x).normalize();
      Vec3 base = player.position().add(0.0, player.getBbHeight() * 0.82, 0.0).subtract(flatForward.scale(0.95));
      for (int i = 0; i < 5; i++) {
         double spread = (i - 2) * 0.44;
         double lift = 0.18 + Math.sin(i * Math.PI / 4.0) * 0.26;
         Vec3 start = base.add(side.scale(spread)).add(0.0, lift, 0.0);
         Vec3 aim = target.getEyePosition().add(side.scale((i - 2) * 0.18)).subtract(start);
         Vec3 direction = aim.lengthSqr() < 1.0E-5 ? forward : aim.normalize();
         GanderProjectileEntity projectile = new GanderProjectileEntity(level, player);
         projectile.setPos(start.x, start.y, start.z);
         projectile.setDeltaMovement(direction.scale(3.1 + i * 0.05));
         projectile.setMagicSource("baobhan_sith_curse", 70.0);
         projectile.setChargeSeconds(1);
         projectile.setVisualScale(0.82F);
         level.addFreshEntity(projectile);
         level.sendParticles(BLOOD_DUST, start.x, start.y, start.z, 7, 0.08, 0.08, 0.08, 0.0);
         level.sendParticles(CURSE_DUST, start.x, start.y, start.z, 5, 0.06, 0.06, 0.06, 0.0);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.42F);
   }

   private static void spawnBloodSpikeFx(ServerLevel level, Vec3 base, double targetHeight) {
      Vec3 root = base.add(0.0, 0.08, 0.0);
      double height = Math.max(1.1, targetHeight * 0.9);
      spawnRing(level, BLOOD_DUST, root, 0.42, 18);
      for (int i = 0; i < 28; i++) {
         double t = i / 27.0;
         double twist = t * Math.PI * 3.5;
         double radius = (1.0 - t) * 0.28;
         Vec3 pos = root.add(Math.cos(twist) * radius, height * t, Math.sin(twist) * radius);
         level.sendParticles(i % 3 == 0 ? CURSE_DUST : BLOOD_DUST, pos.x, pos.y, pos.z, 2, 0.025, 0.025, 0.025, 0.0);
      }
      level.playSound(null, net.minecraft.core.BlockPos.containing(base), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 0.65F);
   }

   private static void spawnBloodThornsFx(ServerLevel level, Vec3 center, double radius) {
      Vec3 ground = center.add(0.0, 0.08, 0.0);
      spawnRing(level, CURSE_DUST, ground, radius, 44);
      for (int thorn = 0; thorn < 9; thorn++) {
         double angle = thorn * Math.PI * 2.0 / 9.0;
         Vec3 base = ground.add(Math.cos(angle) * radius * 0.68, 0.0, Math.sin(angle) * radius * 0.68);
         for (int i = 0; i < 12; i++) {
            double t = i / 11.0;
            Vec3 pos = base.add(Math.cos(angle) * 0.15 * (1.0 - t), t * 1.35, Math.sin(angle) * 0.15 * (1.0 - t));
            level.sendParticles(i % 2 == 0 ? BLOOD_DUST : CURSE_DUST, pos.x, pos.y, pos.z, 2, 0.025, 0.025, 0.025, 0.0);
         }
      }
      level.playSound(null, net.minecraft.core.BlockPos.containing(center), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.1F, 0.55F);
   }

   private static void spawnCurseVolleyBackFx(ServerLevel level, ServerPlayer player, int count) {
      Vec3 look = player.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      flat = flat.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      Vec3 side = new Vec3(-flat.z, 0.0, flat.x);
      Vec3 base = player.position().add(0.0, player.getBbHeight() * 0.82, 0.0).subtract(flat.scale(0.95));
      for (int i = 0; i < count; i++) {
         Vec3 point = base.add(side.scale((i - (count - 1) / 2.0) * 0.44)).add(0.0, 0.18 + Math.sin(i * Math.PI / 4.0) * 0.24, 0.0);
         spawnRoseBloom(level, point, 0.28, 10);
      }
   }

   private static void spawnMediumTriggerFx(ServerLevel level, ServerPlayer player, LivingEntity target, String medium) {
      Vec3 from = player.position().add(0.0, player.getBbHeight() * 0.72, 0.0);
      Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      spawnLine(level, from, to, BaobhanSithCurseService.MEDIUM_HAIR.equals(medium) ? PURPLE_DUST : BLOOD_DUST);
      spawnRoseBloom(level, to, BaobhanSithCurseService.MEDIUM_REMAINS.equals(medium) ? 1.0 : 0.72, 22);
      level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.95F, 0.68F);
   }

   private static void spawnHairCollectFx(ServerLevel level, ServerPlayer player, LivingEntity target) {
      Vec3 hand = player.position().add(0.0, player.getBbHeight() * 0.62, 0.0);
      Vec3 neck = target.position().add(0.0, target.getBbHeight() * 0.72, 0.0);
      spawnLine(level, neck, hand, PURPLE_DUST);
      level.sendParticles(PURPLE_DUST, neck.x, neck.y, neck.z, 8, 0.16, 0.12, 0.16, 0.0);
      level.sendParticles(CURSE_DUST, hand.x, hand.y, hand.z, 5, 0.08, 0.08, 0.08, 0.0);
      level.playSound(null, target.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.35F, 1.55F);
   }

   private static void spawnAttackMediumFx(ServerLevel level, LivingEntity target, String medium) {
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
      DustParticleOptions particle = BaobhanSithCurseService.MEDIUM_HAIR.equals(medium) ? PURPLE_DUST
         : BaobhanSithCurseService.MEDIUM_SKIN.equals(medium) ? CURSE_DUST : BLOOD_DUST;
      level.sendParticles(particle, center.x, center.y, center.z, 8, 0.22, 0.18, 0.22, 0.0);
   }

   private static void spawnSlashFx(ServerLevel level, ServerPlayer player, LivingEntity target) {
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, 22, 0.45, 0.35, 0.45, 0.0);
      spawnRoseBloom(level, center, 0.9, 28);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.25F);
   }

   private static void spawnDrainFx(ServerPlayer player, LivingEntity target) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 from = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 to = player.position().add(0.0, player.getBbHeight() * 0.65, 0.0);
      for (int i = 0; i < 9; i++) {
         Vec3 pos = from.lerp(to, i / 8.0);
         level.sendParticles(BLOOD_DUST, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.0);
         if (i % 2 == 0) {
            level.sendParticles(CURSE_DUST, pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.0);
         }
      }
      spawnRoseBloom(level, from, 0.7, 18);
      level.playSound(null, player.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 0.9F, 0.75F);
   }

   private static void playSelfBuffFx(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         Vec3 center = player.position().add(0.0, 0.15, 0.0);
         level.sendParticles(CURSE_DUST, center.x, center.y, center.z, 28, 1.6, 0.08, 1.6, 0.0);
         level.sendParticles(ParticleTypes.WITCH, center.x, center.y + 0.5, center.z, 16, 0.45, 0.65, 0.45, 0.02);
         spawnRing(level, PURPLE_DUST, center, 0.8, 22);
         spawnRing(level, PURPLE_DUST, center.add(0.0, 0.05, 0.0), 1.45, 32);
         level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.3F);
      }
   }

   private static void playDominionFx(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         Vec3 center = player.position().add(0.0, 0.2, 0.0);
         level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, 64, 3.5, 0.1, 3.5, 0.0);
         level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.8, center.z, 80, 1.75, 0.8, 1.75, 0.05);
         spawnRing(level, BLOOD_DUST, center, 2.0, 40);
         spawnRing(level, GOLD_DUST, center.add(0.0, 0.05, 0.0), 3.2, 56);
         level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 0.82F);
      }
   }

   private static void spawnFetchFailnaughtFx(ServerLevel level, ServerPlayer player, LivingEntity target) {
      Vec3 source = player.position().add(0.0, player.getBbHeight() * 0.72, 0.0);
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
      spawnRing(level, PURPLE_DUST, player.position().add(0.0, 0.12, 0.0), 3.0, 72);
      level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.72F);
      ServerLevel targetLevel = target.level() instanceof ServerLevel serverLevel ? serverLevel : level;
      if (targetLevel == level) {
         spawnLine(level, source, targetCenter, BLOOD_DUST);
         spawnLine(level, source.add(0.0, 0.12, 0.0), targetCenter.add(0.0, -0.12, 0.0), CURSE_DUST);
      }
      spawnRing(targetLevel, BLOOD_DUST, target.position().add(0.0, 0.12, 0.0), 1.6, 42);
      spawnRoseBloom(targetLevel, targetCenter, 1.8, 52);
      targetLevel.sendParticles(ParticleTypes.FLASH, targetCenter.x, targetCenter.y, targetCenter.z, 1, 0.0, 0.0, 0.0, 0.0);
      targetLevel.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.65F);
   }

   private static void spawnHoofPrint(ServerLevel level, Vec3 center) {
      level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, 3, 0.035, 0.0, 0.035, 0.0);
      level.sendParticles(CURSE_DUST, center.x, center.y, center.z, 1, 0.02, 0.0, 0.02, 0.0);
   }

   private static void spawnRoseBloom(ServerLevel level, Vec3 center, double radius, int petals) {
      for (int i = 0; i < petals; i++) {
         double angle = i * Math.PI * 2.0 / petals;
         double wave = Math.sin(angle * 3.0) * 0.18;
         Vec3 pos = center.add(Math.cos(angle) * (radius + wave), Math.sin(i * 0.9) * 0.12, Math.sin(angle) * (radius + wave));
         level.sendParticles(i % 4 == 0 ? PURPLE_DUST : BLOOD_DUST, pos.x, pos.y, pos.z, 1, 0.025, 0.025, 0.025, 0.0);
      }
   }

   private static void spawnRing(ServerLevel level, DustParticleOptions particle, Vec3 center, double radius, int points) {
      for (int i = 0; i < points; i++) {
         double angle = i * Math.PI * 2.0 / points;
         Vec3 pos = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.01, 0.01, 0.01, 0.0);
      }
   }

   private static void spawnLine(ServerLevel level, Vec3 start, Vec3 end, DustParticleOptions particle) {
      Vec3 delta = end.subtract(start);
      double length = delta.length();
      if (length <= 0.01) {
         return;
      }
      Vec3 step = delta.normalize();
      for (double d = 0.0; d <= length; d += 0.35) {
         Vec3 pos = start.add(step.scale(d));
         level.sendParticles(particle, pos.x, pos.y, pos.z, 2, 0.02, 0.02, 0.02, 0.0);
      }
   }
}
