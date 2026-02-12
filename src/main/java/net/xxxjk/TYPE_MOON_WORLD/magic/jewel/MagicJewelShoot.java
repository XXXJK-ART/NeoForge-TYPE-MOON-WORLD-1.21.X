package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby.MagicRubyThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire.MagicSapphireThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald.MagicEmeraldUse;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz.MagicTopazThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.cyan.MagicCyanThrow;

public class MagicJewelShoot {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        int mode = vars.jewel_magic_mode;
        
        switch (mode) {
            case 0: // Ruby
                MagicRubyThrow.execute(player);
                break;
            case 1: // Sapphire
                MagicSapphireThrow.execute(player);
                break;
            case 2: // Emerald
                MagicEmeraldUse.execute(player);
                break;
            case 3: // Topaz
                MagicTopazThrow.execute(player);
                break;
            case 4: // Cyan
                MagicCyanThrow.execute(player);
                break;
            case 5: // Random
                executeRandom(player);
                break;
            default:
                MagicRubyThrow.execute(player);
                break;
        }
    }
    
    private static void executeRandom(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        // Randomly pick one of the 4 basic magics
        // 0: Ruby, 1: Sapphire, 2: Emerald, 3: Topaz
        int randomType = player.getRandom().nextInt(4);
        
        // Adjust power based on type (Ruby is strongest)
        // This is handled inside the individual execute methods or projectiles.
        // But the request says: "Random throw, explosion based on mana, non-ruby weaker than ruby".
        // MagicRubyThrow explodes. Others don't usually explode (Sapphire freezes, Topaz damages).
        // If "Random Throw" implies a NEW effect that just throws a gem and explodes:
        
        // Let's implement a generic "Random Gem Throw" that always explodes but with varying power/color.
        // OR just invoke the existing ones?
        // "Random throw gems, explode based on mana amount, non-ruby weaker than ruby."
        // This sounds like a new custom effect.
        
        // Let's reuse MagicRubyThrow logic but modify the projectile or explosion power?
        // Existing classes are static void execute(Entity). Hard to modify parameters.
        
        // We will create a custom implementation here for the "Random Mode".
        // It consumes ANY gem? Or consumes nothing? Or consumes a random gem from inventory?
        // "Randomly throw gems". Let's assume it consumes 1 gem of random type from inventory, or just consumes mana?
        // Usually Jewel Magic consumes Jewels.
        // Let's assume it consumes a "Random" gem (any gem) and throws it.
        // Or maybe it's a "Gambler's Throw" - consumes mana to generate a temporary gem?
        // Given "Explosion based on mana", it might be a pure mana conversion.
        
        // Let's stick to the prompt: "Random throw gems, explode based on mana".
        // We'll invoke a modified RubyThrow logic.
        
        // Actually, let's just pick a random EXISTING skill for now, as implementing a full new projectile entity/logic is complex without new files.
        // BUT the prompt says "non-ruby weaker than ruby".
        // Existing Ruby Throw: Explodes.
        // Existing Sapphire Throw: Freezes (No explosion).
        // So simply picking random existing skills won't satisfy "explode... weaker than ruby".
        
        // So we need a new logic. We can reuse MagicRubyThrow's PROJECTILE but change its data?
        // MagicRubyThrow spawns a RubyProjectileEntity.
        // We can't easily change the entity's logic without modifying the entity class.
        
        // Let's check RubyProjectileEntity.
        // If we can't modify it easily, we will just use Ruby Throw for all but simulate "weaker" by passing lower stats if possible?
        // Projectiles usually have damage/explosion radius hardcoded or based on Item.
        
        // Alternative: Just call the existing methods.
        // Ruby -> Explodes.
        // Others -> Don't explode.
        // This doesn't fit "explode based on mana".
        
        // Let's try to simulate it by spawning a Ruby Projectile but maybe changing its NBT or something?
        // Or just implementing a simple "Throw Item" that explodes on impact.
        
        // For now, to be safe and quick:
        // Randomly select one of 4 types.
        // If Ruby: Normal Ruby Throw.
        // If Others: Throw them, but if they don't explode, well...
        // The prompt specifically asks for "Random throw, explosion based on mana".
        // This implies a specific new behavior.
        
        // Let's invoke MagicRubyThrow for ALL of them, but pretend it's a different gem?
        // No, that's visual mismatch.
        
        // Okay, let's implement a simple "Shoot Projectile" logic here.
        // We need a projectile that explodes.
        // We can use LargeFireball or SmallFireball? Or TNTPrimed?
        // Or just use RubyProjectileEntity for everything but change the item stack to look like other gems?
        
        // Let's try using RubyProjectileEntity but setting the item to Blue/Green/Yellow gem.
        // And if the entity logic checks item for explosion size...
        
        net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby.MagicRubyThrow.executeRandom(player, randomType);
    }
}
