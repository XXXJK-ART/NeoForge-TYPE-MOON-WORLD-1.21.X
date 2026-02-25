package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.BlockParticleOption;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.damagesource.DamageTypes;
import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

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
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getGameTime() % 200 != 0) return;
        java.util.List<ServerPlayer> players = serverLevel.players();
        if (players.isEmpty()) return;
        if (serverLevel.random.nextFloat() >= 0.02F) return;

        ServerPlayer ref = players.get(serverLevel.random.nextInt(players.size()));
        net.minecraft.core.BlockPos center = ref.blockPosition();
        var biome = serverLevel.getBiome(center);
        if (!biome.is(Biomes.CHERRY_GROVE) && !biome.is(Biomes.MEADOW)) return;
        int radius = 20;

        double checkR = 64.0D;
        AABB box = new AABB(center.getX() - checkR, center.getY() - 6.0D, center.getZ() - checkR, center.getX() + checkR, center.getY() + 6.0D, center.getZ() + checkR);
        java.util.List<MerlinEntity> merlins = serverLevel.getEntitiesOfClass(MerlinEntity.class, box, MerlinEntity::isAlive);
        if (!merlins.isEmpty()) return;

        net.minecraft.core.BlockPos spawnPos = null;
        for (int i = 0; i < 20 && spawnPos == null; i++) {
            int dx = serverLevel.random.nextInt(radius * 2 + 1) - radius;
            int dz = serverLevel.random.nextInt(radius * 2 + 1) - radius;
            net.minecraft.core.BlockPos trial = center.offset(dx, 0, dz);
            net.minecraft.core.BlockPos top = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, trial);
            if (!serverLevel.getBlockState(top).isAir()) continue;
            BlockState below = serverLevel.getBlockState(top.below());
            if (!below.isAir()) {
                spawnPos = top;
            }
        }
        if (spawnPos == null) return;

        MerlinEntity merlin = ModEntities.MERLIN.get().create(serverLevel);
        if (merlin == null) return;
        merlin.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, ref.getYRot(), ref.getXRot());
        serverLevel.addFreshEntity(merlin);
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

        if (player instanceof ServerPlayer serverPlayer) {
            boolean isSleeping = player.isSleeping();
            var tag = player.getPersistentData();
            boolean prevSleeping = tag.getBoolean("TypeMoonMerlinPrevSleeping");

            TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            int favor = vars.merlin_favor;

            if (isSleeping && !prevSleeping) {
                if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                    float chance = 0.15F;
                    if (serverLevel.random.nextFloat() < chance) {
                        if (favor <= -1) {
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 0));
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
                            Component msg = Component.translatable("entity.typemoonworld.merlin.speech.sleep_negative");
                            serverPlayer.displayClientMessage(msg, false);
                        } else if (favor >= 3) {
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
                            Component msg = Component.translatable("entity.typemoonworld.merlin.speech.sleep_positive");
                            serverPlayer.displayClientMessage(msg, false);
                        }
                    }
                }
            }

            tag.putBoolean("TypeMoonMerlinPrevSleeping", isSleeping);

            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                if (favor < 0) {
                    if (serverLevel.random.nextInt(2000) == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                        Component msg = Component.translatable("entity.typemoonworld.merlin.speech.prank_negative");
                        serverPlayer.displayClientMessage(msg, false);
                    }
                } else if (favor > 0 && favor < 3) {
                    if (serverLevel.random.nextInt(2600) == 0) {
                        double baseY = player.getY() + 0.8D + player.level().random.nextDouble() * 0.8D;
                        serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                                player.getX(), baseY, player.getZ(),
                                24, 0.8D, 0.6D, 0.8D, 0.02D);
                        Component msg = Component.translatable("entity.typemoonworld.merlin.speech.prank_positive");
                        serverPlayer.displayClientMessage(msg, false);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        // Reinforcement (Leg): halve fall damage while agility reinforcement effect is active.
        if (event.getSource().is(DamageTypes.FALL) && event.getEntity() instanceof LivingEntity living) {
            if (living.hasEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY) || living.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY)) {
                event.setAmount(event.getAmount() * 0.5F);
            }
        }
        
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.hasEffect(ModMobEffects.NINE_LIVES)) {
                player.removeEffect(ModMobEffects.NINE_LIVES);

                if (player.level() instanceof ServerLevel serverLevel) {
                }
                
                LivingEntity target = event.getEntity();
                double baseDamageAttr = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float baseDamage = (float)baseDamageAttr;
                
                triggerNineLives(player, target, baseDamage);
            }

            if (event.getEntity() instanceof LivingEntity && player.level() instanceof ServerLevel serverLevel) {
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                if (vars.merlin_favor >= 3) {
                    if (serverLevel.random.nextFloat() < 0.05F) {
                        int duration = 1200;
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, true, true));
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, true));

                        double radius = 24.0D;
                        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                                player.getX() - radius, player.getY() - 4.0D, player.getZ() - radius,
                                player.getX() + radius, player.getY() + 4.0D, player.getZ() + radius
                        );
                        java.util.List<MerlinEntity> merlins = serverLevel.getEntitiesOfClass(MerlinEntity.class, box, MerlinEntity::isAlive);
                        if (!merlins.isEmpty()) {
                            net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.translatable("entity.typemoonworld.merlin.speech.buff_support_high");
                            for (ServerPlayer sp : serverLevel.players()) {
                                if (sp.distanceToSqr(player) <= radius * radius) {
                                    sp.displayClientMessage(message, false);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (event.getEntity() instanceof Player damagedPlayer && damagedPlayer.level() instanceof ServerLevel serverLevel) {
            TypeMoonWorldModVariables.PlayerVariables vars = damagedPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.merlin_favor >= 5) {
                if (event.getSource().getEntity() instanceof Player attacker) {
                    TypeMoonWorldModVariables.PlayerVariables attackerVars = attacker.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    if (attackerVars.merlin_favor >= 3) {
                        return;
                    }
                }
                float maxHealth = damagedPlayer.getMaxHealth();
                if (damagedPlayer.getHealth() <= maxHealth * 0.5F) {
                    if (serverLevel.random.nextFloat() >= 0.15F) {
                        return;
                    }
                    double radius = 24.0D;
                    net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                            damagedPlayer.getX() - radius, damagedPlayer.getY() - 4.0D, damagedPlayer.getZ() - radius,
                            damagedPlayer.getX() + radius, damagedPlayer.getY() + 4.0D, damagedPlayer.getZ() + radius
                    );

                    java.util.List<MerlinEntity> merlins = serverLevel.getEntitiesOfClass(MerlinEntity.class, box, MerlinEntity::isAlive);
                    MerlinEntity helper = merlins.isEmpty() ? null : merlins.get(0);

                    if (helper == null) {
                        helper = ModEntities.MERLIN.get().create(serverLevel);
                        if (helper != null) {
                            double dx = (serverLevel.random.nextDouble() - 0.5D) * 2.0D;
                            double dz = (serverLevel.random.nextDouble() - 0.5D) * 2.0D;
                            helper.moveTo(damagedPlayer.getX() + dx, damagedPlayer.getY(), damagedPlayer.getZ() + dz, damagedPlayer.getYRot(), damagedPlayer.getXRot());
                            if (helper.getAttribute(Attributes.MAX_HEALTH) != null) {
                                helper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
                            }
                            helper.setHealth(200.0F);
                            var htag = helper.getPersistentData();
                            htag.putBoolean("TypeMoonHelperClone", true);
                            htag.putString("TypeMoonHelperOwner", damagedPlayer.getUUID().toString());
                            serverLevel.addFreshEntity(helper);
                        }
                    }

                    if (helper != null) {
                        var htag = helper.getPersistentData();
                        if (!htag.getBoolean("TypeMoonHelperClone")) {
                            htag.putBoolean("TypeMoonSummonedFull", true);
                        }
                        net.minecraft.world.phys.AABB mobBox = new net.minecraft.world.phys.AABB(
                                damagedPlayer.getX() - radius, damagedPlayer.getY() - 4.0D, damagedPlayer.getZ() - radius,
                                damagedPlayer.getX() + radius, damagedPlayer.getY() + 4.0D, damagedPlayer.getZ() + radius
                        );
                        java.util.List<Monster> mobs = serverLevel.getEntitiesOfClass(Monster.class, mobBox, m -> m.getTarget() == damagedPlayer);
                        for (Monster mob : mobs) {
                            mob.setTarget(helper);
                        }

                        net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.translatable("entity.typemoonworld.merlin.speech.guard_summon");
                        for (ServerPlayer sp : serverLevel.players()) {
                            if (sp.distanceToSqr(damagedPlayer) <= radius * radius) {
                                sp.displayClientMessage(message, false);
                            }
                        }
                    }
                }
            }
        }

        if (event.getSource().getEntity() instanceof MerlinEntity merlin && event.getEntity() instanceof Monster mob) {
            if (mob.getTarget() == null || mob.getTarget() != merlin) {
                mob.setTarget(merlin);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (event.getSource().getEntity() instanceof Player player && event.getEntity() instanceof Monster mob) {
            LivingEntity target = mob.getTarget();
            if (target instanceof MerlinEntity merlin && merlin.isAlive() && player.level() instanceof ServerLevel) {
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                if (vars.merlin_favor < 5) {
                    vars.merlin_favor = Math.min(5, vars.merlin_favor + 1);
                    vars.syncPlayerVariables(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity e = event.getEntity();
        if (!(e instanceof RyougiShikiEntity)) return;
        RemovalReason reason = e.getRemovalReason();
        if (reason == null) return;
        // Prevent removal in the listed reasons
        if (reason == RemovalReason.KILLED
                || reason == RemovalReason.DISCARDED
                || reason == RemovalReason.UNLOADED_TO_CHUNK
                || reason == RemovalReason.UNLOADED_WITH_PLAYER
                || reason == RemovalReason.CHANGED_DIMENSION) {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                speakNearby(serverLevel, e, "entity.typemoonworld.ryougi_shiki.speech.refused", 20.0);
            }
            if (e instanceof RyougiShikiEntity shiki) {
                shiki.setPersistenceRequired();
                shiki.invulnerableTime = 0;
            }
        }
    }

    private static void speakNearby(ServerLevel level, Entity center, String key, double radius) {
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, center.getBoundingBox().inflate(radius));
        for (ServerPlayer p : players) {
            p.displayClientMessage(Component.translatable(key), false);
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
        final Vec3[] lastCenter = new Vec3[] { new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ()) };
        for (int i = 1; i <= 8; i++) {
            final int tickDelay = i * 2;
            TYPE_MOON_WORLD.queueServerWork(tickDelay, () -> {
                if (player.level() instanceof ServerLevel serverLevel) {
                    Vec3 viewDir = player.getLookAngle().normalize();
                    Vec3 upDir = new Vec3(0, 1, 0);
                    Vec3 rightDir = viewDir.cross(upDir).normalize();
                    if (rightDir.lengthSqr() < 0.01) rightDir = new Vec3(1, 0, 0);
                    Vec3 planeUp = rightDir.cross(viewDir).normalize();
                    float ts = Math.max(target.getBbWidth(), target.getBbHeight());
                    double angle = serverLevel.random.nextDouble() * 2 * Math.PI;
                    double dist = ts * 1.5 + 4.0;
                    double cxOff = (serverLevel.random.nextDouble() - 0.5) * target.getBbWidth() * 0.8;
                    double cyOff = (serverLevel.random.nextDouble() - 0.5) * target.getBbHeight() * 0.8;
                    Vec3 center = target.isAlive()
                            ? new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ()).add(rightDir.scale(cxOff)).add(planeUp.scale(cyOff))
                            : lastCenter[0];
                    lastCenter[0] = center;
                    serverLevel.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 10, 0.5, 0.5, 0.5, 0.5);
                    serverLevel.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 3, 0.3, 0.3, 0.3, 0.05);
                    player.level().playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
                    Vec3 slashDir = rightDir.scale(Math.cos(angle)).add(planeUp.scale(Math.sin(angle))).normalize();
                    Vec3 startPos = center.add(slashDir.scale(-dist));
                    Vec3 endPos = center.add(slashDir.scale(dist));
                    RubyProjectileEntity slash = new RubyProjectileEntity(serverLevel, startPos.x, startPos.y, startPos.z);
                    slash.setItem(ItemStack.EMPTY);
                    slash.setGemType(99);
                    slash.setVisualScale(Math.max(0.5f, ts / 1.8f));
                    slash.setVisualEnd(endPos);
                    slash.setNoGravity(true);
                    slash.setDeltaMovement(Vec3.ZERO);
                    serverLevel.addFreshEntity(slash);
                    double radius = ts * 0.75 + 2.0;
                    net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
                    List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player);
                    for (LivingEntity e : victims) {
                        e.invulnerableTime = 0;
                        e.hurt(player.damageSources().mobAttack(player), damageBase * 2.0F);
                        e.invulnerableTime = 0;
                    }
                }
            });
        }
        TYPE_MOON_WORLD.queueServerWork(26, () -> {
            if (player.level() instanceof ServerLevel serverLevel) {
                Vec3 viewDir = player.getLookAngle().normalize();
                Vec3 upDir = new Vec3(0, 1, 0);
                Vec3 rightDir = viewDir.cross(upDir).normalize();
                if (rightDir.lengthSqr() < 0.01) rightDir = new Vec3(1, 0, 0);
                Vec3 planeUp = rightDir.cross(viewDir).normalize();
                float ts = Math.max(target.getBbWidth(), target.getBbHeight());
                double angle = serverLevel.random.nextDouble() * 2 * Math.PI;
                double dist = ts * 1.5 + 4.0;
                Vec3 center = lastCenter[0];
                Vec3 slashDir = rightDir.scale(Math.cos(angle)).add(planeUp.scale(Math.sin(angle))).normalize();
                Vec3 startPos = center.add(slashDir.scale(-dist));
                Vec3 endPos = center.add(slashDir.scale(dist));
                RubyProjectileEntity slash = new RubyProjectileEntity(serverLevel, startPos.x, startPos.y, startPos.z);
                slash.setItem(ItemStack.EMPTY);
                slash.setGemType(99);
                slash.setVisualScale(Math.max(0.5f, ts / 1.8f));
                slash.setVisualEnd(endPos);
                slash.setNoGravity(true);
                slash.setDeltaMovement(Vec3.ZERO);
                serverLevel.addFreshEntity(slash);
                double radius = ts * 0.75 + 2.0;
                net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
                List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && (!(e instanceof net.minecraft.world.entity.player.Player p) || !p.isCreative()));
                for (LivingEntity e : victims) {
                    e.invulnerableTime = 0;
                    e.hurt(player.damageSources().mobAttack(player), damageBase * 5.0F);
                    e.invulnerableTime = 0;
                }
            }
        });
    }
}
