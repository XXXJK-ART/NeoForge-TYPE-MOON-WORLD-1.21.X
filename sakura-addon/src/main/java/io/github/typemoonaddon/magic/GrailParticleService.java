package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import io.github.typemoonaddon.registry.ModAttachments;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/** Resolves and renders the three persistent Imaginary Magic visual palettes. */
public final class GrailParticleService {
    public static final byte BLACK = 0;
    public static final byte GRAIL_RED_BLACK = 1;
    public static final byte SAKURA = 2;

    public static final DustParticleOptions SAKURA_DUST = new DustParticleOptions(
        new Vector3f(1.0F, 0.48F, 0.70F),
        1.0F
    );

    public static byte palette(Entity source) {
        Player owner = owner(source);
        if (owner == null) {
            return BLACK;
        }
        var data = owner.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (data.sakuraImaginaryEffects()) {
            return SAKURA;
        }
        return data.grailWormAscended() ? GRAIL_RED_BLACK : BLACK;
    }

    public static byte highest(byte first, byte second) {
        return (byte)Math.max(normalize(first), normalize(second));
    }

    public static ParticleOptions particle(byte palette, long sequence, ParticleOptions legacyParticle) {
        return switch (normalize(palette)) {
            case SAKURA -> SAKURA_DUST;
            case GRAIL_RED_BLACK -> Math.floorMod(sequence, 5L) == 0L
                ? DustParticleOptions.REDSTONE
                : ParticleTypes.SQUID_INK;
            default -> legacyParticle;
        };
    }

    public static void send(
        ServerLevel level,
        Entity source,
        ParticleOptions legacyParticle,
        double x,
        double y,
        double z,
        int count,
        double spreadX,
        double spreadY,
        double spreadZ,
        double speed
    ) {
        send(level, palette(source), legacyParticle, x, y, z, count, spreadX, spreadY, spreadZ, speed);
    }

    public static void send(
        ServerLevel level,
        byte palette,
        ParticleOptions legacyParticle,
        double x,
        double y,
        double z,
        int count,
        double spreadX,
        double spreadY,
        double spreadZ,
        double speed
    ) {
        int safeCount = Math.max(0, count);
        if (safeCount == 0) {
            return;
        }
        switch (normalize(palette)) {
            case SAKURA -> level.sendParticles(
                SAKURA_DUST, x, y, z, safeCount, spreadX, spreadY, spreadZ, speed
            );
            case GRAIL_RED_BLACK -> {
                int ratioGroups = Math.max(1, Math.round(safeCount / 5.0F));
                int redCount = ratioGroups;
                int blackCount = ratioGroups * 4;
                level.sendParticles(
                    DustParticleOptions.REDSTONE,
                    x, y, z, redCount, spreadX, spreadY, spreadZ, speed
                );
                level.sendParticles(
                    ParticleTypes.SQUID_INK,
                    x, y, z, blackCount, spreadX, spreadY, spreadZ, speed
                );
            }
            default -> level.sendParticles(
                legacyParticle, x, y, z, safeCount, spreadX, spreadY, spreadZ, speed
            );
        }
    }

    private static byte normalize(byte palette) {
        return palette < BLACK || palette > SAKURA ? BLACK : palette;
    }

    private static Player owner(Entity source) {
        if (source instanceof Player player) {
            return player;
        }
        LivingEntity owner = null;
        if (source instanceof ShadowFamiliarEntity familiar) {
            owner = familiar.getOwner();
        } else if (source instanceof BlackShadowEntity shadow) {
            owner = shadow.getOwner();
        }
        return owner instanceof Player player ? player : null;
    }

    private GrailParticleService() {
    }
}
