package com.captain.servant.compat.superbwarfare;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunDataKt;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.captain.servant.TypeMoonServant;
import com.captain.servant.lancelot.KnightArmoryEntry;
import com.captain.servant.lancelot.KnightArmoryService;
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
import net.minecraft.world.level.levelgen.Heightmap.Types;
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
   private static final String SUMMONED_A10_UUID_TAG = "FateSupportKnightSummonedA10";
   private static final String SUMMONED_A10_EXPIRES_TAG = "FateSupportKnightSummonedA10Expires";
   private static final String VEHICLE_DAMAGE_PROXY_UUID_TAG = "FateSupportVehicleDamageProxy";
   private static final String DAMAGE_PROXY_TAG = "FateSupportVehicleDamageProxyEntity";
   private static final String DAMAGE_PROXY_VEHICLE_UUID_TAG = "FateSupportVehicleDamageProxyTarget";
   private static final String LAST_TYPE_MOON_DAMAGE_TICK_TAG = "FateSupportLastTypeMoonDamageTick";
   private static final String LAST_TYPE_MOON_DAMAGE_SOURCE_TAG = "FateSupportLastTypeMoonDamageSource";
   private static final String LAST_TYPE_MOON_DAMAGE_AMOUNT_TAG = "FateSupportLastTypeMoonDamageAmount";
   private static final long NPC_A10_DURATION = 400L;
   private static final int A10_CANNON_INDEX = 0;
   private static final int A10_ROCKET_INDEX = 1;
   private static final int A10_MISSILE_INDEX = 3;
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
      if (LancelotCombatHelper.remainingKnightOfOwnerCapacity(player) <= 0) {
         player.displayClientMessage(Component.translatable("message.fatesupport.knight_vehicle.capacity"), true);
         return false;
      } else {
         ServerLevel level = player.serverLevel();
         A10Entity a10 = (A10Entity)((EntityType)ModEntities.A_10A.get()).create(level);
         if (a10 == null) {
            return false;
         } else {
            a10.setPos(player.getX(), player.getY() + 15.0, player.getZ());
            a10.setYRot(player.getYRot());
            level.addFreshEntity(a10);
            assimilateVehicle(a10, player.getUUID(), npcVariant ? 400L : 1200L, true, true, npcVariant);
            a10.setEngineStart(true);
            a10.setEngineStartOver(true);
            a10.setPower(1.0F);
            a10.setGearUp(true);
            a10.setTargetSpeed(1.2);
            a10.setAbsoluteSpeed(1.2);
            Vec3 launchDirection = new Vec3(player.getLookAngle().x, 0.22, player.getLookAngle().z);
            if (launchDirection.horizontalDistanceSqr() < 1.0E-4) {
               float yaw = player.getYRot() * (float) (Math.PI / 180.0);
               launchDirection = new Vec3((double)(-Mth.sin(yaw)), 0.22, (double)Mth.cos(yaw));
            }

            a10.setDeltaMovement(launchDirection.normalize().scale(1.2));
            player.startRiding(a10, true);
            player.getPersistentData().putUUID("FateSupportKnightSummonedA10", a10.getUUID());
            player.getPersistentData().putLong("FateSupportKnightSummonedA10Expires", level.getGameTime() + (npcVariant ? 400L : 1200L));
            return true;
         }
      }
   }

   private boolean summonNpcA10(LancelotBerserkerEntity npc, long now) {
      ServerLevel level = (ServerLevel)npc.level();
      A10Entity a10 = (A10Entity)((EntityType)ModEntities.A_10A.get()).create(level);
      if (a10 == null) {
         return false;
      } else {
         a10.setPos(npc.getX(), npc.getY() + 1.0, npc.getZ());
         a10.setYRot(npc.getYRot());
         level.addFreshEntity(a10);
         assimilateVehicle(a10, npc.getUUID(), 400L, true, true, true);
         npc.startRiding(a10, true);
         npc.getPersistentData().putLong("FateSupportKnightNpcA10Cooldown", now + 2000L);
         return true;
      }
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
   public int countOwnedVehicles(ServerPlayer player) {
      int count = 0;

      for (ServerLevel level : player.getServer().getAllLevels()) {
         for (VehicleEntity vehicle : level.getEntitiesOfClass(
            VehicleEntity.class,
            new AABB(-3.0E7, -2048.0, -3.0E7, 3.0E7, 2048.0, 3.0E7),
            candidate -> candidate.getPersistentData().getBoolean("FateSupportKnightVehicle")
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
      if (!(npc.level() instanceof ServerLevel) || !npc.isAlive() || npc.isPassenger() || LancelotCombatHelper.isAroundightMode(npc)) {
         return false;
      }

      if (target == null || !target.isAlive()) {
         return false;
      }

      long now = npc.level().getGameTime();
      return npc.getCurrentMp() >= 200.0 && npc.getPersistentData().getLong("FateSupportKnightNpcA10Cooldown") <= now;
   }

   private boolean deployNpcA10(LancelotBerserkerEntity npc, LivingEntity target) {
      if (!canDeployNpcA10(npc, target)) {
         return false;
      }

      long now = npc.level().getGameTime();
      npc.setCurrentMp(npc.getCurrentMp() - 200.0);
      if (!this.summonNpcA10(npc, now)) {
         npc.setCurrentMp(npc.getCurrentMp() + 200.0);
         return false;
      }

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
         level.sendParticles(ParticleTypes.LARGE_SMOKE, vehicle.getX(), vehicle.getY() + 1.0, vehicle.getZ(), 8, 1.2, 0.7, 1.2, 0.04);
         level.sendParticles(DARK_DUST, vehicle.getX(), vehicle.getY() + 1.0, vehicle.getZ(), 12, 1.4, 0.8, 1.4, 0.02);
         if (now >= data.getLong("FateSupportKnightVehiclePendingUntil")) {
            UUID owner = data.getUUID("FateSupportKnightVehicleOwner");
            boolean a10 = data.getBoolean("FateSupportKnightVehicleA10");
            data.remove("FateSupportKnightVehiclePendingUntil");
            assimilateVehicle(vehicle, owner, 1200L, a10, false, false);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
               player.displayClientMessage(Component.translatable("message.fatesupport.knight_vehicle.assimilated"), true);
            }
         }
      } else if (data.getBoolean("FateSupportKnightVehicle")) {
         if (now >= data.getLong("FateSupportKnightVehicleExpires")) {
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
         LivingEntity target = level.getEntitiesOfClass(
               LivingEntity.class,
               vehicle.getBoundingBox().inflate(192.0),
               candidate -> candidate.isAlive()
                     && candidate != owner
                     && (!(candidate instanceof Player player) || !player.isSpectator())
                     && !isFriendlyDamageProxy(candidate, vehicle, owner, level)
                     && !owner.isAlliedTo(candidate)
                     && owner.hasLineOfSight(candidate)
            )
            .stream()
            .min(Comparator.comparingDouble(vehicle::distanceToSqr))
            .orElse(null);
         String targetId = target == null ? "" : target.getStringUUID();
         vehicle.setAiTurretTargetUUID(targetId);
         vehicle.setAiPassengerWeaponTargetUUID(targetId);
         if (target != null) {
            vehicle.turretAutoAimFromUuid(targetId, owner);
            vehicle.passengerWeaponAutoAimFormUuid(targetId, owner);
         }

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

   private static boolean isFriendlyDamageProxy(LivingEntity candidate, VehicleEntity sourceVehicle, ServerPlayer owner, ServerLevel level) {
      if (!(candidate instanceof ArmorStand proxy)
         || !proxy.getPersistentData().getBoolean("FateSupportVehicleDamageProxyEntity")
         || !proxy.getPersistentData().hasUUID("FateSupportVehicleDamageProxyTarget")) {
         return false;
      }

      if (!(level.getEntity(proxy.getPersistentData().getUUID("FateSupportVehicleDamageProxyTarget")) instanceof VehicleEntity targetVehicle)) {
         return true;
      }

      if (targetVehicle == sourceVehicle) {
         return true;
      } else {
         CompoundTag targetData = targetVehicle.getPersistentData();
         return targetData.hasUUID("FateSupportKnightVehicleOwner") && targetData.getUUID("FateSupportKnightVehicleOwner").equals(owner.getUUID());
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
         LivingEntity target = npc.getTarget();
         if (target != null && target.isAlive()) {
            String targetId = target.getStringUUID();
            vehicle.setAiTurretTargetUUID(targetId);
            vehicle.setAiPassengerWeaponTargetUUID(targetId);
            vehicle.turretAutoAimFromUuid(targetId, npc);
            vehicle.passengerWeaponAutoAimFormUuid(targetId, npc);
            Vec3 offset = vehicle.position().subtract(target.position());
            Vec3 radial = new Vec3(offset.x, 0.0, offset.z);
            if (radial.lengthSqr() < 1.0E-4) {
               radial = new Vec3(1.0, 0.0, 0.0);
            }

            radial = radial.normalize();
            Vec3 tangent = new Vec3(-radial.z, 0.0, radial.x);
            double distance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            double radialCorrection = Mth.clamp((distance - 35.0) / 18.0, -0.7, 0.7);
            Vec3 flight = tangent.scale(0.72).add(radial.scale(-radialCorrection));
            int groundY = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(vehicle.getX()), Mth.floor(vehicle.getZ()));
            double desiredY = Math.max(target.getY() + 12.0, (double)groundY + 10.0);
            double vertical = Mth.clamp((desiredY - vehicle.getY()) * 0.08, -0.18, 0.28);
            Vec3 velocity = new Vec3(flight.x, vertical, flight.z);
            if (velocity.horizontalDistanceSqr() > 1.0E-4) {
               velocity = velocity.normalize().scale(0.78).add(0.0, vertical, 0.0);
            }

            vehicle.setDeltaMovement(velocity);
            Vec3 aimPoint = target.getEyePosition().add(target.getDeltaMovement().scale(2.0));
            Vec3 aimDirection = aimPoint.subtract(vehicle.getShootPos("Cannon", 1.0F));
            if (aimDirection.lengthSqr() < 1.0E-6) {
               return;
            }

            aimDirection = aimDirection.normalize();
            pointNpcA10At(vehicle, npc, aimDirection);
            fireNpcA10Weapons(vehicle, npc, target, aimPoint, aimDirection);
         }

         return;
      }

      vehicle.discard();
   }

   private static void pointNpcA10At(VehicleEntity vehicle, LancelotBerserkerEntity npc, Vec3 direction) {
      float yaw = (float)(Mth.atan2(-direction.x, direction.z) * 180.0F / (float)Math.PI);
      float pitch = Mth.clamp((float)(Mth.atan2(-direction.y, direction.horizontalDistance()) * 180.0F / (float)Math.PI), -35.0F, 35.0F);
      vehicle.setYRot(yaw);
      vehicle.setXRot(pitch);
      vehicle.setServerYaw(yaw);
      vehicle.setServerPitch(pitch);
      npc.setYRot(yaw);
      npc.setYHeadRot(yaw);
      npc.setYBodyRot(yaw);
      npc.setXRot(pitch);
   }

   private static void fireNpcA10Weapons(VehicleEntity vehicle, LancelotBerserkerEntity npc, LivingEntity target, Vec3 aimPoint, Vec3 aimDirection) {
      if (npc.hasLineOfSight(target)) {
         Vec3 cannonDirection = vehicle.getShootVec("Cannon", 1.0F);
         if (!(cannonDirection.lengthSqr() < 1.0E-6) && !(cannonDirection.normalize().dot(aimDirection) < 0.985)) {
            double distance = (double)vehicle.distanceTo(target);
            int seat = vehicle.getSeatIndex(npc);
            if (seat >= 0) {
               if (distance >= 10.0 && distance <= 160.0) {
                  shootNpcA10Weapon(vehicle, npc, target, aimPoint, seat, 0);
               }

               if (distance >= 24.0 && distance <= 120.0 && vehicle.tickCount % 8 == 0) {
                  shootNpcA10Weapon(vehicle, npc, target, aimPoint, seat, 1);
               }

               if (distance >= 40.0 && distance <= 192.0 && vehicle.tickCount % 30 == 0) {
                  shootNpcA10Weapon(vehicle, npc, target, aimPoint, seat, 3);
               }

               vehicle.changeWeapon(seat, 0, false);
            }
         }
      }
   }

   private static void shootNpcA10Weapon(VehicleEntity vehicle, LancelotBerserkerEntity npc, LivingEntity target, Vec3 aimPoint, int seat, int weaponIndex) {
      vehicle.changeWeapon(seat, weaponIndex, false);
      vehicle.vehicleShoot(npc, target.getUUID(), aimPoint);
   }

   private void tickPlayerGuns(ServerPlayer player) {
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (KnightArmoryService.isExpired(stack, player.level().getGameTime())) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
         } else if (KnightArmoryService.isOwnerized(stack) && this.isGun(stack)) {
            this.maintainOwnerizedGun(stack);
         }
      }
   }

   private void tickLancelotNpc(LancelotBerserkerEntity npc, ServerLevel level) {
      LivingEntity target = npc.getTarget();
      if (target != null && target.isAlive() && !LancelotCombatHelper.isAroundightMode(npc) && !npc.isPassenger()) {
         long now = level.getGameTime();
         if (this.shouldPrioritizeA10(npc) && npc.getRandom().nextFloat() < 0.015F && this.deployNpcA10(npc, target)) {
            return;
         }

         double distance = (double)npc.distanceTo(target);
         KnightArmoryEntry desired = distance < 5.0
            ? KnightArmoryEntry.IRON_ROD
            : (
               distance > 18.0
                  ? (npc.tickCount / 100 % 2 == 0 ? KnightArmoryEntry.AWM : KnightArmoryEntry.BARRETT)
                  : (npc.tickCount / 100 % 2 == 0 ? KnightArmoryEntry.MP5 : KnightArmoryEntry.MINIGUN)
            );
         if (desired == KnightArmoryEntry.IRON_ROD) {
            if (KnightArmoryService.entryFor(npc.getMainHandItem()) != KnightArmoryEntry.IRON_ROD
               && npc.getPersistentData().getLong("FateSupportKnightNpcWeaponCooldown") <= now
               && npc.getCurrentMp() >= 20.0) {
               ItemStack rod = KnightArmoryService.markOwnerized(
                  LancelotCombatHelper.knightOfOwnerStack(new ItemStack((ItemLike)ModItems.LANCELOT_IRON_ROD.get()), npc), npc
               );
               npc.setItemSlot(EquipmentSlot.MAINHAND, rod);
               npc.setCurrentMp(npc.getCurrentMp() - 20.0);
               npc.getPersistentData().putString("FateSupportKnightNpcLastWeapon", desired.key());
               npc.getPersistentData().putLong("FateSupportKnightNpcWeaponCooldown", now + 100L);
            }

            KnightArmoryEntry heldEntry = KnightArmoryService.entryFor(npc.getMainHandItem());
            if (heldEntry != null && heldEntry != KnightArmoryEntry.IRON_ROD) {
               this.fireNpcGun(npc, target, heldEntry, level);
            }
         } else {
            if (npc.getPersistentData().getLong("FateSupportKnightNpcWeaponCooldown") <= now
               && !desired.key().equals(npc.getPersistentData().getString("FateSupportKnightNpcLastWeapon"))
               && npc.getCurrentMp() >= 20.0) {
               ItemStack stack = new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(desired.itemId()));
               stack = KnightArmoryService.markOwnerized(LancelotCombatHelper.knightOfOwnerStack(stack, npc), npc);
               this.initializeOwnerizedGun(stack);
               npc.setItemSlot(EquipmentSlot.MAINHAND, stack);
               npc.setCurrentMp(npc.getCurrentMp() - 20.0);
               npc.getPersistentData().putString("FateSupportKnightNpcLastWeapon", desired.key());
               npc.getPersistentData().putLong("FateSupportKnightNpcWeaponCooldown", now + 100L);
            }

            KnightArmoryEntry heldEntry = KnightArmoryService.entryFor(npc.getMainHandItem());
            if (heldEntry != null && heldEntry != KnightArmoryEntry.IRON_ROD) {
               this.fireNpcGun(npc, target, heldEntry, level);
            }
         }
      }
   }

   private void fireNpcGun(LancelotBerserkerEntity npc, LivingEntity target, KnightArmoryEntry entry, ServerLevel level) {
      ItemStack held = npc.getMainHandItem();
      if (this.isGun(held) && entry == KnightArmoryService.entryFor(held)) {
         GunData data = GunDataKt.toGunData(held);
         int magazine = Math.max(1, data.compute().getMagazine());
         data.ammo.set(magazine);
         data.virtualAmmo.set(999999);
         data.backupAmmoCount.set(999999);
         data.tick(npc, false);

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
            Vec3 direction = target.getEyePosition().subtract(npc.getEyePosition()).normalize();
            data.shoot(npc, 0.0, false, target.getUUID(), direction);
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
         ServerPlayer owner = event.getLevel().getServer().getPlayerList().getPlayer(ownerId);
         if (owner != null
            && owner.getPersistentData().hasUUID("FateSupportKnightSummonedA10")
            && owner.getPersistentData().getUUID("FateSupportKnightSummonedA10").equals(entity.getUUID())) {
            clearSummonedA10(owner);
         }
      }
   }

   private void modifyGunProjectile(ProjectileEntity projectile) {
      Entity shooter = projectile.getOwner();
      if (shooter instanceof LivingEntity living) {
         ItemStack gun = findOwnerizedGun(living, projectile.getGunItemId());
         if (!gun.isEmpty()) {
            KnightArmoryEntry entry = KnightArmoryService.entryFor(gun);
            projectile.getPersistentData().putBoolean("FateSupportKnightProjectile", true);
            projectile.setCustomName(Component.literal("FateSupportKnightProjectile"));
            projectile.setRGB(new float[]{0.06F, 0.01F, 0.09F});
            if (entry != KnightArmoryEntry.AWM && entry != KnightArmoryEntry.BARRETT) {
               projectile.setDamage(projectile.getDamage() + 2.0F);
            } else {
               projectile.setDamage(100.0F);
               projectile.setExplosionDamage(100.0F);
               projectile.setExplosionRadius(6.0F);
               if (shooter instanceof ServerPlayer && KnightArmoryService.incrementShots(gun) >= 15) {
                  gun.shrink(1);
                  shooter.sendSystemMessage(Component.translatable("message.fatesupport.knight_armory.sniper_spent"));
               }
            }
         }
      }
   }

   private static ItemStack findOwnerizedGun(LivingEntity shooter, String gunId) {
      ItemStack held = shooter.getMainHandItem();
      if (matchesGunId(held, gunId) && KnightArmoryService.isOwnerized(held)) {
         return held;
      } else {
         if (shooter instanceof Player player) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
               ItemStack stack = player.getInventory().getItem(slot);
               if (KnightArmoryService.isOwnerized(stack) && matchesGunId(stack, gunId)) {
                  return stack;
               }
            }
         }

         return ItemStack.EMPTY;
      }
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
      if (isOwnA10ProjectileDamage(living, source)) {
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

   private static void clearSummonedA10(ServerPlayer player) {
      player.getPersistentData().remove("FateSupportKnightSummonedA10");
      player.getPersistentData().remove("FateSupportKnightSummonedA10Expires");
   }
}
