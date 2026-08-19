package com.example.typemoonaddon.entity;

import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.servant.GillesDeRaisCombatHelper;
import com.example.typemoonaddon.servant.GillesVoiceHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import org.jetbrains.annotations.Nullable;

public final class GillesDeRaisEntity extends ServantEntity {
    public static final String SERVANT_KEY = "gilles_de_rais_caster";
    private static final String TAG_GIANT_UNLOCKED = "GillesGiantSeaMonsterUnlocked";
    private static final String TAG_SPELLBOOK_LOST = "GillesPrelatisSpellbookLost";

    public GillesDeRaisEntity(EntityType<GillesDeRaisEntity> type, Level level) {
        super(type, level, SERVANT_KEY);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 180.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.STEP_HEIGHT, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35);
    }

    @Override
    protected void customServerAiStep() {
        if (this.isShelteredInsideHugeSeaMonster()) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (!this.level().isClientSide() && GillesDeRaisCombatHelper.tickSummonChant(this)) {
            return;
        }
        super.customServerAiStep();
        if (!this.level().isClientSide() && this.isAlive() && !this.isSpiritualDissolving()) {
            GillesDeRaisCombatHelper.tick(this);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isShelteredInsideHugeSeaMonster()) {
            return false;
        }
        if (!GillesDeRaisCombatHelper.isSummonChanting(this)
                && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            this.setTarget(attacker);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && GillesDeRaisCombatHelper.isSummonChanting(this)) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean isPickable() {
        return !this.isShelteredInsideHugeSeaMonster() && super.isPickable();
    }

    @Override
    public boolean isAttackable() {
        return !this.isShelteredInsideHugeSeaMonster() && super.isAttackable();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (GillesDeRaisCombatHelper.isSummonChanting(this)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            GillesVoiceHelper.tryPlayAttack(this);
        }
        return hit;
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        boolean result = super.killedEntity(level, victim);
        GillesVoiceHelper.tryPlayVictory(this, victim);
        return result;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        GillesVoiceHelper.tryPlayFail(this);
    }

    public boolean isGiantSeaMonsterUnlocked() {
        return this.getPersistentData().getBoolean(TAG_GIANT_UNLOCKED);
    }

    public void unlockGiantSeaMonster() {
        this.getPersistentData().putBoolean(TAG_GIANT_UNLOCKED, true);
    }

    public boolean hasUsableSpellbook() {
        return !this.hasLostSpellbook()
                && (this.getMainHandItem().is(AddonItems.PRELATIS_SPELLBOOK.get())
                || this.getOffhandItem().is(AddonItems.PRELATIS_SPELLBOOK.get()));
    }

    public boolean hasLostSpellbook() {
        return this.getPersistentData().getBoolean(TAG_SPELLBOOK_LOST);
    }

    public void loseSpellbook() {
        this.getPersistentData().putBoolean(TAG_SPELLBOOK_LOST, true);
        this.removeSpellbookFromHands();
    }

    public boolean isShelteredInsideHugeSeaMonster() {
        return this.getVehicle() instanceof HugeSeaMonsterEntity hugeSeaMonster && hugeSeaMonster.isAlive();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_SPELLBOOK_LOST, this.hasLostSpellbook());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean(TAG_GIANT_UNLOCKED)) {
            this.unlockGiantSeaMonster();
        }
        if (tag.getBoolean(TAG_SPELLBOOK_LOST)) {
            this.loseSpellbook();
        }
    }

    private void removeSpellbookFromHands() {
        if (this.getMainHandItem().is(AddonItems.PRELATIS_SPELLBOOK.get())) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (this.getOffhandItem().is(AddonItems.PRELATIS_SPELLBOOK.get())) {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }
}
