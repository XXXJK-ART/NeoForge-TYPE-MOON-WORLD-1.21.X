package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class MuramasaSlashHandler {
    
    private static class SlashInstance {
        final UUID playerUUID;
        final ResourceKey<Level> dimension;
        final Vec3 startPos;
        final Vec3 direction;
        final Vec3 right;
        final int charge;
        final int maxDistance;
        final int width;
        final int height;
        
        double currentDistance = 0;
        
        SlashInstance(UUID playerUUID, ResourceKey<Level> dimension, Vec3 startPos, Vec3 direction, int charge, int maxDistLimit, int maxWidthLimit, int maxHeightLimit) {
            this.playerUUID = playerUUID;
            this.dimension = dimension;
            this.startPos = startPos;
            this.direction = direction.normalize();
            this.right = new Vec3(-direction.z, 0, direction.x).normalize(); // Horizontal perpendicular
            this.charge = charge;
            
            // Dimensions
            // Max distance: scaled by charge relative to max possible charge (assumed 100 for normalization, but passed limits are absolute)
            // Let's assume charge is passed as raw ticks or percentage. 
            // Previous logic: maxDistance = charge * 2.
            // New logic: scale linearly up to maxDistLimit based on charge.
            // If we assume standard max charge is ~100.
            
            this.maxDistance = Math.max(20, Math.min(maxDistLimit, (int)(charge * (maxDistLimit / 100.0)))); 
            // Width
            this.width = Math.min(maxWidthLimit, 1 + charge / 10);
            // Height
            this.height = Math.min(maxHeightLimit, Math.max(5, charge)); 
        }
    }

    private static final List<SlashInstance> ACTIVE_SLASHES = new ArrayList<>();

    public static void initiate(ServerLevel level, ServerPlayer player, int charge, int maxDist, int maxWidth, int maxHeight) {
        if (charge <= 0) return;
        
        Vec3 look = player.getLookAngle();
        ACTIVE_SLASHES.add(new SlashInstance(player.getUUID(), level.dimension(), player.position().add(0, player.getEyeHeight() * 0.5, 0), look, charge, maxDist, maxWidth, maxHeight));
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getLevel() instanceof Level level) {
             ResourceKey<Level> dim = level.dimension();
             ACTIVE_SLASHES.removeIf(slash -> slash.dimension.equals(dim));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        ResourceKey<Level> dim = level.dimension();
        
        Iterator<SlashInstance> it = ACTIVE_SLASHES.iterator();
        while (it.hasNext()) {
            SlashInstance slash = it.next();
            if (!slash.dimension.equals(dim)) continue;
            
            // Process Speed: 2 blocks per tick (Reduced from 5 for better visuals)
            // 100 blocks / 2 = 50 ticks = 2.5 seconds to travel full distance.
            double speed = 2.0;
            
            double prevDist = slash.currentDistance;
            slash.currentDistance += speed;
            
            if (slash.currentDistance > slash.maxDistance) {
                slash.currentDistance = slash.maxDistance; // Clamp for final step
            }
            
            // Process the segment from prevDist to currentDist
            processSegment(level, slash, prevDist, slash.currentDistance);
            
            if (slash.currentDistance >= slash.maxDistance) {
                it.remove();
            }
        }
    }
    
    private static void processSegment(ServerLevel level, SlashInstance slash, double startDist, double endDist) {
        // We iterate steps of 0.5 block to ensure coverage, especially for diagonal movements
        for (double d = startDist; d < endDist; d += 0.5) {
            Vec3 center = slash.startPos.add(slash.direction.scale(d));
            
            // Calculate taper at the end (last 20% of distance)
            double progress = d / slash.maxDistance;
            double widthFactor = 1.0;
            if (progress > 0.8) {
                // Scale linearly from 1.0 at 80% to 0.1 at 100%
                widthFactor = Math.max(0.1, 1.0 - (progress - 0.8) * 4.5);
            }
            int currentWidth = Math.max(1, (int)(slash.width * widthFactor));
            double currentWidthDouble = Math.max(0.5, slash.width * widthFactor);
            
            // Calculate Box at this center
            // Width is horizontal (left-right relative to look)
            // Height is vertical (up-down relative to look? Or just Up?)
            // "Vertical clearing" -> World Up.
            
            for (int w = -currentWidth/2; w <= currentWidth/2; w++) {
                // Horizontal offset
                Vec3 wOffset = slash.right.scale(w);
                
                for (int h = -1; h < slash.height; h++) {
                    // Vertical offset (World Up)
                    // Start from -1 (slightly below feet) to height
                    Vec3 posVec = center.add(wOffset).add(0, h, 0);
                    BlockPos pos = BlockPos.containing(posVec);
                    
                    BlockState state = level.getBlockState(pos);
                    // Check block destruction based on charge level
                    boolean isFluid = !level.getFluidState(pos).isEmpty();
                    float hardness = state.getDestroySpeed(level, pos);
                    boolean isBreakable = hardness >= 0; // -1 is unbreakable (Bedrock, Barrier, etc.)
                    
                    boolean canBreak;
                    if (slash.charge > 60) {
                        // High Level: Break everything except Bedrock (and technically unbreakable blocks)
                        // Explicitly check for Bedrock block to be sure, though hardness -1 covers it usually.
                        canBreak = isBreakable && !state.is(Blocks.BEDROCK);
                    } else {
                        // Low Level: Only break soft blocks (< 50 hardness)
                        canBreak = isBreakable && hardness < 50.0f;
                    }

                    if ((!state.isAir() && canBreak) || isFluid) {
                        // Vaporize (No drops)
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        
                        // Particles (Only occasional to reduce lag)
                        if (level.random.nextInt(10) == 0) {
                            level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
            
            // Entity Damage (Hitbox)
            // Box for this slice
            // Use full height and width for the box
            AABB box = new AABB(center.x, center.y, center.z, center.x, center.y, center.z)
                .inflate(currentWidthDouble, 0, currentWidthDouble) // Horizontal spread
                .expandTowards(0, slash.height, 0)    // Vertical spread (Upwards)
                .expandTowards(0, -1, 0);             // Slight downward check

            List<Entity> entities = level.getEntities(null, box); // Get all entities
            
            for (Entity e : entities) {
                if (e instanceof LivingEntity living && !e.getUUID().equals(slash.playerUUID)) {
                    // Damage
                    // Base 20 + 5 per charge. Max 520.
                    float damage = 20.0f + (slash.charge * 5.0f);
                    
                    Player player = level.getPlayerByUUID(slash.playerUUID);
                    if (player != null) {
                        living.hurt(level.damageSources().indirectMagic(player, player), damage);
                        EntityUtils.triggerSwarmAnger(level, player, living);
                    } else {
                        living.hurt(level.damageSources().magic(), damage);
                    }
                    
                    living.igniteForSeconds(5);
                }
            }
            
            // Send particles covering the area
            // Iterate vertically to ensure visibility and density across the full height
            // Standard particle rendering distance is ~32 blocks. Spawning only at center (y+50) makes it invisible from ground.
            double step = 10.0;
            for (double h = 0; h < slash.height; h += step) {
                // Determine chunk bounds
                double currentStepHeight = Math.min(step, slash.height - h);
                double chunkY = center.y + h + (currentStepHeight / 2.0);
                
                // Spawn particles for this vertical slice
                // Density: Adjusted per chunk to maintain high visual impact without overloading client
                
                // Explosions: 1 per chunk ensures a continuous pillar of explosions
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, chunkY, center.z, 1, currentWidthDouble/2.0, currentStepHeight/2.0, currentWidthDouble/2.0, 0.0);
                
                // Flames: High density
                level.sendParticles(ParticleTypes.FLAME, center.x, chunkY, center.z, 20, currentWidthDouble/2.0, currentStepHeight/2.0, currentWidthDouble/2.0, 0.1);
                
                // Lava: Moderate density
                level.sendParticles(ParticleTypes.LAVA, center.x, chunkY, center.z, 3, currentWidthDouble/2.0, currentStepHeight/2.0, currentWidthDouble/2.0, 0.0);
                
                // Smoke: Moderate density
                level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, chunkY, center.z, 2, currentWidthDouble/2.0, currentStepHeight/2.0, currentWidthDouble/2.0, 0.05);
            }
        }
    }
}
