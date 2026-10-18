package com.captain.servant.emiya;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.captain.servant.mixin.accessor.UBWBrokenPhantasmExplosionAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.MagicBrokenPhantasm;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardEmiyaSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;

/** Compatibility rules for the Emiya servant-card loadout. */
public final class EmiyaSupportService {
    private static final String EMIYA_ARCHER_ID = "emiya_archer";
    private static final double EXCALIBUR_COPY_MANA_COST = 600.0D;
    private static final ResourceLocation EXCALIBUR_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "excalibur");
    private static final ResourceLocation GILGAMESH_KEY_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "gilgamesh_bab_ilu");
    private static final ResourceLocation GILGAMESH_EA_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "gilgamesh_ea");
    private static final int EXCALIBUR_WAVE_COUNT = 26;
    private static final int EXCALIBUR_TERRAIN_SHELL_COUNT = 12;
    private static final float EXCALIBUR_CENTER_DAMAGE = 2000.0F;
    private static final float EXCALIBUR_EDGE_DAMAGE = 500.0F;
    private static final float EXCALIBUR_TERRAIN_HARDNESS = 32.0F;

    private EmiyaSupportService() {
    }

    public static boolean shouldUseNormalProjection(ServerPlayer player) {
        return isEmiya(player)
                && isNamelessBow(player.getMainHandItem())
                && isKanshouBakuya(player.getOffhandItem());
    }

    /** Returns null when the base copy action should continue. */
    public static Boolean handleCopy(ServerPlayer player) {
        if (!isEmiya(player)) {
            return null;
        }

        if (ServantCardEmiyaSkills.findCrossSlashCopyTarget(player, 40.0D) != null) {
            return null;
        }

        LivingEntity target = ServantCardEmiyaSkills.findCopyableWeaponTarget(player, 20.0D, 1.6D);
        if (target == null) {
            return null;
        }

        ItemStack source = copiedWeapon(target);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(source.getItem());
        if (isForbiddenCopy(id)) {
            player.displayClientMessage(Component.translatable("message.fatesupport.emiya.copy_forbidden"), true);
            return false;
        }
        if (EXCALIBUR_ID.equals(id)
                && !ServantCardManaService.consume(player, variables(player), EXCALIBUR_COPY_MANA_COST)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
            return false;
        }

        completeCopy(player, target, source);
        return true;
    }

    /** Keeps all copied noble phantasms on one rotation path. */
    public static boolean handleCycleLoadout(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables variables) {
        if (!isEmiya(player) || !hasCopiedNoblePhantasms(variables)) {
            return false;
        }

        List<Item> candidates = cycleCandidates(variables);
        Item selected = candidates.get(player.getRandom().nextInt(candidates.size()));
        if (isExcalibur(selected) && !ServantCardManaService.consume(player, variables, EXCALIBUR_COPY_MANA_COST)) {
            candidates.removeIf(EmiyaSupportService::isExcalibur);
            selected = candidates.get(player.getRandom().nextInt(candidates.size()));
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, generatedProjection(ModItems.NAMELESS_BOW.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, generatedProjection(selected));
        return true;
    }

    public static boolean isExcaliburArrowExplosion(Entity owner, ItemStack payload) {
        return owner instanceof ServerPlayer player && isEmiya(player) && isExcalibur(payload.getItem());
    }

    public static void explodeExcaliburArrow(
            ServerLevel level,
            Entity projectile,
            Entity owner,
            Vec3 center
    ) {
        double radius = pseudoSpiralExplosionRadius() * 2.0D;
        DamageSource damageSource = level.damageSources().explosion(projectile, owner);
        VFXServerEffects.spawn(level, "broken_phantasm_explosion", center, Math.min(128.0D, radius + 24.0D));
        VFXServerEffects.spawn(level, "artoria_excalibur_impact", center, 192.0D);
        UBWBrokenPhantasmExplosionAccessor.fatesupport$spawnShellEffects(level, center, radius);
        queueExcaliburTerrain(level, center, radius);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 12, 0.55D, 0.55D, 0.55D, 0.0D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.7F);

        Set<Integer> hitEntityIds = new HashSet<>();
        double waveStep = radius / EXCALIBUR_WAVE_COUNT;
        for (int wave = 1; wave <= EXCALIBUR_WAVE_COUNT; wave++) {
            int scheduledWave = wave;
            TYPE_MOON_WORLD.queueServerWork(scheduledWave * 2, () -> processExcaliburDamageWave(
                    level,
                    center,
                    scheduledWave * waveStep,
                    Math.max(0.0D, (scheduledWave - 1) * waveStep),
                    damageSource,
                    owner,
                    hitEntityIds,
                    radius
            ));
        }
    }

    private static void queueExcaliburTerrain(ServerLevel level, Vec3 center, double radius) {
        double shellStep = radius / EXCALIBUR_TERRAIN_SHELL_COUNT;
        for (int shell = 1; shell <= EXCALIBUR_TERRAIN_SHELL_COUNT; shell++) {
            int scheduledShell = shell;
            TYPE_MOON_WORLD.queueServerWork(scheduledShell * 2, () -> DeferredTerrainDestruction.queueShell(
                    level,
                    center,
                    scheduledShell * shellStep,
                    Math.max(0.0D, (scheduledShell - 1) * shellStep),
                    1,
                    (serverLevel, position, distanceSquared, totalRadius, explosionCenter) -> canDestroyExcaliburTerrain(serverLevel, position),
                    null
            ));
        }
    }

    private static boolean canDestroyExcaliburTerrain(ServerLevel level, BlockPos position) {
        BlockState state = level.getBlockState(position);
        float hardness = state.getDestroySpeed(level, position);
        return !state.isAir()
                && hardness >= 0.0F
                && hardness <= EXCALIBUR_TERRAIN_HARDNESS
                && !state.is(Blocks.BEDROCK)
                && state.getExplosionResistance(level, position, null) < 1200.0F;
    }

    private static void processExcaliburDamageWave(
            ServerLevel level,
            Vec3 center,
            double outerRadius,
            double innerRadius,
            DamageSource damageSource,
            Entity owner,
            Set<Integer> hitEntityIds,
            double totalRadius
    ) {
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(outerRadius),
                candidate -> candidate.isAlive() && candidate != owner && !EntityUtils.isImmunePlayerTarget(candidate)
        )) {
            double distance = target.position().distanceTo(center);
            if (distance > outerRadius || (innerRadius > 0.0D && distance <= innerRadius) || !hitEntityIds.add(target.getId())) {
                continue;
            }

            float rawDamage = (float) Mth.lerp(distance / totalRadius, EXCALIBUR_CENTER_DAMAGE, EXCALIBUR_EDGE_DAMAGE);
            float damage = MagicResistanceHelper.applyNoblePhantasmMagicResistance(target, rawDamage);
            damage = HeraclesGodHandHelper.applyAntiHeraclesNoblePhantasmSpecialAttack(target, damage);
            target.invulnerableTime = 0;
            disableVanillaShield(target, owner);
            target.hurt(damageSource, damage);
            target.invulnerableTime = 0;

            Vec3 horizontalOffset = target.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
            if (horizontalOffset.lengthSqr() > 1.0E-4D) {
                Vec3 knockback = horizontalOffset.normalize();
                target.push(knockback.x * 0.85D, 0.32D, knockback.z * 0.85D);
                target.hurtMarked = true;
            }
        }

        SuperbWarfareLoader.damageVehiclesInAnnulus(
                level,
                center,
                outerRadius,
                innerRadius,
                owner,
                damageSource,
                Mth.lerp((float) (outerRadius / totalRadius), EXCALIBUR_CENTER_DAMAGE, EXCALIBUR_EDGE_DAMAGE),
                hitEntityIds
        );
    }

    private static List<Item> cycleCandidates(TypeMoonWorldModVariables.PlayerVariables variables) {
        List<Item> candidates = new ArrayList<>();
        candidates.add(ModItems.CRIMSON_HOUND.get());
        candidates.add(ModItems.PSEUDO_SPIRAL_SWORD.get());

        String copied = variables.servant_card_emiya_copied_noble_phantasms;
        if (copied == null || copied.isBlank()) {
            return candidates;
        }

        for (String rawId : copied.split(",")) {
            ResourceLocation id = ResourceLocation.tryParse(rawId.trim());
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(id);
            if (!isForbiddenCopy(id) && item instanceof NoblePhantasmItem && !candidates.contains(item)) {
                candidates.add(item);
            }
        }
        return candidates;
    }

    private static ItemStack generatedProjection(Item item) {
        ItemStack stack = new ItemStack(item);
        PlayerNoblePhantasmHelper.markUbwProjection(stack);
        ServantCardTransformManager.markGeneratedItem(stack, true, true);
        return stack;
    }

    public static boolean hurtBrokenPhantasmTarget(LivingEntity target, DamageSource damageSource, float damage) {
        disableVanillaShield(target, damageSource.getEntity());
        return target.hurt(damageSource, damage);
    }

    private static boolean hasCopiedNoblePhantasms(TypeMoonWorldModVariables.PlayerVariables variables) {
        String copied = variables.servant_card_emiya_copied_noble_phantasms;
        return copied != null && !copied.isBlank();
    }

    private static void completeCopy(ServerPlayer player, LivingEntity target, ItemStack source) {
        ItemStack copied = source.copy();
        copied.setCount(1);
        PlayerNoblePhantasmHelper.markUbwProjection(copied);
        PlayerNoblePhantasmHelper.markServantCardCopiedNoblePhantasm(copied);
        ServantCardTransformManager.markGeneratedItem(copied, true, true);

        TypeMoonWorldModVariables.PlayerVariables variables = variables(player);
        ResourceLocation copiedId = BuiltInRegistries.ITEM.getKey(copied.getItem());
        if (copiedId != null) {
            LinkedHashSet<String> copiedIds = copiedNoblePhantasmIds(variables);
            copiedIds.add(copiedId.toString());
            variables.servant_card_emiya_copied_noble_phantasms = String.join(",", copiedIds);
            variables.syncPlayerVariables(player);
        }

        if (copied.is(ModItems.BIZEN_NAGAMITSU.get())) {
            PlayerNoblePhantasmHelper.armTsubameAfterAnalysis(player, copied);
        } else if (copied.is(ModItems.TEMPLE_STONE_SWORD_AXE.get())) {
            PlayerNoblePhantasmHelper.armNineLives(copied);
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.emiya_nine_lives_ready"), true);
        }

        player.setItemInHand(InteractionHand.OFF_HAND, copied);

        int attackAmplifier = target.getAttributeValue(Attributes.ATTACK_DAMAGE)
                >= player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 8.0D ? 1 : 0;
        int speedAmplifier = target.getAttributeValue(Attributes.MOVEMENT_SPEED)
                > player.getAttributeValue(Attributes.MOVEMENT_SPEED) ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 220, attackAmplifier, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, speedAmplifier, false, true, true));
        if (target.getMaxHealth() > player.getMaxHealth()) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 220, 1, false, true, true));
        }
    }

    private static ItemStack copiedWeapon(LivingEntity target) {
        return target.getMainHandItem().getItem() instanceof NoblePhantasmItem
                ? target.getMainHandItem()
                : target.getOffhandItem();
    }

    private static LinkedHashSet<String> copiedNoblePhantasmIds(TypeMoonWorldModVariables.PlayerVariables variables) {
        LinkedHashSet<String> copiedIds = new LinkedHashSet<>();
        String copied = variables.servant_card_emiya_copied_noble_phantasms;
        if (copied == null || copied.isBlank()) {
            return copiedIds;
        }

        for (String rawId : copied.split(",")) {
            if (!rawId.isBlank()) {
                copiedIds.add(rawId.trim());
            }
        }
        return copiedIds;
    }

    private static boolean isForbiddenCopy(ResourceLocation id) {
        return GILGAMESH_KEY_ID.equals(id) || GILGAMESH_EA_ID.equals(id);
    }

    private static boolean isEmiya(ServerPlayer player) {
        return player != null && player.isAlive() && variables(player).servant_card_transformed
                && EMIYA_ARCHER_ID.equals(variables(player).servant_card_id);
    }

    private static boolean isNamelessBow(ItemStack stack) {
        return stack.is(ModItems.NAMELESS_BOW.get());
    }

    private static boolean isKanshouBakuya(ItemStack stack) {
        return stack.is(ModItems.GAN_JIANG.get())
                || stack.is(ModItems.MO_YE.get())
                || stack.is(ModItems.GAN_JIANG_OVEREDGE.get())
                || stack.is(ModItems.MO_YE_OVEREDGE.get());
    }

    private static boolean isExcalibur(Item item) {
        return item != null && EXCALIBUR_ID.equals(BuiltInRegistries.ITEM.getKey(item));
    }

    private static void disableVanillaShield(LivingEntity target, Entity attacker) {
        if (attacker instanceof ServerPlayer player
                && isEmiya(player)
                && target.isUsingItem()
                && target.getUseItem().is(net.minecraft.world.item.Items.SHIELD)) {
            target.stopUsingItem();
        }
    }

    private static double pseudoSpiralExplosionRadius() {
        float power = (float) (MagicBrokenPhantasm.calculateCost(
                new ItemStack(ModItems.PSEUDO_SPIRAL_SWORD.get()),
                false
        ) / 20.0D);
        power = Math.max(2.0F, power);
        return Mth.clamp(Math.min(power, 6.0F) * 4.2D * 2.0D, 8.0D, 25.0D);
    }

    private static TypeMoonWorldModVariables.PlayerVariables variables(Player player) {
        return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
    }
}
