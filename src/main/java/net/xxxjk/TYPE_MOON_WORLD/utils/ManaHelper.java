
package net.xxxjk.TYPE_MOON_WORLD.utils;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class ManaHelper {
    private static final String NO_EMERGENCY_RESTORE_UNTIL = "TypeMoonNoEmergencyRestoreUntil";

    /**
     * Attempts to consume mana. If mana is insufficient, consumes health instead.
     * 
     * @param player The player attempting to use magic.
     * @param amount The amount of mana required.
     * @return true if the cost was paid (either by mana or health), false if the player died or couldn't pay.
     */
    public static boolean consumeManaOrHealth(ServerPlayer player, double amount) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        // If not awakened, cannot use mana/health conversion
        if (!vars.is_magus) {
            return false;
        }
        
        if (vars.player_mana >= amount) {
            vars.player_mana -= amount;
            vars.syncMana(player);
            return true;
        } else {
            // Not enough mana, check health
            double missingMana = amount - vars.player_mana;
            // Conversion rate: 1 Health (0.5 Hearts) = 10 Mana? Or 1:1?
            // Usually magic mods do something like 1 Health = X Mana.
            // Let's assume a "Life Force" conversion.
            // For now, let's say 1 Health Point = 20 Mana. (So half a heart = 20 mana)
            // Or maybe simpler: 1 Health = 10 Mana.
            // Let's stick to a configurable-like constant: 1 HP = 10 Mana.
            double healthCost = missingMana / 10.0;
            
            // If cost is less than 1 HP (e.g. 5 mana missing = 0.5 HP), it still hurts.
            if (healthCost < 0.5) healthCost = 0.5; 
            
            if (player.getHealth() > healthCost) {
                // Consume all remaining mana first
                vars.player_mana = 0;
                vars.syncMana(player);
                
                // Damage player
                // We need a damage source. MAGIC is appropriate.
                DamageSource source = player.damageSources().magic();
                player.hurt(source, (float)healthCost);
                
                player.displayClientMessage(Component.translatable("message.typemoonworld.mana.health_conversion", String.format("%.1f", healthCost), (int)amount), true);
                return true;
            } else {
                // Not enough health either (would die)
                // Do we allow suicide? Usually no for casting unless specified.
                player.displayClientMessage(Component.translatable("message.typemoonworld.mana.not_enough_health"), true);
                return false;
            }
        }
    }

    /**
     * Attempts to consume mana, then inventory emergency sources, then health.
     * This is intended for projection/analysis where emergency refill sources should be allowed.
     */
    public static boolean consumeManaWithInventoryOrHealth(ServerPlayer player, double amount) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.is_magus) {
            return false;
        }

        if (vars.player_mana >= amount) {
            vars.player_mana -= amount;
            vars.syncMana(player);
            return true;
        }

        refillFromInventoryForCost(player, vars, amount);
        if (vars.player_mana >= amount) {
            vars.player_mana -= amount;
            vars.syncMana(player);
            return true;
        }

        return consumeManaOrHealth(player, amount);
    }

    /**
     * Strict mana consume: no health conversion.
     *
     * @param drainOnFail if true, drains current mana to 0 when insufficient.
     */
    public static boolean consumeManaStrict(ServerPlayer player, double amount, boolean drainOnFail) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.is_magus) {
            return false;
        }
        if (vars.player_mana >= amount) {
            vars.player_mana -= amount;
            vars.syncMana(player);
            return true;
        }

        if (drainOnFail && vars.player_mana > 0) {
            vars.player_mana = 0;
            vars.syncMana(player);
        }
        suppressEmergencyManaRestore(player, 40);
        return false;
    }

    public static void suppressEmergencyManaRestore(ServerPlayer player, int ticks) {
        long now = player.level().getGameTime();
        long until = now + Math.max(1, ticks);
        player.getPersistentData().putLong(NO_EMERGENCY_RESTORE_UNTIL, until);
    }

    public static boolean isEmergencyManaRestoreSuppressed(Entity entity) {
        if (entity == null) return false;
        long now = entity.level().getGameTime();
        long until = entity.getPersistentData().getLong(NO_EMERGENCY_RESTORE_UNTIL);
        return until > now;
    }

    private static final double MAGIC_FRAGMENT_MANA = 10.0;
    private static final double SPIRIT_VEIN_BLOCK_MANA = 90.0;

    private static void refillFromInventoryForCost(Player player, TypeMoonWorldModVariables.PlayerVariables vars, double requiredCost) {
        double need = requiredCost - vars.player_mana;
        while (need > 0.0001D) {
            SourceCandidate best = findBestCandidate(player, need);
            if (best == null) {
                return;
            }
            consumeCandidate(player, best);
            vars.player_mana = Math.min(vars.player_mana + best.mana, vars.player_max_mana);
            need = requiredCost - vars.player_mana;
        }
    }

    private static SourceCandidate findBestCandidate(Player player, double need) {
        SourceCandidate best = null;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            SourceCandidate candidate = createCandidate(i, stack);
            if (candidate == null) continue;

            if (best == null) {
                best = candidate;
                continue;
            }
            boolean candidateEnough = candidate.mana >= need;
            boolean bestEnough = best.mana >= need;
            if (candidateEnough && !bestEnough) {
                best = candidate;
                continue;
            }
            if (candidateEnough && bestEnough && candidate.mana < best.mana) {
                best = candidate;
                continue;
            }
            if (!candidateEnough && !bestEnough && candidate.mana > best.mana) {
                best = candidate;
            }
        }
        return best;
    }

    private static SourceCandidate createCandidate(int slot, ItemStack stack) {
        if (stack.getItem() instanceof FullManaCarvedGemItem fullGemItem) {
            return new SourceCandidate(slot, fullGemItem.getManaAmount(), new ItemStack(fullGemItem.getEmptyGemItem()));
        }
        if (stack.is(ModItems.MAGIC_FRAGMENTS.get())) {
            return new SourceCandidate(slot, MAGIC_FRAGMENT_MANA, ItemStack.EMPTY);
        }
        if (stack.is(ModBlocks.SPIRIT_VEIN_BLOCK.get().asItem())) {
            return new SourceCandidate(slot, SPIRIT_VEIN_BLOCK_MANA, ItemStack.EMPTY);
        }
        return null;
    }

    private static void consumeCandidate(Player player, SourceCandidate candidate) {
        ItemStack stack = player.getInventory().getItem(candidate.slot);
        if (stack.isEmpty()) return;

        if (stack.getCount() > 1) {
            stack.shrink(1);
            if (!candidate.remainder.isEmpty()) {
                ItemStack remainderCopy = candidate.remainder.copy();
                if (!player.getInventory().add(remainderCopy)) {
                    player.drop(remainderCopy, false);
                }
            }
        } else {
            if (candidate.remainder.isEmpty()) {
                player.getInventory().setItem(candidate.slot, ItemStack.EMPTY);
            } else {
                player.getInventory().setItem(candidate.slot, candidate.remainder.copy());
            }
        }
        player.getInventory().setChanged();
    }

    private record SourceCandidate(int slot, double mana, ItemStack remainder) {
    }
}
