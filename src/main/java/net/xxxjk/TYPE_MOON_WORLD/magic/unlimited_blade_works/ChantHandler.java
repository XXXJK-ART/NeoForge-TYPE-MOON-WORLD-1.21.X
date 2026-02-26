
package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.TicketType;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class ChantHandler {

    // Chant intervals (in ticks). Assuming 20 ticks = 1 second.
    // Total lines: 9 lines + 1 activation
    private static final int BASE_CHANT_INTERVAL = 40; // 2 seconds base
    
    // UUID -> Center Position of UBW (In UBW dimension)
    private static final Map<UUID, Vec3> UBW_LOCATIONS = new ConcurrentHashMap<>();
    
    // UUID -> Pending Target Position (Pre-calculated)
    private static final Map<UUID, Vec3> PENDING_UBW_LOCATIONS = new ConcurrentHashMap<>();

    // Store entities to be removed with delay for each player
    // UUID -> List of RemovalEntry
    private static final Map<UUID, List<RemovalEntry>> REMOVAL_QUEUES = new ConcurrentHashMap<>();
    
    // Terrain Backup Storage
    // UUID -> (BlockPos -> BlockBackup)
    private static final Map<UUID, Map<BlockPos, BlockBackup>> BACKUP_BLOCKS = new ConcurrentHashMap<>();
    
    private static record BlockBackup(BlockState state, @org.jetbrains.annotations.Nullable CompoundTag nbt) {}

    // UUID -> List of captured ItemEntities (Stored as ItemStack and Position)
    private static final Map<UUID, List<ItemBackup>> BACKUP_ITEMS = new ConcurrentHashMap<>();
    
    private static record ItemBackup(ItemStack stack, Vec3 position) {}

    // UUID -> Center Position of Chant
    private static final Map<UUID, BlockPos> CHANT_CENTERS = new ConcurrentHashMap<>();
    // UUID -> Current max radius of terrain effect
    private static final Map<UUID, Double> TERRAIN_RADIUS = new ConcurrentHashMap<>();
    // UUID -> Restoration Queue (Positions to restore, ordered or just set)
    private static final Map<UUID, List<BlockPos>> RESTORATION_QUEUES = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        
        // Safety: If player is logging out while chanting, restore terrain instantly
        // This prevents corrupted world state if the server restarts or player doesn't log back in for a while
        // Check local state or persistent variable?
        // Variables are attached to player, but player is about to unload.
        // We can check our Map state.
        if (WAS_CHANTING.getOrDefault(uuid, false)) {
             if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                 restoreTerrainInstantly(serverPlayer);
             }
        }
        
        UBW_LOCATIONS.remove(uuid);
        PENDING_UBW_LOCATIONS.remove(uuid);
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
        WAS_CHANTING.remove(uuid);
    }
    
    // UUID -> List of Entities that were teleported with the player
    private static final Map<UUID, List<EntityReturnData>> TELEPORTED_ENTITIES = new ConcurrentHashMap<>();
    
    // UUID -> List of Entities that were generated/spawned inside UBW (e.g. summons, splits)
    private static final Map<UUID, List<UUID>> GENERATED_ENTITIES = new ConcurrentHashMap<>();

    // UUID -> List of Placed Sword Blocks (for cleanup and density check)
    public static final Map<UUID, List<BlockPos>> PLACED_SWORDS = new ConcurrentHashMap<>();

    // UUID -> List of Active Entity UUIDs inside UBW (for robust retrieval)
    private static final Map<UUID, List<UUID>> ACTIVE_UBW_ENTITIES = new ConcurrentHashMap<>();
    
    // UUID -> Last Known Position of UBW Entities (for chunk loading backup)
    private static final Map<UUID, Vec3> ACTIVE_ENTITY_POSITIONS = new ConcurrentHashMap<>();

    public static void registerPlacedSword(UUID playerUUID, BlockPos pos) {
        PLACED_SWORDS.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(pos);
    }

    private static class EntityReturnData {
        final UUID uuid;
        final Vec3 originalPos;
        
        EntityReturnData(UUID uuid, Vec3 originalPos) {
            this.uuid = uuid;
            this.originalPos = originalPos;
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

    // Store previous chant state to detect falling edge (True -> False)
    private static final Map<UUID, Boolean> WAS_CHANTING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!event.getLevel().dimension().location().equals(ModDimensions.UBW_KEY.location())) return;
        
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity && !(entity instanceof ServerPlayer) && !(entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)) {
            // Check if this entity was teleported IN (Has Owner Tag)
            // If it has tag, we don't need to do anything, it's already tracked by tag.
            TypeMoonWorldModVariables.UBWReturnData data = entity.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
            if (data.ownerUUID != null) {
                return;
            }
            
            // If no tag, it means it's newly spawned inside UBW (e.g. Summon, Split, Natural Spawn)
            // Find which UBW instance this belongs to
            Vec3 pos = entity.position();
            
            for (Map.Entry<UUID, Vec3> entry : UBW_LOCATIONS.entrySet()) {
                if (entry.getValue().distanceToSqr(pos) < 40000) { // 200 block radius squared
                    UUID playerUUID = entry.getKey();
                    
                    // Mark as generated
                    data.ownerUUID = playerUUID;
                    data.generated = true;
                    
                    // Keep legacy list for now
                    GENERATED_ENTITIES.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(entity.getUUID());
                    
                    // Track Active Entity in UBW
                    ACTIVE_UBW_ENTITIES.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(entity.getUUID());
                    ACTIVE_ENTITY_POSITIONS.put(entity.getUUID(), entity.position());
                    
                    break; // Found the owner
                }
            }
        }
    }



    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            // Process removal queue first
            processRemovalQueue(player);
            // Process restoration queue
            processRestorationQueue(player);
            // Process refill queue
            processRefillQueue(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
            
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            


            boolean isChanting = vars.is_chanting_ubw;
            boolean wasChanting = WAS_CHANTING.getOrDefault(player.getUUID(), false);
            
            // Handle Sword Barrel Full Open Tick
            MagicSwordBarrelFullOpen.tick(player);
            
            // Detect falling edge: Player WAS chanting but IS NOT chanting anymore.
            // This covers all cancellation cases: key release, packet update, interruption, etc.
            if (wasChanting && !isChanting && !vars.is_in_ubw) {
                // Stopped chanting and didn't enter UBW (if entered UBW, is_in_ubw would be true)
                // Ensure visual effects are cleared.
                clearVisualSwords(player);
                startTerrainRestoration(player); // Start restoring terrain
                
                // Clear pending location/ticket if canceled
                if (PENDING_UBW_LOCATIONS.containsKey(player.getUUID())) {
                     Vec3 pending = PENDING_UBW_LOCATIONS.remove(player.getUUID());
                     ServerLevel ubwLevel = player.getServer().getLevel(ModDimensions.UBW_KEY);
                     if (ubwLevel != null) {
                         BlockPos targetPos = new BlockPos((int)pending.x, 100, (int)pending.z);
                         ChunkPos chunkPos = new ChunkPos(targetPos);
                         ubwLevel.getChunkSource().removeRegionTicket(TicketType.PLAYER, chunkPos, 3, chunkPos);
                     }
                }
                
                // Reset progress just in case
                if (vars.ubw_chant_progress > 0) {
                    vars.ubw_chant_progress = 0;
                    vars.ubw_chant_timer = 0;
                    vars.syncPlayerVariables(player);
                }
            }
            
            // Update state for next tick
            WAS_CHANTING.put(player.getUUID(), isChanting);

        // Check if player is the owner of the current UBW instance
        boolean isOwner = false;
        if (vars.is_in_ubw) {
            // Check if player is in THEIR OWN UBW
            if (UBW_LOCATIONS.containsKey(player.getUUID())) {
                Vec3 center = UBW_LOCATIONS.get(player.getUUID());
                if (player.level().dimension().location().equals(ModDimensions.UBW_KEY.location()) && player.position().distanceToSqr(center) < 40000) {
                     isOwner = true;
                }
            }
        }

        // Maintenance Cost for UBW (Only for Owner)
        if (isOwner) {
                if (player.tickCount % 20 == 0) {
                     double cost = 10.0;
                     if (vars.player_mana >= cost) {
                         vars.player_mana -= cost;
                         vars.syncMana(player);
                         
                         // Check sword density and refill if needed
                         // Only check every 100 ticks (5 seconds) to save performance
                         if (player.tickCount % 100 == 0) {
                             checkAndRefillSwords(player, vars);
                         }
                     } else {
                         // Not enough mana, force exit
                         player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.mana_depleted"), true);
                         returnFromUBW(player, vars);
                     }
                 }
            }
            
            if (vars.is_chanting_ubw) {
                // ... (chanting logic) ...
                
                // Apply Slowness during chant
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false));
                
                vars.ubw_chant_timer++;
                
                // Terrain Logic
                if (vars.ubw_chant_progress >= 3) {
                     processTerrainEffect(player, vars);
                } else {
                     // Ensure no residual data if restarted before restoration finished (unlikely but safe)
                     // Actually, if we are < 3, we shouldn't have active terrain effects.
                }
                
                // Calculate interval based on proficiency
                // Proficiency 0 -> 40 ticks
                // Proficiency 100 -> 20 ticks (1s, halved time)
                int currentInterval = Math.max(20, BASE_CHANT_INTERVAL - (int)(vars.proficiency_unlimited_blade_works * 0.2));
                
                if (vars.ubw_chant_timer >= currentInterval) {
                    vars.ubw_chant_timer = 0;
                    vars.ubw_chant_progress++;
                    
                    // Increase proficiency slightly on chant step
                    if (vars.proficiency_unlimited_blade_works < 100) {
                        vars.proficiency_unlimited_blade_works = Math.min(100, vars.proficiency_unlimited_blade_works + 0.05);
                        
                        // Check for Layer Write (Sword Barrel Full Open) acquisition
                        // Obtain when UBW proficiency >= 1.0 (1%)
                        if (vars.proficiency_unlimited_blade_works >= 1.0 && !vars.learned_magics.contains("sword_barrel_full_open")) {
                            vars.learned_magics.add("sword_barrel_full_open");
                            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.learned", Component.translatable("magic.typemoonworld.sword_barrel_full_open")), true);
                        }
                    }
                    
                    // Process Chant Steps
                    processChantStep(player, vars);
                }
                
                // Continuous weapon rain from step 3 onwards
                if (vars.ubw_chant_progress >= 3 && vars.ubw_chant_progress <= 9) {
                     // Spawn weapons frequently, not just on step change
                     // Spawn every 8 ticks (0.4s) - Slightly faster than 10
                     if (vars.ubw_chant_timer % 8 == 0) {
                         // Moderate count: Start at 3, increase by 2 per step
                         int count = 3 + (vars.ubw_chant_progress - 3) * 2;
                         double maxRadius = 10.0 + (vars.ubw_chant_progress - 3) * 5.0;
                         spawnVisualSwords(player, vars, count, maxRadius);
                     }
                }
            }
        }
    }
    
    // Helper for random sword generation (Passive)
    // Reduce density/count for "Sword Rain" reduction request
    // This method handles the ambient "refill" of swords.
    private static void checkAndRefillSwords(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        // Only run this logic on the UBW owner/caster to avoid duplicate spawning
        
        if (player.level().dimension().location().equals(ModDimensions.UBW_KEY.location())) {
             // We are in UBW.
             // Iterate all LivingEntities in this dimension (players and mobs)
             // Using getEntities() on the level is more comprehensive
             Iterable<Entity> allEntities = player.getServer().getLevel(ModDimensions.UBW_KEY).getEntities().getAll();
             
            for (Entity entity : allEntities) {
                if (!(entity instanceof LivingEntity target)) continue;
                if (!target.isAlive()) continue;
                // Skip if target is Spectator Player
                if (target instanceof ServerPlayer p && p.isSpectator()) continue;
                
                // Check density around THIS target
                // Range: 30 blocks in UBW
                double checkRadius = 30.0;
                 // AABB checkBox = target.getBoundingBox().inflate(checkRadius);
                 
                 // Check placed blocks density
                 int swordCount = 0;
                 List<BlockPos> placed = PLACED_SWORDS.get(player.getUUID());
                 if (placed != null) {
                     double tX = target.getX();
                     double tZ = target.getZ();
                     for (BlockPos p : placed) {
                         // Fast distance check (Squared)
                         if (p.distSqr(new net.minecraft.core.Vec3i((int)tX, p.getY(), (int)tZ)) < checkRadius * checkRadius) {
                             swordCount++;
                         }
                     }
                 }
                 
                // Check if target is "Powerful"
                 boolean isPowerful = target instanceof ServerPlayer || 
                                      target.getMaxHealth() >= 50.0f || 
                                      target instanceof net.minecraft.world.entity.boss.wither.WitherBoss;

                // Limit: Threshold 60 for Powerful, 20 for others
                int threshold = isPowerful ? 60 : 20;
                 
                if (swordCount < threshold) { 
                    // Spawn rain to refill
                    // Powerful: 80-120 swords. Others: 20-30 swords.
                    int minSpawn = isPowerful ? 80 : 20;
                    int varSpawn = isPowerful ? 40 : 10;
                     int toSpawn = minSpawn + player.getRandom().nextInt(varSpawn);
                     
                    double spawnRadius = 30.0;
                     
                     for (int i = 0; i < toSpawn; i++) {
                        int delay = player.getRandom().nextInt(20); // Fast rain (1s)
                        
                        double r = player.getRandom().nextDouble() * spawnRadius;
                        double theta = player.getRandom().nextDouble() * Math.PI * 2;
                        double x = target.getX() + r * Math.cos(theta);
                        double z = target.getZ() + r * Math.sin(theta);
                        
                        REFILL_QUEUES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>())
                            .add(new RefillEntry(delay, new Vec3(x, target.getY(), z)));
                     }
                 }
            }
        }
    }
    
    private static void spawnPassiveSwords(ServerPlayer owner, LivingEntity target, int count) {
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        ServerLevel level = (ServerLevel) owner.level();
        TypeMoonWorldModVariables.PlayerVariables vars = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        for (int i = 0; i < count; i++) {
             // Spawn falling sword projectile (Rain)
             // Use spawnVisualSwordAt which creates a UBWProjectileEntity
             // UBWProjectileEntity handles block placement on impact
             
             // Random position around target (Radius 15 to match check)
             double r = level.getRandom().nextDouble() * 15.0;
             double theta = level.getRandom().nextDouble() * Math.PI * 2;
             double x = target.getX() + r * Math.cos(theta);
             double z = target.getZ() + r * Math.sin(theta);
             
             spawnVisualSwordAt(owner, vars, new Vec3(x, target.getY(), z));
        }
    }
    
    private static void spawnVisualSwordAt(ServerPlayer player, double centerX, double centerZ, double radius) {
        ServerLevel level = (ServerLevel) player.level();
        RandomSource random = level.getRandom();
        
        double r = random.nextDouble() * radius;
        double theta = random.nextDouble() * Math.PI * 2;
        double x = centerX + r * Math.cos(theta);
        double z = centerZ + r * Math.sin(theta);
        double y = player.getY() + 15.0 + random.nextDouble() * 10.0;
        
        // Create falling sword projectile (visual only or low damage)
        net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity sword = new net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.UBW_PROJECTILE.get(), level);
        sword.setPos(x, y, z);
        sword.setOwner(player);
        sword.setDeltaMovement(0, -0.5 - random.nextDouble() * 0.5, 0); // Fall down
        sword.setXRot(90.0f); // Point down
        
        level.addFreshEntity(sword);
    }

    // New Inner Class for Refill Queue
    private static class RefillEntry {
        int delay;
        final Vec3 exactPos; // Stores the exact target position
        
        RefillEntry(int delay, Vec3 exactPos) {
            this.delay = delay;
            this.exactPos = exactPos;
        }
    }
    
    // New Map for Refill Queue
    private static final Map<UUID, List<RefillEntry>> REFILL_QUEUES = new ConcurrentHashMap<>();
    
    // In onPlayerTick, process this queue
    private static void processRefillQueue(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        UUID uuid = player.getUUID();
        List<RefillEntry> queue = REFILL_QUEUES.get(uuid);
        
        if (queue != null && !queue.isEmpty()) {
            Iterator<RefillEntry> it = queue.iterator();
            while (it.hasNext()) {
                RefillEntry entry = it.next();
                entry.delay--;
                if (entry.delay <= 0) {
                    // Spawn ONE sword at EXACT position
                    spawnVisualSwordAt(player, vars, entry.exactPos);
                    it.remove();
                }
            }
            if (queue.isEmpty()) {
                REFILL_QUEUES.remove(uuid);
            }
        }
    }
    
    // Modified helper to spawn at specific location
    private static void spawnVisualSwordAt(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, Vec3 targetPos) {
        if (player.level().isClientSide) return;
        
        List<ItemStack> weapons = new ArrayList<>();
        for (ItemStack stack : vars.analyzed_items) {
            // Exclude Noble Phantasms from common sword rain
            if (stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem) continue;
            
            if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem) {
                weapons.add(stack);
            }
        }
        
        if (!weapons.isEmpty()) {
            ItemStack weapon = weapons.get(player.getRandom().nextInt(weapons.size())).copy();
            
            double x = targetPos.x;
            double z = targetPos.z;
            
            // Adapt to terrain height
            int surfaceY = player.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)x, (int)z);
            // Spawn above target OR surface, whichever is higher (Ensure rain falls ON target)
            double baseY = Math.max(surfaceY, targetPos.y);
            double y = baseY + 12 + player.getRandom().nextDouble() * 5; 

            // Obstruction check (if spawn point is somehow obstructed, e.g. very high mountain cap or barrier)
            if (!player.level().getBlockState(net.minecraft.core.BlockPos.containing(x, y, z)).isAir()) {
                 // Try to find a safe spot upwards
                 y = baseY + 20; 
            }
            
            UBWProjectileEntity projectile = new UBWProjectileEntity(player.level(), player, weapon);
            projectile.setPos(x, y, z);
            projectile.setDeltaMovement(0, -2.0, 0); 
            projectile.setXRot(-90.0f); 
            player.level().addFreshEntity(projectile);
            
            if (player.level() instanceof ServerLevel serverLevel) {
                 serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z, 5, 0.2, 0.2, 0.2, 0.05);
                 // Reduced volume for mass spawning
                 serverLevel.playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.ILLUSIONER_PREPARE_MIRROR, net.minecraft.sounds.SoundSource.PLAYERS, 0.3f, 1.5f);
            }
        }
    }

    private static void processTerrainEffect(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        if (player.level().isClientSide) return;
        UUID uuid = player.getUUID();
        
        BlockPos center = CHANT_CENTERS.computeIfAbsent(uuid, k -> player.blockPosition());
        Map<BlockPos, BlockBackup> backups = BACKUP_BLOCKS.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        
        // Calculate target radius based on progress and timer
        // Progress 3 -> 9. (7 steps)
        // Max radius 40.
        // We want the radius to reach Max (40) exactly when chant ends (step 9).
        
        double maxRadius = 40.0;
        int maxStep = 9;
        int currentStep = vars.ubw_chant_progress;
        
        // Retrieve current radius
        double currentRadius = TERRAIN_RADIUS.getOrDefault(uuid, 0.0);
        
        // Target for THIS step
        double stepTargetRadius = (double)currentStep / maxStep * maxRadius;
        
        // Smoothly interpolate to step target
        // If currentRadius < stepTargetRadius, expand.
        double dist = stepTargetRadius - currentRadius;
        double speed = 0.0;
        
        if (dist > 0) {
            speed = Math.max(0.1, dist / 20.0); // Reach target in ~1 second
            currentRadius += speed;
        } else {
             currentRadius = stepTargetRadius;
        }

        TERRAIN_RADIUS.put(uuid, currentRadius);
            
        int r = (int)Math.ceil(currentRadius);
        int prevR = (int)Math.floor(currentRadius - speed);
            
        // Only iterate if radius increased to a new integer block
        if (r > prevR) {
            // Iterate logic
            // Floor: center.y - 1
            // Air: center.y to center.y + r
            
            int floorY = center.getY() - 1;
            
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double distSqr = x*x + z*z;
                    if (distSqr <= currentRadius * currentRadius) {
                         BlockPos pos = center.offset(x, 0, z); // Base pos at center Y
                         
                         // 1. Handle Surface (y-1) -> Red Sand
                         BlockPos floorPos = new BlockPos(pos.getX(), floorY, pos.getZ());
                         if (!backups.containsKey(floorPos)) {
                             BlockState oldState = player.level().getBlockState(floorPos);
                             // Don't replace Bedrock or Unbreakable
                             if (oldState.getDestroySpeed(player.level(), floorPos) >= 0) {
                                 // Save Tile Entity
                                 CompoundTag nbt = null;
                                 BlockEntity be = player.level().getBlockEntity(floorPos);
                                 if (be != null) {
                                     nbt = be.saveWithFullMetadata(player.level().registryAccess());
                                 }
                                 
                                 backups.put(floorPos, new BlockBackup(oldState, nbt));
                                 // Set to Red Sand
                                 player.level().setBlock(floorPos, Blocks.RED_SAND.defaultBlockState(), 18);
                             }
                         }

                         // 2. Handle Base (y-2) -> Red Sandstone
                         BlockPos basePos = floorPos.below();
                         if (!backups.containsKey(basePos)) {
                             BlockState oldState = player.level().getBlockState(basePos);
                             if (oldState.getDestroySpeed(player.level(), basePos) >= 0) {
                                 // Save Tile Entity
                                 CompoundTag nbt = null;
                                 BlockEntity be = player.level().getBlockEntity(basePos);
                                 if (be != null) {
                                     nbt = be.saveWithFullMetadata(player.level().registryAccess());
                                 }

                                 backups.put(basePos, new BlockBackup(oldState, nbt));
                                 player.level().setBlock(basePos, Blocks.RED_SANDSTONE.defaultBlockState(), 18);
                             }
                         }
                         
                         // 3. Handle Air above (Hemisphere clearing)
                         int maxY = (int)Math.sqrt(Math.max(0, maxRadius * maxRadius - distSqr)); 
                         
                         for (int yOffset = 0; yOffset <= maxY; yOffset++) {
                             BlockPos airPos = new BlockPos(pos.getX(), center.getY() + yOffset, pos.getZ());
                             if (!backups.containsKey(airPos)) {
                                 BlockState oldState = player.level().getBlockState(airPos);
                                 // Skip UBW Weapon Blocks
                                 if (oldState.getBlock() instanceof net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock) continue;
                                 
                                 if (!oldState.isAir() && oldState.getDestroySpeed(player.level(), airPos) >= 0) {
                                     // Save Tile Entity
                                     CompoundTag nbt = null;
                                     BlockEntity be = player.level().getBlockEntity(airPos);
                                     if (be != null) {
                                         nbt = be.saveWithFullMetadata(player.level().registryAccess());
                                     }

                                     backups.put(airPos, new BlockBackup(oldState, nbt));
                                     player.level().setBlock(airPos, Blocks.AIR.defaultBlockState(), 18);
                                     
                                     // Check for Item Entities at this position and store them
                                    AABB box = new AABB(airPos);
                                    List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, box);
                                    if (!items.isEmpty()) {
                                        List<ItemBackup> itemBackups = BACKUP_ITEMS.computeIfAbsent(uuid, k -> new ArrayList<>());
                                        for (ItemEntity item : items) {
                                            // Don't store items, just discard them (Delete drops from broken containers)
                                            // itemBackups.add(new ItemBackup(item.getItem().copy(), item.position()));
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

    private static void startTerrainRestoration(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Map<BlockPos, BlockBackup> backups = BACKUP_BLOCKS.get(uuid);
        if (backups == null || backups.isEmpty()) {
            CHANT_CENTERS.remove(uuid);
            TERRAIN_RADIUS.remove(uuid);
            return;
        }
        
        BlockPos center = CHANT_CENTERS.get(uuid);
        if (center == null) center = player.blockPosition(); // Fallback
        
        // Create a list of positions sorted by distance from center (Outer -> Inner)
        // Actually, user said "From Outside to Inside".
        // Distance large -> Distance small.
        List<BlockPos> sortedPos = new ArrayList<>(backups.keySet());
        final BlockPos finalCenter = center;
        
        Collections.sort(sortedPos, (p1, p2) -> {
            double d1 = p1.distSqr(finalCenter);
            double d2 = p2.distSqr(finalCenter);
            return Double.compare(d2, d1); // Descending order
        });
        
        RESTORATION_QUEUES.put(uuid, sortedPos);
    }
    
    private static void processRestorationQueue(ServerPlayer player) {
        UUID uuid = player.getUUID();
        List<BlockPos> queue = RESTORATION_QUEUES.get(uuid);
        
        if (queue != null && !queue.isEmpty()) {
            Map<BlockPos, BlockBackup> backups = BACKUP_BLOCKS.get(uuid);
            if (backups == null) {
                RESTORATION_QUEUES.remove(uuid);
                return;
            }
            
            // Restore speed: 50 blocks per tick?
            // If total is ~2000 blocks, it takes 40 ticks (2s). Good.
            int blocksPerTick = 400; 
            
            Iterator<BlockPos> it = queue.iterator();
            int processed = 0;
            
            while (it.hasNext() && processed < blocksPerTick) {
                BlockPos pos = it.next();
                BlockBackup backup = backups.get(pos);
                if (backup != null) {
                    player.level().setBlock(pos, backup.state(), 18);
                    // Restore Tile Entity
                    if (backup.nbt() != null) {
                        BlockEntity be = player.level().getBlockEntity(pos);
                        if (be != null) {
                            be.loadWithComponents(backup.nbt(), player.level().registryAccess());
                        }
                    }
                }
                it.remove();
                // Also remove from backups to keep clean? Not strictly necessary if we clear all at end.
                processed++;
            }
            
            if (queue.isEmpty()) {
                // Restore Items
                List<ItemBackup> items = BACKUP_ITEMS.remove(uuid);
                if (items != null) {
                    for (ItemBackup itemBackup : items) {
                        ItemEntity itemEntity = new ItemEntity(player.level(), itemBackup.position().x, itemBackup.position().y, itemBackup.position().z, itemBackup.stack());
                        itemEntity.setDeltaMovement(0, 0, 0);
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
        Map<BlockPos, BlockBackup> backups = BACKUP_BLOCKS.get(uuid);
        if (backups != null) {
            for (Map.Entry<BlockPos, BlockBackup> entry : backups.entrySet()) {
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
        
        // Restore Items
        List<ItemBackup> items = BACKUP_ITEMS.remove(uuid);
        if (items != null) {
            for (ItemBackup itemBackup : items) {
                ItemEntity itemEntity = new ItemEntity(player.level(), itemBackup.position().x, itemBackup.position().y, itemBackup.position().z, itemBackup.stack());
                itemEntity.setDeltaMovement(0, 0, 0);
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
            List<RemovalEntry> queue = REMOVAL_QUEUES.get(playerId);
            if (queue == null || queue.isEmpty()) {
                REMOVAL_QUEUES.remove(playerId);
                return;
            }
            
            Iterator<RemovalEntry> iterator = queue.iterator();
            while (iterator.hasNext()) {
                RemovalEntry entry = iterator.next();
                if (!entry.entity.isAlive()) {
                    iterator.remove();
                    continue;
                }
                
                entry.delay--;
                if (entry.delay <= 0) {
                    // Spawn particle effect before removing
                    if (entry.entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                         serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, 
                             entry.entity.getX(), entry.entity.getY() + 0.5, entry.entity.getZ(), 
                             3, 0.1, 0.1, 0.1, 0.05);
                    }
                    entry.entity.discard();
                    iterator.remove();
                }
            }
            
            if (queue.isEmpty()) {
                REMOVAL_QUEUES.remove(playerId);
            }
        }
    }
    
    // Helper to spawn swords around player on entry
    private static void spawnEntrySwords(ServerPlayer player) {
        // We need to spawn them in the UBW dimension, but player is currently in Overworld (about to TP).
        // Wait, player is NOT YET teleported in activateUBW.
        // But we want them to appear "immediately upon entry".
        // So we should spawn them in the TARGET dimension (UBW) at the target location.
        // We know the target location: PENDING_UBW_LOCATIONS.get(player.getUUID()) or similar logic.
        // Actually, activateUBW uses PENDING_UBW_LOCATIONS to find where to teleport.
        
        Vec3 targetCenter = PENDING_UBW_LOCATIONS.get(player.getUUID());
        if (targetCenter == null) return;
        
        ServerLevel ubwLevel = player.getServer().getLevel(ModDimensions.UBW_KEY);
        if (ubwLevel == null) return;
        
        RandomSource random = player.getRandom();
        // Reduced count for "Sword Rain" reduction request?
        // User said: "鍑忓皬鎵€鏈夌殑鏃犻檺鍓戝埗鍐呯殑鍓戦洦锛堜笉鍖呮嫭灞傚啓锛?
        // And "鏃犻檺鍓戝埗杩涘叆绾害鏃剁帺瀹跺懆鍥寸殑鍦颁笂鐩存帴鐢熸垚鏂瑰潡鍓?
        // So this is a NEW feature, not the rain itself.
        // Let's spawn a reasonable amount, e.g. 30.
        int count = 30;
        double radius = 15.0;
        
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double dist = random.nextDouble() * radius;
            double x = targetCenter.x + Math.cos(angle) * dist;
            double z = targetCenter.z + Math.sin(angle) * dist;
            
            // Y is usually 100 flat in UBW
            BlockPos pos = new BlockPos((int)x, 100, (int)z);
            
            // Place Sword Block
            // Check if air
            if (ubwLevel.getBlockState(pos).isAir()) {
                 BlockState state = net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks.SWORD_BARREL_BLOCK.get().defaultBlockState()
                        .setValue(net.xxxjk.TYPE_MOON_WORLD.block.custom.SwordBarrelBlock.FACING, net.minecraft.core.Direction.UP) // Facing UP
                        .setValue(net.xxxjk.TYPE_MOON_WORLD.block.custom.SwordBarrelBlock.ROTATION_A, random.nextBoolean())
                        .setValue(net.xxxjk.TYPE_MOON_WORLD.block.custom.SwordBarrelBlock.ROTATION_B, random.nextBoolean());
                 
                 ubwLevel.setBlock(pos, state, 3);
                 
                 // Register
                 registerPlacedSword(player.getUUID(), pos);
            }
        }
    }

    private static void processChantStep(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        int progress = vars.ubw_chant_progress;
        double cost = 50.0;
        String chantText = "";
        
        // Line 1: 50 mana (handled in MagicUnlimitedBladeWorks)
        // Lines 2-9: 50 mana each
        // Total steps: 1 (start), 2, 3, 4, 5, 6, 7, 8, 9 (final chant), 10 (activation)
        
        if (progress == 1) {
             // Pre-calculate UBW location and load chunk
             ServerLevel ubwLevel = player.getServer().getLevel(ModDimensions.UBW_KEY);
             if (ubwLevel != null) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 2000000;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 2000000;
                offsetX = Math.round(offsetX / 16) * 16 + 0.5;
                offsetZ = Math.round(offsetZ / 16) * 16 + 0.5;
                
                BlockPos targetPos = new BlockPos((int)offsetX, 100, (int)offsetZ);
                ChunkPos chunkPos = new ChunkPos(targetPos);
                ubwLevel.getChunkSource().addRegionTicket(TicketType.PLAYER, chunkPos, 3, chunkPos);
                
                PENDING_UBW_LOCATIONS.put(player.getUUID(), new Vec3(offsetX, 0, offsetZ));
             }
        } else if (progress == 2) {
            chantText = "搂bSteel is my body, and fire is my blood.";
        } else if (progress == 3) {
            chantText = "搂bI have created over a thousand blades.";
            // Initial burst spawn - slightly increased from 8
            spawnVisualSwords(player, vars, 10, 10.0);
        } else if (progress == 4) {
            chantText = "搂bUnaware of loss.";
        } else if (progress == 5) {
            chantText = "搂bNor aware of gain.";
        } else if (progress == 6) {
            chantText = "搂bWithstood pain to create weapons,waiting for one's arrival.";
        } else if (progress == 7) {
            chantText = "搂bI have no regrets.";
        } else if (progress == 8) {
            chantText = "搂bThis is the only path.";
        } else if (progress == 9) {
            chantText = "搂bMy whole life was,";
        } else if (progress > 9) {
            // Activation Phase
            if (ManaHelper.consumeManaOrHealth(player, cost)) {
                activateUBW(player, vars);
            } else {
                interruptChant(player, vars, "message.typemoonworld.unlimited_blade_works.mana_depleted");
            }
            return;
        }
        
        // Consume Cost
        if (ManaHelper.consumeManaOrHealth(player, cost)) {
            player.displayClientMessage(Component.literal(chantText), true);
            vars.syncPlayerVariables(player);
        } else {
            // Not enough mana, interrupt
            interruptChant(player, vars, "message.typemoonworld.unlimited_blade_works.mana_depleted");
        }
    }
    
    private static void spawnVisualSwords(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int count, double maxRadius) {
        // Wrapper for compatibility with old calls in Chant
        for (int i = 0; i < count; i++) {
             // Chant visuals use ring/random distribution
             // Bias towards outer edge for chant visual
             double rNormalized = Math.sqrt(player.getRandom().nextDouble());
             if (rNormalized < 0.2) rNormalized = 0.2 + player.getRandom().nextDouble() * 0.8;
             double radius = rNormalized * maxRadius;
             double angle = player.getRandom().nextDouble() * Math.PI * 2;
             
             double x = player.getX() + radius * Math.cos(angle);
             double z = player.getZ() + radius * Math.sin(angle);
             
             // Prevent spawning directly above player
             if (Math.abs(x - player.getX()) < 1.0 && Math.abs(z - player.getZ()) < 1.0) continue;
             
             spawnVisualSwordAt(player, vars, new Vec3(x, player.getY(), z));
        }
    }
    
    private static void clearVisualSwords(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        
        UUID uuid = player.getUUID();
        
        // 0. Clear Placed Blocks
        List<BlockPos> placed = PLACED_SWORDS.remove(uuid);
        if (placed != null) {
            for (BlockPos pos : placed) {
                if (player.level().getBlockState(pos).getBlock() instanceof net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock) {
                    player.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    if (player.level() instanceof ServerLevel serverLevel) {
                         serverLevel.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }
        }
        
        double terrainRadius = TERRAIN_RADIUS.getOrDefault(uuid, 0.0);
        // Expanded clear range to ensure no leftovers (chunk size)
        int scanR = (int)Math.max(terrainRadius, 80);
        
        AABB scanBox = new AABB(center).inflate(scanR, 64, scanR);
        
        // 1. Clear Projected Swords (Ground) - Handled by PLACED_SWORDS
        // No entity clearing needed for blocks
        
        // 2. Clear UBW Projectiles (Falling)
        List<UBWProjectileEntity> projectiles = player.level().getEntitiesOfClass(UBWProjectileEntity.class, scanBox);
        for (UBWProjectileEntity projectile : projectiles) {
            projectile.discard();
            if (player.level() instanceof ServerLevel serverLevel) {
                 serverLevel.sendParticles(ParticleTypes.POOF, projectile.getX(), projectile.getY(), projectile.getZ(), 2, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }
    
    // Reset dimension logic
    // We can't delete dimension at runtime easily in vanilla/forge without mixins or complex hacks.
    // However, we can "reset" it by clearing blocks around the spawn or simply not deleting it but ensuring it's clean.
    // User requested "Leave -> Delete, Enter -> New".
    // Deleting a dimension folder while server is running is dangerous and likely to fail or cause corruption.
    // Alternative: 
    // 1. Teleport player to a FAR AWAY location in the same dimension each time (e.g. x += 10000).
    // 2. Clear the area around the new location.
    // This simulates a "new" dimension.
    
    // Let's implement the "Offset Strategy".
    // We store a global offset for the UBW dimension.
    // Or better, store it in WorldData or just use a random far coordinate.
    
    // Helper to find safe spawn Y (Feet position)
    private static int findSafeSpawnY(ServerLevel level, int x, int z) {
        // We can't trust simple heightmaps in unknown terrain generators.
        // The safest way is to scan from the SKY downwards.
        // Standard max height is 320.
        
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 320, z);
        
        // 1. Scan down to find the FIRST solid block (Terrain Surface)
        int surfaceY = -999;
        for (int y = 320; y > -64; y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP)) {
                surfaceY = y;
                break;
            }
        }
        
        // If we fell into void, default to 64
        if (surfaceY == -999) return 64;
        
        // 2. The safe spawn is on top of that surface
        return surfaceY + 1;
    }

    private static void activateUBW(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        vars.is_chanting_ubw = false;
        vars.ubw_chant_progress = 0;
        vars.ubw_chant_timer = 0;
        
        vars.is_in_ubw = true;
        
        // Save current location
        vars.ubw_return_x = player.getX();
        vars.ubw_return_y = player.getY();
        vars.ubw_return_z = player.getZ();
        vars.ubw_return_dimension = player.level().dimension().location().toString();
        
        vars.syncPlayerVariables(player);
        
        player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.activated"), true);
        
        // Restore Terrain Instantly BEFORE Teleport
        restoreTerrainInstantly(player);
        
        // Clear visual effects immediately
        clearVisualSwords(player);
        
        // Reset Active Entity List
        ACTIVE_UBW_ENTITIES.remove(player.getUUID());
        ACTIVE_ENTITY_POSITIONS.remove(player.getUUID());
        
        // Spawn Sword Blocks around the player immediately upon entry
        // Radius: 20 blocks
        // Count: ~50 swords?
        spawnEntrySwords(player);
        
        // Find entities to teleport (Range 40)
        double range = 40.0;
        AABB tpBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, tpBox, e -> e != player && e.isAlive() && !(e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon));
        List<EntityReturnData> teleported = new ArrayList<>();
        // Pre-register the list to the map to ensure onEntityJoinLevel can see these UUIDs
        // and differentiate them from new spawns.
        TELEPORTED_ENTITIES.put(player.getUUID(), teleported);
        
        // List of entities in the new dimension (Player + Teleported) for sword rain
        List<Entity> swordRainTargets = new ArrayList<>();
        
        // Teleport to Reality Marble dimension
            ServerLevel ubwLevel = player.getServer().getLevel(ModDimensions.UBW_KEY);
            if (ubwLevel != null) {
                // "New" Dimension Strategy:
                // Teleport to a random far location to simulate a fresh world.
                // Use pre-calculated location if available (from chant step 1)
                
                double offsetX;
                double offsetZ;
                
                if (PENDING_UBW_LOCATIONS.containsKey(player.getUUID())) {
                    Vec3 pending = PENDING_UBW_LOCATIONS.remove(player.getUUID());
                    offsetX = pending.x;
                    offsetZ = pending.z;
                } else {
                    // Fallback if not pre-calculated
                    offsetX = (player.getRandom().nextDouble() - 0.5) * 2000000;
                    offsetZ = (player.getRandom().nextDouble() - 0.5) * 2000000;
                    
                    // Align to chunk center to be nice
                    offsetX = Math.round(offsetX / 16) * 16 + 0.5;
                    offsetZ = Math.round(offsetZ / 16) * 16 + 0.5;
                }
                
                // Get safe surface height using new logic
                int safeY = findSafeSpawnY(ubwLevel, (int)offsetX, (int)offsetZ);
                double targetY = safeY; 
                
                // Teleport Player
                player.teleportTo(ubwLevel, offsetX, targetY, offsetZ, player.getYRot(), player.getXRot());
                
                // Remove pre-load ticket now that player is there
                BlockPos targetPos = new BlockPos((int)offsetX, (int)targetY, (int)offsetZ);
                ChunkPos chunkPos = new ChunkPos(targetPos);
                ubwLevel.getChunkSource().removeRegionTicket(TicketType.PLAYER, chunkPos, 3, chunkPos);
                
                UBW_LOCATIONS.put(player.getUUID(), new Vec3(offsetX, targetY, offsetZ));
                // Only add player to initial fill targets to avoid lag
                swordRainTargets.add(player);
                
                // Teleport Entities
                for (LivingEntity target : targets) {
                    // Store original position
                    Vec3 originalPos = target.position();

                    // TAGGING ENTITY
                    TypeMoonWorldModVariables.UBWReturnData data = target.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
                    data.ownerUUID = player.getUUID();
                    data.returnX = originalPos.x;
                    data.returnY = originalPos.y;
                    data.returnZ = originalPos.z;
                    data.returnDim = player.level().dimension().location().toString();
                    
                    // Calculate relative position
                    double relX = target.getX() - vars.ubw_return_x;
                    double relZ = target.getZ() - vars.ubw_return_z;
                    
                    double entityTargetX = offsetX + relX;
                    double entityTargetZ = offsetZ + relZ;
                    
                    // Find surface for entity using new logic
                    int entitySafeY = findSafeSpawnY(ubwLevel, (int)entityTargetX, (int)entityTargetZ);
                    double entityTargetY = entitySafeY;
                    
                    // Add to list BEFORE teleporting (Keep legacy list for now just in case, but rely on tags)
                    EntityReturnData dataObj = new EntityReturnData(target.getUUID(), originalPos);
                    teleported.add(dataObj);
                    
                    // Teleport to new pos
                    Entity newEntity = target.changeDimension(new DimensionTransition(ubwLevel, new Vec3(entityTargetX, entityTargetY, entityTargetZ), Vec3.ZERO, target.getYRot(), target.getXRot(), DimensionTransition.DO_NOTHING));
                    if (newEntity != null) {
                        // Track Active Entity in UBW
                        ACTIVE_UBW_ENTITIES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(newEntity.getUUID());
                        ACTIVE_ENTITY_POSITIONS.put(newEntity.getUUID(), newEntity.position());

                        // Explicitly copy NBT tags to new entity to ensure persistence
                        TypeMoonWorldModVariables.UBWReturnData newData = newEntity.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
                        newData.ownerUUID = player.getUUID();
                        newData.returnX = originalPos.x;
                        newData.returnY = originalPos.y;
                        newData.returnZ = originalPos.z;
                        newData.returnDim = player.level().dimension().location().toString();
                        
                        // Check if newEntity is a player and is chanting
                        if (newEntity instanceof ServerPlayer targetPlayer) {
                             TypeMoonWorldModVariables.PlayerVariables targetVars = targetPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                             if (targetVars.is_chanting_ubw) {
                                 interruptChant(targetPlayer, targetVars, "message.typemoonworld.unlimited_blade_works.interrupted");
                             }
                        }
                    }
                }
                
                // Trigger Initial Sword Burst (Fill the chunk around player only)
                initialUBWFill(player, vars, swordRainTargets);
            }
        
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
    }
    
    private static void initialUBWFill(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, List<Entity> targets) {
        if (player.level().isClientSide) return;
        
        // If targets not provided (old call), find nearby
        if (targets == null || targets.isEmpty()) {
             targets = new ArrayList<>();
             targets.add(player);
             double scanRange = 64.0;
             AABB scanBox = player.getBoundingBox().inflate(scanRange);
             List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, scanBox);
             targets.addAll(nearby);
        }
        
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity living)) continue;
            
            // Very High density spawn for initial fill
            // Spawn ~500 swords per entity area to cover the screen
            int swordCount = 400 + player.getRandom().nextInt(200);
            double spawnRadius = 64.0; // Fill a larger area (radius 64 = diameter 128 = 8 chunks)
            
            // To avoid massive lag spike, we can use the Queue system too?
            // Or just spawn them directly but carefully.
            // Let's spawn 200 directly and queue the rest?
            // For now, let's just queue ALL of them but with very short delay (0-40 ticks = 2s)
            // This creates a cool "raining down" effect over 2 seconds instead of instant freeze.
            
            for (int i = 0; i < swordCount; i++) {
                // Delay 0 to 40 ticks
                int delay = player.getRandom().nextInt(40);
                
                // Calculate position here for Center-Dense distribution
                // Square the random factor to bias towards 0 (center)
                double r = player.getRandom().nextDouble() * player.getRandom().nextDouble() * spawnRadius;
                double angle = player.getRandom().nextDouble() * Math.PI * 2;
                
                double x = target.getX() + r * Math.cos(angle);
                double z = target.getZ() + r * Math.sin(angle);
                
                REFILL_QUEUES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>())
                    .add(new RefillEntry(delay, new Vec3(x, target.getY(), z)));
            }
        }
    }
    
    private static void interruptChant(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, String reasonKey) {
        vars.is_chanting_ubw = false;
        vars.ubw_chant_progress = 0;
        vars.ubw_chant_timer = 0;
        vars.syncPlayerVariables(player);
        
        // Clear visual effects
        clearVisualSwords(player);
        
        // Start terrain restoration
        startTerrainRestoration(player);
        
        player.displayClientMessage(Component.translatable(reasonKey), true);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        // If player dies in UBW, return everyone
        if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.is_in_ubw) {
                returnFromUBW(player, vars);
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
             TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
             
             // Check if leaving UBW
             if (event.getFrom().location().equals(ModDimensions.UBW_KEY.location())) {
                 // If is_in_ubw is true, it means the player left WITHOUT going through returnFromUBW
                 // (e.g. /tp command, portal, or other mod teleportation)
                 // We must manually bring the entities back to the new dimension.
                 
                 if (vars.is_in_ubw) {
                     ServerLevel sourceLevel = player.getServer().getLevel(ModDimensions.UBW_KEY);
                     ServerLevel targetLevel = (ServerLevel) player.level();
                     
                     if (sourceLevel != null) {
                         // Use UBW Center location for scanning if available, otherwise player's last known pos (which is current pos before TP? No, player is already in new dim)
                         // We use UBW_LOCATIONS
                         Vec3 center = UBW_LOCATIONS.get(player.getUUID());
                         if (center == null) center = new Vec3(player.getX(), player.getY(), player.getZ()); // Fallback (might be wrong coords if TP'd)
                         
                         clearVisualSwords(player); // This might fail to find blocks if player is far, but entities are tracked
                         returnEntitiesOnly(player, vars, sourceLevel, targetLevel, center);
                     }
                     
                     vars.is_in_ubw = false;
                     vars.syncPlayerVariables(player);
                     UBW_LOCATIONS.remove(player.getUUID());
                 }
             }
        }
    }

    // Helper to return everyone from UBW
    public static void returnFromUBW(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        // Determine Return Level
        ServerLevel returnLevel = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(vars.ubw_return_dimension)));
        if (returnLevel == null) returnLevel = player.getServer().overworld();
        
        final ServerLevel finalReturnLevel = returnLevel;
        
        // Entities are currently in player.level() (UBW)
        if (player.level() instanceof ServerLevel sourceLevel) {
            // Check if player is actually in UBW dimension before scanning
            if (sourceLevel.dimension().location().equals(ModDimensions.UBW_KEY.location())) {
                 // Clear visual swords before leaving
                 clearVisualSwords(player);
                 
                 // Use Player's CURRENT position as center for relative teleportation
                 // This ensures entities stay in the same relative position to the player
                 Vec3 center = player.position();
                 
                 returnEntitiesOnly(player, vars, sourceLevel, finalReturnLevel, center);
            }
        }
        
        // Update state BEFORE teleporting to prevent onPlayerChangedDimension from triggering cleanup again
        vars.is_in_ubw = false;
        vars.syncPlayerVariables(player);
        UBW_LOCATIONS.remove(player.getUUID());
        ACTIVE_UBW_ENTITIES.remove(player.getUUID());
        ACTIVE_ENTITY_POSITIONS.remove(player.getUUID());
        
        // Return Player
        player.teleportTo(finalReturnLevel, vars.ubw_return_x, vars.ubw_return_y, vars.ubw_return_z, player.getYRot(), player.getXRot());
    }
    
    private static void returnEntitiesOnly(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ServerLevel sourceLevel, ServerLevel targetLevel, Vec3 centerPos) {
        UUID playerUUID = player.getUUID();
        
        // Strategy Change: Combine Memory List + Scan
        // This ensures we catch entities that ran far away or are in unloaded chunks (if kept in memory).
        
        // Use Set to avoid duplicates and LinkedHashSet to preserve order
        java.util.Set<LivingEntity> toReturn = new java.util.LinkedHashSet<>();
        
        // 1. Check Memory List (ACTIVE_UBW_ENTITIES)
        List<UUID> activeUUIDs = ACTIVE_UBW_ENTITIES.get(playerUUID);
        int memoryCount = 0;
        if (activeUUIDs != null) {
            for (UUID uuid : activeUUIDs) {
                Entity e = sourceLevel.getEntity(uuid);
                if (e instanceof LivingEntity living && e.isAlive()) {
                    toReturn.add(living);
                    memoryCount++;
                } else {
                     // Debug: Entity missing or unloaded
                }
            }
        }

        // 2. Fallback Scan (for legacy or untracked entities)
        Iterable<Entity> allEntities = sourceLevel.getEntities().getAll();
        int scanCount = 0;
        
        for (Entity entity : allEntities) {
            if (entity instanceof LivingEntity livingEntity && 
                entity != player && 
                entity.isAlive() && 
                !(entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)) {
                
                // Set automatically handles duplicates, but check for efficiency if needed
                if (toReturn.contains(livingEntity)) continue;

                TypeMoonWorldModVariables.UBWReturnData data = livingEntity.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
                if (data.ownerUUID != null && data.ownerUUID.equals(playerUUID)) {
                    toReturn.add(livingEntity);
                    scanCount++;
                }
            }
        }
        
        for (LivingEntity entity : toReturn) {
            TypeMoonWorldModVariables.UBWReturnData data = entity.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
            
            // Unified Relative Positioning Strategy
            // Regardless of whether the entity was generated or captured, we now move it relative to the player.
            // This prevents issues where the player moves far from the entry point and entities get left behind or misplaced.
            
            double relX = entity.getX() - centerPos.x;
            double relY = entity.getY() - centerPos.y;
            double relZ = entity.getZ() - centerPos.z;
            
            double targetX = vars.ubw_return_x + relX;
            double targetY = vars.ubw_return_y + relY;
            double targetZ = vars.ubw_return_z + relZ;
            
            // Safety Height Check
            BlockPos targetBlockPos = new BlockPos((int)targetX, (int)targetY, (int)targetZ);
            BlockState state = targetLevel.getBlockState(targetBlockPos);
            
            // 1. Check Suffocation
            if (state.isSuffocating(targetLevel, targetBlockPos)) {
                    boolean foundSafe = false;
                    // Try upwards first
                    for (int i = 1; i <= 5; i++) {
                        if (!targetLevel.getBlockState(targetBlockPos.above(i)).isSuffocating(targetLevel, targetBlockPos.above(i))) {
                            targetY += i;
                            foundSafe = true;
                            break;
                        }
                    }
                    // If stuck in wall, force to surface
                    if (!foundSafe) {
                        targetY = targetLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int)targetX, (int)targetZ);
                    }
            } 
            
            targetY += 0.1;
            
            // Clear Tags
            data.ownerUUID = null;
            data.returnDim = "";
            data.returnX = 0;
            data.returnY = 0;
            data.returnZ = 0;
            data.generated = false;
            
            // Teleport
            entity.setPortalCooldown(0);
            if (entity.isPassenger()) entity.stopRiding();
            
            Entity newEntity = entity.changeDimension(new DimensionTransition(targetLevel, new Vec3(targetX, targetY, targetZ), Vec3.ZERO, entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
        }
        
        // Clear legacy lists just in case
        TELEPORTED_ENTITIES.remove(playerUUID);
        GENERATED_ENTITIES.remove(playerUUID);
    }
    
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;
        if (!level.dimension().location().equals(ModDimensions.UBW_KEY.location())) return;
        
        // Run check frequently (every 20 ticks = 1 second) to ensure cleanup is prompt
        if (level.getGameTime() % 20 != 0) return;
        
        if (level instanceof ServerLevel serverLevel) {
            // Update Active Entity Positions
            for (Map.Entry<UUID, List<UUID>> entry : ACTIVE_UBW_ENTITIES.entrySet()) {
                List<UUID> entityUUIDs = entry.getValue();
                for (UUID entityUUID : entityUUIDs) {
                    Entity entity = serverLevel.getEntity(entityUUID);
                    if (entity != null && entity.isAlive()) {
                        ACTIVE_ENTITY_POSITIONS.put(entityUUID, entity.position());
                    }
                }
            }

            Iterable<Entity> allEntities = serverLevel.getEntities().getAll();
            
            // Collect entities that need to be returned (Orphaned entities)
            List<LivingEntity> orphans = new ArrayList<>();
            
            for (Entity entity : allEntities) {
                if (entity instanceof LivingEntity living && !(entity instanceof ServerPlayer)) {
                    TypeMoonWorldModVariables.UBWReturnData data = living.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
                    if (data.ownerUUID != null) {
                        UUID ownerUUID = data.ownerUUID;
                        
                        // Check if owner is still active in UBW
                        // UBW_LOCATIONS contains active UBW owners
                        if (!UBW_LOCATIONS.containsKey(ownerUUID)) {
                            orphans.add(living);
                        }
                    }
                }
            }
            
            // Process Orphans
            for (LivingEntity orphan : orphans) {
                TypeMoonWorldModVariables.UBWReturnData data = orphan.getData(TypeMoonWorldModVariables.UBW_RETURN_DATA);
                
                // Retrieve Return Information
                String dimStr = data.returnDim;
                double retX = data.returnX;
                double retY = data.returnY;
                double retZ = data.returnZ;
                
                ServerLevel targetLevel = serverLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimStr)));
                if (targetLevel == null) targetLevel = serverLevel.getServer().overworld();
                
                // Safety Height Check (Absolute Position)
                BlockPos targetBlockPos = new BlockPos((int)retX, (int)retY, (int)retZ);
                BlockState state = targetLevel.getBlockState(targetBlockPos);
                
                if (state.isSuffocating(targetLevel, targetBlockPos)) {
                     retY = targetLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int)retX, (int)retZ) + 0.1;
                }
                
                // Clear Tags
                data.ownerUUID = null;
                data.returnDim = "";
                data.returnX = 0;
                data.returnY = 0;
                data.returnZ = 0;
                data.generated = false;
                
                // Teleport
                orphan.setPortalCooldown(0);
                if (orphan.isPassenger()) orphan.stopRiding();
                
                Entity newEntity = orphan.changeDimension(new DimensionTransition(targetLevel, new Vec3(retX, retY, retZ), Vec3.ZERO, orphan.getYRot(), orphan.getXRot(), DimensionTransition.DO_NOTHING));
            }
        }
    }
    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Sovereignty Protection Logic
            if (vars.is_in_ubw) {
                 if (UBW_LOCATIONS.containsKey(player.getUUID())) {
                     Vec3 center = UBW_LOCATIONS.get(player.getUUID());
                     if (player.level().dimension().location().equals(ModDimensions.UBW_KEY.location()) && player.position().distanceToSqr(center) < 40000) {
                          // Player is the owner of this UBW instance
                          // Check if damage source is from UBW sword
                          if (event.getSource().getDirectEntity() instanceof UBWProjectileEntity || event.getSource().getDirectEntity() instanceof net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity) {
                              event.setCanceled(true);
                              return;
                          }
                          // Also check if damage is MAGIC or generic damage from own spells if needed
                          // For now, only blocking swords as requested "Sovereignty... can not be hurt"
                          // If user meant invincibility, we would cancel all.
                          // Usually in Fate, Shirou is not invincible in UBW, but he controls the swords.
                          // So blocking UBWProjectileEntity is correct.
                     }
                 }
            }

            if (vars.is_chanting_ubw) {
                float damage = event.getAmount();
                // Threshold for interruption: 4.0 damage (2 hearts)
                if (damage >= 4.0f) {
                    interruptChant(player, vars, "message.typemoonworld.unlimited_blade_works.interrupted");
                }
            }
        }
    }
}
