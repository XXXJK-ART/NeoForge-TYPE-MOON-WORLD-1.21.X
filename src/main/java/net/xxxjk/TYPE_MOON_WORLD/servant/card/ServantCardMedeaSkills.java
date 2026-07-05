package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWInterceptorSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EnkiduEarthWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaBeamEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ProjectionCircuitEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RedSkeletonHajunEntity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenMedeaCraftScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaWorkshopHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

import static net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils.*;

public final class ServantCardMedeaSkills {
   private static final String WORKSHOP_ACTIVE_TAG = "ServantCardMedeaWorkshopActive";
   private static final String WORKSHOP_X_TAG = "ServantCardMedeaWorkshopX";
   private static final String WORKSHOP_Y_TAG = "ServantCardMedeaWorkshopY";
   private static final String WORKSHOP_Z_TAG = "ServantCardMedeaWorkshopZ";
   private static final String WORKSHOP_RADIUS_TAG = "ServantCardMedeaWorkshopRadius";
   private static final String MINOR_MAGIC_ACTIVE_TAG = "ServantCardMedeaMinorMagicActive";
   private static final String MINOR_MAGIC_LAST_TICK_TAG = "ServantCardMedeaMinorMagicLastTick";
   private static final String MINOR_MAGIC_MODE_TAG = "ServantCardMedeaMinorMagicMode";
   private static final String STOCKS_READY_TAG = "ServantCardMedeaStocksReady";
   private static final String DRAGONFANG_STOCK_TAG = "ServantCardMedeaDragonfangStock";
   private static final String MANA_CHARM_STOCK_TAG = "ServantCardMedeaManaCharmStock";
   private static final String HEAL_CHARM_STOCK_TAG = "ServantCardMedeaHealCharmStock";
   private static final String LAST_STOCK_SYNC_TAG = "ServantCardMedeaLastStockSync";
   private static final String CARD_SUMMON_TAG = "ServantCardMedeaSummon";
   private static final String CARD_SUMMON_OWNER_TAG = "ServantCardMedeaSummoner";
   private static final double WORKSHOP_RADIUS = 15.0;

   private ServantCardMedeaSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"medea".equals(vars.servant_card_id)) {
         clear(player, vars);
         return;
      }
      ensureMedeaStocks(player, vars);
      tickMedeaWorkshop(player, vars);
      tickMedeaMinorMagic(player, vars);
      tickMedeaDragonfangs(player, vars);
   }

   private static void clear(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(WORKSHOP_ACTIVE_TAG)
         && !data.getBoolean(MINOR_MAGIC_ACTIVE_TAG)
         && !data.getBoolean(STOCKS_READY_TAG)
         && vars.servant_card_medea_dragonfang_stock == 0
         && vars.servant_card_medea_mana_charm_stock == 0
         && vars.servant_card_medea_heal_charm_stock == 0) {
         return;
      }
      recallMedeaDragonfangs(player, vars, false);
      data.remove(WORKSHOP_ACTIVE_TAG);
      data.remove(WORKSHOP_X_TAG);
      data.remove(WORKSHOP_Y_TAG);
      data.remove(WORKSHOP_Z_TAG);
      data.remove(WORKSHOP_RADIUS_TAG);
      data.remove(MINOR_MAGIC_ACTIVE_TAG);
      data.remove(MINOR_MAGIC_LAST_TICK_TAG);
      data.remove(MINOR_MAGIC_MODE_TAG);
      data.remove(STOCKS_READY_TAG);
      data.remove(DRAGONFANG_STOCK_TAG);
      data.remove(MANA_CHARM_STOCK_TAG);
      data.remove(HEAL_CHARM_STOCK_TAG);
      vars.servant_card_medea_dragonfang_stock = 0;
      vars.servant_card_medea_mana_charm_stock = 0;
      vars.servant_card_medea_heal_charm_stock = 0;
   }

   public static boolean performMedeaWorkshop(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      CompoundTag data = player.getPersistentData();
      if (data.getBoolean(WORKSHOP_ACTIVE_TAG)) {
         if (!isInsideMedeaWorkshop(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.medea_workshop_need_inside"), true);
            return false;
         }
         clearMedeaWorkshop(data);
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.4, player.getZ(), 30, 0.7, 0.2, 0.7, 0.035);
         level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.75F, 1.25F);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.medea_workshop_removed"), true);
         return true;
      }
      if (!ServantCardManaService.consume(player, vars, 30.0)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      data.putBoolean(WORKSHOP_ACTIVE_TAG, true);
      data.putDouble(WORKSHOP_X_TAG, player.getX());
      data.putDouble(WORKSHOP_Y_TAG, player.getY());
      data.putDouble(WORKSHOP_Z_TAG, player.getZ());
      data.putDouble(WORKSHOP_RADIUS_TAG, WORKSHOP_RADIUS);
      VFXServerEffects.spawn(level, "servant_medea_workshop", player, 128.0);
      spawnMedeaWorkshopHighlight(level, workshopCenter(data), WORKSHOP_RADIUS, true);
      level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.85F);
      return true;
   }

   public static boolean toggleMedeaMinorMagic(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      boolean active = !data.getBoolean(MINOR_MAGIC_ACTIVE_TAG);
      data.putBoolean(MINOR_MAGIC_ACTIVE_TAG, active);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(active ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.45, 0.35, 0.45, 0.02);
         level.playSound(null, player.blockPosition(), active ? SoundEvents.ENCHANTMENT_TABLE_USE : SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.75F, active ? 1.35F : 0.75F);
      }
      player.displayClientMessage(Component.translatable(active ? "message.typemoonworld.servant_card.medea_minor_on" : "message.typemoonworld.servant_card.medea_minor_off"), true);
      return true;
   }

   public static boolean performMedeaCraftItem(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ensureMedeaStocks(player, vars);
      PacketDistributor.sendToPlayer(
         player,
         new OpenMedeaCraftScreenMessage(getDragonfangStock(player), getManaCharmStock(player), getHealCharmStock(player)),
         new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]
      );
      return true;
   }

   public static boolean craftSelectedMedeaItem(ServerPlayer player, int choice) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"medea".equals(vars.servant_card_id)) {
         return false;
      }
      ensureMedeaStocks(player, vars);
      if (!canCraftMedeaChoice(player, choice)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.medea_stock_full"), true);
         return false;
      }
      if (!ServantCardManaService.consume(player, vars, adjustedMedeaCost(player, 30.0))) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      Component made;
      if (choice == 0) {
         setDragonfangStock(player, vars, getDragonfangStock(player) + 1);
         made = Component.translatable("hud.typemoonworld.servant_card.medea_dragonfang");
      } else if (choice == 1) {
         setManaCharmStock(player, vars, getManaCharmStock(player) + 1);
         made = Component.translatable("hud.typemoonworld.servant_card.medea_mana_charm");
      } else {
         setHealCharmStock(player, vars, getHealCharmStock(player) + 1);
         made = Component.translatable("hud.typemoonworld.servant_card.medea_heal_charm");
      }
      syncMedeaStocks(player, vars);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 28, 0.45, 0.35, 0.45, 0.02);
         level.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.9F, 1.2F);
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.medea_crafted", made), true);
      return true;
   }

   public static boolean performMedeaDragonfang(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ensureMedeaStocks(player, vars);
      int active = countOwnedMedeaDragonfangs(level, player, 160.0);
      if (active > 0) {
         int recalled = recallMedeaDragonfangs(player, vars, true);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.medea_dragonfang_recalled", recalled), true);
         return true;
      }
      int stock = getDragonfangStock(player);
      if (stock <= 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.medea_no_dragonfang"), true);
         return false;
      }
      int count = Math.min(stock, MedeaWorkshopHelper.MAX_ACTIVE_DRAGONFANG);
      Vec3 look = PlayerNoblePhantasmHelper.horizontalLook(player);
      for (int i = 0; i < count; i++) {
         DragonfangSoldierEntity summon = ModEntities.DRAGONFANG_SOLDIER.get().create(level);
         if (summon == null) {
            continue;
         }
         double offsetIndex = i - (count - 1) * 0.5;
         Vec3 side = new Vec3(-look.z, 0.0, look.x).scale(offsetIndex * 1.35);
         summon.moveTo(player.getX() + look.x * 2.0 + side.x, player.getY(), player.getZ() + look.z * 2.0 + side.z, player.getYRot(), 0.0F);
         summon.finalizeSpawn(level, level.getCurrentDifficultyAt(summon.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
         summon.setSummoner(player);
         summon.getPersistentData().putBoolean(CARD_SUMMON_TAG, true);
         summon.getPersistentData().putString(CARD_SUMMON_OWNER_TAG, player.getUUID().toString());
         level.addFreshEntity(summon);
         level.sendParticles(ParticleTypes.SOUL, summon.getX(), summon.getY() + 0.4, summon.getZ(), 18, 0.28, 0.25, 0.28, 0.035);
      }
      setDragonfangStock(player, vars, stock - count);
      syncMedeaStocks(player, vars);
      level.playSound(null, player.blockPosition(), SoundEvents.BONE_BLOCK_PLACE, SoundSource.PLAYERS, 1.0F, 0.75F);
      return true;
   }

   public static void performMedeaBlinkVolley(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 look = player.getLookAngle().normalize();
      Vec3 back = PlayerNoblePhantasmHelper.horizontalLook(player).scale(-3.0);
      player.teleportTo(player.getX() + back.x, player.getY() + 0.15, player.getZ() + back.z);
      player.hurtMarked = true;
      LivingEntity target = findLookTarget(player, 22.0, 2.0);
      Vec3 aim = target == null ? player.getEyePosition().add(look.scale(20.0)) : target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      for (int i = 0; i < 5; i++) {
         MedeaMagicBoltEntity bolt = new MedeaMagicBoltEntity(level, player);
         bolt.setMagicDamage(14.0F);
         bolt.setMode(i % 2 == 0 ? MedeaMagicBoltEntity.Mode.BOLT : MedeaMagicBoltEntity.Mode.FROST_BOLT);
         Vec3 spawn = player.getEyePosition().add((player.getRandom().nextDouble() - 0.5) * 1.2, (player.getRandom().nextDouble() - 0.5) * 0.7, (player.getRandom().nextDouble() - 0.5) * 1.2);
         bolt.setPos(spawn.x, spawn.y, spawn.z);
         Vec3 dir = aim.add((player.getRandom().nextDouble() - 0.5) * 1.3, (player.getRandom().nextDouble() - 0.5) * 0.8, (player.getRandom().nextDouble() - 0.5) * 1.3).subtract(spawn).normalize();
         bolt.setDeltaMovement(dir.scale(1.8));
         level.addFreshEntity(bolt);
      }
      level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.8, player.getZ(), 34, 0.45, 0.45, 0.45, 0.08);
      level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.75F, 1.35F);
   }

   public static void performMedeaBind(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 20.0, 2.0);
      if (target == null) {
         return;
      }
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_medea_bind", target, 96.0);
         level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 24, 0.45, 0.55, 0.45, 0.035);
      }
   }

   public static void performMedeaBarrier(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 220, 1, false, true, true));
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      double radius = 6.0;
      for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius), p -> p.isAlive() && p.getOwner() != player)) {
         Vec3 away = projectile.position().subtract(player.position());
         Vec3 dir = away.lengthSqr() < 0.01 ? player.getLookAngle().scale(-1.0) : away.normalize();
         projectile.setDeltaMovement(dir.scale(1.8).add(0.0, 0.18, 0.0));
         projectile.hurtMarked = true;
      }
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2.0, radius), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 away = living.position().subtract(player.position());
         if (away.horizontalDistanceSqr() > radius * radius || away.horizontalDistanceSqr() < 0.01) {
            continue;
         }
         Vec3 dir = new Vec3(away.x, 0.0, away.z).normalize();
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), 10.0F);
         living.invulnerableTime = 0;
         living.push(dir.x * 1.15, 0.28, dir.z * 1.15);
         living.hurtMarked = true;
      }
      VFXServerEffects.spawn(level, "servant_medea_barrier", player, 128.0);
      for (double r = 1.0; r <= radius; r += 1.0) {
         int points = Math.max(12, Mth.floor(r * 7.0));
         for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            level.sendParticles(ParticleTypes.ENCHANT, player.getX() + Math.cos(angle) * r, player.getY() + 0.7, player.getZ() + Math.sin(angle) * r, 1, 0.0, 0.08, 0.0, 0.015);
         }
      }
      level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.25F);
   }

   public static boolean hasMedeaWorkshop(ServerPlayer player) {
      return player.getPersistentData().getBoolean(WORKSHOP_ACTIVE_TAG);
   }

   public static boolean performMedeaEscape(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(WORKSHOP_ACTIVE_TAG)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.medea_no_workshop"), true);
         return false;
      }
      Vec3 center = workshopCenter(data);
      level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.8, player.getZ(), 30, 0.45, 0.55, 0.45, 0.08);
      player.teleportTo(center.x, center.y + 0.2, center.z);
      player.hurtMarked = true;
      player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, true, true));
      level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.8, player.getZ(), 30, 0.45, 0.55, 0.45, 0.08);
      spawnMedeaWorkshopHighlight(level, center, Math.max(3.0, data.getDouble(WORKSHOP_RADIUS_TAG)), false);
      level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.85F, 1.1F);
      return true;
   }

   public static void performMedeaThunder(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 24.0, 2.5);
      final UUID targetUuid = target == null ? null : target.getUUID();
      final Vec3 fixedCenter = target == null ? player.position().add(player.getLookAngle().normalize().scale(8.0)) : target.position();
      level.playSound(null, BlockPos.containing(fixedCenter), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.0F, 1.25F);
      for (int delay = 0; delay <= 35; delay += 5) {
         final int wave = delay / 5;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> strikeMedeaThunderstorm(player, targetUuid, fixedCenter, wave));
      }
   }

   public static void performMedeaBeam(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 30.0, 2.5);
      int shotCount = isInsideMedeaWorkshop(player) ? 5 : 3;
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.25F);
      for (int i = 0; i < shotCount; i++) {
         final int index = i;
         final UUID targetUuid = target == null ? null : target.getUUID();
         final Vec3 fallbackEnd = player.getEyePosition().add(player.getLookAngle().normalize().scale(30.0 + i * 2.0));
         TYPE_MOON_WORLD.queueServerWork(i * 6 + 1, () -> fireMedeaBeam(player, targetUuid, fallbackEnd, index, shotCount));
      }
   }

   private static void strikeMedeaThunderstorm(ServerPlayer player, UUID targetUuid, Vec3 fixedCenter, int wave) {
      if (!(player.level() instanceof ServerLevel level) || !player.isAlive()) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"medea".equals(vars.servant_card_id)) {
         return;
      }
      Vec3 center = fixedCenter;
      if (targetUuid != null && level.getEntity(targetUuid) instanceof LivingEntity target && target.isAlive()) {
         center = target.position();
      }

      double radius = 5.75;
      float damage = (float)applyMedeaWorkshopDamage(player, 8.0);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(center, center).inflate(radius, 3.2, radius),
         e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), damage);
         living.invulnerableTime = 0;
      }

      for (int i = 0; i < 3; i++) {
         Vec3 strike = center.add((player.getRandom().nextDouble() - 0.5) * radius * 1.65, 0.0, (player.getRandom().nextDouble() - 0.5) * radius * 1.65);
         spawnMedeaLightning(level, strike, true);
      }
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1.0, center.z, 2, 1.4, 0.5, 1.4, 0.0);
      for (int i = 0; i < 7; i++) {
         level.sendParticles(
            ParticleTypes.ELECTRIC_SPARK,
            center.x + (player.getRandom().nextDouble() - 0.5) * radius * 1.8,
            center.y + 0.25 + player.getRandom().nextDouble() * 3.2,
            center.z + (player.getRandom().nextDouble() - 0.5) * radius * 1.8,
            10,
            0.1,
            0.45,
            0.1,
            0.06
         );
      }
      if (wave % 2 == 0) {
         level.playSound(null, BlockPos.containing(center), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.75F, 1.45F + wave * 0.03F);
      }
   }

   private static void fireMedeaBeam(ServerPlayer player, UUID targetUuid, Vec3 fallbackEnd, int index, int shotCount) {
      if (!(player.level() instanceof ServerLevel level) || !player.isAlive()) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"medea".equals(vars.servant_card_id)) {
         return;
      }
      Vec3 look = player.getLookAngle().normalize();
      Vec3 side = new Vec3(-look.z, 0.0, look.x);
      if (side.lengthSqr() < 0.0001) {
         side = new Vec3(1.0, 0.0, 0.0);
      } else {
         side = side.normalize();
      }
      double spread = shotCount <= 1 ? 0.0 : (index - (shotCount - 1) * 0.5) * 0.42;
      Vec3 start = player.getEyePosition().add(look.scale(0.55)).add(side.scale(spread)).add(0.0, 0.08 * Math.sin(index), 0.0);
      Vec3 end = fallbackEnd;
      if (targetUuid != null && level.getEntity(targetUuid) instanceof LivingEntity target && target.isAlive()) {
         end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).add(side.scale(spread * 0.35));
      }
      MedeaBeamEffectEntity beam = new MedeaBeamEffectEntity(level, player, start, end, (float)applyMedeaWorkshopDamage(player, 26.0), 12);
      beam.setBreakBlocks(true);
      level.addFreshEntity(beam);
      spawnMedeaBeamCircle(level, start, look, 0.9 + index * 0.08);
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 12, 0.08, 0.08, 0.08, 0.02);
      level.playSound(null, BlockPos.containing(start), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.85F, 1.25F + index * 0.08F);
   }

   private static void spawnMedeaLightning(ServerLevel level, Vec3 center, boolean visualOnly) {
      LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
      if (lightning == null) {
         return;
      }
      lightning.moveTo(center.x, center.y, center.z);
      lightning.setVisualOnly(visualOnly);
      level.addFreshEntity(lightning);
   }

   private static void spawnMedeaBeamCircle(ServerLevel level, Vec3 start, Vec3 look, double radius) {
      Vec3 axis = look.normalize();
      Vec3 side = new Vec3(-axis.z, 0.0, axis.x);
      if (side.lengthSqr() < 0.0001) {
         side = new Vec3(1.0, 0.0, 0.0);
      } else {
         side = side.normalize();
      }
      Vec3 up = axis.cross(side).normalize();
      for (int i = 0; i < 32; i++) {
         double angle = Math.PI * 2.0 * i / 32.0;
         Vec3 pos = start.add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
         level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
         if (i % 4 == 0) {
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 1, 0.01, 0.01, 0.01, 0.0);
         }
      }
   }

   private static void ensureMedeaStocks(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(STOCKS_READY_TAG)) {
         data.putBoolean(STOCKS_READY_TAG, true);
         data.putInt(DRAGONFANG_STOCK_TAG, Math.max(data.getInt(DRAGONFANG_STOCK_TAG), 12));
         data.putInt(MANA_CHARM_STOCK_TAG, Math.max(data.getInt(MANA_CHARM_STOCK_TAG), 2));
         data.putInt(HEAL_CHARM_STOCK_TAG, Math.max(data.getInt(HEAL_CHARM_STOCK_TAG), 2));
      }
      syncMedeaStocks(player, vars);
   }

   private static void syncMedeaStocks(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      int dragonfang = getDragonfangStock(player);
      int manaCharm = getManaCharmStock(player);
      int healCharm = getHealCharmStock(player);
      boolean changed = vars.servant_card_medea_dragonfang_stock != dragonfang
         || vars.servant_card_medea_mana_charm_stock != manaCharm
         || vars.servant_card_medea_heal_charm_stock != healCharm;
      vars.servant_card_medea_dragonfang_stock = dragonfang;
      vars.servant_card_medea_mana_charm_stock = manaCharm;
      vars.servant_card_medea_heal_charm_stock = healCharm;
      long now = player.level().getGameTime();
      CompoundTag data = player.getPersistentData();
      if (changed || now - data.getLong(LAST_STOCK_SYNC_TAG) >= 40L) {
         data.putLong(LAST_STOCK_SYNC_TAG, now);
         vars.syncPlayerVariables(player);
      }
   }

   private static int getDragonfangStock(ServerPlayer player) {
      return Mth.clamp(player.getPersistentData().getInt(DRAGONFANG_STOCK_TAG), 0, MedeaWorkshopHelper.MAX_DRAGONFANG_STOCK);
   }

   private static int getManaCharmStock(ServerPlayer player) {
      return Mth.clamp(player.getPersistentData().getInt(MANA_CHARM_STOCK_TAG), 0, MedeaWorkshopHelper.MAX_MANA_CHARM_STOCK);
   }

   private static int getHealCharmStock(ServerPlayer player) {
      return Mth.clamp(player.getPersistentData().getInt(HEAL_CHARM_STOCK_TAG), 0, MedeaWorkshopHelper.MAX_HEAL_CHARM_STOCK);
   }

   private static void setDragonfangStock(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int value) {
      player.getPersistentData().putInt(DRAGONFANG_STOCK_TAG, Mth.clamp(value, 0, MedeaWorkshopHelper.MAX_DRAGONFANG_STOCK));
      vars.servant_card_medea_dragonfang_stock = getDragonfangStock(player);
   }

   private static void setManaCharmStock(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int value) {
      player.getPersistentData().putInt(MANA_CHARM_STOCK_TAG, Mth.clamp(value, 0, MedeaWorkshopHelper.MAX_MANA_CHARM_STOCK));
      vars.servant_card_medea_mana_charm_stock = getManaCharmStock(player);
   }

   private static void setHealCharmStock(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int value) {
      player.getPersistentData().putInt(HEAL_CHARM_STOCK_TAG, Mth.clamp(value, 0, MedeaWorkshopHelper.MAX_HEAL_CHARM_STOCK));
      vars.servant_card_medea_heal_charm_stock = getHealCharmStock(player);
   }

   private static boolean canCraftMedeaChoice(ServerPlayer player, int choice) {
      return switch (choice) {
         case 0 -> getDragonfangStock(player) < MedeaWorkshopHelper.MAX_DRAGONFANG_STOCK;
         case 1 -> getManaCharmStock(player) < MedeaWorkshopHelper.MAX_MANA_CHARM_STOCK;
         case 2 -> getHealCharmStock(player) < MedeaWorkshopHelper.MAX_HEAL_CHARM_STOCK;
         default -> false;
      };
   }

   private static void tickMedeaWorkshop(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(WORKSHOP_ACTIVE_TAG)) {
         return;
      }
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = workshopCenter(data);
      double radius = data.getDouble(WORKSHOP_RADIUS_TAG);
      if (radius <= 0.0) {
         radius = WORKSHOP_RADIUS;
         data.putDouble(WORKSHOP_RADIUS_TAG, radius);
      }
      if (player.tickCount % 20 == 0) {
         spawnMedeaWorkshopHighlight(level, center, radius, false);
      }
      if (isInsideMedeaWorkshop(player) && player.tickCount % 10 == 0) {
         vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + 5.0);
         vars.syncPlayerVariables(player);
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 0, false, false, false));
      }
   }

   private static void tickMedeaMinorMagic(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(MINOR_MAGIC_ACTIVE_TAG) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long now = level.getGameTime();
      if (now - data.getLong(MINOR_MAGIC_LAST_TICK_TAG) < 12L) {
         return;
      }
      LivingEntity target = findLookTarget(player, 24.0, 2.2);
      if (target == null) {
         return;
      }
      double cost = adjustedMedeaCost(player, 5.0);
      if (!ServantCardManaService.consume(player, vars, cost)) {
         data.putBoolean(MINOR_MAGIC_ACTIVE_TAG, false);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.medea_minor_off"), true);
         return;
      }
      data.putLong(MINOR_MAGIC_LAST_TICK_TAG, now);
      int modeIndex = Math.floorMod(data.getInt(MINOR_MAGIC_MODE_TAG), 4);
      data.putInt(MINOR_MAGIC_MODE_TAG, modeIndex + 1);
      Vec3 spawn = player.getEyePosition().add(player.getLookAngle().normalize().scale(0.55));
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      if (modeIndex == 3) {
         fireMedeaMinorBeam(player, level, spawn, aim);
         target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 35, 0, false, true, true));
         return;
      }
      MedeaMagicBoltEntity.Mode mode = switch (modeIndex) {
         case 1 -> MedeaMagicBoltEntity.Mode.FIRE_BOLT;
         case 2 -> MedeaMagicBoltEntity.Mode.FROST_BOLT;
         default -> MedeaMagicBoltEntity.Mode.BOLT;
      };
      Vec3 dir = aim.subtract(spawn).normalize();
      MedeaMagicBoltEntity bolt = new MedeaMagicBoltEntity(level, player);
      bolt.setMode(mode);
      bolt.setMagicDamage((float)applyMedeaWorkshopDamage(player, mode == MedeaMagicBoltEntity.Mode.BOLT ? 10.0 : 13.0));
      bolt.setPos(spawn.x, spawn.y, spawn.z);
      bolt.shoot(dir.x, dir.y, dir.z, 2.55F, 0.08F);
      level.addFreshEntity(bolt);
      VFXServerEffects.spawn(level, mode == MedeaMagicBoltEntity.Mode.FROST_BOLT ? "medea_magic_orb" : "medea_magic_orb", spawn, 80.0);
   }

   private static void fireMedeaMinorBeam(ServerPlayer player, ServerLevel level, Vec3 start, Vec3 end) {
      Vec3 look = end.subtract(start).normalize();
      MedeaBeamEffectEntity beam = new MedeaBeamEffectEntity(level, player, start, end, (float)applyMedeaWorkshopDamage(player, 14.0), 7);
      beam.setBreakBlocks(false);
      level.addFreshEntity(beam);
      spawnMedeaBeamCircle(level, start, look, 0.45);
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 8, 0.05, 0.05, 0.05, 0.015);
      level.playSound(null, BlockPos.containing(start), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.45F, 1.7F);
   }

   private static void tickMedeaDragonfangs(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 0) {
         return;
      }
      int recalled = 0;
      for (DragonfangSoldierEntity summon : level.getEntitiesOfClass(
         DragonfangSoldierEntity.class,
         new AABB(player.position(), player.position()).inflate(192.0),
         summon -> isOwnedMedeaDragonfang(player, summon) && summon.distanceToSqr(player) > 48.0 * 48.0
      )) {
         if (summon.isAlive()) {
            summon.discard();
            recalled++;
         }
      }
      if (recalled > 0) {
         setDragonfangStock(player, vars, getDragonfangStock(player) + recalled);
         syncMedeaStocks(player, vars);
      }
   }

   private static int recallMedeaDragonfangs(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, boolean refundStock) {
      if (!(player.level() instanceof ServerLevel level)) {
         return 0;
      }
      int recalled = 0;
      for (DragonfangSoldierEntity summon : level.getEntitiesOfClass(
         DragonfangSoldierEntity.class,
         new AABB(player.position(), player.position()).inflate(192.0),
         summon -> isOwnedMedeaDragonfang(player, summon)
      )) {
         if (summon.isAlive()) {
            summon.discard();
            recalled++;
         }
      }
      if (refundStock && recalled > 0) {
         setDragonfangStock(player, vars, getDragonfangStock(player) + recalled);
         syncMedeaStocks(player, vars);
      }
      return recalled;
   }

   private static int countOwnedMedeaDragonfangs(ServerLevel level, ServerPlayer player, double radius) {
      return level.getEntitiesOfClass(
         DragonfangSoldierEntity.class,
         new AABB(player.position(), player.position()).inflate(radius),
         summon -> isOwnedMedeaDragonfang(player, summon)
      ).size();
   }

   private static boolean isOwnedMedeaDragonfang(ServerPlayer player, DragonfangSoldierEntity summon) {
      if (summon == null || !summon.isAlive()) {
         return false;
      }
      UUID owner = summon.getSummonerUuid();
      if (player.getUUID().equals(owner)) {
         return true;
      }
      CompoundTag data = summon.getPersistentData();
      return data.getBoolean(CARD_SUMMON_TAG) && player.getUUID().toString().equals(data.getString(CARD_SUMMON_OWNER_TAG));
   }

   private static boolean isInsideMedeaWorkshop(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(WORKSHOP_ACTIVE_TAG)) {
         return false;
      }
      Vec3 center = workshopCenter(data);
      double radius = data.getDouble(WORKSHOP_RADIUS_TAG);
      return center.distanceToSqr(player.position()) <= radius * radius;
   }

   private static Vec3 workshopCenter(CompoundTag data) {
      return new Vec3(data.getDouble(WORKSHOP_X_TAG), data.getDouble(WORKSHOP_Y_TAG), data.getDouble(WORKSHOP_Z_TAG));
   }

   private static void clearMedeaWorkshop(CompoundTag data) {
      data.remove(WORKSHOP_ACTIVE_TAG);
      data.remove(WORKSHOP_X_TAG);
      data.remove(WORKSHOP_Y_TAG);
      data.remove(WORKSHOP_Z_TAG);
      data.remove(WORKSHOP_RADIUS_TAG);
   }

   private static void spawnMedeaWorkshopHighlight(ServerLevel level, Vec3 center, double radius, boolean burst) {
      level.addFreshEntity(new ProjectionCircuitEffectEntity(level, center.x, center.y + 0.06, center.z, (float)(burst ? 1.0 : radius * 0.92), (float)radius, burst ? 0.32F : 0.18F, burst ? 34 : 26, 0x8E72FF));
      int points = burst ? 96 : 64;
      for (int i = 0; i < points; i++) {
         double angle = Math.PI * 2.0 * i / points;
         double x = center.x + Math.cos(angle) * radius;
         double z = center.z + Math.sin(angle) * radius;
         level.sendParticles(ParticleTypes.ENCHANT, x, center.y + 0.16, z, 1, 0.0, 0.015, 0.0, 0.0);
         if (burst && (i % 8 == 0)) {
            level.sendParticles(ParticleTypes.WITCH, x, center.y + 0.18, z, 1, 0.02, 0.04, 0.02, 0.0);
         }
      }
   }

   private static double adjustedMedeaCost(ServerPlayer player, double baseCost) {
      return isInsideMedeaWorkshop(player) ? baseCost * 0.8 : baseCost;
   }

   private static double applyMedeaWorkshopDamage(ServerPlayer player, double baseDamage) {
      return isInsideMedeaWorkshop(player) ? baseDamage * 1.3 : baseDamage;
   }

}

