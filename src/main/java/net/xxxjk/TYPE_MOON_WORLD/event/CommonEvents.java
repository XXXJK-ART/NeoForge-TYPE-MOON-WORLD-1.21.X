package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.BlockParticleOption;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class CommonEvents {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;

        if (event.getEntity() instanceof Monster monster) {
            // Add goal to target Ryougi Shiki
            // Priority 2 or 3, usually after Player (which is typically 1 or 2)
            // But we want them to actively target her too.
            try {
                monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, RyougiShikiEntity.class, true));
            } catch (Exception e) {
                // Ignore if AI is locked or incompatible (e.g. Slimes might not have standard goal selector)
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        
        Player player = event.getEntity();
        
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof TempleStoneSwordAxeItem) {
            boolean hasStrength = player.hasEffect(MobEffects.DAMAGE_BOOST);
            
            if (hasStrength) {
                // Remove debuffs if present
                if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                if (player.hasEffect(MobEffects.DIG_SLOWDOWN)) player.removeEffect(MobEffects.DIG_SLOWDOWN);
            } else {
                // Apply debuffs if not present (simulating weight)
                // Short duration to ensure it wears off quickly if item is unequipped, but constantly reapplied
                if (!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) 
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                if (!player.hasEffect(MobEffects.DIG_SLOWDOWN)) 
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 2));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.hasEffect(ModMobEffects.NINE_LIVES)) {
                // Consume Buff
                player.removeEffect(ModMobEffects.NINE_LIVES);

                if (player.level() instanceof ServerLevel serverLevel) {
                    // White Steam (Overheat/Activation) - Only when triggering
                    // Not here, moved to when effect is applied
                }
                
                LivingEntity target = event.getEntity();
                // Get Base Damage (Attribute)
                double baseDamageAttr = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float baseDamage = (float)baseDamageAttr;
                
                triggerNineLives(player, target, baseDamage);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect() == ModMobEffects.NINE_LIVES && !event.getEntity().level().isClientSide) {
             if (event.getEntity().level() instanceof ServerLevel serverLevel) {
                 // Enhanced Steam Effect (Duration + Density + Fancy)
                 // Duration: ~2 seconds (20 loops * 2 ticks = 40 ticks)
                 for (int i = 0; i < 20; i++) {
                     final int delay = i * 2;
                     TYPE_MOON_WORLD.queueServerWork(delay, () -> {
                         // Steam/Smoke - Low and wide spread to avoid blocking view, fast outward speed
                         // Y+0.2 (Feet/Legs), Spread X/Z=2.0 (Wide), Speed=0.4 (Fast)
                         serverLevel.sendParticles(ParticleTypes.CLOUD, event.getEntity().getX(), event.getEntity().getY() + 0.2, event.getEntity().getZ(), 40, 2.0, 0.2, 2.0, 0.4);
                         serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, event.getEntity().getX(), event.getEntity().getY() + 0.2, event.getEntity().getZ(), 10, 1.5, 0.5, 1.5, 0.2);
                         
                         // Occasional bursts - Low
                         if (serverLevel.random.nextFloat() < 0.3f) {
                             serverLevel.sendParticles(ParticleTypes.POOF, event.getEntity().getX(), event.getEntity().getY() + 0.5, event.getEntity().getZ(), 15, 1.0, 0.2, 1.0, 0.3);
                             serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, event.getEntity().getX(), event.getEntity().getY() + 0.5, event.getEntity().getZ(), 5, 0.5, 0.2, 0.5, 0.2);
                         }
                     });
                 }
                 
                 // Sound Effects (Hissing steam)
                 serverLevel.playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
                 TYPE_MOON_WORLD.queueServerWork(10, () -> 
                     serverLevel.playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.6f)
                 );
                 TYPE_MOON_WORLD.queueServerWork(25, () -> 
                     serverLevel.playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0f, 0.5f)
                 );
             }
        }
    }

    private static void triggerNineLives(Player player, LivingEntity target, float damageBase) {
        // 8 Hits: 2x Damage, 2 ticks interval
        for (int i = 1; i <= 8; i++) {
            final int tickDelay = i * 2;
            TYPE_MOON_WORLD.queueServerWork(tickDelay, () -> {
                if (target.isAlive()) {
                    target.invulnerableTime = 0;
                    target.hurt(player.damageSources().mobAttack(player), damageBase * 2.0F);
                    target.invulnerableTime = 0;
                    
                    if (player.level() instanceof ServerLevel serverLevel) {
                        // Gold/White Slash Trails
                        serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 10, 0.5, 0.5, 0.5, 0.5); // Gold
                        serverLevel.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 1, target.getZ(), 3, 0.3, 0.3, 0.3, 0.05); // White Speed/Smoke
                        player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);

                        // Single slash per hit
                    // Gem Trail Slash Effect (White)
                    
                    // Determine Scale based on Target Hitbox
                    float targetSize = Math.max(target.getBbWidth(), target.getBbHeight());
                    float visualScale = Math.max(0.5f, targetSize / 1.8f);
                    
                    // Planar Slash Logic: Perpendicular to Player View
                    Vec3 viewDir = player.getLookAngle().normalize();
                    Vec3 upDir = new Vec3(0, 1, 0);
                    Vec3 rightDir = viewDir.cross(upDir).normalize();
                    if (rightDir.lengthSqr() < 0.01) rightDir = new Vec3(1, 0, 0); // Handle looking straight up/down
                    Vec3 planeUp = rightDir.cross(viewDir).normalize();
                    
                    double angle = serverLevel.random.nextDouble() * 2 * Math.PI;
                    double dist = targetSize * 1.5 + 4.0; // Longer distance for planar sweep
                    
                    // Random offset within the plane for the "center" of the slash
                    // This ensures slashes don't all cross exactly at the center point
                    double centerOffsetX = (serverLevel.random.nextDouble() - 0.5) * target.getBbWidth() * 0.8;
                    double centerOffsetY = (serverLevel.random.nextDouble() - 0.5) * target.getBbHeight() * 0.8;
                    
                    // Center point in world space (offset from target center)
                    Vec3 centerPoint = new Vec3(target.getX(), target.getY() + target.getBbHeight()/2, target.getZ())
                        .add(rightDir.scale(centerOffsetX))
                        .add(planeUp.scale(centerOffsetY));
                    
                    // Calculate Slash Direction in the plane
                    Vec3 slashDir = rightDir.scale(Math.cos(angle)).add(planeUp.scale(Math.sin(angle))).normalize();
                    
                    // Start Position: Center - Direction * Distance
                    Vec3 startPos = centerPoint.add(slashDir.scale(-dist));
                    Vec3 endPos = centerPoint.add(slashDir.scale(dist));
                    
                    RubyProjectileEntity slash = new RubyProjectileEntity(serverLevel, startPos.x, startPos.y, startPos.z);
                    slash.setItem(ItemStack.EMPTY);
                    slash.setGemType(99); // Visual Only White
                    slash.setVisualScale(visualScale);
                    slash.setVisualEnd(endPos);
                    slash.setNoGravity(true);
                    slash.setDeltaMovement(Vec3.ZERO);
                    
                    serverLevel.addFreshEntity(slash);
                    }
                }
            });
        }
        
        // 9th Hit: 5x Damage, 10 ticks after 8th (16 + 10 = 26)
        TYPE_MOON_WORLD.queueServerWork(26, () -> {
            if (target.isAlive()) {
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().mobAttack(player), damageBase * 5.0F);
                target.invulnerableTime = 0;
                
                if (player.level() instanceof ServerLevel serverLevel) {
                    // Finisher Slash
                    float targetSize = Math.max(target.getBbWidth(), target.getBbHeight());
                    float visualScale = Math.max(0.5f, targetSize / 1.8f);

                    // Planar Slash Logic: Perpendicular to Player View
                    Vec3 viewDir = player.getLookAngle().normalize();
                    Vec3 upDir = new Vec3(0, 1, 0);
                    Vec3 rightDir = viewDir.cross(upDir).normalize();
                    if (rightDir.lengthSqr() < 0.01) rightDir = new Vec3(1, 0, 0); // Handle looking straight up/down
                    Vec3 planeUp = rightDir.cross(viewDir).normalize();

                    double angle = serverLevel.random.nextDouble() * 2 * Math.PI;
                    double dist = targetSize * 1.5 + 4.0; // Longer distance

                    // Random offset within the plane for the "center" of the slash
                    double centerOffsetX = (serverLevel.random.nextDouble() - 0.5) * target.getBbWidth() * 0.8;
                    double centerOffsetY = (serverLevel.random.nextDouble() - 0.5) * target.getBbHeight() * 0.8;

                    // Center point in world space (offset from target center)
                    Vec3 centerPoint = new Vec3(target.getX(), target.getY() + target.getBbHeight()/2, target.getZ())
                        .add(rightDir.scale(centerOffsetX))
                        .add(planeUp.scale(centerOffsetY));

                    // Calculate Slash Direction in the plane
                    Vec3 slashDir = rightDir.scale(Math.cos(angle)).add(planeUp.scale(Math.sin(angle))).normalize();

                    // Start Position: Center - Direction * Distance
                    Vec3 startPos = centerPoint.add(slashDir.scale(-dist));
                    Vec3 endPos = centerPoint.add(slashDir.scale(dist));

                    RubyProjectileEntity slash = new RubyProjectileEntity(serverLevel, startPos.x, startPos.y, startPos.z);
                    slash.setItem(ItemStack.EMPTY);
                    slash.setGemType(99); // Visual Only White
                    slash.setVisualScale(visualScale);
                    slash.setVisualEnd(endPos);
                    slash.setNoGravity(true);
                    slash.setDeltaMovement(Vec3.ZERO);
                    
                    serverLevel.addFreshEntity(slash);

                    // Finisher: Slash Only (No explosion/blood/debris)
                }
            }
        });

        // Cleanup: Remove all visual slashes after delay
        // Finisher is at 26 ticks. + 80 ticks (4s) = 106 ticks.
        // Actually RubyProjectileEntity handles its own cleanup now (80 ticks lifetime).
        // So we don't need explicit cleanup here anymore, or just keep it as failsafe.
        // We will remove this block to rely on entity logic.
    }
}
