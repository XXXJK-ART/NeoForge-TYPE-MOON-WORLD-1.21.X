package com.example.typemoonaddon.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class EvilSpiritEntity extends SummonedSpiritEntity {
    public EvilSpiritEntity(EntityType<EvilSpiritEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D);
    }

    public void configure(double proficiency) {
        double ratio = Math.max(0.0D, Math.min(100.0D, proficiency)) / 100.0D;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0D + 100.0D * ratio);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10.0D + 10.0D * ratio);
        setHealth(getMaxHealth());
    }

    @Override protected double getAttackSearchRadius() { return 32.0D; }
    @Override protected double getAttackRange() { return 2.8D; }
    @Override protected double getMoveSpeed() { return 0.34D; }
    @Override protected double getFollowDistance() { return 12.0D; }
    @Override protected float getSpiritDamage() { return (float)getAttributeValue(Attributes.ATTACK_DAMAGE); }
}
