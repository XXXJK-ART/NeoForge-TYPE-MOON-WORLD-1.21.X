package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.SwordBarrelBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWInterceptorSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwChantRippleEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwSkyGearEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class ChantHandler {
   private static final int BASE_CHANT_INTERVAL = 40;
   private static final int UBW_ACTIVATION_WAIT_MAX_ATTEMPTS = 10;
   private static final Map<UUID, Vec3> UBW_LOCATIONS = new ConcurrentHashMap<>();
   private static final Map<UUID, Vec3> PENDING_UBW_LOCATIONS = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> PENDING_UBW_ACTIVATION_ATTEMPTS = new ConcurrentHashMap<>();
   private static final Map<UUID, List<ChantHandler.RemovalEntry>> REMOVAL_QUEUES = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<BlockPos, ChantHandler.BlockBackup>> BACKUP_BLOCKS = new ConcurrentHashMap<>();
   private static final Map<UUID, List<ChantHandler.ItemBackup>> BACKUP_ITEMS = new ConcurrentHashMap<>();
   private static final Map<UUID, BlockPos> CHANT_CENTERS = new ConcurrentHashMap<>();
   private static final Map<UUID, Double> TERRAIN_RADIUS = new ConcurrentHashMap<>();
   private static final Map<UUID, List<BlockPos>> RESTORATION_QUEUES = new ConcurrentHashMap<>();
   private static final Map<UUID, List<ChantHandler.EntityReturnData>> TELEPORTED_ENTITIES = new ConcurrentHashMap<>();
   private static final Map<UUID, List<UUID>> GENERATED_ENTITIES = new ConcurrentHashMap<>();
   public static final Map<UUID, List<BlockPos>> PLACED_SWORDS = new ConcurrentHashMap<>();
   private static final Map<UUID, List<UUID>> ACTIVE_UBW_ENTITIES = new ConcurrentHashMap<>();
   private static final Map<UUID, Vec3> ACTIVE_ENTITY_POSITIONS = new ConcurrentHashMap<>();
   private static final Map<UUID, Boolean> WAS_CHANTING = new ConcurrentHashMap<>();
   private static final Map<UUID, List<ChantHandler.RefillEntry>> REFILL_QUEUES = new ConcurrentHashMap<>();
   private static final Map<UUID, UUID> ACTIVE_UBW_RIPPLES = new ConcurrentHashMap<>();

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      UUID uuid = event.getEntity().getUUID();
      MinecraftServer server = event.getEntity().getServer();
      if (event.getEntity().level() instanceof ServerLevel level) {
         Vec3 center = UBW_LOCATIONS.getOrDefault(uuid, event.getEntity().position());
         clearServantCardUbwGears(level, uuid, center);
      }
      if (WAS_CHANTING.getOrDefault(uuid, false) && event.getEntity() instanceof ServerPlayer serverPlayer) {
         restoreTerrainInstantly(serverPlayer);
      }

      UBW_LOCATIONS.remove(uuid);
      PENDING_UBW_LOCATIONS.remove(uuid);
      PENDING_UBW_ACTIVATION_ATTEMPTS.remove(uuid);
      REMOVAL_QUEUES.remove(uuid);
      BACKUP_BLOCKS.remove(uuid);
      BACKUP_ITEMS.remove(uuid);
      CHANT_CENTERS.remove(uuid);
      TERRAIN_RADIUS.remove(uuid);
      RESTORATION_QUEUES.remove(uuid);
      TELEPORTED_ENTITIES.remove(uuid);
      GENERATED_ENTITIES.remove(uuid);
      PLACED_SWORDS.remove(uuid);
      ACTIVE_UBW_ENTITIES.remove(uuid);
      ACTIVE_ENTITY_POSITIONS.remove(uuid);
      REFILL_QUEUES.remove(uuid);
      discardUbwRipple(server == null ? null : server.overworld(), uuid);
      WAS_CHANTING.remove(uuid);
      UBWInstanceManager.scheduleDeleteInstance(server, uuid);
   }

   public static void registerPlacedSword(UUID playerUUID, BlockPos pos) {
      PLACED_SWORDS.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(pos);
   }

   private static ServerLevel resolveDimensionOrOverworld(MinecraftServer server, String dimensionId) {
      if (server == null) {
         return null;
      } else {
         if (dimensionId != null && !dimensionId.isEmpty()) {
            try {
               ResourceLocation rl = ResourceLocation.parse(dimensionId);
               ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
               if (level != null) {
                  return level;
               }
            } catch (Exception var4) {
               TYPE_MOON_WORLD.LOGGER.warn("Invalid dimension id '{}', fallback to overworld", dimensionId);
            }
         }

         return server.overworld();
      }
   }

   private static void attachGeneratedEntityToOwner(Entity entity, UUID playerUUID, ServerLevel serverLevel) {
      TypeMoonWorldModVariables.UBWReturnData data = (TypeMoonWorldModVariables.UBWReturnData)entity.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
      data.ownerUUID = playerUUID;
      data.generated = true;
      ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(playerUUID);
      if (owner == null) {
         data.returnX = entity.getX();
         data.returnY = entity.getY();
         data.returnZ = entity.getZ();
         data.returnDim = serverLevel.getServer().overworld().dimension().location().toString();
      } else {
         TypeMoonWorldModVariables.PlayerVariables ownerVars = (TypeMoonWorldModVariables.PlayerVariables)owner.getData(
            TypeMoonWorldModVariables.PLAYER_VARIABLES
         );
         data.returnX = ownerVars.ubw_return_x;
         data.returnY = ownerVars.ubw_return_y;
         data.returnZ = ownerVars.ubw_return_z;
         data.returnDim = ownerVars.ubw_return_dimension != null && !ownerVars.ubw_return_dimension.isEmpty()
            ? ownerVars.ubw_return_dimension
            : owner.serverLevel().dimension().location().toString();
      }

      GENERATED_ENTITIES.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(entity.getUUID());
      ACTIVE_UBW_ENTITIES.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(entity.getUUID());
      ACTIVE_ENTITY_POSITIONS.put(entity.getUUID(), entity.position());
   }

   @SubscribeEvent
   public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide) {
         if (UBWInstanceManager.isUbwDimension(event.getLevel().dimension().location())) {
            Entity entity = event.getEntity();
            if (event.getLevel() instanceof ServerLevel serverLevel && entity instanceof Projectile projectile) {
               spawnUbwProjectileInterceptor(serverLevel, projectile);
            }

            if (entity instanceof LivingEntity && !(entity instanceof ServerPlayer) && !(entity instanceof EnderDragon)) {
               TypeMoonWorldModVariables.UBWReturnData data = (TypeMoonWorldModVariables.UBWReturnData)entity.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
               if (data.ownerUUID != null) {
                  return;
               }

               Vec3 pos = entity.position();
               if (event.getLevel() instanceof ServerLevel serverLevel) {
                  UUID instanceOwner = UBWInstanceManager.getOwnerId(serverLevel.dimension());
                  if (instanceOwner != null && serverLevel.getServer().getPlayerList().getPlayer(instanceOwner) != null) {
                     attachGeneratedEntityToOwner(entity, instanceOwner, serverLevel);
                     return;
                  }
               }

               for (Entry<UUID, Vec3> entry : UBW_LOCATIONS.entrySet()) {
                  if (entry.getValue().distanceToSqr(pos) < 40000.0) {
                     UUID playerUUID = entry.getKey();
                     if (event.getLevel() instanceof ServerLevel serverLevel) {
                        attachGeneratedEntityToOwner(entity, playerUUID, serverLevel);
                     }

                     break;
                  }
               }
            }
         }
      }
   }

   private static void spawnUbwProjectileInterceptor(ServerLevel serverLevel, Projectile projectile) {
      if (projectile instanceof UBWInterceptorSwordEntity) {
         return;
      }

      UUID ownerId = resolveUbwOwner(serverLevel, projectile.position());
      if (ownerId == null) {
         return;
      }

      Entity shooter = projectile.getOwner();
      if (shooter != null && ownerId.equals(shooter.getUUID())) {
         return;
      }

      Vec3 spawnPos = findInterceptorSpawnPos(projectile);
      UBWInterceptorSwordEntity interceptor = new UBWInterceptorSwordEntity(serverLevel, projectile, ownerId, spawnPos);
      serverLevel.addFreshEntity(interceptor);
   }

   @Nullable
   private static UUID resolveUbwOwner(ServerLevel serverLevel, Vec3 pos) {
      UUID instanceOwner = UBWInstanceManager.getOwnerId(serverLevel.dimension());
      if (instanceOwner != null) {
         return instanceOwner;
      }

      for (Entry<UUID, Vec3> entry : UBW_LOCATIONS.entrySet()) {
         if (entry.getValue().distanceToSqr(pos) < 40000.0) {
            return entry.getKey();
         }
      }

      return null;
   }

   private static Vec3 findInterceptorSpawnPos(Projectile projectile) {
      RandomSource random = projectile.level().getRandom();
      Vec3 projectileMotion = projectile.getDeltaMovement();
      Vec3 baseDirection;
      if (projectileMotion.lengthSqr() > 1.0E-4) {
         baseDirection = projectileMotion.normalize().scale(-1.0);
      } else {
         double angle = random.nextDouble() * Math.PI * 2.0;
         baseDirection = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
      }

      Vec3 side = baseDirection.cross(new Vec3(0.0, 1.0, 0.0));
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(1.0, 0.0, 0.0);
      } else {
         side = side.normalize();
      }

      Vec3 up = side.cross(baseDirection).normalize();
      Vec3 lateralOffset = side.scale((random.nextDouble() - 0.5) * 1.4).add(up.scale((random.nextDouble() - 0.5) * 1.0));
      double distance = 5.0 + random.nextDouble() * 5.0;
      return projectile.position().add(baseDirection.scale(distance)).add(lateralOffset).add(0.0, 0.4 + random.nextDouble() * 0.8, 0.0);
   }

   @SubscribeEvent
   public static void onPlayerTick(Post event) {
      if (!event.getEntity().level().isClientSide) {
         if (event.getEntity() instanceof ServerPlayer player) {
            UBWInstanceManager.processPendingDeletions(player.getServer());
            processRemovalQueue(player);
            processRestorationQueue(player);
            processRefillQueue(player, (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            boolean isChanting = vars.is_chanting_ubw;
            boolean wasChanting = WAS_CHANTING.getOrDefault(player.getUUID(), false);
            MagicSwordBarrelFullOpen.tick(player);
            if (wasChanting && !isChanting && !vars.is_in_ubw) {
               clearVisualSwords(player);
               startTerrainRestoration(player);
               beginCollapseUbwRipple(player);
               if (PENDING_UBW_LOCATIONS.containsKey(player.getUUID())) {
                  PENDING_UBW_LOCATIONS.remove(player.getUUID());
                  PENDING_UBW_ACTIVATION_ATTEMPTS.remove(player.getUUID());
                  UBWInstanceManager.scheduleDeleteInstance(player.getServer(), player.getUUID());
               }

               if (vars.ubw_chant_progress > 0) {
                  vars.ubw_chant_progress = 0;
                  vars.ubw_chant_timer = 0;
                  vars.syncPlayerVariables(player);
               }
            }

            WAS_CHANTING.put(player.getUUID(), isChanting);
            boolean isOwner = false;
            if (vars.is_in_ubw && UBW_LOCATIONS.containsKey(player.getUUID())) {
               Vec3 center = UBW_LOCATIONS.get(player.getUUID());
               if (UBWInstanceManager.isUbwDimension(player.level()) && player.position().distanceToSqr(center) < 40000.0) {
                  isOwner = true;
               }
            }

            if (isOwner && player.tickCount % 20 == 0) {
               double cost = 10.0;
               if (vars.servant_card_transformed && ServantCardManaService.consume(player, vars, cost)) {
                  if (player.tickCount % 100 == 0) {
                     checkAndRefillSwords(player, vars);
                  }
               } else if (!vars.servant_card_transformed && vars.player_mana >= cost) {
                  vars.player_mana -= cost;
                  vars.syncMana(player);
                  if (player.tickCount % 100 == 0) {
                     checkAndRefillSwords(player, vars);
                  }
               } else {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.mana_depleted"), true);
                  returnFromUBW(player, vars);
               }
            }

            if (vars.is_chanting_ubw) {
               if (vars.ubw_chant_progress >= 3) {
                  updateUbwRipple(player, vars);
                  processTerrainEffect(player, vars);
               }
               player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false));
               vars.ubw_chant_timer++;

               int currentInterval = Math.max(20, 40 - (int)(vars.proficiency_unlimited_blade_works * 0.2));
               if (vars.ubw_chant_progress > 9) {
                  processChantStep(player, vars);
               } else if (vars.ubw_chant_timer >= currentInterval) {
                  vars.ubw_chant_timer = 0;
                  vars.ubw_chant_progress++;
                  if (vars.proficiency_unlimited_blade_works < 100.0) {
                     vars.proficiency_unlimited_blade_works = Math.min(100.0, vars.proficiency_unlimited_blade_works + 0.05);
                     if (vars.proficiency_unlimited_blade_works >= 1.0 && !vars.learned_magics.contains("sword_barrel_full_open")) {
                        vars.learned_magics.add("sword_barrel_full_open");
                        player.displayClientMessage(
                           Component.translatable(
                              "message.typemoonworld.magic.learned", Component.translatable("magic.typemoonworld.sword_barrel_full_open")
                           ),
                           true
                        );
                     }
                  }

                  processChantStep(player, vars);
               }

               if (vars.ubw_chant_progress >= 3 && vars.ubw_chant_progress <= 9 && vars.ubw_chant_timer % 8 == 0) {
                  int count = 3 + (vars.ubw_chant_progress - 3) * 2;
                  double maxRadius = 10.0 + (vars.ubw_chant_progress - 3) * 5.0;
                  spawnVisualSwords(player, vars, count, maxRadius);
               }
            } else {
               beginCollapseUbwRipple(player);
            }
         }
      }
   }

   private static void updateUbwRipple(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player.level() instanceof ServerLevel serverLevel) {
         UUID playerId = player.getUUID();
         UbwChantRippleEntity ripple = null;
         UUID rippleId = ACTIVE_UBW_RIPPLES.get(playerId);
         if (rippleId != null && serverLevel.getEntity(rippleId) instanceof UbwChantRippleEntity existing && existing.isAlive()) {
            ripple = existing;
         }

         if (ripple == null) {
            ripple = new UbwChantRippleEntity(serverLevel, player);
            serverLevel.addFreshEntity(ripple);
            ACTIVE_UBW_RIPPLES.put(playerId, ripple.getUUID());
         }
      }
   }

   private static void beginCollapseUbwRipple(ServerPlayer player) {
      if (player != null && player.level() instanceof ServerLevel serverLevel) {
         UUID rippleId = ACTIVE_UBW_RIPPLES.get(player.getUUID());
         if (rippleId != null && serverLevel.getEntity(rippleId) instanceof UbwChantRippleEntity ripple) {
            ripple.beginCollapse();
         }
      }
   }

   private static void discardUbwRipple(@Nullable ServerLevel level, UUID playerId) {
      if (playerId != null) {
         UUID rippleId = ACTIVE_UBW_RIPPLES.remove(playerId);
         if (level != null && rippleId != null && level.getEntity(rippleId) instanceof UbwChantRippleEntity ripple) {
            ripple.discard();
         }
      }
   }

   private static void checkAndRefillSwords(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (UBWInstanceManager.isUbwDimension(player.level())) {
         for (Entity entity : player.serverLevel().getEntities().getAll()) {
            if (entity instanceof LivingEntity target && target.isAlive() && (!(target instanceof ServerPlayer p) || !p.isSpectator())) {
               double checkRadius = 30.0;
               int swordCount = 0;
               List<BlockPos> placed = PLACED_SWORDS.get(player.getUUID());
               if (placed != null) {
                  double tX = target.getX();
                  double tZ = target.getZ();

                  for (BlockPos px : placed) {
                     if (px.distSqr(new Vec3i((int)tX, px.getY(), (int)tZ)) < checkRadius * checkRadius) {
                        swordCount++;
                     }
                  }
               }

               boolean isPowerful = target instanceof ServerPlayer || target.getMaxHealth() >= 50.0F || target instanceof WitherBoss;
               int threshold = isPowerful ? 60 : 20;
               if (swordCount < threshold) {
                  int minSpawn = isPowerful ? 80 : 20;
                  int varSpawn = isPowerful ? 40 : 10;
                  int toSpawn = minSpawn + player.getRandom().nextInt(varSpawn);
                  double spawnRadius = 30.0;

                  for (int i = 0; i < toSpawn; i++) {
                     int delay = player.getRandom().nextInt(20);
                     double r = player.getRandom().nextDouble() * spawnRadius;
                     double theta = player.getRandom().nextDouble() * Math.PI * 2.0;
                     double x = target.getX() + r * Math.cos(theta);
                     double z = target.getZ() + r * Math.sin(theta);
                     REFILL_QUEUES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>())
                        .add(new ChantHandler.RefillEntry(delay, new Vec3(x, target.getY(), z)));
                  }
               }
            }
         }
      }
   }

   private static void spawnPassiveSwords(ServerPlayer owner, LivingEntity target, int count) {
      if (!(target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
         ServerLevel level = (ServerLevel)owner.level();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

         for (int i = 0; i < count; i++) {
            double r = level.getRandom().nextDouble() * 15.0;
            double theta = level.getRandom().nextDouble() * Math.PI * 2.0;
            double x = target.getX() + r * Math.cos(theta);
            double z = target.getZ() + r * Math.sin(theta);
            spawnVisualSwordAt(owner, vars, new Vec3(x, target.getY(), z));
         }
      }
   }

   private static void spawnVisualSwordAt(ServerPlayer player, double centerX, double centerZ, double radius) {
      ServerLevel level = (ServerLevel)player.level();
      RandomSource random = level.getRandom();
      double r = random.nextDouble() * radius;
      double theta = random.nextDouble() * Math.PI * 2.0;
      double x = centerX + r * Math.cos(theta);
      double z = centerZ + r * Math.sin(theta);
      double y = player.getY() + 15.0 + random.nextDouble() * 10.0;
      UBWProjectileEntity sword = new UBWProjectileEntity((EntityType<? extends ThrowableItemProjectile>)ModEntities.UBW_PROJECTILE.get(), level);
      sword.setPos(x, y, z);
      sword.setOwner(player);
      sword.setVisualColorRgb(MagicCircuitColorHelper.ensureColor(player));
      sword.setDeltaMovement(0.0, -0.5 - random.nextDouble() * 0.5, 0.0);
      sword.setXRot(90.0F);
      level.addFreshEntity(sword);
   }

   private static void processRefillQueue(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      UUID uuid = player.getUUID();
      List<ChantHandler.RefillEntry> queue = REFILL_QUEUES.get(uuid);
      if (queue != null && !queue.isEmpty()) {
         Iterator<ChantHandler.RefillEntry> it = queue.iterator();

         while (it.hasNext()) {
            ChantHandler.RefillEntry entry = it.next();
            entry.delay--;
            if (entry.delay <= 0) {
               spawnVisualSwordAt(player, vars, entry.exactPos);
               it.remove();
            }
         }

         if (queue.isEmpty()) {
            REFILL_QUEUES.remove(uuid);
         }
      }
   }

   private static void spawnVisualSwordAt(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, Vec3 targetPos) {
      if (!player.level().isClientSide) {
         List<ItemStack> weapons = new ArrayList<>();

         for (ItemStack stack : vars.analyzed_items) {
            if (!(stack.getItem() instanceof NoblePhantasmItem)
               && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem)) {
               weapons.add(stack);
            }
         }

         if (!weapons.isEmpty()) {
            ItemStack weapon = weapons.get(player.getRandom().nextInt(weapons.size())).copy();
            double x = targetPos.x;
            double z = targetPos.z;
            if (!player.level().hasChunk((int)x >> 4, (int)z >> 4)) {
               return;
            }
            int surfaceY = player.level().getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)x, (int)z);
            double baseY = Math.max((double)surfaceY, targetPos.y);
            double y = baseY + 12.0 + player.getRandom().nextDouble() * 5.0;
            if (!player.level().getBlockState(BlockPos.containing(x, y, z)).isAir()) {
               y = baseY + 20.0;
            }

            UBWProjectileEntity projectile = new UBWProjectileEntity(player.level(), player, weapon);
            projectile.setPos(x, y, z);
            projectile.setVisualColorRgb(MagicCircuitColorHelper.ensureColor(vars));
            projectile.setDeltaMovement(0.0, -2.0, 0.0);
            projectile.setXRot(-90.0F);
            player.level().addFreshEntity(projectile);
            if (player.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z, 5, 0.2, 0.2, 0.2, 0.05);
               serverLevel.playSound(null, x, y, z, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.3F, 1.5F);
            }
         }
      }
   }

   private static void processTerrainEffect(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!player.level().isClientSide) {
         UUID uuid = player.getUUID();
         BlockPos center = CHANT_CENTERS.computeIfAbsent(uuid, k -> player.blockPosition());
         Map<BlockPos, ChantHandler.BlockBackup> backups = BACKUP_BLOCKS.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
         double maxRadius = 40.0;
         int maxStep = 9;
         int currentStep = vars.ubw_chant_progress;
         double currentRadius = TERRAIN_RADIUS.getOrDefault(uuid, 0.0);
         double stepTargetRadius = (double)currentStep / maxStep * maxRadius;
         double dist = stepTargetRadius - currentRadius;
         double speed = 0.0;
         if (dist > 0.0) {
            speed = Math.max(0.1, dist / 20.0);
            currentRadius += speed;
         } else {
            currentRadius = stepTargetRadius;
         }

         TERRAIN_RADIUS.put(uuid, currentRadius);
         int r = (int)Math.ceil(currentRadius);
         int prevR = (int)Math.floor(currentRadius - speed);
         if (r > prevR) {
            int floorY = center.getY() - 1;

            for (int x = -r; x <= r; x++) {
               for (int z = -r; z <= r; z++) {
                  double distSqr = x * x + z * z;
                  if (distSqr <= currentRadius * currentRadius) {
                     BlockPos pos = center.offset(x, 0, z);
                     if (!player.level().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                        continue;
                     }
                     BlockPos floorPos = new BlockPos(pos.getX(), floorY, pos.getZ());
                     if (!backups.containsKey(floorPos)) {
                        BlockState oldState = player.level().getBlockState(floorPos);
                        if (oldState.getDestroySpeed(player.level(), floorPos) >= 0.0F) {
                           CompoundTag nbt = null;
                           BlockEntity be = player.level().getBlockEntity(floorPos);
                           if (be != null) {
                              nbt = be.saveWithFullMetadata(player.level().registryAccess());
                           }

                           backups.put(floorPos, new ChantHandler.BlockBackup(oldState, nbt));
                           player.level().setBlock(floorPos, Blocks.RED_SAND.defaultBlockState(), 18);
                        }
                     }

                     BlockPos basePos = floorPos.below();
                     if (!backups.containsKey(basePos)) {
                        BlockState oldState = player.level().getBlockState(basePos);
                        if (oldState.getDestroySpeed(player.level(), basePos) >= 0.0F) {
                           CompoundTag nbt = null;
                           BlockEntity be = player.level().getBlockEntity(basePos);
                           if (be != null) {
                              nbt = be.saveWithFullMetadata(player.level().registryAccess());
                           }

                           backups.put(basePos, new ChantHandler.BlockBackup(oldState, nbt));
                           player.level().setBlock(basePos, Blocks.RED_SANDSTONE.defaultBlockState(), 18);
                        }
                     }

                     int maxY = (int)Math.sqrt(Math.max(0.0, maxRadius * maxRadius - distSqr));

                     for (int yOffset = 0; yOffset <= maxY; yOffset++) {
                        BlockPos airPos = new BlockPos(pos.getX(), center.getY() + yOffset, pos.getZ());
                        if (!backups.containsKey(airPos)) {
                           BlockState oldState = player.level().getBlockState(airPos);
                           if (!(oldState.getBlock() instanceof UBWWeaponBlock)
                              && !oldState.isAir()
                              && oldState.getDestroySpeed(player.level(), airPos) >= 0.0F) {
                              CompoundTag nbt = null;
                              BlockEntity be = player.level().getBlockEntity(airPos);
                              if (be != null) {
                                 nbt = be.saveWithFullMetadata(player.level().registryAccess());
                              }

                              backups.put(airPos, new ChantHandler.BlockBackup(oldState, nbt));
                              player.level().setBlock(airPos, Blocks.AIR.defaultBlockState(), 18);
                              AABB box = new AABB(airPos);
                              List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, box);
                              if (!items.isEmpty()) {
                                 List<ChantHandler.ItemBackup> itemBackups = BACKUP_ITEMS.computeIfAbsent(uuid, k -> new ArrayList<>());

                                 for (ItemEntity item : items) {
                                    item.discard();
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void startTerrainRestoration(ServerPlayer player) {
      UUID uuid = player.getUUID();
      Map<BlockPos, ChantHandler.BlockBackup> backups = BACKUP_BLOCKS.get(uuid);
      if (backups != null && !backups.isEmpty()) {
         BlockPos center = CHANT_CENTERS.get(uuid);
         if (center == null) {
            center = player.blockPosition();
         }

         List<BlockPos> sortedPos = new ArrayList<>(backups.keySet());
         BlockPos finalCenter = center;
         Collections.sort(sortedPos, (p1, p2) -> {
            double d1 = p1.distSqr(finalCenter);
            double d2 = p2.distSqr(finalCenter);
            return Double.compare(d2, d1);
         });
         RESTORATION_QUEUES.put(uuid, sortedPos);
      } else {
         CHANT_CENTERS.remove(uuid);
         TERRAIN_RADIUS.remove(uuid);
      }
   }

   private static void processRestorationQueue(ServerPlayer player) {
      UUID uuid = player.getUUID();
      List<BlockPos> queue = RESTORATION_QUEUES.get(uuid);
      if (queue != null && !queue.isEmpty()) {
         Map<BlockPos, ChantHandler.BlockBackup> backups = BACKUP_BLOCKS.get(uuid);
         if (backups == null) {
            RESTORATION_QUEUES.remove(uuid);
            return;
         }

         int blocksPerTick = 400;
         Iterator<BlockPos> it = queue.iterator();

         for (int processed = 0; it.hasNext() && processed < blocksPerTick; processed++) {
            BlockPos pos = it.next();
            ChantHandler.BlockBackup backup = backups.get(pos);
            if (backup != null) {
               player.level().setBlock(pos, backup.state(), 18);
               if (backup.nbt() != null) {
                  BlockEntity be = player.level().getBlockEntity(pos);
                  if (be != null) {
                     be.loadWithComponents(backup.nbt(), player.level().registryAccess());
                  }
               }
            }

            it.remove();
         }

         if (queue.isEmpty()) {
            List<ChantHandler.ItemBackup> items = BACKUP_ITEMS.remove(uuid);
            if (items != null) {
               for (ChantHandler.ItemBackup itemBackup : items) {
                  ItemEntity itemEntity = new ItemEntity(
                     player.level(), itemBackup.position().x, itemBackup.position().y, itemBackup.position().z, itemBackup.stack()
                  );
                  itemEntity.setDeltaMovement(0.0, 0.0, 0.0);
                  player.level().addFreshEntity(itemEntity);
               }
            }

            RESTORATION_QUEUES.remove(uuid);
            BACKUP_BLOCKS.remove(uuid);
            CHANT_CENTERS.remove(uuid);
            TERRAIN_RADIUS.remove(uuid);
         }
      }
   }

   private static void restoreTerrainInstantly(ServerPlayer player) {
      UUID uuid = player.getUUID();
      Map<BlockPos, ChantHandler.BlockBackup> backups = BACKUP_BLOCKS.get(uuid);
      if (backups != null) {
         for (Entry<BlockPos, ChantHandler.BlockBackup> entry : backups.entrySet()) {
            player.level().setBlock(entry.getKey(), entry.getValue().state(), 18);
            if (entry.getValue().nbt() != null) {
               BlockEntity be = player.level().getBlockEntity(entry.getKey());
               if (be != null) {
                  be.loadWithComponents(entry.getValue().nbt(), player.level().registryAccess());
               }
            }
         }

         BACKUP_BLOCKS.remove(uuid);
      }

      List<ChantHandler.ItemBackup> items = BACKUP_ITEMS.remove(uuid);
      if (items != null) {
         for (ChantHandler.ItemBackup itemBackup : items) {
            ItemEntity itemEntity = new ItemEntity(
               player.level(), itemBackup.position().x, itemBackup.position().y, itemBackup.position().z, itemBackup.stack()
            );
            itemEntity.setDeltaMovement(0.0, 0.0, 0.0);
            player.level().addFreshEntity(itemEntity);
         }
      }

      CHANT_CENTERS.remove(uuid);
      TERRAIN_RADIUS.remove(uuid);
      RESTORATION_QUEUES.remove(uuid);
   }

   private static void processRemovalQueue(ServerPlayer player) {
      UUID playerId = player.getUUID();
      if (REMOVAL_QUEUES.containsKey(playerId)) {
         List<ChantHandler.RemovalEntry> queue = REMOVAL_QUEUES.get(playerId);
         if (queue == null || queue.isEmpty()) {
            REMOVAL_QUEUES.remove(playerId);
            return;
         }

         Iterator<ChantHandler.RemovalEntry> iterator = queue.iterator();

         while (iterator.hasNext()) {
            ChantHandler.RemovalEntry entry = iterator.next();
            if (!entry.entity.isAlive()) {
               iterator.remove();
            } else {
               entry.delay--;
               if (entry.delay <= 0) {
                  if (entry.entity.level() instanceof ServerLevel serverLevel) {
                     serverLevel.sendParticles(ParticleTypes.POOF, entry.entity.getX(), entry.entity.getY() + 0.5, entry.entity.getZ(), 3, 0.1, 0.1, 0.1, 0.05);
                  }

                  entry.entity.discard();
                  iterator.remove();
               }
            }
         }

         if (queue.isEmpty()) {
            REMOVAL_QUEUES.remove(playerId);
         }
      }
   }

   private static void spawnEntrySwords(ServerPlayer player, Vec3 targetCenter) {
      if (targetCenter != null) {
         ServerLevel ubwLevel = player.serverLevel();
         if (ubwLevel != null) {
            RandomSource random = player.getRandom();
            int count = 30;
            double radius = 15.0;

            for (int i = 0; i < count; i++) {
               double angle = random.nextDouble() * 2.0 * Math.PI;
               double dist = random.nextDouble() * radius;
               double x = targetCenter.x + Math.cos(angle) * dist;
               double z = targetCenter.z + Math.sin(angle) * dist;
               BlockPos pos = new BlockPos((int)x, 100, (int)z);
               if (ubwLevel.getBlockState(pos).isAir()) {
                  BlockState state = (BlockState)((BlockState)((BlockState)((SwordBarrelBlock)ModBlocks.SWORD_BARREL_BLOCK.get())
                           .defaultBlockState()
                           .setValue(SwordBarrelBlock.FACING, Direction.UP))
                        .setValue(SwordBarrelBlock.ROTATION_A, random.nextBoolean()))
                     .setValue(SwordBarrelBlock.ROTATION_B, random.nextBoolean());
                  ubwLevel.setBlock(pos, state, 3);
                  registerPlacedSword(player.getUUID(), pos);
               }
            }
         }
      }
   }

   private static void processChantStep(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      int progress = vars.ubw_chant_progress;
      double cost = 50.0;
      String chantText = "";
      if (progress == 1) {
         ServerLevel ubwLevel = UBWInstanceManager.getOrCreateFreshPlayerInstance(player);
         if (ubwLevel == null) {
            interruptChant(player, vars, "message.typemoonworld.unlimited_blade_works.occupied");
            return;
         }

         Vec3 entryPos = UBWInstanceManager.randomEntryPosition(player.getRandom());
         double offsetX = entryPos.x;
         double offsetZ = entryPos.z;
         BlockPos targetPos = new BlockPos((int)offsetX, 100, (int)offsetZ);
         UBWInstanceManager.keepInstanceTicking(player.getUUID(), ubwLevel, targetPos);
         PENDING_UBW_LOCATIONS.put(player.getUUID(), new Vec3(offsetX, 0.0, offsetZ));
      } else if (progress == 2) {
         chantText = "\u00A7bSteel is my body, and fire is my blood.";
      } else if (progress == 3) {
         chantText = "\u00A7bI have created over a thousand blades.";
         spawnVisualSwords(player, vars, 10, 10.0);
      } else if (progress == 4) {
         chantText = "\u00A7bUnaware of loss.";
      } else if (progress == 5) {
         chantText = "\u00A7bNor aware of gain.";
      } else if (progress == 6) {
         chantText = "\u00A7bWithstood pain to create weapons, waiting for one's arrival.";
      } else if (progress == 7) {
         chantText = "\u00A7bI have no regrets.";
      } else if (progress == 8) {
         chantText = "\u00A7bThis is the only path.";
      } else if (progress == 9) {
         chantText = "\u00A7bMy whole life was,";
      } else if (progress > 9) {
         if (UBWInstanceManager.ensureRegisteredPlayerInstance(player) == null) {
            int attempts = PENDING_UBW_ACTIVATION_ATTEMPTS.merge(player.getUUID(), 1, Integer::sum);
            if (attempts >= UBW_ACTIVATION_WAIT_MAX_ATTEMPTS) {
               PENDING_UBW_ACTIVATION_ATTEMPTS.remove(player.getUUID());
               interruptChant(player, vars, "message.typemoonworld.unlimited_blade_works.interrupted");
               UBWInstanceManager.scheduleDeleteInstance(player.getServer(), player.getUUID());
            } else {
               vars.ubw_chant_timer = 0;
               vars.syncPlayerVariables(player);
            }
         } else if (consumeUbwMana(player, vars, cost)) {
            PENDING_UBW_ACTIVATION_ATTEMPTS.remove(player.getUUID());
            if (!activateUBW(player, vars)) {
               vars.ubw_chant_timer = 0;
               vars.syncPlayerVariables(player);
            } else if (vars.servant_card_transformed && "emiya_archer".equals(vars.servant_card_id)) {
               ServantCardTransformManager.setNoblePhantasmCooldown(player, vars, Math.max(vars.servant_card_np_cooldown, 2400));
               vars.syncPlayerVariables(player);
            }
         } else {
            interruptChant(player, vars, "message.typemoonworld.unlimited_blade_works.mana_depleted");
         }

         return;
      }

      if (consumeUbwMana(player, vars, cost)) {
         player.displayClientMessage(Component.literal(chantText), true);
         vars.syncPlayerVariables(player);
      } else {
         interruptChant(player, vars, "message.typemoonworld.unlimited_blade_works.mana_depleted");
      }
   }

   private static boolean consumeUbwMana(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double cost) {
      if (vars.servant_card_transformed) {
         return ServantCardManaService.consume(player, vars, cost);
      }
      return ManaHelper.consumeManaOrHealth(player, cost);
   }

   private static void spawnVisualSwords(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int count, double maxRadius) {
      for (int i = 0; i < count; i++) {
         double rNormalized = Math.sqrt(player.getRandom().nextDouble());
         if (rNormalized < 0.2) {
            rNormalized = 0.2 + player.getRandom().nextDouble() * 0.8;
         }

         double radius = rNormalized * maxRadius;
         double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
         double x = player.getX() + radius * Math.cos(angle);
         double z = player.getZ() + radius * Math.sin(angle);
         if (!(Math.abs(x - player.getX()) < 1.0) || !(Math.abs(z - player.getZ()) < 1.0)) {
            spawnVisualSwordAt(player, vars, new Vec3(x, player.getY(), z));
         }
      }
   }

   private static void clearVisualSwords(ServerPlayer player) {
      BlockPos center = player.blockPosition();
      UUID uuid = player.getUUID();
      List<BlockPos> placed = PLACED_SWORDS.remove(uuid);
      if (placed != null) {
         for (BlockPos pos : placed) {
            if (player.level().getBlockState(pos).getBlock() instanceof UBWWeaponBlock) {
               player.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
               if (player.level() instanceof ServerLevel serverLevel) {
                  serverLevel.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.05);
               }
            }
         }
      }

      double terrainRadius = TERRAIN_RADIUS.getOrDefault(uuid, 0.0);
      int scanR = (int)Math.max(terrainRadius, 80.0);
      AABB scanBox = new AABB(center).inflate(scanR, 64.0, scanR);

      for (UBWProjectileEntity projectile : player.level().getEntitiesOfClass(UBWProjectileEntity.class, scanBox)) {
         projectile.discard();
         if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, projectile.getX(), projectile.getY(), projectile.getZ(), 2, 0.1, 0.1, 0.1, 0.05);
         }
      }
   }

   private static int findSafeSpawnY(ServerLevel level, int x, int z) {
      MutableBlockPos pos = new MutableBlockPos(x, 320, z);
      int surfaceY = -999;

      for (int y = 320; y > -64; y--) {
         pos.setY(y);
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && state.isFaceSturdy(level, pos, Direction.UP)) {
            surfaceY = y;
            break;
         }
      }

      return surfaceY == -999 ? 64 : surfaceY + 1;
   }

   private static boolean activateUBW(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      ServerLevel ubwLevel = UBWInstanceManager.getRegisteredPlayerInstance(player);
      if (ubwLevel == null) {
         return false;
      }

      PENDING_UBW_ACTIVATION_ATTEMPTS.remove(player.getUUID());
      beginCollapseUbwRipple(player);
      vars.is_chanting_ubw = false;
      vars.ubw_chant_progress = 0;
      vars.ubw_chant_timer = 0;
      vars.is_in_ubw = true;
      vars.ubw_return_x = player.getX();
      vars.ubw_return_y = player.getY();
      vars.ubw_return_z = player.getZ();
      vars.ubw_return_dimension = player.level().dimension().location().toString();
      vars.syncPlayerVariables(player);
      player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
      player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.activated"), true);
      restoreTerrainInstantly(player);
      clearVisualSwords(player);
      ACTIVE_UBW_ENTITIES.remove(player.getUUID());
      ACTIVE_ENTITY_POSITIONS.remove(player.getUUID());
      Vec3 entrySwordCenter = null;
      double range = 40.0;
      double halfSphereMinY = player.getY() - 2.0;
      AABB tpBox = player.getBoundingBox().inflate(range);
      List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, tpBox, e -> e != player && e.isAlive() && !(e instanceof EnderDragon)
         && e.getY() >= halfSphereMinY
         && e.distanceToSqr(player.getX(), player.getY(), player.getZ()) <= range * range);
      List<ChantHandler.EntityReturnData> teleported = new ArrayList<>();
      TELEPORTED_ENTITIES.put(player.getUUID(), teleported);
      List<Entity> swordRainTargets = new ArrayList<>();
      if (ubwLevel != null) {
         double offsetX;
         double offsetZ;
         if (PENDING_UBW_LOCATIONS.containsKey(player.getUUID())) {
            Vec3 pending = PENDING_UBW_LOCATIONS.remove(player.getUUID());
            offsetX = pending.x;
            offsetZ = pending.z;
         } else {
            Vec3 entryPos = UBWInstanceManager.randomEntryPosition(player.getRandom());
            offsetX = entryPos.x;
            offsetZ = entryPos.z;
         }

         int safeY = findSafeSpawnY(ubwLevel, (int)offsetX, (int)offsetZ);
         double targetY = safeY;
         entrySwordCenter = new Vec3(offsetX, targetY, offsetZ);
         player.teleportTo(ubwLevel, offsetX, targetY, offsetZ, player.getYRot(), player.getXRot());
         BlockPos targetPos = new BlockPos((int)offsetX, (int)targetY, (int)offsetZ);
         UBWInstanceManager.keepInstanceTicking(player.getUUID(), ubwLevel, targetPos);
         UBW_LOCATIONS.put(player.getUUID(), new Vec3(offsetX, targetY, offsetZ));
         swordRainTargets.add(player);

         for (LivingEntity target : targets) {
            Vec3 originalPos = target.position();
            TypeMoonWorldModVariables.UBWReturnData data = (TypeMoonWorldModVariables.UBWReturnData)target.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
            data.ownerUUID = player.getUUID();
            data.returnX = originalPos.x;
            data.returnY = originalPos.y;
            data.returnZ = originalPos.z;
            data.returnDim = vars.ubw_return_dimension;
            double relX = target.getX() - vars.ubw_return_x;
            double relZ = target.getZ() - vars.ubw_return_z;
            double entityTargetX = offsetX + relX;
            double entityTargetZ = offsetZ + relZ;
            int entitySafeY = findSafeSpawnY(ubwLevel, (int)entityTargetX, (int)entityTargetZ);
            double entityTargetY = entitySafeY;
            ChantHandler.EntityReturnData dataObj = new ChantHandler.EntityReturnData(target.getUUID(), originalPos);
            teleported.add(dataObj);
            Entity newEntity = target.changeDimension(
               new DimensionTransition(
                  ubwLevel,
                  new Vec3(entityTargetX, entityTargetY, entityTargetZ),
                  Vec3.ZERO,
                  target.getYRot(),
                  target.getXRot(),
                  DimensionTransition.DO_NOTHING
               )
            );
            if (newEntity != null) {
               ACTIVE_UBW_ENTITIES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(newEntity.getUUID());
               ACTIVE_ENTITY_POSITIONS.put(newEntity.getUUID(), newEntity.position());
               TypeMoonWorldModVariables.UBWReturnData newData = (TypeMoonWorldModVariables.UBWReturnData)newEntity.getData(
                  TypeMoonWorldModVariables.UBW_RETURN_DATA
               );
               newData.ownerUUID = player.getUUID();
               newData.returnX = originalPos.x;
               newData.returnY = originalPos.y;
               newData.returnZ = originalPos.z;
               newData.returnDim = vars.ubw_return_dimension;
               if (newEntity instanceof ServerPlayer targetPlayer) {
                  TypeMoonWorldModVariables.PlayerVariables targetVars = (TypeMoonWorldModVariables.PlayerVariables)targetPlayer.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  if (targetVars.is_chanting_ubw) {
                     interruptChant(targetPlayer, targetVars, "message.typemoonworld.unlimited_blade_works.interrupted");
                  }
               }
            }
         }

         initialUBWFill(player, vars, swordRainTargets);
      }

      if (entrySwordCenter != null) {
         spawnEntrySwords(player, entrySwordCenter);
         if (vars.servant_card_transformed && "emiya_archer".equals(vars.servant_card_id) && player.level() instanceof ServerLevel activeLevel) {
            spawnServantCardUbwGears(player, activeLevel, entrySwordCenter);
         }
      }

      player.level().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0F, 1.0F);
      return true;
   }

   private static void spawnServantCardUbwGears(ServerPlayer player, ServerLevel level, Vec3 center) {
      int gearCount = 9;
      for (int i = 0; i < gearCount; i++) {
         int variant = player.getRandom().nextInt(3);
         float scale = 8.0F + player.getRandom().nextFloat() * 8.0F;
         double angle = Math.PI * 2.0 * i / gearCount + (player.getRandom().nextDouble() - 0.5) * 0.18;
         double distance = 50.0 + player.getRandom().nextDouble() * 45.0;
         double x = center.x + Math.cos(angle) * distance;
         double z = center.z + Math.sin(angle) * distance;
         double cloudY = Math.max(level.getMinBuildHeight() + 42.0, Math.min(158.0, level.getMaxBuildHeight() - 32.0));
         double y = cloudY + (player.getRandom().nextDouble() - 0.5) * 10.0;
         level.addFreshEntity(new UbwSkyGearEntity(level, x, y, z, variant, scale, 0, player.getUUID()));
      }
   }

   private static void clearServantCardUbwGears(ServerLevel level, UUID ownerUUID, Vec3 center) {
      AABB scanBox = new AABB(
         center.x - 192.0,
         level.getMinBuildHeight(),
         center.z - 192.0,
         center.x + 192.0,
         level.getMaxBuildHeight(),
         center.z + 192.0
      );
      for (UbwSkyGearEntity gear : level.getEntitiesOfClass(UbwSkyGearEntity.class, scanBox, gear -> gear.isOwnedBy(ownerUUID))) {
         gear.discard();
      }
   }

   private static void initialUBWFill(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, List<Entity> targets) {
      if (!player.level().isClientSide) {
         if (targets == null || targets.isEmpty()) {
            targets = new ArrayList<>();
            targets.add(player);
            double scanRange = 64.0;
            AABB scanBox = player.getBoundingBox().inflate(scanRange);
            List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, scanBox);
            targets.addAll(nearby);
         }

         for (Entity target : targets) {
            if (target instanceof LivingEntity living) {
               int swordCount = 400 + player.getRandom().nextInt(200);
               double spawnRadius = 64.0;

               for (int i = 0; i < swordCount; i++) {
                  int delay = player.getRandom().nextInt(40);
                  double r = player.getRandom().nextDouble() * player.getRandom().nextDouble() * spawnRadius;
                  double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
                  double x = target.getX() + r * Math.cos(angle);
                  double z = target.getZ() + r * Math.sin(angle);
                  REFILL_QUEUES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>())
                     .add(new ChantHandler.RefillEntry(delay, new Vec3(x, target.getY(), z)));
               }
            }
         }
      }
   }

   private static void interruptChant(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, String reasonKey) {
      vars.is_chanting_ubw = false;
      vars.ubw_chant_progress = 0;
      vars.ubw_chant_timer = 0;
      vars.syncPlayerVariables(player);
      PENDING_UBW_ACTIVATION_ATTEMPTS.remove(player.getUUID());
      clearVisualSwords(player);
      startTerrainRestoration(player);
      player.displayClientMessage(Component.translatable(reasonKey), true);
   }

   @SubscribeEvent
   public static void onLivingDeath(LivingDeathEvent event) {
      if (!event.getEntity().level().isClientSide) {
         if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            if (vars.is_in_ubw) {
               returnFromUBW(player, vars);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (UBWInstanceManager.isUbwDimension(event.getFrom().location()) && vars.is_in_ubw) {
            ServerLevel sourceLevel = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, event.getFrom().location()));
            ServerLevel targetLevel = (ServerLevel)player.level();
            if (sourceLevel != null) {
               Vec3 center = UBW_LOCATIONS.get(player.getUUID());
               if (center == null) {
                  center = new Vec3(player.getX(), player.getY(), player.getZ());
               }

               clearVisualSwords(player);
               clearServantCardUbwGears(sourceLevel, player.getUUID(), center);
               returnEntitiesOnly(player, vars, sourceLevel, targetLevel, center);
            }

            vars.is_in_ubw = false;
            vars.syncPlayerVariables(player);
            UBW_LOCATIONS.remove(player.getUUID());
            UBWInstanceManager.scheduleDeleteInstance(player.getServer(), player.getUUID());
         }
      }
   }

   public static void returnFromUBW(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      beginCollapseUbwRipple(player);
      ServerLevel returnLevel = resolveDimensionOrOverworld(player.getServer(), vars.ubw_return_dimension);
      if (returnLevel == null) {
         returnLevel = player.serverLevel();
      }

      if (player.level() instanceof ServerLevel sourceLevel && UBWInstanceManager.isUbwDimension(sourceLevel)) {
         clearVisualSwords(player);
         Vec3 center = UBW_LOCATIONS.getOrDefault(player.getUUID(), player.position());
         clearServantCardUbwGears(sourceLevel, player.getUUID(), center);
         returnEntitiesOnly(player, vars, sourceLevel, returnLevel, player.position());
      }

      vars.is_in_ubw = false;
      vars.syncPlayerVariables(player);
      UBW_LOCATIONS.remove(player.getUUID());
      ACTIVE_UBW_ENTITIES.remove(player.getUUID());
      ACTIVE_ENTITY_POSITIONS.remove(player.getUUID());
      player.teleportTo(returnLevel, vars.ubw_return_x, vars.ubw_return_y, vars.ubw_return_z, player.getYRot(), player.getXRot());
      UBWInstanceManager.scheduleDeleteInstance(player.getServer(), player.getUUID());
   }

   public static boolean activateServantCardUbwNow(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null) {
         return false;
      }
      if (UBWInstanceManager.ensureRegisteredPlayerInstance(player) == null) {
         ServerLevel ubwLevel = UBWInstanceManager.getOrCreateFreshPlayerInstance(player);
         if (ubwLevel == null) {
            return false;
         }
         Vec3 entryPos = UBWInstanceManager.randomEntryPosition(player.getRandom());
         BlockPos targetPos = new BlockPos((int)entryPos.x, 100, (int)entryPos.z);
         UBWInstanceManager.keepInstanceTicking(player.getUUID(), ubwLevel, targetPos);
         PENDING_UBW_LOCATIONS.put(player.getUUID(), new Vec3(entryPos.x, 0.0, entryPos.z));
      }
      vars.has_unlimited_blade_works = true;
      vars.is_chanting_ubw = true;
      vars.ubw_chant_progress = 10;
      vars.ubw_chant_timer = 0;
      if (player.getPersistentData().getBoolean("ServantCardUbwTransitionPending")) {
         return true;
      }
      player.getPersistentData().putBoolean("ServantCardUbwTransitionPending", true);
      var sourceDimension = player.level().dimension();
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 4, false, false, false));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 6, false, false, false));
      vars.syncPlayerVariables(player);
      TYPE_MOON_WORLD.queueServerWork(40, () -> {
         player.getPersistentData().remove("ServantCardUbwTransitionPending");
         TypeMoonWorldModVariables.PlayerVariables current = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (player.isAlive()
            && player.level().dimension().equals(sourceDimension)
            && current.servant_card_transformed
            && "emiya_archer".equals(current.servant_card_id)) {
            activateUBW(player, current);
         } else {
            current.is_chanting_ubw = false;
            current.ubw_chant_progress = 0;
            current.syncPlayerVariables(player);
         }
      });
      return true;
   }

   private static void returnEntitiesOnly(
      ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ServerLevel sourceLevel, ServerLevel targetLevel, Vec3 centerPos
   ) {
      UUID playerUUID = player.getUUID();
      Set<LivingEntity> toReturn = new LinkedHashSet<>();
      List<UUID> activeUUIDs = ACTIVE_UBW_ENTITIES.get(playerUUID);
      int memoryCount = 0;
      if (activeUUIDs != null) {
         for (UUID uuid : activeUUIDs) {
            Entity e = sourceLevel.getEntity(uuid);
            if (e instanceof LivingEntity living && e.isAlive()) {
               toReturn.add(living);
               memoryCount++;
            }
         }
      }

      Iterable<Entity> allEntities = sourceLevel.getEntities().getAll();
      int scanCount = 0;

      for (Entity entity : allEntities) {
         if (entity instanceof LivingEntity livingEntity
            && entity != player
            && entity.isAlive()
            && !(entity instanceof EnderDragon)
            && !toReturn.contains(livingEntity)) {
            TypeMoonWorldModVariables.UBWReturnData data = (TypeMoonWorldModVariables.UBWReturnData)livingEntity.getData(
               TypeMoonWorldModVariables.UBW_RETURN_DATA
            );
            if (data.ownerUUID != null && data.ownerUUID.equals(playerUUID)) {
               toReturn.add(livingEntity);
               scanCount++;
            }
         }
      }

      for (LivingEntity entityx : toReturn) {
         TypeMoonWorldModVariables.UBWReturnData data = (TypeMoonWorldModVariables.UBWReturnData)entityx.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
         double relX = entityx.getX() - centerPos.x;
         double relY = entityx.getY() - centerPos.y;
         double relZ = entityx.getZ() - centerPos.z;
         double targetX = vars.ubw_return_x + relX;
         double targetY = vars.ubw_return_y + relY;
         double targetZ = vars.ubw_return_z + relZ;
         BlockPos targetBlockPos = new BlockPos((int)targetX, (int)targetY, (int)targetZ);
         BlockState state = targetLevel.getBlockState(targetBlockPos);
         if (state.isSuffocating(targetLevel, targetBlockPos)) {
            boolean foundSafe = false;

            for (int i = 1; i <= 5; i++) {
               if (!targetLevel.getBlockState(targetBlockPos.above(i)).isSuffocating(targetLevel, targetBlockPos.above(i))) {
                  targetY += i;
                  foundSafe = true;
                  break;
               }
            }

            if (!foundSafe) {
               targetY = targetLevel.getHeight(Types.MOTION_BLOCKING, (int)targetX, (int)targetZ);
            }
         }

         targetY += 0.1;
         data.ownerUUID = null;
         data.returnDim = "";
         data.returnX = 0.0;
         data.returnY = 0.0;
         data.returnZ = 0.0;
         data.generated = false;
         entityx.setPortalCooldown(0);
         if (entityx.isPassenger()) {
            entityx.stopRiding();
         }

         Entity var39 = entityx.changeDimension(
            new DimensionTransition(
               targetLevel, new Vec3(targetX, targetY, targetZ), Vec3.ZERO, entityx.getYRot(), entityx.getXRot(), DimensionTransition.DO_NOTHING
            )
         );
      }

      TELEPORTED_ENTITIES.remove(playerUUID);
      GENERATED_ENTITIES.remove(playerUUID);
   }

   @SubscribeEvent
   public static void onLevelTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
      Level level = event.getLevel();
      if (!level.isClientSide) {
         if (UBWInstanceManager.isUbwDimension(level)) {
            if (level.getGameTime() % 20L == 0L) {
               if (level instanceof ServerLevel serverLevel) {
                  for (Entry<UUID, List<UUID>> entry : ACTIVE_UBW_ENTITIES.entrySet()) {
                     for (UUID entityUUID : entry.getValue()) {
                        Entity entity = serverLevel.getEntity(entityUUID);
                        if (entity != null && entity.isAlive()) {
                           ACTIVE_ENTITY_POSITIONS.put(entityUUID, entity.position());
                        }
                     }
                  }

                  Iterable<Entity> allEntities = serverLevel.getEntities().getAll();
                  List<LivingEntity> orphans = new ArrayList<>();

                  for (Entity entity : allEntities) {
                     if (entity instanceof LivingEntity living && !(entity instanceof ServerPlayer)) {
                        TypeMoonWorldModVariables.UBWReturnData data = (TypeMoonWorldModVariables.UBWReturnData)living.getData(
                           TypeMoonWorldModVariables.UBW_RETURN_DATA
                        );
                        if (data.ownerUUID != null) {
                           UUID ownerUUID = data.ownerUUID;
                           if (!UBW_LOCATIONS.containsKey(ownerUUID)) {
                              orphans.add(living);
                           }
                        }
                     }
                  }

                  for (LivingEntity orphan : orphans) {
                     TypeMoonWorldModVariables.UBWReturnData data = (TypeMoonWorldModVariables.UBWReturnData)orphan.getData(
                        TypeMoonWorldModVariables.UBW_RETURN_DATA
                     );
                     String dimStr = data.returnDim;
                     double retX = data.returnX;
                     double retY = data.returnY;
                     double retZ = data.returnZ;
                     ServerLevel targetLevel = resolveDimensionOrOverworld(serverLevel.getServer(), dimStr);
                     if (targetLevel == null) {
                        targetLevel = serverLevel;
                     }

                     BlockPos targetBlockPos = new BlockPos((int)retX, (int)retY, (int)retZ);
                     BlockState state = targetLevel.getBlockState(targetBlockPos);
                     if (state.isSuffocating(targetLevel, targetBlockPos)) {
                        retY = targetLevel.getHeight(Types.MOTION_BLOCKING, (int)retX, (int)retZ) + 0.1;
                     }

                     data.ownerUUID = null;
                     data.returnDim = "";
                     data.returnX = 0.0;
                     data.returnY = 0.0;
                     data.returnZ = 0.0;
                     data.generated = false;
                     orphan.setPortalCooldown(0);
                     if (orphan.isPassenger()) {
                        orphan.stopRiding();
                     }

                     Entity var18 = orphan.changeDimension(
                        new DimensionTransition(
                           targetLevel, new Vec3(retX, retY, retZ), Vec3.ZERO, orphan.getYRot(), orphan.getXRot(), DimensionTransition.DO_NOTHING
                        )
                     );
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLivingDamage(LivingIncomingDamageEvent event) {
      if (!event.getEntity().level().isClientSide) {
         if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            if (vars.is_in_ubw && UBW_LOCATIONS.containsKey(player.getUUID())) {
               Vec3 center = UBW_LOCATIONS.get(player.getUUID());
               if (UBWInstanceManager.isUbwDimension(player.level())
                  && player.position().distanceToSqr(center) < 40000.0
                  && (
                     event.getSource().getDirectEntity() instanceof UBWProjectileEntity
                        || event.getSource().getDirectEntity() instanceof SwordBarrelProjectileEntity
                  )) {
                  event.setCanceled(true);
                  return;
               }
            }

            if (vars.is_chanting_ubw) {
               float damage = event.getAmount();
               if (damage >= 4.0F) {
                  interruptChant(player, vars, "message.typemoonworld.unlimited_blade_works.interrupted");
               }
            }
         }
      }
   }

   private record BlockBackup(BlockState state, @Nullable CompoundTag nbt) {
   }

   private static class EntityReturnData {
      final UUID uuid;
      final Vec3 originalPos;

      EntityReturnData(UUID uuid, Vec3 originalPos) {
         this.uuid = uuid;
         this.originalPos = originalPos;
      }
   }

   private record ItemBackup(ItemStack stack, Vec3 position) {
   }

   private static class RefillEntry {
      int delay;
      final Vec3 exactPos;

      RefillEntry(int delay, Vec3 exactPos) {
         this.delay = delay;
         this.exactPos = exactPos;
      }
   }

   private static class RemovalEntry {
      final Entity entity;
      int delay;

      RemovalEntry(Entity entity, int delay) {
         this.entity = entity;
         this.delay = delay;
      }
   }
}

