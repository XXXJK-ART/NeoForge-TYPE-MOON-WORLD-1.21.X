package com.captain.servant.compat.superbwarfare;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunDataKt;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.captain.servant.TypeMoonServant;
import com.captain.servant.lancelot.KnightArmoryEntry;
import com.captain.servant.lancelot.KnightArmoryService;
import com.captain.servant.lancelot.KnightArmoryRules;
import com.captain.servant.spiritual.SpiritualizationService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PlayerVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardLancelotBerserkerSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;
import org.joml.Vector3f;

public final class SuperbWarfareIntegration implements SuperbWarfareBridge {
   public static final String VEHICLE_TAG = "FateSupportKnightVehicle";
   public static final String VEHICLE_OWNER_TAG = "FateSupportKnightVehicleOwner";
   public static final String VEHICLE_EXPIRES_TAG = "FateSupportKnightVehicleExpires";
   public static final String VEHICLE_A10_TAG = "FateSupportKnightVehicleA10";
   public static final String VEHICLE_SUMMONED_TAG = "FateSupportKnightVehicleSummoned";
   public static final String VEHICLE_NPC_TAG = "FateSupportKnightVehicleNpc";
   public static final String VEHICLE_PENDING_UNTIL_TAG = "FateSupportKnightVehiclePendingUntil";
   public static final String VEHICLE_ORIGINAL_HEALTH_TAG = "FateSupportKnightVehicleOriginalHealth";
   public static final String PROJECTILE_TAG = "FateSupportKnightProjectile";
   public static final String PROJECTILE_OWNER_TAG = "FateSupportKnightProjectileOwner";
   public static final String PROJECTILE_VEHICLE_TAG = "FateSupportKnightProjectileVehicle";
   public static final String RENDER_OVERRIDE_MARKER = "\"FateSupportKnightOfOwner\":true";
   public static final String VISUAL_OVERRIDE_MARKER = "\"FateSupportKnightVisual\":true";
   public static final String PROJECTILE_RENDER_MARKER = "FateSupportKnightProjectile";
   private static final String CRASH_GRACE_TAG = "FateSupportKnightVehicleCrashGrace";
   private static final String NPC_A10_COOLDOWN_TAG = "FateSupportKnightNpcA10Cooldown";
   private static final String NPC_WEAPON_COOLDOWN_TAG = "FateSupportKnightNpcWeaponCooldown";
   private static final String NPC_LAST_WEAPON_TAG = "FateSupportKnightNpcLastWeapon";
   private static final String NPC_HEAVY_LOCK_UNTIL_TAG = "FateSupportKnightNpcHeavyLockUntil";
   private static final String SUMMONED_A10_UUID_TAG = "FateSupportKnightSummonedA10";
   private static final String SUMMONED_A10_EXPIRES_TAG = "FateSupportKnightSummonedA10Expires";
   private static final String VEHICLE_DAMAGE_PROXY_UUID_TAG = "FateSupportVehicleDamageProxy";
   private static final String DAMAGE_PROXY_TAG = "FateSupportVehicleDamageProxyEntity";
   private static final String DAMAGE_PROXY_VEHICLE_UUID_TAG = "FateSupportVehicleDamageProxyTarget";
   private static final String LAST_TYPE_MOON_DAMAGE_TICK_TAG = "FateSupportLastTypeMoonDamageTick";
   private static final String LAST_TYPE_MOON_DAMAGE_SOURCE_TAG = "FateSupportLastTypeMoonDamageSource";
   private static final String LAST_TYPE_MOON_DAMAGE_AMOUNT_TAG = "FateSupportLastTypeMoonDamageAmount";
   private static final String VEHICLE_DETONATED_TAG = "FateSupportKnightVehicleDetonated";
   private static final TagKey<Item> SNIPER_GUN_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("superbwarfare", "sniper_rifle"));
   private static final TagKey<Item> LAUNCHER_GUN_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("superbwarfare", "launcher"));
   private static final DustParticleOptions DARK_DUST = new DustParticleOptions(new Vector3f(0.06F, 0.01F, 0.09F), 1.35F);

   @Override
   public boolean isGun(ItemStack stack) {
      return stack != null && !stack.isEmpty() && GunDataKt.isGunItem(stack);
   }

   @Override
   public void initializeOwnerizedGun(ItemStack stack) {
      if (this.isGun(stack)) {
         GunData data = GunDataKt.toGunData(stack);
         int magazine = Math.max(1, data.compute().getMagazine());
         data.ammo.set(magazine);
         if (hasInfiniteAmmo(stack)) {
            data.virtualAmmo.set(999999);
            data.backupAmmoCount.set(999999);
            if (KnightArmoryService.entryFor(stack) == KnightArmoryEntry.MINIGUN) {
               data.ammo.set(magazine);
            }
         }

         data.save();
      }
   }

   private static boolean hasInfiniteAmmo(ItemStack stack) {
      return !stack.is(SNIPER_GUN_TAG) && !stack.is(LAUNCHER_GUN_TAG);
   }

   private void maintainOwnerizedGun(ItemStack stack) {
      if (this.isGun(stack) && hasInfiniteAmmo(stack)) {
         GunData data = GunDataKt.toGunData(stack);
         if (KnightArmoryService.entryFor(stack) == KnightArmoryEntry.MINIGUN) {
            int magazine = Math.max(1, data.compute().getMagazine());
            data.ammo.set(magazine);
         }

         data.virtualAmmo.set(999999);
         data.backupAmmoCount.set(999999);
         data.save();
      }
   }

   @Override
   public int beginVehicleOwnerization(ServerPlayer player) {
      VehicleEntity vehicle = vehicleUnder(player);
      if (vehicle == null
         || vehicle.getPersistentData().getBoolean("FateSupportKnightVehicle")
         || vehicle.getPersistentData().contains("FateSupportKnightVehiclePendingUntil")) {
         return 0;
      } else if (LancelotCombatHelper.remainingKnightOfOwnerCapacity(player) <= 0) {
         player.displayClientMessage(Component.translatable("message.fatesupport.knight_vehicle.capacity"), true);
         return -1;
      } else {
         boolean a10 = vehicle instanceof A10Entity;
         double manaCost = a10 ? 500.0 : 300.0;
         PlayerVariables variables = (PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!ServantCardManaService.consume(player, variables, manaCost)) {
            return -1;
         } else {
            long completeAt = player.level().getGameTime() + 60L;
            CompoundTag data = vehicle.getPersistentData();
            data.putUUID("FateSupportKnightVehicleOwner", player.getUUID());
            data.putLong("FateSupportKnightVehiclePendingUntil", completeAt);
            data.putBoolean("FateSupportKnightVehicleA10", a10);
            Entity driver = vehicle.getFirstPassenger();
            if (driver != null && !driver.getUUID().equals(player.getUUID())) {
               driver.kill();
            }

            player.displayClientMessage(Component.translatable("message.fatesupport.knight_vehicle.assimilating"), true);
            return 1;
         }
      }
   }

   @Override
   public boolean summonA10(ServerPlayer player, boolean npcVariant) {
      return summonAircraft(player, false);
   }

   private boolean summonAircraft(LivingEntity owner, boolean npc) {
      if (!(owner.level() instanceof ServerLevel level) || owner.isPassenger()
            || LancelotCombatHelper.remainingKnightOfOwnerCapacity(owner) <= 0) return false;
      A10Entity aircraft = ModEntities.A_10A.get().create(level);
      if (aircraft == null) return false;
      aircraft.setPos(owner.getX(), owner.getY() + 15.0, owner.getZ());
      aircraft.setYRot(owner.getYRot());
      AABB launchBox = aircraft.getBoundingBox();
      if (launchBox.maxY >= level.getMaxBuildHeight()
            || !level.getWorldBorder().isWithinBounds(launchBox)
            || !level.noCollision(aircraft, launchBox)) {
         if (owner instanceof ServerPlayer player) player.displayClientMessage(
               Component.translatable("message.fatesupport.knight_armory.no_launch_space"), true);
         return false;
      }
      assimilateVehicle(aircraft, owner.getUUID(), KnightArmoryRules.A10_DURATION_TICKS, true, true, npc);
      aircraft.setEngineStart(true);
      aircraft.setEngineStartOver(true);
      aircraft.setPower(1.0F);
      aircraft.setGearUp(true);
      aircraft.setTargetSpeed(1.2);
      aircraft.setAbsoluteSpeed(1.2);
      Vec3 launch = new Vec3(owner.getLookAngle().x, 0.22, owner.getLookAngle().z);
      if (launch.horizontalDistanceSqr() < 1.0E-4) {
         float yaw = owner.getYRot() * (float)(Math.PI / 180.0);
         launch = new Vec3(-Mth.sin(yaw), 0.22, Mth.cos(yaw));
      }
      aircraft.setDeltaMovement(launch.normalize().scale(1.2));
      if (!level.addFreshEntity(aircraft)) return false;
      if (!owner.startRiding(aircraft, true)) {
         aircraft.discard();
         return false;
      }
      owner.getPersistentData().putUUID(SUMMONED_A10_UUID_TAG, aircraft.getUUID());
      owner.getPersistentData().putLong(SUMMONED_A10_EXPIRES_TAG,
            level.getGameTime() + KnightArmoryRules.A10_DURATION_TICKS);
      return true;
   }
   @Override
   public boolean hasActiveSummonedA10(ServerPlayer player) {
      if (!player.getPersistentData().hasUUID("FateSupportKnightSummonedA10")) {
         return false;
      } else {
         UUID summonedId = player.getPersistentData().getUUID("FateSupportKnightSummonedA10");

         for (ServerLevel level : player.getServer().getAllLevels()) {
            if (level.getEntity(summonedId) instanceof A10Entity a10 && !a10.isRemoved()) {
               return true;
            }
         }

         if (player.getPersistentData().getLong("FateSupportKnightSummonedA10Expires") > player.level().getGameTime()) {
            return true;
         } else {
            clearSummonedA10(player);
            return false;
         }
      }
   }

   @Override
   public int countOwnedVehicles(LivingEntity player) {
      int count = 0;

      for (ServerLevel level : player.getServer().getAllLevels()) {
         for (VehicleEntity vehicle : level.getEntitiesOfClass(
            VehicleEntity.class,
            new AABB(-3.0E7, -2048.0, -3.0E7, 3.0E7, 2048.0, 3.0E7),
            candidate -> (candidate.getPersistentData().getBoolean(VEHICLE_TAG)
                  || candidate.getPersistentData().contains(VEHICLE_PENDING_UNTIL_TAG))
                  && candidate.getPersistentData().hasUUID("FateSupportKnightVehicleOwner")
                  && candidate.getPersistentData().getUUID("FateSupportKnightVehicleOwner").equals(player.getUUID())
         )) {
            if (!vehicle.isRemoved()) {
               count++;
            }
         }
      }

      return count;
   }

   @Override
   public boolean forceTypeMoonDamage(Entity entity, DamageSource source, float amount) {
      if (!(entity instanceof VehicleEntity vehicle) || amount <= 0.0F) {
         return false;
      }

      if (!isTypeMoonDamage(entity, source)) {
         return false;
      } else {
         CompoundTag data = vehicle.getPersistentData();
         long gameTime = vehicle.level().getGameTime();
         Entity sourceEntity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
         int sourceId = sourceEntity == null ? 0 : sourceEntity.getId();
         if (data.getLong("FateSupportLastTypeMoonDamageTick") == gameTime
            && data.getInt("FateSupportLastTypeMoonDamageSource") == sourceId
            && Float.compare(data.getFloat("FateSupportLastTypeMoonDamageAmount"), amount) == 0) {
            return true;
         } else {
            data.putLong("FateSupportLastTypeMoonDamageTick", gameTime);
            data.putInt("FateSupportLastTypeMoonDamageSource", sourceId);
            data.putFloat("FateSupportLastTypeMoonDamageAmount", amount);
            vehicle.setHealth(vehicle.getHealth() - amount);
            vehicle.onHurt(amount, source.getEntity(), false);
            if (vehicle.getHealth() <= 0.0F) {
               vehicle.destroy();
            }

            return true;
         }
      }
   }

   @Override
   public void damageVehiclesInBox(ServerLevel level, AABB box, Entity owner, DamageSource source, float amount) {
      if (!(amount <= 0.0F)) {
         for (VehicleEntity vehicle : level.getEntitiesOfClass(VehicleEntity.class, box, candidate -> candidate.isAlive() && !isSameVehicle(owner, candidate))) {
            this.forceTypeMoonDamage(vehicle, source, amount);
         }
      }
   }

   @Override
   public void damageVehiclesInAnnulus(
      ServerLevel level, Vec3 center, double outerRadius, double innerRadius, Entity owner, DamageSource source, float amount, Set<Integer> damagedEntities
   ) {
      if (!(amount <= 0.0F) && !(outerRadius <= 0.0)) {
         AABB box = new AABB(center, center).inflate(outerRadius + 2.0);

         for (VehicleEntity vehicle : level.getEntitiesOfClass(VehicleEntity.class, box, candidate -> candidate.isAlive() && !isSameVehicle(owner, candidate))) {
            double distance = vehicle.position().distanceTo(center);
            if (distance <= outerRadius && distance > innerRadius && (damagedEntities == null || damagedEntities.add(vehicle.getId()))) {
               this.forceTypeMoonDamage(vehicle, source, amount);
            }
         }
      }
   }

   @Override
   public void damageVehiclesInForwardArc(ServerPlayer player, Vec3 direction, double range, float amount) {
      Vec3 origin = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
      Vec3 forward = new Vec3(direction.x, 0.0, direction.z);
      if (!(forward.lengthSqr() < 1.0E-4)) {
         forward = forward.normalize();
         AABB box = player.getBoundingBox().inflate(range, 3.0, range);
         DamageSource source = player.damageSources().playerAttack(player);

         for (VehicleEntity vehicle : player.level()
            .getEntitiesOfClass(VehicleEntity.class, box, candidate -> candidate.isAlive() && !isSameVehicle(player, candidate))) {
            Vec3 to = vehicle.position().subtract(origin);
            Vec3 horizontal = new Vec3(to.x, 0.0, to.z);
            if (horizontal.lengthSqr() <= range * range && horizontal.lengthSqr() > 1.0E-4 && horizontal.normalize().dot(forward) >= 0.45) {
               this.forceTypeMoonDamage(vehicle, source, amount);
            }
         }
      }
   }

   @Override
   public void damageVehiclesInBeam(ServerLevel level, Entity beam, LivingEntity owner, Vec3 start, Vec3 end, double width, double height, float amount) {
      Vec3 segment = end.subtract(start);
      double lengthSquared = segment.lengthSqr();
      if (!(amount <= 0.0F) && !(lengthSquared < 1.0E-4)) {
         AABB box = new AABB(start, end).inflate(width + 2.0, height + 2.0, width + 2.0);
         DamageSource source = owner.damageSources().mobProjectile(beam, owner);

         for (VehicleEntity vehicle : level.getEntitiesOfClass(VehicleEntity.class, box, candidate -> candidate.isAlive() && !isSameVehicle(owner, candidate))) {
            Vec3 relative = vehicle.position().subtract(start);
            double along = Mth.clamp(relative.dot(segment) / lengthSquared, 0.0, 1.0);
            Vec3 closest = start.add(segment.scale(along));
            Vec3 offset = vehicle.position().subtract(closest);
            if (Math.abs(offset.y) <= height && offset.x * offset.x + offset.z * offset.z <= width * width) {
               this.forceTypeMoonDamage(vehicle, source, amount);
            }
         }
      }
   }

   private static boolean isSameVehicle(Entity owner, VehicleEntity vehicle) {
      return owner == vehicle || vehicle.getPassengers().contains(owner) || owner != null && owner.isPassenger() && owner.getVehicle() == vehicle;
   }

   private static boolean isTypeMoonDamage(Entity target, DamageSource source) {
      ResourceLocation damageId = target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getKey(source.type());
      if (isTypeMoonId(damageId)) {
         return true;
      } else {
         Entity direct = source.getDirectEntity();
         Entity attacker = source.getEntity();
         return isTypeMoonEntity(direct) || isTypeMoonEntity(attacker) || isTransformedServant(attacker);
      }
   }

   private static boolean isTypeMoonEntity(Entity entity) {
      if (entity == null) {
         return false;
      } else {
         ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
         return isTypeMoonId(entityId) || entity.getClass().getName().startsWith("net.xxxjk.TYPE_MOON_WORLD.");
      }
   }

   private static boolean isTypeMoonId(ResourceLocation id) {
      return id != null && "typemoonworld".equals(id.getNamespace());
   }

   private static boolean isTransformedServant(Entity entity) {
      return entity instanceof ServerPlayer player
         ? ((PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).servant_card_transformed
         : false;
   }

   @Override
   public boolean shouldPrioritizeA10(Entity entity) {
      if (entity instanceof LancelotBerserkerEntity npc
         && canDeployNpcA10(npc, npc.getTarget())) {
         return true;
      }

      return false;
   }

   @Override
   public boolean tryDeployNpcA10(LancelotBerserkerEntity npc, LivingEntity target) {
      if (!canDeployNpcA10(npc, target)) {
         return false;
      }

      npc.setTarget(target);
      return this.deployNpcA10(npc, target);
   }

   @Override
   public boolean tryCounterNoblePhantasm(LancelotBerserkerEntity npc, ServantEntity attacker, LivingEntity target) {
      if (attacker == null
         || !attacker.isAlive()
         || attacker == npc
         || npc.isAlliedTo(attacker)
         || attacker.isAlliedTo(npc)
         || !canDeployNpcA10(npc, attacker)) {
         return false;
      }

      return this.tryDeployNpcA10(npc, attacker);
   }

   private static boolean canDeployNpcA10(LancelotBerserkerEntity npc, LivingEntity target) {
      if (!(npc.level() instanceof ServerLevel) || !npc.isAlive() || npc.isPassenger()
            || LancelotCombatHelper.isUnable(npc, npc.level().getGameTime())
            || !KnightArmoryRules.npcCanUseAtHealth(KnightArmoryEntry.A10, npc.getHealth(), npc.getMaxHealth())
            || !LancelotCombatHelper.canUseKnightOfOwner(npc)) {
         return false;
      }

      long now = npc.level().getGameTime();
      boolean activeA10 = npc.getPersistentData().getLong(SUMMONED_A10_EXPIRES_TAG) > now;
      if (!KnightArmoryRules.npcCanChooseHeavyWeapon(
            KnightArmoryService.entryFor(npc.getMainHandItem()) == KnightArmoryEntry.MINIGUN,
            activeA10, now, npc.getPersistentData().getLong(NPC_HEAVY_LOCK_UNTIL_TAG))) {
         return false;
      }

      if (target == null || !EntityUtils.isValidCombatTarget(npc, target)
            || ServantMasterProtection.isProtectedMaster(npc, target)) {
         return false;
      }

      return npc.getCurrentMp() >= KnightArmoryEntry.A10.manaCost()
            && KnightArmoryService.cooldownUntil(npc, KnightArmoryEntry.A10) <= now
            && npc.getPersistentData().getLong(SUMMONED_A10_EXPIRES_TAG) <= now
            && LancelotCombatHelper.remainingKnightOfOwnerCapacity(npc) > 0;
   }

   private boolean deployNpcA10(LancelotBerserkerEntity npc, LivingEntity target) {
      if (!canDeployNpcA10(npc, target)) {
         return false;
      }

      long now = npc.level().getGameTime();
      npc.setCurrentMp(npc.getCurrentMp() - KnightArmoryEntry.A10.manaCost());
      if (!this.summonAircraft(npc, true)) {
         npc.setCurrentMp(npc.getCurrentMp() + KnightArmoryEntry.A10.manaCost());
         return false;
      }

      KnightArmoryService.startCooldown(npc, KnightArmoryEntry.A10);
      npc.getPersistentData().putLong(NPC_HEAVY_LOCK_UNTIL_TAG,
            now + KnightArmoryRules.NPC_HEAVY_WEAPON_LOCK_TICKS);
      npc.getPersistentData().putInt(NPC_LAST_WEAPON_TAG, KnightArmoryEntry.A10.networkId());
      npc.getNavigation().stop();
      ServantCombatSystem.broadcastNoblePhantasmWindup(npc, target, 20, true);
      return true;
   }

   @SubscribeEvent
   public void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
      MinecraftServer server = event.getServer();

      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         this.tickPlayerGuns(player);
      }

      for (ServerLevel level : server.getAllLevels()) {
         this.tickLevel(level);
      }
   }

   private void tickLevel(ServerLevel level) {
      ArrayList<Entity> entities = new ArrayList<>();
      level.getAllEntities().forEach(entities::add);

      for (Entity entity : entities) {
         if (entity instanceof ArmorStand armorStand && armorStand.getPersistentData().getBoolean("FateSupportVehicleDamageProxyEntity")) {
            tickDamageProxy(armorStand, level);
            continue;
         }

         if (entity instanceof ItemEntity itemEntity && KnightArmoryService.isExpired(itemEntity.getItem(), level.getGameTime())) {
            itemEntity.discard();
            continue;
         }

         if (entity instanceof VehicleEntity vehicle) {
            this.maintainDamageProxy(vehicle, level);
            if (vehicle.getPersistentData().getBoolean("FateSupportKnightVehicle")
               || vehicle.getPersistentData().contains("FateSupportKnightVehiclePendingUntil")) {
               this.tickVehicle(vehicle, level);
            }
         } else if (entity instanceof LancelotBerserkerEntity npc) {
            this.tickLancelotNpc(npc, level);
         }

         if (entity.getPersistentData().getBoolean("FateSupportKnightProjectile") && entity.tickCount % 2 == 0) {
            level.sendParticles(DARK_DUST, entity.getX(), entity.getY(), entity.getZ(), 2, 0.08, 0.08, 0.08, 0.01);
         }
      }
   }

   private void tickVehicle(VehicleEntity vehicle, ServerLevel level) {
      CompoundTag data = vehicle.getPersistentData();
      long now = level.getGameTime();
      if (data.contains("FateSupportKnightVehiclePendingUntil")) {
         ServerPlayer assimilatingOwner = data.hasUUID(VEHICLE_OWNER_TAG)
               ? level.getServer().getPlayerList().getPlayer(data.getUUID(VEHICLE_OWNER_TAG)) : null;
         if (assimilatingOwner == null || assimilatingOwner.level() != level
               || !assimilatingOwner.isAlive() || !ServantCardLancelotBerserkerSkills.isActiveCard(assimilatingOwner)
               || LancelotCombatHelper.isAroundightMode(assimilatingOwner)) {
            data.remove(VEHICLE_PENDING_UNTIL_TAG);
            data.remove(VEHICLE_OWNER_TAG);
            data.remove(VEHICLE_A10_TAG);
            return;
         }
         level.sendParticles(ParticleTypes.LARGE_SMOKE, vehicle.getX(), vehicle.getY() + 1.0, vehicle.getZ(), 8, 1.2, 0.7, 1.2, 0.04);
         level.sendParticles(DARK_DUST, vehicle.getX(), vehicle.getY() + 1.0, vehicle.getZ(), 12, 1.4, 0.8, 1.4, 0.02);
         if (now >= data.getLong("FateSupportKnightVehiclePendingUntil")) {
            UUID owner = data.getUUID("FateSupportKnightVehicleOwner");
            boolean a10 = data.getBoolean("FateSupportKnightVehicleA10");
            data.remove("FateSupportKnightVehiclePendingUntil");
            assimilateVehicle(vehicle, owner, a10 ? KnightArmoryRules.A10_DURATION_TICKS : 1200L, a10, false, false);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
               player.displayClientMessage(Component.translatable("message.fatesupport.knight_vehicle.assimilated"), true);
            }
         }
      } else if (data.getBoolean("FateSupportKnightVehicle")) {
         UUID ownerId = data.hasUUID(VEHICLE_OWNER_TAG) ? data.getUUID(VEHICLE_OWNER_TAG) : null;
         Entity ownerEntity = ownerId == null ? null : level.getEntity(ownerId);
         boolean summoned = data.getBoolean(VEHICLE_SUMMONED_TAG);
         if (summoned && (!(ownerEntity instanceof LivingEntity living) || !living.isAlive()
               || !isLancelot(ownerEntity) || LancelotCombatHelper.isAroundightMode((LivingEntity) ownerEntity)
               || ownerEntity instanceof ServerPlayer player && SpiritualizationService.isRestricted(player))) {
            releasePassengers(vehicle, now);
            vehicle.discard();
            return;
         }
         if (data.getBoolean(VEHICLE_A10_TAG) && KnightArmoryRules.shouldExplode(now,
               data.getLong(VEHICLE_EXPIRES_TAG), vehicle.tickCount,
               vehicle.horizontalCollision || vehicle.getCrash() || hasRammingImpact(vehicle, ownerEntity, level),
               vehicle.verticalCollision || vehicle.onGround())) {
            detonateAircraft(vehicle, now);
            return;
         }
         if (now >= data.getLong("FateSupportKnightVehicleExpires")) {
            releasePassengers(vehicle, now);
            vehicle.discard();
         } else {
            vehicle.setEnergy(vehicle.getMaxEnergy());
            maintainVehicleAmmo(vehicle);
            UUID owner = data.hasUUID("FateSupportKnightVehicleOwner") ? data.getUUID("FateSupportKnightVehicleOwner") : null;

            for (Entity passenger : vehicle.getPassengers()) {
               if (isLancelot(passenger)) {
                  passenger.getPersistentData().putLong("FateSupportKnightVehicleCrashGrace", now + 100L);
               } else if (passenger instanceof LivingEntity) {
                  LivingEntity living = (LivingEntity)passenger;
                  if (vehicle.tickCount % 20 == 0) {
                     living.hurt(vehicle.damageSources().magic(), 5.0F);
                  }
               }
            }

            if (data.getBoolean("FateSupportKnightVehicleNpc")) {
               this.tickNpcVehicle(vehicle, level, owner);
            } else if (data.getBoolean("FateSupportKnightVehicleA10") && owner != null) {
               this.tickPlayerA10(vehicle, level, owner);
            }
         }
      }
   }

   private static void releasePassengers(VehicleEntity vehicle, long now) {
      for (Entity passenger : java.util.List.copyOf(vehicle.getPassengers())) {
         passenger.getPersistentData().putLong(CRASH_GRACE_TAG, now + 100L);
         passenger.stopRiding();
         passenger.fallDistance = 0.0F;
      }
   }

   private static void detonateAircraft(VehicleEntity vehicle, long now) {
      if (vehicle.isRemoved() || vehicle.getPersistentData().getBoolean(VEHICLE_DETONATED_TAG)) return;
      vehicle.getPersistentData().putBoolean(VEHICLE_DETONATED_TAG, true);
      releasePassengers(vehicle, now);
      vehicle.vehicleExplosion(vehicle.computed().getDestroyInfo());
      vehicle.discard();
   }

   private static boolean hasRammingImpact(VehicleEntity vehicle, Entity owner, ServerLevel level) {
      if (!(owner instanceof LivingEntity living)) return false;
      return !level.getEntitiesOfClass(LivingEntity.class, vehicle.getBoundingBox(),
            candidate -> candidate != owner && !vehicle.hasPassenger(candidate)
                  && !candidate.getPersistentData().getBoolean(DAMAGE_PROXY_TAG)
                  && EntityUtils.isValidCombatTarget(living, candidate)
                  && !ServantMasterProtection.isProtectedMaster(living, candidate)).isEmpty();
   }

   private void maintainDamageProxy(VehicleEntity vehicle, ServerLevel level) {
      boolean needed = level.players()
         .stream()
         .anyMatch(
            player -> player.distanceToSqr(vehicle) <= 65536.0
                  && ((PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).servant_card_transformed
         );
      CompoundTag vehicleData = vehicle.getPersistentData();
      ArmorStand proxy = null;
      if (vehicleData.hasUUID("FateSupportVehicleDamageProxy")
         && level.getEntity(vehicleData.getUUID("FateSupportVehicleDamageProxy")) instanceof ArmorStand armorStand
         && !armorStand.isRemoved()) {
         proxy = armorStand;
      }

      if (!needed) {
         if (proxy != null) {
            proxy.discard();
         }

         vehicleData.remove("FateSupportVehicleDamageProxy");
      } else {
         if (proxy == null) {
            proxy = (ArmorStand)EntityType.ARMOR_STAND.create(level);
            if (proxy == null) {
               return;
            }

            proxy.setInvisible(true);
            proxy.setNoGravity(true);
            proxy.setSilent(true);
            proxy.setNoBasePlate(true);
            proxy.setShowArms(false);
            proxy.getPersistentData().putBoolean("FateSupportVehicleDamageProxyEntity", true);
            proxy.getPersistentData().putUUID("FateSupportVehicleDamageProxyTarget", vehicle.getUUID());
            level.addFreshEntity(proxy);
            vehicleData.putUUID("FateSupportVehicleDamageProxy", proxy.getUUID());
         }

         proxy.setPos(vehicle.getX(), vehicle.getBoundingBox().minY + (double)vehicle.getBbHeight() * 0.5 - (double)proxy.getBbHeight() * 0.5, vehicle.getZ());
      }
   }

   private static void tickDamageProxy(ArmorStand proxy, ServerLevel level) {
      CompoundTag data = proxy.getPersistentData();
      if (!data.hasUUID("FateSupportVehicleDamageProxyTarget")
         || !(level.getEntity(data.getUUID("FateSupportVehicleDamageProxyTarget")) instanceof VehicleEntity vehicle)
         || vehicle.isRemoved()) {
         proxy.discard();
      }
   }

   private void tickPlayerA10(VehicleEntity vehicle, ServerLevel level, UUID ownerId) {
      if (level.getEntity(ownerId) instanceof ServerPlayer owner && ServantCardLancelotBerserkerSkills.isActiveCard(owner)) {
         vehicle.setAiTurretTargetUUID("");
         vehicle.setAiPassengerWeaponTargetUUID("");

         if (vehicle.tickCount < 20) {
            vehicle.setEngineStart(true);
            vehicle.setEngineStartOver(true);
            vehicle.setPower(1.0F);
            vehicle.setGearUp(true);
            vehicle.setTargetSpeed(Math.max(1.0, vehicle.getTargetSpeed()));
         }

         return;
      }
   }

   private void tickNpcVehicle(VehicleEntity vehicle, ServerLevel level, UUID ownerId) {
      if ((ownerId == null ? null : level.getEntity(ownerId)) instanceof LancelotBerserkerEntity npc && npc.isAlive()) {
         if (!npc.isPassenger()) {
            npc.startRiding(vehicle, true);
         }

         vehicle.setEngineStart(true);
         vehicle.setEngineStartOver(true);
         vehicle.setPower(1.0F);
         vehicle.setForwardInputDown(true);
         vehicle.setSprintInputDown(true);
         vehicle.setAiTurretTargetUUID("");
         vehicle.setAiPassengerWeaponTargetUUID("");
         LivingEntity target = npc.getTarget();
         if (target != null && EntityUtils.isValidCombatTarget(npc, target)
               && !ServantMasterProtection.isProtectedMaster(npc, target)) {
            Vec3 aimPoint = target.getBoundingBox().getCenter();
            Vec3 aimDirection = aimPoint.subtract(vehicle.position());
            if (aimDirection.lengthSqr() < 1.0E-6) {
               return;
            }

            aimDirection = aimDirection.normalize();
            vehicle.setDeltaMovement(aimDirection.scale(0.95));
            pointNpcA10At(vehicle, npc, aimDirection);
         }

         return;
      }

      vehicle.discard();
   }

   private static void pointNpcA10At(VehicleEntity vehicle, LancelotBerserkerEntity npc, Vec3 direction) {
      float yaw = (float)(Mth.atan2(-direction.x, direction.z) * 180.0F / (float)Math.PI);
      float pitch = (float)(Mth.atan2(-direction.y, direction.horizontalDistance()) * 180.0F / (float)Math.PI);
      vehicle.setYRot(yaw);
      vehicle.setXRot(pitch);
      vehicle.setServerYaw(yaw);
      vehicle.setServerPitch(pitch);
      npc.setYRot(yaw);
      npc.setYHeadRot(yaw);
      npc.setYBodyRot(yaw);
      npc.setXRot(pitch);
   }

   private void tickPlayerGuns(ServerPlayer player) {
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (KnightArmoryService.isExpired(stack, player.level().getGameTime())) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
         } else if (ServantCardLancelotBerserkerSkills.isActiveCard(player)
               && isOwnedGun(stack, player) && this.isGun(stack)) {
            this.maintainOwnerizedGun(stack);
         }
      }
   }

   private void tickLancelotNpc(LancelotBerserkerEntity npc, ServerLevel level) {
      long now = level.getGameTime();
      if (KnightArmoryService.isExpired(npc.getMainHandItem(), now)) {
         npc.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }
      LivingEntity target = npc.getTarget();
      if (target == null || !EntityUtils.isValidCombatTarget(npc, target)
            || ServantMasterProtection.isProtectedMaster(npc, target)) {
         target = level.getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(48),
               candidate -> EntityUtils.isValidCombatTarget(npc, candidate)
                     && !ServantMasterProtection.isProtectedMaster(npc, candidate))
               .stream().min(Comparator.comparingDouble(npc::distanceToSqr)).orElse(null);
         npc.setTarget(target);
      }
      if (target == null || !EntityUtils.isValidCombatTarget(npc, target)
            || ServantMasterProtection.isProtectedMaster(npc, target)
            || LancelotCombatHelper.isUnable(npc, now)
            || npc.isSpiritualTransitionLocked()
            || !LancelotCombatHelper.canUseKnightOfOwner(npc) || npc.isPassenger()) return;
      boolean lowHealth = KnightArmoryRules.npcCanUseAtHealth(
            KnightArmoryEntry.MINIGUN, npc.getHealth(), npc.getMaxHealth());
      double distance = npc.distanceTo(target);
      KnightArmoryEntry entry = KnightArmoryService.entryFor(npc.getMainHandItem());
      long heavyLockUntil = npc.getPersistentData().getLong(NPC_HEAVY_LOCK_UNTIL_TAG);
      if (entry == KnightArmoryEntry.MINIGUN) {
         if (!lowHealth || distance <= 5.0 || now >= heavyLockUntil) {
            npc.setItemSlot(EquipmentSlot.MAINHAND,
                  LancelotCombatHelper.knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), npc));
            entry = KnightArmoryEntry.IRON_ROD;
         }
      } else if (entry != KnightArmoryEntry.IRON_ROD) {
         npc.setItemSlot(EquipmentSlot.MAINHAND,
               LancelotCombatHelper.knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), npc));
         entry = KnightArmoryEntry.IRON_ROD;
      }
      if (entry == KnightArmoryEntry.IRON_ROD && lowHealth
            && now >= heavyLockUntil && npc.tickCount % 40 == 0
            && LancelotCombatHelper.remainingKnightOfOwnerCapacity(npc) > 0) {
         boolean activeA10 = npc.getPersistentData().getLong(SUMMONED_A10_EXPIRES_TAG) > now;
         boolean heavyChoiceAvailable = KnightArmoryRules.npcCanChooseHeavyWeapon(false, activeA10, now, heavyLockUntil);
         boolean a10Available = distance >= 18.0 && heavyChoiceAvailable && this.shouldPrioritizeA10(npc)
               && npc.getCurrentMp() >= KnightArmoryEntry.A10.manaCost();
         boolean minigunAvailable = distance >= 7.0 && distance <= 30.0 && heavyChoiceAvailable
               && SuperbWarfareLoader.isAvailable()
               && npc.getCurrentMp() >= KnightArmoryEntry.MINIGUN.manaCost();
         KnightArmoryEntry desired = KnightArmoryRules.chooseNpcArmoryEntry(
               true, a10Available, minigunAvailable, npc.getRandom().nextFloat());
         if (desired == KnightArmoryEntry.A10 && this.deployNpcA10(npc, target)) return;
         if (desired == KnightArmoryEntry.MINIGUN
               && KnightArmoryService.cooldownUntil(npc, desired) <= now) {
            ItemStack stack = KnightArmoryService.ownerizedWeapon(npc, desired);
            if (!stack.isEmpty()) {
               npc.setItemSlot(EquipmentSlot.MAINHAND, stack);
               npc.setCurrentMp(npc.getCurrentMp() - desired.manaCost());
               KnightArmoryService.startCooldown(npc, desired);
               npc.getPersistentData().putLong(NPC_HEAVY_LOCK_UNTIL_TAG,
                     now + KnightArmoryRules.NPC_HEAVY_WEAPON_LOCK_TICKS);
               npc.getPersistentData().putInt(NPC_LAST_WEAPON_TAG, desired.networkId());
               entry = desired;
            }
         }
      }
      if (entry == KnightArmoryEntry.MINIGUN && npc.hasLineOfSight(target)) {
         npc.setSprinting(distance > 4.0);
         npc.getNavigation().moveTo(target, 1.65);
         this.fireNpcGun(npc, target, entry, level);
      }
   }
   private void fireNpcGun(LancelotBerserkerEntity npc, LivingEntity target, KnightArmoryEntry entry, ServerLevel level) {
      ItemStack held = npc.getMainHandItem();
      if (this.isGun(held) && entry == KnightArmoryService.entryFor(held)) {
         GunData data = GunDataKt.toGunData(held);
         this.maintainOwnerizedGun(held);
         data = GunDataKt.toGunData(held);
         data.tick(npc, true);
         if (data.shouldStartReloading(npc)) data.startReload();

         int interval = switch (entry) {
            case MINIGUN -> 2;
            case MP5 -> 4;
            case AWM, BARRETT -> 30;
            default -> 10;
         };
         if (npc.tickCount % interval != 0) {
            data.save();
         } else if (!data.canShoot(npc)) {
            data.save();
         } else {
            // targetPos only guides missiles. Ordinary bullets use shootDirection;
            // do not depend on the delayed look control or another AI's rotation.
            Vec3 origin = npc.getEyePosition();
            Vec3 aimPoint = target.getEyePosition();
            Vec3 direction = aimPoint.subtract(origin).normalize();
            npc.faceVector(direction);
            npc.setXRot((float)(-Mth.atan2(direction.y, direction.horizontalDistance()) * 180.0 / Math.PI));
            data.shoot(new ShootParameters(npc, npc, level, origin, direction, data,
                  0.0, false, target.getUUID(), aimPoint));
            data.save();
         }
      }
   }

   @SubscribeEvent
   public void onEntityJoin(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide()) {
         if (event.getEntity() instanceof ProjectileEntity projectile) {
            this.modifyGunProjectile(projectile);
         }

         Entity owner = projectileOwner(event.getEntity());
         VehicleEntity firingVehicle = owner instanceof VehicleEntity vehiclex
            ? vehiclex
            : (owner != null && owner.getVehicle() instanceof VehicleEntity vehicle ? vehicle : null);
         if (firingVehicle != null
            && firingVehicle.getPersistentData().getBoolean("FateSupportKnightVehicle")
            && firingVehicle.getPersistentData().getBoolean("FateSupportKnightVehicleA10")) {
            CompoundTag projectileData = event.getEntity().getPersistentData();
            CompoundTag vehicleData = firingVehicle.getPersistentData();
            projectileData.putBoolean("FateSupportKnightProjectile", true);
            projectileData.putUUID("FateSupportKnightProjectileVehicle", firingVehicle.getUUID());
            if (vehicleData.hasUUID("FateSupportKnightVehicleOwner")) {
               projectileData.putUUID("FateSupportKnightProjectileOwner", vehicleData.getUUID("FateSupportKnightVehicleOwner"));
            }

            event.getEntity().setCustomName(Component.literal("FateSupportKnightProjectile"));
         }
      }
   }

   @SubscribeEvent
   public void onEntityLeave(EntityLeaveLevelEvent event) {
      Entity entity = event.getEntity();
      if (!entity.level().isClientSide()
         && entity.getPersistentData().getBoolean("FateSupportKnightVehicleSummoned")
         && entity.getPersistentData().hasUUID("FateSupportKnightVehicleOwner")
         && entity.getRemovalReason() != null
         && entity.getRemovalReason().shouldDestroy()) {
         UUID ownerId = entity.getPersistentData().getUUID("FateSupportKnightVehicleOwner");
         LivingEntity owner = event.getLevel() instanceof ServerLevel level && level.getEntity(ownerId) instanceof LivingEntity living
               ? living : event.getLevel().getServer().getPlayerList().getPlayer(ownerId);
         if (owner != null
            && owner.getPersistentData().hasUUID("FateSupportKnightSummonedA10")
            && owner.getPersistentData().getUUID("FateSupportKnightSummonedA10").equals(entity.getUUID())) {
            clearSummonedA10(owner);
         }
      }
   }

   private void modifyGunProjectile(ProjectileEntity projectile) {
      Entity shooter = projectile.getOwner();
      if (shooter instanceof LivingEntity living && isLancelot(living) && LancelotCombatHelper.canUseKnightOfOwner(living)) {
         ItemStack gun = findOwnerizedGun(living, projectile.getGunItemId());
         if (!gun.isEmpty()) {
            KnightArmoryEntry entry = KnightArmoryService.entryFor(gun);
            projectile.getPersistentData().putBoolean("FateSupportKnightProjectile", true);
            projectile.getPersistentData().putUUID(PROJECTILE_OWNER_TAG, living.getUUID());
            projectile.setCustomName(Component.literal("FateSupportKnightProjectile"));
            projectile.setRGB(new float[]{0.06F, 0.01F, 0.09F});
            if (entry != KnightArmoryEntry.AWM && entry != KnightArmoryEntry.BARRETT) {
               projectile.setDamage(projectile.getDamage() + 2.0F);
            } else {
               projectile.setDamage(100.0F);
               projectile.setExplosionDamage(100.0F);
               projectile.setExplosionRadius(6.0F);
               if (KnightArmoryService.incrementShots(gun) >= 15) {
                  gun.shrink(1);
                  shooter.sendSystemMessage(Component.translatable("message.fatesupport.knight_armory.sniper_spent"));
               }
            }
         }
      }
   }

   private static ItemStack findOwnerizedGun(LivingEntity shooter, String gunId) {
      ItemStack held = shooter.getMainHandItem();
      if (matchesGunId(held, gunId)) {
         return isOwnedGun(held, shooter) ? held : ItemStack.EMPTY;
      }
      ItemStack offhand = shooter.getOffhandItem();
      return matchesGunId(offhand, gunId) && isOwnedGun(offhand, shooter) ? offhand : ItemStack.EMPTY;
   }

   private static boolean isOwnedGun(ItemStack stack, LivingEntity shooter) {
      CompoundTag data = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
      return KnightArmoryService.isOwnerized(stack) && (data.hasUUID(KnightArmoryService.OWNER_UUID_TAG)
            ? data.getUUID(KnightArmoryService.OWNER_UUID_TAG).equals(shooter.getUUID())
            : LancelotCombatHelper.isKnightOfOwnerOwnedBy(stack, shooter));
   }

   private static boolean matchesGunId(ItemStack stack, String gunId) {
      if (stack != null && !stack.isEmpty() && gunId != null) {
         ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
         return gunId.equals(stack.getDescriptionId()) || gunId.equals(itemId == null ? "" : itemId.toString());
      } else {
         return false;
      }
   }

   private static Entity projectileOwner(Entity entity) {
      return entity instanceof Projectile projectile ? projectile.getOwner() : null;
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onIncomingDamage(LivingIncomingDamageEvent event) {
      LivingEntity living = event.getEntity();
      DamageSource source = event.getSource();
      if (isFriendlyKnightDamage(living, source) || isOwnA10ProjectileDamage(living, source)) {
         event.setCanceled(true);
      } else {
         if (living instanceof ArmorStand proxy && proxy.getPersistentData().getBoolean("FateSupportVehicleDamageProxyEntity")) {
            if (proxy.getPersistentData().hasUUID("FateSupportVehicleDamageProxyTarget")
               && proxy.level() instanceof ServerLevel level
               && level.getEntity(proxy.getPersistentData().getUUID("FateSupportVehicleDamageProxyTarget")) instanceof VehicleEntity vehicle) {
               vehicle.hurt(source, event.getAmount());
            }

            event.setCanceled(true);
            return;
         }

         ResourceLocation id = living.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getKey(source.type());
         VehicleEntity explodingVehicle = explodingVehicle(source, living);
         boolean lancelot = isLancelot(living);
         if (lancelot && isLancelotVehicleDamage(source, id, explodingVehicle)) {
            event.setCanceled(true);
         } else if (!isVehicleExplosion(id, explodingVehicle)) {
            if (lancelot && living.getPersistentData().getLong("FateSupportKnightVehicleCrashGrace") >= living.level().getGameTime()) {
               if (source.is(DamageTypes.FALL)) {
                  event.setCanceled(true);
               }
            }
         } else {
            if (lancelot || isServant(living) && !isOwnerizedVehicle(explodingVehicle)) {
               event.setCanceled(true);
            }
         }
      }
   }

   private static boolean isLancelotVehicleDamage(DamageSource source, ResourceLocation damageId, VehicleEntity explodingVehicle) {
      if (damageId != null && "superbwarfare".equals(damageId.getNamespace())) {
         String path = damageId.getPath();
         if ("vehicle_explosion".equals(path) || "air_crash".equals(path)) {
            return true;
         }

         if ("custom_explosion".equals(path) && explodingVehicle != null) {
            return true;
         }
      }

      return explodingVehicle != null && source.is(DamageTypeTags.IS_EXPLOSION);
   }

   private static boolean isFriendlyKnightDamage(LivingEntity victim, DamageSource source) {
      Entity direct = source.getDirectEntity();
      if (direct == null || !(victim.level() instanceof ServerLevel level)) return false;
      CompoundTag data = direct.getPersistentData();
      UUID ownerId = data.getBoolean(PROJECTILE_TAG) && data.hasUUID(PROJECTILE_OWNER_TAG)
            ? data.getUUID(PROJECTILE_OWNER_TAG)
            : data.getBoolean(VEHICLE_TAG) && data.hasUUID(VEHICLE_OWNER_TAG)
                  ? data.getUUID(VEHICLE_OWNER_TAG) : null;
      Entity owner = ownerId == null ? null : level.getEntity(ownerId);
      return owner instanceof LivingEntity living && (owner == victim
            || living.isAlliedTo(victim) || victim.isAlliedTo(living)
            || ServantMasterProtection.isProtectedMaster(living, victim));
   }

   private static boolean isOwnA10ProjectileDamage(LivingEntity living, DamageSource source) {
      Entity projectile = source.getDirectEntity();
      if (projectile == null) {
         return false;
      } else {
         CompoundTag data = projectile.getPersistentData();
         return data.getBoolean("FateSupportKnightProjectile")
            && data.hasUUID("FateSupportKnightProjectileOwner")
            && data.getUUID("FateSupportKnightProjectileOwner").equals(living.getUUID());
      }
   }

   private static boolean isVehicleExplosion(ResourceLocation damageId, VehicleEntity explodingVehicle) {
      return damageId != null && "superbwarfare".equals(damageId.getNamespace())
         ? "vehicle_explosion".equals(damageId.getPath()) || "custom_explosion".equals(damageId.getPath()) && explodingVehicle != null
         : false;
   }

   private static VehicleEntity explodingVehicle(DamageSource source, LivingEntity victim) {
      Entity var3 = source.getDirectEntity();
      if (var3 instanceof VehicleEntity) {
         return (VehicleEntity)var3;
      } else {
         var3 = source.getEntity();
         if (var3 instanceof VehicleEntity) {
            return (VehicleEntity)var3;
         } else {
            return victim.getVehicle() instanceof VehicleEntity vehicle ? vehicle : null;
         }
      }
   }

   private static boolean isOwnerizedVehicle(VehicleEntity vehicle) {
      return vehicle != null && vehicle.getPersistentData().getBoolean("FateSupportKnightVehicle");
   }

   private static boolean isServant(Entity entity) {
      if (entity instanceof ServantEntity) {
         return true;
      } else {
         if (entity instanceof ServerPlayer player && ((PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).servant_card_transformed) {
            return true;
         }

         return false;
      }
   }

   private static boolean isLancelot(Entity entity) {
      if (entity instanceof LancelotBerserkerEntity) {
         return true;
      } else {
         if (entity instanceof ServerPlayer player && ServantCardLancelotBerserkerSkills.isActiveCard(player)) {
            return true;
         }

         return false;
      }
   }

   private static VehicleEntity vehicleUnder(ServerPlayer player) {
      AABB area = player.getBoundingBox().inflate(1.5, 0.8, 1.5).move(0.0, -0.8, 0.0);
      return player.level().getEntitiesOfClass(VehicleEntity.class, area).stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
   }

   private static void assimilateVehicle(VehicleEntity vehicle, UUID owner, long duration, boolean a10, boolean summoned, boolean npc) {
      float originalMaxHealth = vehicle.getMaxHealth();
      CompoundTag data = vehicle.getPersistentData();
      data.putBoolean("FateSupportKnightVehicle", true);
      data.putUUID("FateSupportKnightVehicleOwner", owner);
      data.putLong("FateSupportKnightVehicleExpires", vehicle.level().getGameTime() + duration);
      data.putBoolean("FateSupportKnightVehicleA10", a10);
      data.putBoolean("FateSupportKnightVehicleSummoned", summoned);
      data.putBoolean("FateSupportKnightVehicleNpc", npc);
      data.putFloat("FateSupportKnightVehicleOriginalHealth", originalMaxHealth);
      vehicle.setOverride(ownerizedOverride(vehicle.getOverride(), owner));
      vehicle.setHealth(originalMaxHealth * 2.0F);
      vehicle.setEnergy(vehicle.getMaxEnergy());
      maintainVehicleAmmo(vehicle);
   }

   private static void maintainVehicleAmmo(VehicleEntity vehicle) {
      for (String weaponName : vehicle.getGunDataMap().keySet()) {
         vehicle.modifyGunData(weaponName, data -> {
            data.virtualAmmo.set(999999);
            data.backupAmmoCount.set(999999);
         });
      }
   }

   private static String ownerizedOverride(String current, UUID owner) {
      if (current != null && current.contains("\"FateSupportKnightOfOwner\":true") && current.contains("\"FateSupportKnightVisual\":true")) {
         return current;
      } else {
         String fields = "\"FateSupportKnightOfOwner\":true,\"FateSupportKnightVisual\":true,\"FateSupportKnightOwner\":\"" + owner + "\"";
         if (current != null && !current.isBlank() && !"{}".equals(current.trim())) {
            String trimmed = current.trim();
            if (!trimmed.endsWith("}")) {
               return "{" + fields + "}";
            } else {
               return trimmed.contains("\"FateSupportKnightOfOwner\":true")
                  ? trimmed.substring(0, trimmed.length() - 1) + ",\"FateSupportKnightVisual\":true}"
                  : trimmed.substring(0, trimmed.length() - 1) + "," + fields + "}";
            }
         } else {
            return "{" + fields + "}";
         }
      }
   }

   private static void clearSummonedA10(LivingEntity player) {
      player.getPersistentData().remove("FateSupportKnightSummonedA10");
      player.getPersistentData().remove("FateSupportKnightSummonedA10Expires");
   }
}
