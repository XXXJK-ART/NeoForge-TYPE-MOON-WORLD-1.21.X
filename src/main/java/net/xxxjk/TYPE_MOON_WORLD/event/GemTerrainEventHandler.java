package net.xxxjk.TYPE_MOON_WORLD.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.util.ModTags;
import net.xxxjk.TYPE_MOON_WORLD.world.gem.GemTerrainState;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class GemTerrainEventHandler {
    private static final String KEY_ECHO_CHUNKS = "TypeMoonGemEchoChunks";
    private static final String KEY_ECHO_COOLDOWN = "TypeMoonGemEchoCooldown";
    private static final int MAX_ECHO_RECORDS = 128;
    private static final int ECHO_SCAN_RADIUS = 6;

    private static final Map<ResourceKeyKey, GemTerrainState> STATES = new ConcurrentHashMap<>();

    private record ResourceKeyKey(String value) {}

    public static boolean isResonanceActive(ServerLevel level, BlockPos pos) {
        if (!Config.gemResonanceEnabled || Config.gemResonanceCycleTicks <= 0) return false;
        if (!isGemTerrain(level, pos)) return false;
        long cycleTick = Math.floorMod(level.getGameTime(), Config.gemResonanceCycleTicks);
        return cycleTick < Config.gemResonanceDurationTicks;
    }

    private static boolean isGemTerrain(ServerLevel level, BlockPos pos) {
        var biomeHolder = level.getBiome(pos);
        return biomeHolder.is(ModTags.Biomes.IS_GEM_TERRAIN);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.isClientSide) return;
        if (level.dimension() != Level.OVERWORLD) return;

        boolean active = Config.gemResonanceEnabled
                && Config.gemResonanceCycleTicks > 0
                && Math.floorMod(level.getGameTime(), Config.gemResonanceCycleTicks) < Config.gemResonanceDurationTicks;

        ResourceKeyKey key = new ResourceKeyKey(level.dimension().location().toString());
        GemTerrainState state = STATES.computeIfAbsent(key, unused -> new GemTerrainState());

        if (state.isResonanceActive() != active) {
            state.setResonanceActive(active);
            for (ServerPlayer player : level.players()) {
                if (!isGemTerrain(level, player.blockPosition())) continue;
                if (active) {
                    player.displayClientMessage(Component.translatable("message.typemoonworld.gem_resonance.start"), true);
                    level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 0.65F, 1.05F);
                    level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), 16, 0.6D, 0.4D, 0.6D, 0.02D);
                } else {
                    player.displayClientMessage(Component.translatable("message.typemoonworld.gem_resonance.end"), true);
                    level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.45F, 0.85F);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.tickCount % 20 != 0) return;
        if (!isGemTerrain(level, player.blockPosition())) return;

        if (isResonanceActive(level, player.blockPosition()) && player.tickCount % 40 == 0) {
            level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 0.8D, player.getZ(), 6, 0.45D, 0.25D, 0.45D, 0.01D);
            if (level.random.nextFloat() < 0.15F) {
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.AMBIENT, 0.35F, 1.25F);
            }
        }

        applyCrystalMistPulse(player, level);
        triggerGeodeEcho(player, level);
    }

    private static void applyCrystalMistPulse(ServerPlayer player, ServerLevel level) {
        if (!Config.gemMistEnabled) return;
        if (!level.isNight()) return;
        if (level.random.nextInt(160) != 0) return;

        boolean hastePulse = level.random.nextBoolean();
        if (hastePulse) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 220, 0, false, true, true));
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem_mist.haste"), true);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 220, 0, false, true, true));
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem_mist.slow"), true);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.AMBIENT, 0.4F, hastePulse ? 1.35F : 0.75F);
    }

    private static void triggerGeodeEcho(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        long cooldownUntil = player.getPersistentData().getLong(KEY_ECHO_COOLDOWN);
        if (cooldownUntil > now) return;

        BlockPos center = player.blockPosition();
        BlockPos found = findNearbyGeodeBlock(level, center, ECHO_SCAN_RADIUS);
        if (found == null) return;

        long chunkKey = chunkKey(found.getX() >> 4, found.getZ() >> 4);
        if (hasEchoRecord(player, chunkKey)) return;

        rememberEcho(player, chunkKey);
        player.getPersistentData().putLong(KEY_ECHO_COOLDOWN, now + 240L);
        player.displayClientMessage(Component.translatable("message.typemoonworld.gem_echo.found"), true);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.AMBIENT, 0.55F, 1.2F);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.9D, player.getZ(), 20, 0.9D, 0.5D, 0.9D, 0.02D);
    }

    private static BlockPos findNearbyGeodeBlock(ServerLevel level, BlockPos center, int radius) {
        for (int y = -2; y <= 2; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if ((x * x) + (z * z) > radius * radius) continue;
                    BlockPos pos = center.offset(x, y, z);
                    LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
                    if (chunk == null || !chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) continue;
                    if (level.getBlockState(pos).is(ModTags.Blocks.GEM_GEODE_BLOCKS)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasEchoRecord(ServerPlayer player, long chunkKey) {
        long[] records = player.getPersistentData().getLongArray(KEY_ECHO_CHUNKS);
        for (long record : records) {
            if (record == chunkKey) return true;
        }
        return false;
    }

    private static void rememberEcho(ServerPlayer player, long chunkKey) {
        long[] oldRecords = player.getPersistentData().getLongArray(KEY_ECHO_CHUNKS);
        int oldLen = oldRecords.length;
        if (oldLen >= MAX_ECHO_RECORDS) {
            long[] trimmed = new long[MAX_ECHO_RECORDS];
            System.arraycopy(oldRecords, oldLen - (MAX_ECHO_RECORDS - 1), trimmed, 0, MAX_ECHO_RECORDS - 1);
            trimmed[MAX_ECHO_RECORDS - 1] = chunkKey;
            player.getPersistentData().putLongArray(KEY_ECHO_CHUNKS, trimmed);
            return;
        }

        long[] next = new long[oldLen + 1];
        System.arraycopy(oldRecords, 0, next, 0, oldLen);
        next[oldLen] = chunkKey;
        player.getPersistentData().putLongArray(KEY_ECHO_CHUNKS, next);
    }

    private static long chunkKey(int x, int z) {
        return (x & 0xffffffffL) | ((z & 0xffffffffL) << 32);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().putLongArray(KEY_ECHO_CHUNKS, event.getOriginal().getPersistentData().getLongArray(KEY_ECHO_CHUNKS));
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        STATES.clear();
    }
}

