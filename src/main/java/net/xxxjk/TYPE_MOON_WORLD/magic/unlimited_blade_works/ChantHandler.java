package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class ChantHandler {

    // Chant intervals (in ticks). Assuming 20 ticks = 1 second.
    // Total lines: 9 lines + 1 activation
    private static final int BASE_CHANT_INTERVAL = 40; // 2 seconds base
    
    // Store entities to be removed with delay for each player
    // UUID -> List of RemovalEntry
    private static final Map<UUID, List<RemovalEntry>> REMOVAL_QUEUES = new ConcurrentHashMap<>();
    
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
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            // Process removal queue first
            processRemovalQueue(player);
            
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            boolean isChanting = vars.is_chanting_ubw;
            boolean wasChanting = WAS_CHANTING.getOrDefault(player.getUUID(), false);
            
            // Detect falling edge: Player WAS chanting but IS NOT chanting anymore.
            // This covers all cancellation cases: key release, packet update, interruption, etc.
            if (wasChanting && !isChanting && !vars.is_in_ubw) {
                // Stopped chanting and didn't enter UBW (if entered UBW, is_in_ubw would be true)
                // Ensure visual effects are cleared.
                clearVisualSwords(player);
                // Reset progress just in case
                if (vars.ubw_chant_progress > 0) {
                    vars.ubw_chant_progress = 0;
                    vars.ubw_chant_timer = 0;
                    vars.syncPlayerVariables(player);
                }
            }
            
            // Update state for next tick
            WAS_CHANTING.put(player.getUUID(), isChanting);
            
            if (vars.is_chanting_ubw) {
                // ... (chanting logic) ...
                
                // Apply Slowness during chant
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false));
                
                vars.ubw_chant_timer++;
                
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
                    }
                    
                    // Process Chant Steps
                    processChantStep(player, vars);
                }
                
                // Continuous weapon rain from step 3 onwards
                if (vars.ubw_chant_progress >= 3 && vars.ubw_chant_progress <= 9) {
                     // Spawn weapons frequently, not just on step change
                     // Spawn every 10 ticks (0.5s) approx to feel like "rain"
                     // Or check if current timer % 10 == 0
                     if (vars.ubw_chant_timer % 10 == 0) {
                         spawnVisualSwords(player, vars, 3); // Spawn a few each time
                     }
                }
            }
        }
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
    
    private static void processChantStep(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        int progress = vars.ubw_chant_progress;
        double cost = 50.0;
        String chantText = "";
        
        // Line 1: 50 mana (handled in MagicUnlimitedBladeWorks)
        // Lines 2-9: 50 mana each
        // Total steps: 1 (start), 2, 3, 4, 5, 6, 7, 8, 9 (final chant), 10 (activation)
        
        if (progress == 2) {
            chantText = "§bSteel is my body, and fire is my blood.";
        } else if (progress == 3) {
            chantText = "§bI have created over a thousand blades.";
            // Initial burst spawn
            spawnVisualSwords(player, vars, 8);
        } else if (progress == 4) {
            chantText = "§bUnaware of loss.";
        } else if (progress == 5) {
            chantText = "§bNor aware of gain.";
        } else if (progress == 6) {
            chantText = "§bWithstood pain to create weapons,waiting for one's arrival.";
        } else if (progress == 7) {
            chantText = "§bI have no regrets.";
        } else if (progress == 8) {
            chantText = "§bThis is the only path.";
        } else if (progress == 9) {
            chantText = "§bMy whole life was,";
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
    
    private static void spawnVisualSwords(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, int count) {
        if (player.level().isClientSide) return;
        
        List<ItemStack> weapons = new ArrayList<>();
        for (ItemStack stack : vars.analyzed_items) {
            if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem) {
                weapons.add(stack);
            }
        }
        
        if (!weapons.isEmpty()) {
            for (int i = 0; i < count; i++) {
                ItemStack weapon = weapons.get(player.getRandom().nextInt(weapons.size())).copy();
                
                double radius = player.getRandom().nextDouble() * 10.0;
                double angle = player.getRandom().nextDouble() * Math.PI * 2;
                double x = player.getX() + radius * Math.cos(angle);
                double z = player.getZ() + radius * Math.sin(angle);
                double y = player.getY() + 4 + player.getRandom().nextDouble() * 2; // 4-6 blocks high

                // Prevent spawning directly above player's head (small exclusion zone)
                // Radius check: if x,z is within 1 block of player
                if (Math.abs(x - player.getX()) < 1.0 && Math.abs(z - player.getZ()) < 1.0) {
                     continue; // Skip this sword
                }
                
                // Check for obstruction
                if (!player.level().getBlockState(net.minecraft.core.BlockPos.containing(x, y, z)).isAir()) {
                    y = player.getY() + 0.5; // Fallback
                }
                
                BrokenPhantasmProjectileEntity projectile = new BrokenPhantasmProjectileEntity(player.level(), player, weapon);
                projectile.setPos(x, y, z);
                projectile.setDeltaMovement(0, -0.8, 0); // Downward velocity
                projectile.setUBWPhantasm(true);
                player.level().addFreshEntity(projectile);
            }
        }
    }
    
    private static void clearVisualSwords(ServerPlayer player) {
        // Clear entities in a large radius around the player
        double maxRadius = 32.0;
        AABB box = player.getBoundingBox().inflate(maxRadius);
        List<Entity> entities = player.level().getEntities(player, box, e -> 
            (e instanceof Display.ItemDisplay && e.getTags().contains("ubw_visual_sword")) ||
            (e instanceof BrokenPhantasmProjectileEntity && ((BrokenPhantasmProjectileEntity)e).isUBWPhantasm())
        );
        
        List<RemovalEntry> queue = new ArrayList<>();
        
        for (Entity e : entities) {
            double dist = e.distanceTo(player);
            // Delay calculation: Outer (far) -> Delay Small (0)
            // Inner (close) -> Delay Large (maxRadius)
            // delay = (maxRadius - dist) * factor
            // Let's use 0.5 tick per block to make it faster (16 ticks total spread)
            int delay = (int) Math.max(0, (maxRadius - dist) * 1.0);
            
            queue.add(new RemovalEntry(e, delay));
        }
        
        if (!queue.isEmpty()) {
            // Merge or replace queue? Usually clear is called once at end.
            // Replace is safer to avoid duplicates if called multiple times.
            REMOVAL_QUEUES.put(player.getUUID(), queue);
        }
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
        
        // Clear visual effects immediately (instant remove, no animation delay)
        // Because player is leaving this dimension anyway.
        // Or if we want animation, we can leave it, but since we tp away, better clear to save resources.
        
        // The user requested: "Upon entering UBW dimension, items in overworld should be cleared directly."
        // We can reuse clearVisualSwords but with 0 delay for all.
        
        // Actually, clearVisualSwords uses a queue. We should probably clear instantly here.
        AABB box = player.getBoundingBox().inflate(64.0); // Large radius
        List<Entity> entities = player.level().getEntities(player, box, e -> 
            (e instanceof Display.ItemDisplay && e.getTags().contains("ubw_visual_sword")) ||
            (e instanceof BrokenPhantasmProjectileEntity && ((BrokenPhantasmProjectileEntity)e).isUBWPhantasm())
        );
        
        for (Entity e : entities) {
            e.discard();
        }
        
        // Also clear any pending removal queue for this player to stop processing
        REMOVAL_QUEUES.remove(player.getUUID());
        
        // Teleport to Reality Marble dimension
        net.minecraft.server.level.ServerLevel ubwLevel = player.server.getLevel(net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions.UBW_KEY);
        if (ubwLevel != null) {
            // Target Y calculated based on flat world generation
            double targetY = -12; 
            player.teleportTo(ubwLevel, 0.5, targetY, 0.5, player.getYRot(), player.getXRot());
        }
        
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
    }
    
    private static void interruptChant(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, String reasonKey) {
        vars.is_chanting_ubw = false;
        vars.ubw_chant_progress = 0;
        vars.ubw_chant_timer = 0;
        vars.syncPlayerVariables(player);
        
        // Clear visual effects
        clearVisualSwords(player);
        
        player.displayClientMessage(Component.translatable(reasonKey), true);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
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
