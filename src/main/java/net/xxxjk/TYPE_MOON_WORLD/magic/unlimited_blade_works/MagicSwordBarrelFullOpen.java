package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import net.minecraft.util.RandomSource;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;

import net.minecraft.core.BlockPos;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.SwordBarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

public class MagicSwordBarrelFullOpen {
    
    // Cooldown in ticks (e.g. 10 ticks = 0.5s)
    private static final int COOLDOWN = 10;
    // Mana cost per activation (continuous drain)
    private static final double MANA_COST = 30.0;
    
    // Clear Mode State
    // We can use a custom tag on player or just iterate per tick if simple
    // For "from furthest to inner", we need to scan once, sort, and remove over time.
    // Or just scan a growing/shrinking radius.
    // Shrinking radius is easiest: Scan max radius, if dist > currentR, remove.
    // But "furthest to inner" means we start at Max R and decrease R.
    // Let's use a temporary variable in PlayerVariables if needed, or just simple scan-and-remove all instantly?
    // User said "Effect reference interrupt UBW... from furthest to inner disappear".
    // This implies an animated process.
    
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        // Check if UBW is unlocked (Prerequisite)
        if (!vars.has_unlimited_blade_works) return;
        
        // Additional Unlock Condition: UBW Proficiency >= 1.0%
        if (vars.proficiency_unlimited_blade_works < 1.0) {
            // Optional: Show message "Not proficient enough"
            // player.displayClientMessage(Component.translatable("message.typemoonworld.overedge_requirement_not_met"), true);
            return;
        }
        
        int mode = vars.sword_barrel_mode;
        
        if (mode == 4) {
            // Mode 4: Clear Swords
            if (!vars.is_sword_barrel_active) {
                vars.is_sword_barrel_active = true;
                vars.syncPlayerVariables(player);
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_TRACE_OFF), true);
                player.getPersistentData().putDouble("SwordBarrelClearRadius", 40.0);
            } else {
                vars.is_sword_barrel_active = false;
                vars.syncPlayerVariables(player);
                player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.cancelled"), true);
            }
            return;
        }

        if (mode == 3) {
            // Mode 3: Toggle Broken Phantasm Mode (Explosion on/off)
            boolean isBP = player.getPersistentData().getBoolean("UBWBrokenPhantasmEnabled");
            isBP = !isBP;
            player.getPersistentData().putBoolean("UBWBrokenPhantasmEnabled", isBP);
            vars.ubw_broken_phantasm_enabled = isBP;
            vars.syncPlayerVariables(player);
            
            if (isBP) {
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_UBW_BROKEN_PHANTASM_ON), true);
            } else {
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_UBW_BROKEN_PHANTASM_OFF), true);
            }
            // Do not change is_sword_barrel_active state
            return;
        }

        // Toggle State for other modes (0, 1, 2)
        vars.is_sword_barrel_active = !vars.is_sword_barrel_active;
        vars.syncPlayerVariables(player);
        
        if (vars.is_sword_barrel_active) {
            if (mode != 4) { // Should not be 4 if we are here
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_TRACE_ON), true);
            }
        } else {
            if (mode != 2) {
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_TRACE_OFF), true);
            }
        }
    }
    
    // Called from PlayerTick
    public static void tick(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        if (!vars.is_sword_barrel_active) return;
        
        // Interrupt Conditions:
        // 1. Magic Circuit Closed
        // 2. Magic Selection Changed (Must be sword_barrel_full_open)
        // 3. UBW Lost
        
        boolean interrupt = false;
        
        if (!vars.is_magic_circuit_open) {
            interrupt = true;
        } else if (!vars.has_unlimited_blade_works) {
            interrupt = true;
        } else {
            // Check selected magic
            if (vars.selected_magics.isEmpty() || vars.current_magic_index < 0 || vars.current_magic_index >= vars.selected_magics.size()) {
                interrupt = true;
            } else {
                String currentMagic = vars.selected_magics.get(vars.current_magic_index);
                if (!"sword_barrel_full_open".equals(currentMagic)) {
                    interrupt = true;
                }
            }
        }
        
        if (interrupt) {
            vars.is_sword_barrel_active = false;
            vars.syncPlayerVariables(player);
            // player.displayClientMessage(Component.translatable("message.typemoonworld.trace_off"), true);
            return;
        }
        
        int mode = vars.sword_barrel_mode;
        
        if (mode == 4) {
            executeMode4(player, vars);
            return;
        }
        
        if (mode == 3) {
            // Mode 3 is just a toggle, shouldn't be "active" for tick logic
            // But if somehow active, disable it
            vars.is_sword_barrel_active = false;
            vars.syncPlayerVariables(player);
            return;
        }
        
        // Fire rate control (e.g. every 5 ticks = 0.25s)
        // Check "No Cooldown" mode
        boolean noCooldown = player.getPersistentData().getBoolean("TypeMoonNoCooldown");
        if (!noCooldown && player.tickCount % 5 != 0) return;

        // Cost Check (Per shot)
        double cost = MANA_COST;
        if (player.level().dimension() == ModDimensions.UBW_KEY) {
            cost = MANA_COST / 2.0;
        }
        // Broken Phantasm (Explosion Mode) has higher cost
        if (player.getPersistentData().getBoolean("UBWBrokenPhantasmEnabled")) {
            cost *= 2.0;
        }

        // Bypass mana cost if No Cooldown (usually implies unlimited mana too for testing?)
        // User said "No cooldown refers to magic usage no cooldown".
        // Often implies free cost or just speed.
        // Let's assume it only affects speed/cooldown, but if user spamming, they might need mana.
        // But strict interpretation: Cooldown only.
        // However, standard "No Cooldown" cheats usually imply free usage.
        // Let's stick to cooldown first. If user wants unlimited mana, they can use set max mana + high regen.
        
        if (!ManaHelper.consumeManaOrHealth(player, cost)) {
             // If No Cooldown is active, maybe we allow it even without mana?
             // "无冷却指令" -> "No Cooldown Command".
             // Let's strictly follow "No Cooldown".
             // Mana is separate.
             
            player.displayClientMessage(Component.translatable(MagicConstants.MSG_NOT_ENOUGH_MANA), true);
            vars.is_sword_barrel_active = false; // Auto-disable if out of mana
            vars.syncPlayerVariables(player);
            return;
        }

        if (mode == 0) {
            executeMode0(player, vars);
        } else if (mode == 1) {
            executeMode1(player, vars);
        } else if (mode == 2) {
            executeMode2(player, vars);
        } else {
            // WIP
        }
    }
    
    private static void executeMode4(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        // Mode 4: Clear Swords (World Scan Ring Animation)
        // No list dependency. Scans the world shell between currentR and nextR.
        // Radius: 40.0 -> 0.0
        
        double currentR = player.getPersistentData().getDouble("SwordBarrelClearRadius");
        
        if (currentR <= 0) {
            // Done
            vars.is_sword_barrel_active = false;
            vars.syncPlayerVariables(player);
            return;
        }
        
        // Shrink radius
        double speed = 1.0; // Slower speed for 40 blocks (40 ticks = 2s)
        double nextR = currentR - speed;
        if (nextR < 0) nextR = 0;
        
        BlockPos center = player.blockPosition();
        Level level = player.level();
        
        // Optimization: Iterate only the bounding box of the shell
        // Integer bounds
        int minX = (int)Math.floor(-currentR);
        int maxX = (int)Math.ceil(currentR);
        int minY = (int)Math.floor(-currentR);
        int maxY = (int)Math.ceil(currentR);
        int minZ = (int)Math.floor(-currentR);
        int maxZ = (int)Math.ceil(currentR);
        
        double currentRSqr = currentR * currentR;
        double nextRSqr = nextR * nextR;
        
        // Scan the shell
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double distSqr = x*x + y*y + z*z;
                    
                    // Check if inside the shell (nextR < dist <= currentR)
                    if (distSqr <= currentRSqr && distSqr > nextRSqr) {
                        BlockPos pos = center.offset(x, y, z);
                        
                        // Check if block is SwordBarrelBlock
                        // Use getBlockState only if necessary (inside shell)
                        if (level.getBlockState(pos).getBlock() instanceof SwordBarrelBlock) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            if (level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.05);
                            }
                        }
                    }
                }
            }
        }
        
        player.getPersistentData().putDouble("SwordBarrelClearRadius", nextR);
    }

    private static void executeMode2(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        // Mode 2: Focus Fire (25 swords, horizontal spread, single trigger)
        
        // 1. Get Weapons
        List<ItemStack> weapons = new ArrayList<>();
        for (ItemStack stack : vars.analyzed_items) {
            if (stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem) continue;
            if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem) {
                weapons.add(stack);
            }
        }
        
        if (weapons.isEmpty()) {
            vars.is_sword_barrel_active = false; // Turn off immediately if no weapons
            vars.syncPlayerVariables(player);
            return;
        }

        // 2. Find Target (RayTrace)
        // Max distance: 40 blocks
        double range = 40.0;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));
        
        Level level = player.level();
        Entity targetEntity = null;
        Vec3 targetPos = endPos;
        
        // Raytrace for entity
        AABB rayBox = new AABB(eyePos, endPos).inflate(2.0);
        List<Entity> entities = level.getEntities(player, rayBox, e -> e instanceof LivingEntity && !e.isSpectator() && !(e instanceof net.minecraft.world.entity.player.Player p && p.isCreative()));
        
        double closestDist = range * range;
        
        for (Entity e : entities) {
            AABB entityBox = e.getBoundingBox().inflate(0.5);
            java.util.Optional<Vec3> hit = entityBox.clip(eyePos, endPos);
            if (hit.isPresent()) {
                double dist = eyePos.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    targetEntity = e;
                    targetPos = hit.get();
                }
            }
        }
        
        // 3. Generate Swords (Horizontal Line)
        RandomSource random = player.getRandom();
        
        // Base count calculation
        int baseCount = getSwordCount(vars.proficiency_unlimited_blade_works, 75);
        
        // Reduce count if outside UBW dimension (2/3 of normal)
        boolean isInsideUBW = player.level().dimension().location().equals(ModDimensions.UBW_KEY.location());
        if (!isInsideUBW) {
            baseCount = (int)(baseCount * 0.66);
            if (baseCount < 5) baseCount = 5; // Maintain minimum limit
        }
        
        int swordCount = baseCount;
        
        // Horizontal Spread Logic
        // Calculate "Right" vector
        Vec3 horizontalLook = new Vec3(lookVec.x, 0, lookVec.z).normalize();
        Vec3 rightVec = horizontalLook.cross(new Vec3(0, 1, 0)).normalize();
        
        // Center of the line: Behind and Up
        double centerOffsetH = -1.5;
        double centerOffsetV = 2.5;
        Vec3 centerPos = player.position().add(0, player.getEyeHeight() * 0.8, 0)
            .add(horizontalLook.scale(centerOffsetH))
            .add(0, centerOffsetV, 0);
            
        // Line Width: ~20 blocks (Multiplied by 2, was 10)
        double lineWidth = 20.0;
        double spacing = lineWidth / (swordCount - 1);
        double startOffset = -lineWidth / 2.0;
        
        for (int i = 0; i < swordCount; i++) {
            // Calculate spawn position along the line
            double offset = startOffset + (i * spacing);
            // Add some jitter
            offset += (random.nextDouble() - 0.5) * 0.5;
            
            // Increased Vertical Spread (Mode 2)
            double vJitter = (random.nextDouble() - 0.5) * 4.0; // Increased from 0.5 to 4.0
            
            Vec3 spawnPos = centerPos.add(rightVec.scale(offset)).add(0, vJitter, 0);
            
            // Check obstruction
            BlockPos blockPos = BlockPos.containing(spawnPos);
            if (!level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty()) {
                 spawnPos = spawnPos.add(0, -1, 0); // Try lower
            }
            
            // Spawn Sword
            ItemStack weapon = weapons.get(random.nextInt(weapons.size())).copy();
            SwordBarrelProjectileEntity projectile = new SwordBarrelProjectileEntity(level, player, weapon);
            projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            
            // Check Broken Phantasm Mode
            if (player.getPersistentData().getBoolean("UBWBrokenPhantasmEnabled")) {
                projectile.setBrokenPhantasm(true);
            }
            
            if (targetEntity != null) {
                projectile.setTargetEntity(targetEntity.getId());
            }
            
            // Determine Target Point for Hover (Aiming)
            // If we have a target entity, aim at it. Otherwise aim at the raytrace end point.
            // But wait, if targetEntity moves, aimTarget is static here.
            // We need to update target dynamically in Entity.tick().
            // However, setHover takes a static Vec3.
            // Let's pass the entity ID if possible? No, we use SynchedEntityData for position.
            // We should ideally update the synched target pos in tick() if we have a target entity.
            // But currently we only have static position target.
            // The user wants "follow player looking position slowly".
            // This implies the projectile should track the PLAYER'S LOOK TARGET.
            
            // For now, let's just set initial target. The entity tick logic needs to be updated to track player look.
            // OR, the projectile should just turn towards its current target (which is static).
            // "停滞的时候也跟着玩家看向的位置缓慢变向" -> The target position itself changes?
            // If so, we need to update TARGET_POS from the shooter's tick or projectile's tick.
            
            Vec3 aimTarget = targetEntity != null ? targetEntity.position().add(0, targetEntity.getBbHeight() * 0.5, 0) : targetPos;
            projectile.setHover(20, aimTarget);
            projectile.setOwner(player); // Ensure owner is set for tracking
            projectile.setMode2Tracking(true); // Enable tracking for Mode 2
            
            // Initial Rotation (Facing target)
            Vec3 dir = aimTarget.subtract(spawnPos).normalize();
            projectile.setXRot((float)(Math.toDegrees(Math.asin(-dir.y))));
            projectile.setYRot((float)(Math.toDegrees(Math.atan2(-dir.x, dir.z)))); 
            
            level.addFreshEntity(projectile);
            
            if (level instanceof ServerLevel serverLevel) {
                 serverLevel.sendParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y, spawnPos.z, 3, 0.1, 0.1, 0.1, 0.05);
            }
        }
        
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ILLUSIONER_PREPARE_MIRROR, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
        
        // Turn off automatically after one burst
        vars.is_sword_barrel_active = false;
        vars.syncPlayerVariables(player);
        // Mode 2: Only ON, no OFF prompt.
        // player.displayClientMessage(Component.translatable("message.typemoonworld.trace_off"), true);
    }
    
    private static void executeMode0(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        // Mode 0: AOE Rain (Modified Vertical Rain)
        // Behind player, 4-5 blocks up, angled forward.
        
        // 1. Get Weapons
        List<ItemStack> weapons = new ArrayList<>();
        for (ItemStack stack : vars.analyzed_items) {
            if (stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem) continue;
            if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem) {
                weapons.add(stack);
            }
        }
        
        if (weapons.isEmpty()) {
            // Fallback weapon? Or just return.
            return;
        }
        
        RandomSource random = player.getRandom();
        int swordCount = getSwordCount(vars.proficiency_unlimited_blade_works, 20);
        
        // Player facing vector
        Vec3 lookVec = player.getLookAngle();
        // Horizontal facing (ignore Y for positioning behind)
        Vec3 horizontalLook = new Vec3(lookVec.x, 0, lookVec.z).normalize();
        
        // Calculate "Behind" position center
        // Behind 2.0 blocks (slightly further back), Up 2.0 blocks (Lowered from 3.0)
        double centerOffsetH = -2.0;
        double centerOffsetV = 2.0;
        
        Vec3 centerPos = player.position().add(0, player.getEyeHeight() * 0.8, 0)
            .add(horizontalLook.scale(centerOffsetH))
            .add(0, centerOffsetV, 0);
            
        // Calculate "Right" vector for spread (Cross product of Look and Up)
        Vec3 rightVec = horizontalLook.cross(new Vec3(0, 1, 0)).normalize();
        
        for (int i = 0; i < swordCount; i++) {
            ItemStack weapon = weapons.get(random.nextInt(weapons.size())).copy();
            
            // Spread along the "Right" vector (Line formation behind player)
            // Width: approx 18 blocks (was 9.0)
            double width = 18.0;
            double spread = (random.nextDouble() - 0.5) * width;
            
            // Add some vertical noise (Increased for Mode 0)
            double vNoise = (random.nextDouble() - 0.5) * 4.0; // Increased from 1.5 to 4.0
            
            Vec3 spawnPos = centerPos.add(rightVec.scale(spread)).add(0, vNoise, 0);
            
            // Check obstruction
            if (!player.level().getBlockState(net.minecraft.core.BlockPos.containing(spawnPos)).isAir()) {
                // Try slightly lower or closer
                spawnPos = spawnPos.add(0, -1, 0);
            }
            
            SwordBarrelProjectileEntity projectile = new SwordBarrelProjectileEntity(player.level(), player, weapon);
            projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            
            // Check Broken Phantasm Mode
            if (player.getPersistentData().getBoolean("UBWBrokenPhantasmEnabled")) {
                projectile.setBrokenPhantasm(true);
            }
            
            // Velocity: Follow player look direction
            // Target: Point roughly 20 blocks in front of player view
            Vec3 targetPoint = player.getEyePosition().add(lookVec.scale(20.0));
            
            // Direction vector
            Vec3 dir = targetPoint.subtract(spawnPos).normalize();
            
            // Add slight spread to direction
            dir = dir.add((random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15).normalize();
            
            double speed = 1.8 + random.nextDouble() * 0.6;
            projectile.setDeltaMovement(dir.scale(speed));
            
            // Set rotation to match direction (approximately)
            projectile.setXRot((float)(Math.toDegrees(Math.asin(-dir.y))));
            projectile.setYRot((float)(Math.toDegrees(Math.atan2(-dir.x, dir.z))));
            
            player.level().addFreshEntity(projectile);
            
            if (player.level() instanceof ServerLevel serverLevel) {
                 serverLevel.sendParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y, spawnPos.z, 3, 0.1, 0.1, 0.1, 0.05);
            }
        }
        
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ILLUSIONER_PREPARE_MIRROR, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    private static void executeMode1(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        // Mode 1: Aimed Rain (Surround Target)
        
        // 1. Get Weapons
        List<ItemStack> weapons = new ArrayList<>();
        for (ItemStack stack : vars.analyzed_items) {
            if (stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem) continue;
            if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem) {
                weapons.add(stack);
            }
        }
        
        if (weapons.isEmpty()) {
            return;
        }
        
        // 2. Find Target (RayTrace)
        // Max distance: 64 blocks
        double range = 64.0;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));
        
        Level level = player.level();
        
        // First check block
        BlockHitResult blockHit = level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        
        Vec3 targetPos = endPos; // Default to max range
        Entity targetEntity = null;
        
        if (blockHit.getType() != HitResult.Type.MISS) {
            targetPos = blockHit.getLocation();
        }
        
        // Then check entities along the ray
        AABB rayBox = new AABB(eyePos, targetPos).inflate(1.0);
        List<Entity> entities = level.getEntities(player, rayBox, e -> e instanceof LivingEntity && !e.isSpectator() && !(e instanceof net.minecraft.world.entity.player.Player p && p.isCreative()));
        
        double closestDist = range * range;
        
        for (Entity e : entities) {
            AABB entityBox = e.getBoundingBox().inflate(0.5); // Slightly larger hitbox for easier aiming
            java.util.Optional<Vec3> hit = entityBox.clip(eyePos, endPos);
            if (hit.isPresent()) {
                double dist = eyePos.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    targetEntity = e;
                    targetPos = hit.get();
                }
            }
        }
        
        // Determine Surround Center and Radius
        Vec3 center = targetPos;
        double radius = 4.0; // Base radius
        
        if (targetEntity != null) {
            // Adjust to entity center
            center = targetEntity.position().add(0, targetEntity.getBbHeight() * 0.5, 0);
            // Adjust radius based on hitbox
            radius = Math.max(3.0, targetEntity.getBbWidth() * 2.0 + 2.0);
        } else {
            // If block, maybe check if it's solid and adjust center?
            // Keep as is for now.
        }
        
        // 3. Generate Swords
        RandomSource random = player.getRandom();
        int swordCount = getSwordCount(vars.proficiency_unlimited_blade_works, 12);
        
        for (int i = 0; i < swordCount; i++) {
            // Random point on sphere surface
            // Uniform distribution on sphere:
            // phi = 2*PI*random
            // cos(theta) = 2*random - 1  => theta = acos(...)
            
            double phi = random.nextDouble() * 2.0 * Math.PI;
            double u = random.nextDouble() * 2.0 - 1.0;
            double theta = Math.acos(u); // 0 to PI
            
            double x = radius * Math.sin(theta) * Math.cos(phi);
            double y = radius * Math.sin(theta) * Math.sin(phi);
            double z = radius * Math.cos(theta);
            
            // Limit Y to not be too low (don't spawn underground if possible)
            // If target is on ground, lower hemisphere might be underground.
            // Let's bias towards upper hemisphere if target is on ground?
            // Or just check collision.
            
            Vec3 spawnOffset = new Vec3(x, y, z);
            Vec3 spawnPos = center.add(spawnOffset);
            
            // Check if spawn position is valid (not inside solid block)
            // Also check if block below is valid (as per requirement: "if block below has block, don't spawn" -> wait, 
            // user said: "if creature or block has block BELOW IT, don't spawn corresponding position sword"
            // "if biological or square underneath has square, cannot generate do not generate corresponding position sword, change to other can generate position generate"
            // Interpretation: If the sword spawn location is obstructed, find another one.
            
            BlockPos blockPos = BlockPos.containing(spawnPos);
            if (!level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty()) {
                // Obstructed. Try again (up to 3 times)
                boolean found = false;
                for (int tryCount = 0; tryCount < 3; tryCount++) {
                     phi = random.nextDouble() * 2.0 * Math.PI;
                     u = random.nextDouble() * 2.0 - 1.0; // Full sphere
                     // Maybe bias to upper hemisphere? u from -0.2 to 1.0?
                     
                     theta = Math.acos(u);
                     x = radius * Math.sin(theta) * Math.cos(phi);
                     y = radius * Math.sin(theta) * Math.sin(phi);
                     z = radius * Math.cos(theta);
                     spawnPos = center.add(x, y, z);
                     blockPos = BlockPos.containing(spawnPos);
                     
                     if (level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty()) {
                         found = true;
                         break;
                     }
                }
                if (!found) continue; // Skip this sword if no valid spot found
            }
            
            // Spawn Sword
            ItemStack weapon = weapons.get(random.nextInt(weapons.size())).copy();
            SwordBarrelProjectileEntity projectile = new SwordBarrelProjectileEntity(level, player, weapon);
            projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            
            // Check Broken Phantasm Mode
            if (player.getPersistentData().getBoolean("UBWBrokenPhantasmEnabled")) {
                projectile.setBrokenPhantasm(true);
            }
            
            if (targetEntity != null) {
                projectile.setTargetEntity(targetEntity.getId());
            }
            
            // Set Hover: 20 ticks (1s)
            projectile.setHover(20, center);
            projectile.setOwner(player); // Add tracking owner for Mode 1
            projectile.setMode1Tracking(true); // Enable homing flight

            
            // Initial Rotation (Facing center)
            Vec3 dir = center.subtract(spawnPos).normalize();
            projectile.setXRot((float)(Math.toDegrees(Math.asin(-dir.y))));
            // Align with Mode 2 flight direction logic (standard atan2)
            // Fix Mode 2 inversion: if facing Z-axis front,抬头=剑向下, 低头=剑向上, 右转=剑左转, 左转=剑右转
            // This means Pitch and Yaw are inverted.
            // XRot (Pitch): Minecraft is +90 down, -90 up.
            // asin(-dir.y): if dir.y is positive (up), asin is negative (up). This matches Minecraft standard.
            // BUT user says "抬头(look up) -> 剑向下(point down)".
            // If player looks up, target is high. dir.y > 0. asin(-y) < 0 (XRot negative).
            // If XRot is negative, renderer (135 - xRot) -> 135 - (-neg) = 135 + pos > 135.
            // If renderer expects standard pitch, then maybe the renderer logic (135 - xRot) is the culprit for the inversion feel?
            // User wants: Look up -> Sword point up.
            // Currently: Look up -> XRot negative -> Sword points... wait.
            
            // Let's assume standard behavior first.
            // Standard: Look up -> dir.y > 0 -> XRot < 0.
            // Renderer: 135 - XRot.
            // If XRot = -45 (up), Renderer = 180.
            // If XRot = +45 (down), Renderer = 90.
            // This seems to be the inversion source if 90 is "forward" and 180 is "down"? No.
            
            // The user report is clear: EVERYTHING IS INVERTED.
            // Inverted Yaw (Left/Right) AND Inverted Pitch (Up/Down).
            // This means we are calculating rotation towards the TARGET, but the result is somehow opposite?
            // Or the renderer is interpreting it opposite.
            
            // If I look RIGHT (+X), target is at +X. dir.x > 0.
            // atan2(-dir.x, dir.z). -dir.x is negative.
            // atan2(neg, z). If z is 0, angle is -90.
            // Minecraft Yaw: -90 is EAST (+X).
            // So Yaw calculation seems CORRECT for Minecraft standard.
            
            // BUT "Left turn -> Sword Right turn".
            // If I turn Left (Yaw decreases), sword turns Right (Yaw increases?).
            // This implies the sword is rotating in the opposite direction.
            
            // SOLUTION: Invert the calculated rotation before setting.
            // But wait, "center.subtract(spawnPos)" is vector FROM sword TO target.
            // That is correct for "facing target".
            
            // Maybe the issue is visual relative to player?
            // If sword is BEHIND player and facing target (which is in front of player),
            // Sword should look like it's parallel to player look.
            
            // Let's try inverting the logic as requested to "fix" the inversion observation.
            // Instead of asin(-dir.y), let's try asin(dir.y)? -> Inverts Pitch.
            // Instead of atan2(-x, z), let's try atan2(x, z)? -> Inverts Yaw (mirrors over Z axis).
            
            // HOWEVER, "Left turn -> Sword Right turn" suggests mirroring.
            
            // Let's try swapping the signs to negate the current behavior.
            // Current: asin(-y), atan2(-x, z)
            // New: asin(y), atan2(x, z) ?
            
            // Let's analyze "Low head -> Sword Up".
            // Low head -> Look down -> Target Y < Sword Y -> dir.y < 0.
            // Current: asin(-(-val)) = asin(pos) = positive XRot (Down).
            // User says "Sword Up". Sword Up means XRot should be negative?
            // So if dir.y < 0, we want XRot < 0.
            // So we want asin(dir.y).
            
            // Let's analyze "Right turn -> Sword Left turn".
            // Turn Right -> Look East (+X). Target X > Sword X. dir.x > 0.
            // Current: atan2(-pos, z). If z~0, atan2(-1, 0) = -90.
            // -90 is East.
            // User says "Sword Left turn". Left is West (+90).
            // So if dir.x > 0, we want Yaw to be +90?
            // No, standard is -90.
            // If user sees "Left turn", maybe they mean the sword rotates CCW when they turn CW?
            
            // If the user says it is inverted, let's invert the vector components used for calculation.
            
            projectile.setXRot((float)(Math.toDegrees(Math.asin(dir.y)))); // Inverted Pitch sign
            projectile.setYRot((float)(Math.toDegrees(Math.atan2(dir.x, dir.z)))); // Inverted Yaw X sign
            
            level.addFreshEntity(projectile);
            
            if (level instanceof ServerLevel serverLevel) {
                 serverLevel.sendParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y, spawnPos.z, 3, 0.1, 0.1, 0.1, 0.05);
            }
        }
        
        level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.ILLUSIONER_PREPARE_MIRROR, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
    }
    
    private static int getSwordCount(double proficiency, int maxCount) {
        // Minimum 5, Maximum maxCount
        // Proficiency assumed 0-100
        double ratio = Math.min(Math.max(proficiency, 0.0), 100.0) / 100.0;
        return 5 + (int)((maxCount - 5) * ratio);
    }
}
