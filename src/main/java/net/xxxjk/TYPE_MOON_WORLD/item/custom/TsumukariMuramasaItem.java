package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class TsumukariMuramasaItem extends RedswordItem {

    public TsumukariMuramasaItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxManaCost() {
        return 1000;
    }

    @Override
    public double getManaCostPerTick() {
        return 10.0;
    }

    @Override
    public int getMaxSlashDistance() {
        return 300;
    }

    @Override
    public int getMaxSlashWidth() {
        return 10;
    }

    @Override
    public int getMaxSlashHeight() {
        return 100;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
    
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseTicks) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        
        int useDuration = getUseDuration(stack, livingEntity) - remainingUseTicks;
        
        // Growth: 1 point every tick (0.05s).
        // Max charge 100 reached in 100 ticks (5 seconds).
        int currentCharge = useDuration;
        if (currentCharge > 100) currentCharge = 100;
        
        // Mana Cost: 10 per point (every tick), but stop at 100%
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        boolean isMaxCharge = currentCharge >= 100;
        
        if (isMaxCharge || vars.player_mana >= 10) {
            if (!isMaxCharge) {
                vars.player_mana -= 10;
                vars.syncMana(player);
            }
            
            // Feedback
            player.displayClientMessage(Component.literal(currentCharge > 60 ? "§4蓄力: " + currentCharge + "%" : "§c蓄力: " + currentCharge + "%"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.5f, 1.0f + (currentCharge / 100.0f));
            
            // Surrounding Flame Particles
            if (level instanceof ServerLevel serverLevel) {
                double radius = 1.0 + (currentCharge / 50.0); // Radius grows with charge
                int particleCount = 2 + (currentCharge / 10); // More particles with charge
                
                // 更华丽的特效：螺旋上升
                for (int i = 0; i < particleCount; i++) {
                    double angle = (2 * Math.PI * i / particleCount) + (level.getGameTime() * 0.2); // Rotating faster
                    double heightOffset = (level.getGameTime() % 20) / 10.0; // Rising effect
                    
                    double px = player.getX() + radius * Math.cos(angle);
                    double pz = player.getZ() + radius * Math.sin(angle);
                    double py = player.getY() + heightOffset + (level.random.nextDouble() * 0.5); 
                    
                    if (currentCharge > 60) {
                         // Red/Dark particles for high charge
                         serverLevel.sendParticles(ParticleTypes.LAVA, px, py, pz, 1, 0, 0, 0, 0);
                         serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, 0, 0, 0, 0.05);
                    } else {
                         serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0, 0.05, 0, 0.05);
                    }
                }
            }

            if (currentCharge == 100 && useDuration == 100) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.5f, 2.0f);
            }
        } else {
            // Out of mana, release immediately
            player.releaseUsingItem();
            player.displayClientMessage(Component.literal("§c魔力不足!"), true);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        super.releaseUsing(stack, level, livingEntity, timeCharged);
        
        if (!level.isClientSide && livingEntity instanceof Player player) {
            // Self-destruct logic
            // Only if charged enough? User didn't specify condition, just "after use".
            // Assuming any use triggers it, or maybe only significant use.
            // Let's assume any successful slash triggers it.
            
            // Wait, super.releaseUsing checks if charge > 0.
            int useDuration = getUseDuration(stack, livingEntity) - timeCharged;
            int charge = useDuration;
            if (charge > 100) charge = 100;

            if (charge > 60) {
                // Explode
                level.explode(null, player.getX(), player.getY(), player.getZ(), 10.0f, true, ExplosionInteraction.TNT);
                // Kill player
                if (!player.isCreative()) {
                    player.kill();
                }
            }
        }
    }
}
