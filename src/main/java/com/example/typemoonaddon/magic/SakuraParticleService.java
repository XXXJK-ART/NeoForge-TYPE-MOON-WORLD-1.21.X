package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.entity.SakuraBlackShadowEntity;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import com.example.typemoonaddon.registry.AddonAttachments;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public final class SakuraParticleService {
    public static final byte BLACK = 0;
    public static final byte GRAIL_RED_BLACK = 1;
    public static final byte SAKURA = 2;
    public static final DustParticleOptions SAKURA_DUST = new DustParticleOptions(
            new Vector3f(1.0F, 0.48F, 0.70F),
            1.0F
    );

    public static void send(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        level.sendParticles(particle, x, y, z, count, dx, dy, dz, speed);
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
            case SAKURA -> level.sendParticles(SAKURA_DUST, x, y, z, safeCount, spreadX, spreadY, spreadZ, speed);
            case GRAIL_RED_BLACK -> {
                int ratioGroups = Math.max(1, Math.round(safeCount / 5.0F));
                level.sendParticles(DustParticleOptions.REDSTONE, x, y, z, ratioGroups, spreadX, spreadY, spreadZ, speed);
                level.sendParticles(ParticleTypes.SQUID_INK, x, y, z, ratioGroups * 4, spreadX, spreadY, spreadZ, speed);
            }
            default -> level.sendParticles(legacyParticle, x, y, z, safeCount, spreadX, spreadY, spreadZ, speed);
        }
    }

    public static void around(LivingEntity entity, ParticleOptions particle, int count, double speed) {
        if (entity.level() instanceof ServerLevel level) {
            send(
                    level,
                    particle,
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.5D,
                    entity.getZ(),
                    count,
                    entity.getBbWidth() * 0.65D,
                    entity.getBbHeight() * 0.45D,
                    entity.getBbWidth() * 0.65D,
                    speed
            );
        }
    }

    public static byte palette(Entity source) {
        Player owner = owner(source);
        if (owner == null) {
            return BLACK;
        }
        var data = owner.getData(AddonAttachments.IMAGINARY_SPACE.get());
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
            case GRAIL_RED_BLACK -> Math.floorMod(sequence, 5L) == 0L ? DustParticleOptions.REDSTONE : ParticleTypes.SQUID_INK;
            default -> legacyParticle;
        };
    }

    private static byte normalize(byte palette) {
        return palette < BLACK || palette > SAKURA ? BLACK : palette;
    }

    private static Player owner(Entity source) {
        if (source instanceof Player player) {
            return player;
        }
        LivingEntity owner = null;
        if (source instanceof SakuraShadowFamiliarEntity familiar) {
            owner = familiar.getOwner();
        } else if (source instanceof SakuraBlackShadowEntity shadow) {
            owner = shadow.getOwner();
        }
        return owner instanceof Player player ? player : null;
    }

    private SakuraParticleService() {
    }
}
