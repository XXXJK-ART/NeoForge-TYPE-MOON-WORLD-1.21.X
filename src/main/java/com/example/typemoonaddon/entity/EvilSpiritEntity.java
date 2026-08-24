package com.example.typemoonaddon.entity;

import com.example.typemoonaddon.registry.AddonEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class EvilSpiritEntity extends SummonedSpiritEntity {
    private static final double LARGE_BASE_HEALTH = 100.0D;
    private static final double LARGE_BASE_DAMAGE = 10.0D;
    private static final double SMALL_BASE_HEALTH = 30.0D;
    private static final double SMALL_BASE_DAMAGE = 4.0D;

    public EvilSpiritEntity(EntityType<EvilSpiritEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, LARGE_BASE_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, LARGE_BASE_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D);
    }

    public static AttributeSupplier.Builder createSmallAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, SMALL_BASE_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, SMALL_BASE_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25D);
    }

    public boolean isSmall() {
        return getType() == AddonEntities.EVIL_SPIRIT_SMALL.get();
    }

    public void configure(double proficiency) {
        double ratio = Math.max(0.0D, Math.min(100.0D, proficiency)) / 100.0D;
        double baseHealth = isSmall() ? SMALL_BASE_HEALTH : LARGE_BASE_HEALTH;
        double baseDamage = isSmall() ? SMALL_BASE_DAMAGE : LARGE_BASE_DAMAGE;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHealth + baseHealth * ratio);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseDamage + baseDamage * ratio);
        setHealth(getMaxHealth());
    }

    @Override protected double getAttackSearchRadius() { return isSmall() ? 24.0D : 32.0D; }
    @Override protected double getAttackRange() { return isSmall() ? 1.8D : 2.8D; }
    @Override protected double getMoveSpeed() { return isSmall() ? 0.40D : 0.34D; }
    @Override protected double getFollowDistance() { return isSmall() ? 8.0D : 12.0D; }
    @Override protected float getSpiritDamage() { return (float)getAttributeValue(Attributes.ATTACK_DAMAGE); }
}
