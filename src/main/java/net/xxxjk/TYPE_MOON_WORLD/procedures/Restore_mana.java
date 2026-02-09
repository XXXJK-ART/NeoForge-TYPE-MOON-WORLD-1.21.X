package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

import javax.annotation.Nullable;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber
public class Restore_mana {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        execute(event, event.getEntity().level(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Delay slightly to ensure player entity is fully initialized in the world
        TYPE_MOON_WORLD.queueServerWork(20, () -> {
            execute(event, event.getEntity().level(), event.getEntity());
        });
    }

    public static void execute(LevelAccessor world, Entity entity) {
        execute(null, world, entity);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
        if (entity == null)
            return;
        if (entity.isAlive()) {
            TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            double manaRegen = _vars.player_mana_egenerated_every_moment;
            
            // 基础回魔间隔（tick）
            double regenInterval = _vars.player_restore_magic_moment;

            if (_vars.is_magic_circuit_open) {
                // 开启魔术回路时，回魔间隔减半（速度翻倍）
                regenInterval /= 2;
                
                // 确保间隔至少为 1 tick
                if (regenInterval < 1.0) regenInterval = 1.0;
                
                // 每次回魔调用时增加计时器（因为现在调用频率可能变了，但这里逻辑需要稍微调整，见下文分析）
                // 实际上 execute 是被 queueServerWork 调用的，delay 决定了频率。
                // 所以我们只需要在这里计算 delay 即可。
                
                // 计时器累加逻辑：需要更精确的 tick 计数，或者近似处理。
                // 既然 execute 是每隔 delay tick 运行一次，那么计时器加上 delay 即可。
                _vars.magic_circuit_open_timer += regenInterval; 
                
                // 72000 ticks = 3 days. Check if timer exceeded limit
                if (_vars.magic_circuit_open_timer >= 72000) {
                     _vars.is_magic_circuit_open = false;
                     _vars.magic_circuit_open_timer = 0;
                     if (entity instanceof LivingEntity _entity) {
                         _entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 1)); // Nausea
                         _entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1)); // Weakness
                         _entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, 1)); // Mining Fatigue
                     }
                     if (entity instanceof Player _player && !_player.level().isClientSide()) {
                         _player.displayClientMessage(Component.literal("魔术回路因过载而强制关闭！"), true);
                     }
                }
            } else {
                _vars.magic_circuit_open_timer = 0;
            }

            // Mana Overload Mechanic
            double maxMana = _vars.player_max_mana;
            double currentMana = _vars.player_mana;
            
            if (currentMana > maxMana) {
                // Overload: No natural regeneration
                // Warning logic
                if (entity instanceof Player _player && !_player.level().isClientSide()) {
                    String color = "\u00A7e"; // Yellow
                    if (currentMana > maxMana * 1.25) {
                         color = "\u00A74"; // Dark Red
                    } else if (currentMana > maxMana * 1.2) {
                         color = "\u00A7c"; // Red
                    }
                    
                    _player.displayClientMessage(Component.literal(color + "警告：魔力过载！ (" + (int)currentMana + "/" + (int)maxMana + ")"), true);
                }

                // Explosion logic (130%)
                if (currentMana > maxMana * 1.3) {
                     if (entity instanceof LivingEntity _living) {
                         // Explode/Die
                         _living.hurt(_living.damageSources().magic(), Float.MAX_VALUE);
                         if (entity instanceof Player _player) {
                             _player.sendSystemMessage(Component.literal("\u00A74你因无法承受过量的魔力而爆体身亡！"));
                         }
                     }
                }
            } else {
                // Normal Regeneration
                _vars.player_mana = Math.min(_vars.player_mana + manaRegen, _vars.player_max_mana);
            }
            
            // Cooldown Logic
            if (_vars.magic_cooldown > 0) {
                // Decrease cooldown by the elapsed time (approx regenInterval)
                // Since this loop runs every 'delay' ticks, we subtract 'delay'
                _vars.magic_cooldown = Math.max(0, _vars.magic_cooldown - regenInterval);
            }
            
            _vars.syncMana(entity);

            if (entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana < 20) {
                if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) > 2) {
                    {
                        TypeMoonWorldModVariables.PlayerVariables _varsInner = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                        _varsInner.player_mana = Math.min(entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana
                                + 20, entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_max_mana);
                        _varsInner.syncMana(entity);
                    }
                    LivingEntity _entity = (LivingEntity) entity;
                    _entity.setHealth((float) (_entity.getHealth() - 2));
                } else {
                    if (entity instanceof Player _player && !_player.level().isClientSide())
                        _player.displayClientMessage(Component.literal("生命力已耗尽......"), true);
                }
            }

            int delay = (int) regenInterval;
            if (delay < 1) { // 允许更快的频率，只要不小于1
                delay = 1;
            }
            TYPE_MOON_WORLD.queueServerWork(delay, () -> {
                Restore_mana.execute(world, entity);
            });
        }
    }
}
