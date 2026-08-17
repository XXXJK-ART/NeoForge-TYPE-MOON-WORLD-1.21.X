package net.xxxjk.TYPE_MOON_WORLD.chain.config;

public final class ChainConfig {
    public static final int CHAIN_COUNT = 10;
    public static final int SKILL_GATES_PER_PLANE = 10;
    public static final int SKILL_TARGET_CAP = 3;
    public static final int SKILL_GATE_LAUNCH_DELAY = 12;
    public static final double SKILL_GATE_RADIUS = 6.0D;
    public static final double SKILL_GATE_VERTICAL_OFFSET = 4.5D;
    public static final int SKILL_GATE_REFRESH_TICKS = 16;
    public static final float CHAIN_MAX_HEALTH = 100.0F;
    public static final double TARGET_RADIUS = 50.0D;
    public static final double TARGET_RADIUS_SQR = TARGET_RADIUS * TARGET_RADIUS;
    public static final double MAX_EXTENSION = 100.0D;
    public static final double MAX_EXTENSION_SQR = MAX_EXTENSION * MAX_EXTENSION;
    public static final int SEEK_TIMEOUT_TICKS = 100;
    public static final int LONG_PRESS_TICKS = 10;
    public static final int OWNER_SCAN_INTERVAL = 5;
    public static final int DAMAGE_INTERVAL_TICKS = 20;
    public static final float DAMAGE_PER_INTERVAL = 10.0F;
    public static final double LAUNCH_SPEED = 3.5D;
    public static final double MAX_STEERING_RADIANS = Math.toRadians(10.0D);
    public static final double CHAIN_HEAD_HITBOX_INFLATION = 0.3D;
    public static final double RETRACT_SPEED = 5.0D;
    public static final int HIT_PART_COUNT = 32;
    public static final float HIT_PART_SIZE = 1.35F;
    public static final int RENDER_SEGMENT_CAP = 256;
    public static final int DISSOLVE_PARTICLE_CAP = 96;
    public static final int ENUMA_DISSOLVE_PARTICLE_CAP = 12;
    public static final int ENUMA_BINDING_DISSOLVE_WAX_PARTICLES = 6;
    public static final int ENUMA_BINDING_DISSOLVE_END_ROD_PARTICLES = 2;
    public static final int ENUMA_RENDER_LINK_CAP = 96;
    public static final double CHAIN_HEAD_CONNECTION_OFFSET = 0.40D;
    public static final int ENUMA_CHAIN_COUNT = 100;
    public static final float ENUMA_AGGREGATED_MAX_HEALTH = 500.0F;
    public static final int ENUMA_WINDUP_TICKS = 200;
    public static final int ENUMA_ASCENT_TICKS = 36;
    public static final int ENUMA_BIND_CONNECTION_TICKS = 12;
    public static final int ENUMA_MERGED_LINKS_PER_TETHER = 24;
    public static final int ENUMA_MIN_BIND_TICKS = 200;
    public static final double ENUMA_FIELD_RADIUS = 30.0D;
    public static final double ENUMA_GATHER_RADIUS = 10.0D;
    public static final double ENUMA_HEAD_COLLISION_RADIUS = 0.65D;
    public static final double ENUMA_CROWN_COLLISION_RADIUS = 4.5D;
    public static final int ENUMA_CLIENT_FIELD_PARTICLES_PER_TICK = 256;
    public static final int ENUMA_ORPHAN_GRACE_TICKS = 40;

    private ChainConfig() {
    }
}

