package com.example.typemoonaddon.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class WraithEntity extends SummonedSpiritEntity {
    public WraithEntity(EntityType<WraithEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override protected double getAttackSearchRadius() { return 18.0D; }
    @Override protected double getAttackRange() { return 2.4D; }
    @Override protected double getMoveSpeed() { return 0.38D; }
    @Override protected double getFollowDistance() { return 8.0D; }
    @Override protected float getSpiritDamage() { return 5.0F; }
}
