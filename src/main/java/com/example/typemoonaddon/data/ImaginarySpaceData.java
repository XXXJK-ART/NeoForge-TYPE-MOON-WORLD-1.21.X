package com.example.typemoonaddon.data;

import com.example.typemoonaddon.config.GameplayConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class ImaginarySpaceData implements INBTSerializable<CompoundTag> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ImaginarySpaceData> STREAM_CODEC = StreamCodec.of(
        (buffer, data) -> data.writeToNetwork(buffer),
        ImaginarySpaceData::readFromNetwork
    );
    public static final int DATA_VERSION = GameplayConfig.DATA_VERSION;
    public static final int MENU_CAPACITY = GameplayConfig.MENU_CAPACITY;
    public static final int MAX_SPACE_PAGES = 256;
    public static final int PROTECTION_DURATION_TICKS = GameplayConfig.PROTECTION_DURATION_TICKS;
    public static final int PROTECTION_COOLDOWN_TICKS = GameplayConfig.PROTECTION_COOLDOWN_TICKS;
    public static final int PROTECTION_RECOVERY_DELAY_TICKS = GameplayConfig.PROTECTION_RECOVERY_DELAY_TICKS;
    public static final float PROTECTION_MAX_SHIELD = GameplayConfig.PROTECTION_MAX_SHIELD;
    public static final float PROTECTION_RECOVERY_PER_TICK = GameplayConfig.PROTECTION_RECOVERY_PER_TICK;
    public static final int SHADOW_ART_RIBBON_COUNT = GameplayConfig.SHADOW_ART_RIBBON_COUNT;
    public static final int SHADOW_ART_RIBBON_RESPAWN_TICKS = GameplayConfig.SHADOW_ART_RIBBON_RESPAWN_TICKS;

    private static final int MAX_DECODED_PAGES = 1024;

    private final List<NonNullList<ItemStack>> pages = new ArrayList<>();
    private final List<CorruptedServantRecord> corruptedServants = new ArrayList<>();
    private final Set<UUID> selectedCorruptedServants = new HashSet<>();
    private boolean learned;
    private boolean imaginaryAffinity;
    private boolean crestWormAssimilated;
    private boolean crestWormExpelled;
    private boolean grailWormAscended;
    private boolean sakuraImaginaryEffects;
    private boolean forbiddenMagicUnlocked;
    private ShadowArtState shadowArtState = ShadowArtState.LOCKED;
    private ShadowArtMode shadowArtMode = ShadowArtMode.BALANCED;
    private final int[] shadowArtRespawnTicks = new int[GameplayConfig.SHADOW_ART_RIBBON_COUNT];
    private int grailErosion;
    private long grailErosionCompletionTick;
    private CursedArmorState cursedArmorState = CursedArmorState.NONE;
    private long cursedArmorStageStartTick;
    private long lastServantOutcomeDay = Long.MIN_VALUE;
    private boolean grailPowerSnapshotPresent;
    private double preGrailMana;
    private double preGrailMaximumMana;
    private double preGrailManaRegen;
    private double preGrailRestoreSpeed;
    private boolean preGrailWaterAttribute;
    private boolean preGrailMagus;
    private MagicMode magicMode = MagicMode.STORAGE;
    private BlackMudSummonMode blackMudSummonMode = BlackMudSummonMode.RELEASE;
    private ShadowCommandMode selectedShadowCommandMode = ShadowCommandMode.FREE;
    private ShadowCommandMode activeShadowCommandMode = ShadowCommandMode.FREE;
    private boolean shadowAttackAround;
    private int shadowDismissalGeneration;
    private long completedNightMissionDay = Long.MIN_VALUE;
    private boolean protectionEnabled;
    private int protectionTicksRemaining;
    private float protectionShield = GameplayConfig.PROTECTION_MAX_SHIELD;
    private int protectionCooldownTicks;
    private int protectionRecoveryDelayTicks;
    private int selectedPage;

    public enum MagicMode {
        STORAGE("storage"),
        PROTECTION("protection");

        private final String serializedName;

        MagicMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static MagicMode fromSerialized(String value) {
            return PROTECTION.serializedName.equals(value) ? PROTECTION : STORAGE;
        }
    }

    public enum ShadowCommandMode {
        FREE("free"),
        SPREAD("spread"),
        HOLD("hold"),
        ATTACK_AROUND("attack_around"),
        GATHER("gather"),
        HUNT("hunt"),
        DISMISS("dismiss");

        private final String serializedName;

        ShadowCommandMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static ShadowCommandMode fromSerialized(String value) {
            for (ShadowCommandMode mode : values()) {
                if (mode.serializedName.equals(value)) {
                    return mode;
                }
            }
            return FREE;
        }
    }

    public enum BlackMudSummonMode {
        RELEASE("release"),
        DISMISS("dismiss");

        private final String serializedName;

        BlackMudSummonMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static BlackMudSummonMode fromSerialized(String value) {
            return DISMISS.serializedName.equals(value) ? DISMISS : RELEASE;
        }
    }

    public enum CursedArmorState {
        NONE("none"),
        FORMING("forming"),
        ACTIVE("active"),
        DISSOLVING("dissolving"),
        REMOVED("removed");

        private final String serializedName;

        CursedArmorState(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static CursedArmorState fromSerialized(String value) {
            for (CursedArmorState state : values()) {
                if (state.serializedName.equals(value)) {
                    return state;
                }
            }
            return NONE;
        }
    }

    public enum ShadowArtState {
        LOCKED, AVAILABLE, ACTIVATING, ACTIVE, DEACTIVATING, DISPELLED;

        public boolean usable() {
            return this == AVAILABLE || this == ACTIVATING || this == ACTIVE || this == DEACTIVATING;
        }
    }

    public enum ShadowArtMode {
        AUTO_DEFENSE("auto_defense"), AUTO_ATTACK("auto_attack"), BALANCED("balanced");
        private final String serializedName;
        ShadowArtMode(String serializedName) { this.serializedName = serializedName; }
        public String serializedName() { return serializedName; }
        public static ShadowArtMode fromSerialized(String value) {
            for (ShadowArtMode mode : values()) if (mode.serializedName.equals(value)) return mode;
            return BALANCED;
        }
    }

    public ImaginarySpaceData() {
        pages.add(createPage());
    }

    public boolean learned() {
        return learned;
    }

    public void unlock() {
        learned = true;
        imaginaryAffinity = true;
    }

    public boolean imaginaryAffinity() {
        return imaginaryAffinity;
    }

    public boolean crestWormAssimilated() {
        return crestWormAssimilated;
    }

    public boolean assimilateCrestWorm() {
        if (crestWormAssimilated) {
            return false;
        }
        crestWormAssimilated = true;
        magicMode = MagicMode.STORAGE;
        return true;
    }

    public boolean crestWormExpelled() {
        return crestWormExpelled;
    }

    public boolean expelCrestWorm() {
        if (!crestWormAssimilated || crestWormExpelled) {
            return false;
        }
        crestWormExpelled = true;
        return true;
    }

    public boolean grailWormAscended() {
        return grailWormAscended;
    }

    public boolean sakuraImaginaryEffects() {
        return sakuraImaginaryEffects;
    }

    public boolean enableSakuraImaginaryEffects() {
        if (sakuraImaginaryEffects) {
            return false;
        }
        sakuraImaginaryEffects = true;
        return true;
    }

    public boolean forbiddenMagicUnlocked() {
        return forbiddenMagicUnlocked;
    }

    public boolean unlockForbiddenMagic() {
        if (forbiddenMagicUnlocked) {
            return false;
        }
        forbiddenMagicUnlocked = true;
        return true;
    }

    public ShadowArtState shadowArtState() { return shadowArtState; }
    public ShadowArtMode shadowArtMode() { return shadowArtMode; }
    public boolean shadowArtUnlocked() { return shadowArtState.usable(); }
    public boolean shadowArtActive() {
        return shadowArtState == ShadowArtState.ACTIVATING || shadowArtState == ShadowArtState.ACTIVE
            || shadowArtState == ShadowArtState.DEACTIVATING;
    }
    public boolean unlockShadowArt() {
        if (shadowArtState != ShadowArtState.LOCKED) return false;
        shadowArtState = ShadowArtState.AVAILABLE;
        return true;
    }
    public boolean activateShadowArt() {
        if (shadowArtState != ShadowArtState.AVAILABLE) return false;
        shadowArtState = ShadowArtState.ACTIVATING;
        return true;
    }
    public void finishShadowArtActivation() {
        if (shadowArtState == ShadowArtState.ACTIVATING) shadowArtState = ShadowArtState.ACTIVE;
    }
    public boolean deactivateShadowArt() {
        if (!shadowArtActive() || shadowArtState == ShadowArtState.DEACTIVATING) return false;
        shadowArtState = ShadowArtState.DEACTIVATING;
        return true;
    }
    public void finishShadowArtDeactivation() {
        if (shadowArtState == ShadowArtState.DEACTIVATING) shadowArtState = ShadowArtState.AVAILABLE;
    }
    public boolean dispelShadowArt() {
        if (shadowArtState == ShadowArtState.DISPELLED) return false;
        shadowArtState = ShadowArtState.DISPELLED;
        java.util.Arrays.fill(shadowArtRespawnTicks, 0);
        return true;
    }
    public void setShadowArtMode(ShadowArtMode mode) { shadowArtMode = mode == null ? ShadowArtMode.BALANCED : mode; }
    public int shadowArtRespawnTicks(int index) { return shadowArtRespawnTicks[Math.clamp(index, 0, shadowArtRespawnTicks.length - 1)]; }
    public void breakShadowArtRibbon(int index) { shadowArtRespawnTicks[Math.clamp(index, 0, shadowArtRespawnTicks.length - 1)] = GameplayConfig.SHADOW_ART_RIBBON_RESPAWN_TICKS; }
    public boolean tickShadowArtRibbonRecovery(int index) {
        index = Math.clamp(index, 0, shadowArtRespawnTicks.length - 1);
        if (shadowArtRespawnTicks[index] <= 0) return false;
        return --shadowArtRespawnTicks[index] == 0;
    }

    public boolean ascendGrailWorm() {
        if (grailWormAscended) {
            return false;
        }
        grailWormAscended = true;
        grailErosion = 0;
        return true;
    }

    public int grailErosion() {
        return grailErosion;
    }

    public boolean grailErosionFull() {
        return grailErosion >= 3 && grailErosionCompletionTick == 0L;
    }

    public boolean increaseGrailErosion() {
        return increaseGrailErosion(0L);
    }

    public boolean increaseGrailErosion(long completionTick) {
        if (!grailWormAscended || grailErosion >= 3) {
            return false;
        }
        grailErosion++;
        if (grailErosion >= 3) {
            grailErosionCompletionTick = Math.max(1L, completionTick);
        }
        return true;
    }

    public boolean grailErosionPending() {
        return grailErosion >= 3 && grailErosionCompletionTick > 0L;
    }

    public float grailErosionProgress() {
        return grailErosionPending() ? 0.99F : Math.clamp(grailErosion / 3.0F, 0.0F, 1.0F);
    }

    public boolean completePendingGrailErosion(long gameTime) {
        if (!grailErosionPending() || gameTime < grailErosionCompletionTick) {
            return false;
        }
        grailErosionCompletionTick = 0L;
        return true;
    }

    public CursedArmorState cursedArmorState() {
        return cursedArmorState;
    }

    public long cursedArmorStageStartTick() {
        return cursedArmorStageStartTick;
    }

    public boolean cursedArmorPresent() {
        return cursedArmorState == CursedArmorState.FORMING
            || cursedArmorState == CursedArmorState.ACTIVE
            || cursedArmorState == CursedArmorState.DISSOLVING;
    }

    public boolean beginCursedArmorFormation(long gameTime) {
        if (cursedArmorState != CursedArmorState.NONE) {
            return false;
        }
        cursedArmorState = CursedArmorState.FORMING;
        cursedArmorStageStartTick = gameTime;
        return true;
    }

    public boolean activateCursedArmor() {
        if (cursedArmorState != CursedArmorState.FORMING) {
            return false;
        }
        cursedArmorState = CursedArmorState.ACTIVE;
        cursedArmorStageStartTick = 0L;
        return true;
    }

    public boolean beginCursedArmorDissolution(long gameTime) {
        if (!cursedArmorPresent() || cursedArmorState == CursedArmorState.DISSOLVING) {
            return false;
        }
        cursedArmorState = CursedArmorState.DISSOLVING;
        cursedArmorStageStartTick = gameTime;
        return true;
    }

    public boolean finishCursedArmorDissolution() {
        if (cursedArmorState != CursedArmorState.DISSOLVING) {
            return false;
        }
        cursedArmorState = CursedArmorState.REMOVED;
        cursedArmorStageStartTick = 0L;
        return true;
    }

    public void markCursedArmorRemoved() {
        cursedArmorState = CursedArmorState.REMOVED;
        cursedArmorStageStartTick = 0L;
    }

    public void markServantOutcome(long day) {
        lastServantOutcomeDay = day;
    }

    public boolean hasServantOutcomeOn(long day) {
        return lastServantOutcomeDay == day;
    }

    public boolean revokeGrailWormAscension() {
        boolean changed = grailWormAscended || protectionEnabled || protectionTicksRemaining > 0;
        grailWormAscended = false;
        grailErosion = 0;
        grailErosionCompletionTick = 0L;
        lastServantOutcomeDay = Long.MIN_VALUE;
        protectionEnabled = false;
        protectionTicksRemaining = 0;
        protectionShield = GameplayConfig.PROTECTION_MAX_SHIELD;
        protectionCooldownTicks = 0;
        protectionRecoveryDelayTicks = 0;
        magicMode = MagicMode.STORAGE;
        blackMudSummonMode = BlackMudSummonMode.RELEASE;
        selectedShadowCommandMode = ShadowCommandMode.FREE;
        activeShadowCommandMode = ShadowCommandMode.FREE;
        shadowAttackAround = false;
        shadowDismissalGeneration++;
        return changed;
    }

    public void captureGrailPowerSnapshot(
        double mana,
        double maximumMana,
        double manaRegen,
        double restoreSpeed,
        boolean waterAttribute,
        boolean magus
    ) {
        if (grailPowerSnapshotPresent) {
            return;
        }
        grailPowerSnapshotPresent = true;
        preGrailMana = mana;
        preGrailMaximumMana = maximumMana;
        preGrailManaRegen = manaRegen;
        preGrailRestoreSpeed = restoreSpeed;
        preGrailWaterAttribute = waterAttribute;
        preGrailMagus = magus;
    }

    @Nullable
    public GrailPowerSnapshot grailPowerSnapshot() {
        return grailPowerSnapshotPresent
            ? new GrailPowerSnapshot(
                preGrailMana,
                preGrailMaximumMana,
                preGrailManaRegen,
                preGrailRestoreSpeed,
                preGrailWaterAttribute,
                preGrailMagus
            )
            : null;
    }

    public void clearGrailPowerSnapshot() {
        grailPowerSnapshotPresent = false;
        preGrailMana = 0.0D;
        preGrailMaximumMana = 0.0D;
        preGrailManaRegen = 0.0D;
        preGrailRestoreSpeed = 0.0D;
        preGrailWaterAttribute = false;
        preGrailMagus = false;
    }

    public boolean completedNightMissionOn(long day) {
        return completedNightMissionDay == day;
    }

    public void markNightMissionCompleted(long day) {
        completedNightMissionDay = day;
    }

    public boolean clearNightMissionCompletion() {
        if (completedNightMissionDay == Long.MIN_VALUE) {
            return false;
        }
        completedNightMissionDay = Long.MIN_VALUE;
        return true;
    }

    public List<CorruptedServantRecord> corruptedServants() {
        return List.copyOf(corruptedServants);
    }

    public boolean addCorruptedServant(UUID rosterId, CompoundTag entitySnapshot, UUID activeEntityId) {
        if (rosterId == null || entitySnapshot == null || corruptedServants.stream().anyMatch(record -> record.rosterId.equals(rosterId))) {
            return false;
        }
        corruptedServants.add(new CorruptedServantRecord(rosterId, entitySnapshot.copy(), activeEntityId));
        selectedCorruptedServants.add(rosterId);
        return true;
    }

    public Set<UUID> selectedCorruptedServants() {
        return Set.copyOf(selectedCorruptedServants);
    }

    public boolean setSelectedCorruptedServants(Set<UUID> rosterIds) {
        Set<UUID> validated = new HashSet<>();
        if (rosterIds != null) {
            for (CorruptedServantRecord record : corruptedServants) {
                if (rosterIds.contains(record.rosterId)) {
                    validated.add(record.rosterId);
                }
            }
        }
        if (selectedCorruptedServants.equals(validated)) {
            return false;
        }
        selectedCorruptedServants.clear();
        selectedCorruptedServants.addAll(validated);
        return true;
    }

    public void setCorruptedServantActive(UUID rosterId, @Nullable UUID activeEntityId) {
        for (CorruptedServantRecord record : corruptedServants) {
            if (record.rosterId.equals(rosterId)) {
                record.activeEntityId = activeEntityId;
                return;
            }
        }
    }

    @Nullable
    public UUID activeCorruptedServant(UUID rosterId) {
        for (CorruptedServantRecord record : corruptedServants) {
            if (record.rosterId.equals(rosterId)) {
                return record.activeEntityId;
            }
        }
        return null;
    }

    public void clearCorruptedServantActive(UUID rosterId, UUID expectedEntityId) {
        for (CorruptedServantRecord record : corruptedServants) {
            if (record.rosterId.equals(rosterId) && expectedEntityId.equals(record.activeEntityId)) {
                record.activeEntityId = null;
                return;
            }
        }
    }

    public MagicMode magicMode() {
        return magicMode;
    }

    public boolean setMagicMode(MagicMode mode) {
        MagicMode validated = mode == null ? MagicMode.STORAGE : mode;
        if (magicMode == validated) {
            return false;
        }
        magicMode = validated;
        return true;
    }

    public BlackMudSummonMode blackMudSummonMode() {
        return blackMudSummonMode;
    }

    public boolean setBlackMudSummonMode(BlackMudSummonMode mode) {
        BlackMudSummonMode validated = mode == null ? BlackMudSummonMode.RELEASE : mode;
        if (blackMudSummonMode == validated) {
            return false;
        }
        blackMudSummonMode = validated;
        return true;
    }

    public ShadowCommandMode selectedShadowCommandMode() {
        return selectedShadowCommandMode;
    }

    public boolean setSelectedShadowCommandMode(ShadowCommandMode mode) {
        ShadowCommandMode validated = mode == null ? ShadowCommandMode.FREE : mode;
        if (selectedShadowCommandMode == validated) {
            return false;
        }
        selectedShadowCommandMode = validated;
        return true;
    }

    public ShadowCommandMode activeShadowCommandMode() {
        return activeShadowCommandMode;
    }

    public boolean setActiveShadowCommandMode(ShadowCommandMode mode) {
        ShadowCommandMode validated = mode == null ? ShadowCommandMode.FREE : mode;
        if (activeShadowCommandMode == validated) {
            return false;
        }
        activeShadowCommandMode = validated;
        return true;
    }

    public boolean applySelectedShadowCommandMode() {
        if (selectedShadowCommandMode == ShadowCommandMode.ATTACK_AROUND) {
            shadowAttackAround = !shadowAttackAround;
            return true;
        }
        if (selectedShadowCommandMode == ShadowCommandMode.DISMISS) {
            return false;
        }
        if (activeShadowCommandMode == selectedShadowCommandMode) {
            return false;
        }
        activeShadowCommandMode = selectedShadowCommandMode;
        return true;
    }

    public boolean shadowAttackAround() {
        return shadowAttackAround;
    }

    public boolean setShadowAttackAround(boolean enabled) {
        if (shadowAttackAround == enabled) {
            return false;
        }
        shadowAttackAround = enabled;
        return true;
    }

    public int shadowDismissalGeneration() {
        return shadowDismissalGeneration;
    }

    public int advanceShadowDismissalGeneration() {
        shadowDismissalGeneration++;
        return shadowDismissalGeneration;
    }

    public int protectionTicksRemaining() {
        return protectionTicksRemaining;
    }

    public boolean protectionActive() {
        return protectionEnabled;
    }

    public void activateProtection() {
        protectionEnabled = true;
        protectionTicksRemaining = GameplayConfig.PROTECTION_DURATION_TICKS;
        if (protectionShield <= 0.0F) {
            protectionShield = GameplayConfig.PROTECTION_MAX_SHIELD;
        }
    }

    public boolean togglePermanentProtection() {
        if (protectionCooldownTicks > 0 || !protectionEnabled && protectionShield <= 0.0F) {
            return false;
        }
        protectionEnabled = !protectionEnabled;
        protectionTicksRemaining = 0;
        protectionCooldownTicks = GameplayConfig.PROTECTION_COOLDOWN_TICKS;
        return true;
    }

    public boolean clearProtection() {
        if (!protectionEnabled) {
            return false;
        }
        protectionEnabled = false;
        protectionTicksRemaining = 0;
        return true;
    }

    public float protectionShield() {
        return protectionShield;
    }

    public int protectionCooldownTicks() {
        return protectionCooldownTicks;
    }

    /** Returns the part of a hit not absorbed by the shield. */
    public float absorbProtectionDamage(float amount) {
        if (!protectionEnabled || amount <= 0.0F) {
            return Math.max(0.0F, amount);
        }
        float absorbed = Math.min(amount, protectionShield);
        protectionShield -= absorbed;
        protectionRecoveryDelayTicks = GameplayConfig.PROTECTION_RECOVERY_DELAY_TICKS;
        if (protectionShield <= 0.0F) {
            protectionShield = 0.0F;
            protectionEnabled = false;
            protectionTicksRemaining = 0;
            protectionCooldownTicks = Math.max(
                protectionCooldownTicks,
                GameplayConfig.PROTECTION_COOLDOWN_TICKS
            );
        }
        return amount - absorbed;
    }

    /** Advances duration, cooldown and delayed shield regeneration. */
    public boolean tickProtection() {
        boolean changed = false;
        if (protectionCooldownTicks > 0) {
            protectionCooldownTicks--;
            changed = true;
        }
        if (protectionRecoveryDelayTicks > 0) {
            protectionRecoveryDelayTicks--;
            changed = true;
        } else if (protectionCooldownTicks == 0
            && protectionShield < GameplayConfig.PROTECTION_MAX_SHIELD) {
            protectionShield = Math.min(
                GameplayConfig.PROTECTION_MAX_SHIELD,
                protectionShield + GameplayConfig.PROTECTION_RECOVERY_PER_TICK
            );
            changed = true;
        }
        if (protectionEnabled && !grailWormAscended && protectionTicksRemaining > 0) {
            protectionTicksRemaining--;
            changed = true;
            if (protectionTicksRemaining == 0) {
                protectionEnabled = false;
            }
        }
        return changed;
    }

    public int size() {
        return GameplayConfig.MENU_CAPACITY;
    }

    public int pageCount() {
        return pages.size();
    }

    public int selectedPage() {
        return selectedPage;
    }

    public boolean selectPage(int page) {
        if (!validPage(page)) {
            return false;
        }
        selectedPage = page;
        return true;
    }

    public ItemStack getItem(int page, int slot) {
        return validPage(page) && validSlot(slot) ? pages.get(page).get(slot) : ItemStack.EMPTY;
    }

    public void setItem(int page, int slot, ItemStack stack) {
        if (!validPage(page) || !validSlot(slot)) {
            return;
        }
        ItemStack stored = stack.copy();
        stored.limitSize(stored.getMaxStackSize());
        pages.get(page).set(slot, stored);
    }

    public ItemStack removeItem(int page, int slot, int amount) {
        return validPage(page) && validSlot(slot)
            ? ContainerHelper.removeItem(pages.get(page), slot, amount)
            : ItemStack.EMPTY;
    }

    public ItemStack removeItemNoUpdate(int page, int slot) {
        return validPage(page) && validSlot(slot)
            ? ContainerHelper.takeItem(pages.get(page), slot)
            : ItemStack.EMPTY;
    }

    public boolean isEmpty() {
        return pages.stream().flatMap(List::stream).allMatch(ItemStack::isEmpty);
    }

    public void clear() {
        pages.clear();
        pages.add(createPage());
        selectedPage = 0;
    }

    public int occupiedSlots() {
        int result = 0;
        for (List<ItemStack> page : pages) {
            for (ItemStack item : page) {
                if (!item.isEmpty()) {
                    result++;
                }
            }
        }
        return result;
    }

    public boolean canFit(ItemStack incoming) {
        if (incoming.isEmpty()) {
            return true;
        }
        int remaining = incoming.getCount();
        for (List<ItemStack> page : pages) {
            for (ItemStack current : page) {
                if (current.isEmpty()) {
                    remaining -= incoming.getMaxStackSize();
                } else if (ItemStack.isSameItemSameComponents(current, incoming)) {
                    remaining -= Math.max(0, current.getMaxStackSize() - current.getCount());
                }
                if (remaining <= 0) {
                    return true;
                }
            }
        }

        int availableNewPages = Math.max(0, maximumPages() - pages.size());
        long newPageCapacity = (long) availableNewPages * GameplayConfig.MENU_CAPACITY * incoming.getMaxStackSize();
        return remaining <= newPageCapacity;
    }

    /**
     * Inserts a copy and returns any remainder. Callers must use {@link #canFit(ItemStack)}
     * before starting a server-side transaction.
     */
    public ItemStack insert(ItemStack incoming) {
        ItemStack remainder = incoming.copy();
        List<NonNullList<ItemStack>> working = copyPages();

        for (List<ItemStack> page : working) {
            for (ItemStack current : page) {
                if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, remainder)) {
                    int moved = Math.min(remainder.getCount(), current.getMaxStackSize() - current.getCount());
                    if (moved > 0) {
                        current.grow(moved);
                        remainder.shrink(moved);
                    }
                }
                if (remainder.isEmpty()) {
                    break;
                }
            }
        }

        insertIntoEmptySlots(working, remainder);
        boolean createdPage = false;
        while (!remainder.isEmpty() && working.size() < maximumPages()) {
            working.add(createPage());
            createdPage = true;
            insertIntoEmptySlots(List.of(working.getLast()), remainder);
        }

        if (remainder.isEmpty()) {
            pages.clear();
            pages.addAll(working);
            if (createdPage) {
                selectedPage = pages.size() - 1;
            }
            selectedPage = Math.clamp(selectedPage, 0, pages.size() - 1);
        }
        return remainder;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", GameplayConfig.DATA_VERSION);
        tag.putBoolean("Learned", learned);
        tag.putBoolean("ImaginaryAffinity", imaginaryAffinity);
        tag.putBoolean("CrestWormAssimilated", crestWormAssimilated);
        tag.putBoolean("CrestWormExpelled", crestWormExpelled);
        tag.putBoolean("GrailWormAscended", grailWormAscended);
        tag.putBoolean("SakuraImaginaryEffects", sakuraImaginaryEffects);
        tag.putBoolean("ForbiddenMagicUnlocked", forbiddenMagicUnlocked);
        tag.putString("ShadowArtState", shadowArtState.name());
        tag.putString("ShadowArtMode", shadowArtMode.serializedName());
        tag.putIntArray("ShadowArtRespawnTicks", shadowArtRespawnTicks);
        tag.putInt("GrailErosion", grailErosion);
        tag.putLong("GrailErosionCompletionTick", grailErosionCompletionTick);
        tag.putString("CursedArmorState", cursedArmorState.serializedName());
        tag.putLong("CursedArmorStageStartTick", cursedArmorStageStartTick);
        tag.putLong("LastServantOutcomeDay", lastServantOutcomeDay);
        tag.putBoolean("GrailPowerSnapshotPresent", grailPowerSnapshotPresent);
        if (grailPowerSnapshotPresent) {
            tag.putDouble("PreGrailMana", preGrailMana);
            tag.putDouble("PreGrailMaximumMana", preGrailMaximumMana);
            tag.putDouble("PreGrailManaRegen", preGrailManaRegen);
            tag.putDouble("PreGrailRestoreSpeed", preGrailRestoreSpeed);
            tag.putBoolean("PreGrailWaterAttribute", preGrailWaterAttribute);
            tag.putBoolean("PreGrailMagus", preGrailMagus);
        }
        tag.putString("MagicMode", magicMode.serializedName());
        tag.putString("BlackMudSummonMode", blackMudSummonMode.serializedName());
        tag.putString("SelectedShadowCommandMode", selectedShadowCommandMode.serializedName());
        tag.putString("ActiveShadowCommandMode", activeShadowCommandMode.serializedName());
        tag.putBoolean("ShadowAttackAround", shadowAttackAround);
        tag.putInt("ShadowDismissalGeneration", shadowDismissalGeneration);
        tag.putLong("CompletedNightMissionDay", completedNightMissionDay);
        tag.putBoolean("ProtectionEnabled", protectionEnabled);
        tag.putInt("ProtectionTicks", protectionTicksRemaining);
        tag.putFloat("ProtectionShield", protectionShield);
        tag.putInt("ProtectionCooldown", protectionCooldownTicks);
        tag.putInt("ProtectionRecoveryDelay", protectionRecoveryDelayTicks);
        tag.putInt("SelectedPage", selectedPage);

        ListTag serializedServants = new ListTag();
        for (CorruptedServantRecord record : corruptedServants) {
            CompoundTag recordTag = new CompoundTag();
            recordTag.putUUID("RosterId", record.rosterId);
            recordTag.put("Entity", record.entitySnapshot.copy());
            if (record.activeEntityId != null) {
                recordTag.putUUID("ActiveEntity", record.activeEntityId);
            }
            serializedServants.add(recordTag);
        }
        tag.put("CorruptedServants", serializedServants);

        ListTag serializedSelection = new ListTag();
        for (UUID rosterId : selectedCorruptedServants) {
            CompoundTag selectedTag = new CompoundTag();
            selectedTag.putUUID("RosterId", rosterId);
            serializedSelection.add(selectedTag);
        }
        tag.put("SelectedCorruptedServants", serializedSelection);

        ListTag serializedPages = new ListTag();
        for (NonNullList<ItemStack> page : pages) {
            CompoundTag pageTag = new CompoundTag();
            ContainerHelper.saveAllItems(pageTag, page, provider);
            serializedPages.add(pageTag);
        }
        tag.put("Pages", serializedPages);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        int savedDataVersion = tag.getInt("DataVersion");
        pages.clear();
        corruptedServants.clear();
        selectedCorruptedServants.clear();
        if (tag.contains("Pages", Tag.TAG_LIST)) {
            ListTag serializedPages = tag.getList("Pages", Tag.TAG_COMPOUND);
            int decodedPages = Math.min(serializedPages.size(), MAX_DECODED_PAGES);
            for (int index = 0; index < decodedPages; index++) {
                NonNullList<ItemStack> page = createPage();
                ContainerHelper.loadAllItems(serializedPages.getCompound(index), page, provider);
                pages.add(page);
            }
        }
        if (pages.isEmpty()) {
            // Version 1 stored its single page directly in the attachment root.
            NonNullList<ItemStack> legacyPage = createPage();
            ContainerHelper.loadAllItems(tag, legacyPage, provider);
            pages.add(legacyPage);
        }
        learned = tag.getBoolean("Learned");
        imaginaryAffinity = tag.contains("ImaginaryAffinity") ? tag.getBoolean("ImaginaryAffinity") : learned;
        crestWormAssimilated = tag.getBoolean("CrestWormAssimilated");
        crestWormExpelled = tag.getBoolean("CrestWormExpelled");
        grailWormAscended = tag.getBoolean("GrailWormAscended");
        sakuraImaginaryEffects = tag.getBoolean("SakuraImaginaryEffects");
        forbiddenMagicUnlocked = tag.getBoolean("ForbiddenMagicUnlocked");
        try { shadowArtState = ShadowArtState.valueOf(tag.getString("ShadowArtState")); }
        catch (IllegalArgumentException exception) { shadowArtState = ShadowArtState.LOCKED; }
        shadowArtMode = ShadowArtMode.fromSerialized(tag.getString("ShadowArtMode"));
        int[] savedRibbonTicks = tag.getIntArray("ShadowArtRespawnTicks");
        for (int i = 0; i < shadowArtRespawnTicks.length && i < savedRibbonTicks.length; i++) {
            shadowArtRespawnTicks[i] = Math.clamp(savedRibbonTicks[i], 0, GameplayConfig.SHADOW_ART_RIBBON_RESPAWN_TICKS);
        }
        grailErosion = tag.contains("GrailErosion", Tag.TAG_INT)
            ? Math.clamp(tag.getInt("GrailErosion"), 0, 3)
            : grailWormAscended ? 3 : 0;
        if (savedDataVersion < 11 && !tag.contains("ShadowArtState", Tag.TAG_STRING)
            && grailErosion >= 3 && grailWormAscended) {
            shadowArtState = ShadowArtState.AVAILABLE;
        }
        grailErosionCompletionTick = tag.getLong("GrailErosionCompletionTick");
        cursedArmorState = tag.contains("CursedArmorState", Tag.TAG_STRING)
            ? CursedArmorState.fromSerialized(tag.getString("CursedArmorState"))
            : CursedArmorState.NONE;
        cursedArmorStageStartTick = tag.getLong("CursedArmorStageStartTick");
        lastServantOutcomeDay = tag.contains("LastServantOutcomeDay", Tag.TAG_LONG)
            ? tag.getLong("LastServantOutcomeDay")
            : Long.MIN_VALUE;
        grailPowerSnapshotPresent = tag.getBoolean("GrailPowerSnapshotPresent");
        preGrailMana = tag.getDouble("PreGrailMana");
        preGrailMaximumMana = tag.getDouble("PreGrailMaximumMana");
        preGrailManaRegen = tag.getDouble("PreGrailManaRegen");
        preGrailRestoreSpeed = tag.getDouble("PreGrailRestoreSpeed");
        preGrailWaterAttribute = tag.getBoolean("PreGrailWaterAttribute");
        preGrailMagus = tag.getBoolean("PreGrailMagus");
        magicMode = MagicMode.fromSerialized(tag.getString("MagicMode"));
        blackMudSummonMode = BlackMudSummonMode.fromSerialized(tag.getString("BlackMudSummonMode"));
        selectedShadowCommandMode = ShadowCommandMode.fromSerialized(tag.getString("SelectedShadowCommandMode"));
        activeShadowCommandMode = ShadowCommandMode.fromSerialized(tag.getString("ActiveShadowCommandMode"));
        shadowAttackAround = tag.getBoolean("ShadowAttackAround");
        shadowDismissalGeneration = tag.getInt("ShadowDismissalGeneration");
        completedNightMissionDay = tag.contains("CompletedNightMissionDay", Tag.TAG_LONG)
            ? tag.getLong("CompletedNightMissionDay")
            : Long.MIN_VALUE;
        if (activeShadowCommandMode == ShadowCommandMode.ATTACK_AROUND) {
            activeShadowCommandMode = ShadowCommandMode.FREE;
            shadowAttackAround = true;
        } else if (activeShadowCommandMode == ShadowCommandMode.DISMISS
            || activeShadowCommandMode == ShadowCommandMode.HUNT) {
            activeShadowCommandMode = ShadowCommandMode.FREE;
        }
        protectionTicksRemaining = Math.clamp(
            tag.getInt("ProtectionTicks"),
            0,
            GameplayConfig.PROTECTION_DURATION_TICKS
        );
        protectionEnabled = tag.contains("ProtectionEnabled")
            ? tag.getBoolean("ProtectionEnabled")
            : protectionTicksRemaining > 0;
        protectionShield = tag.contains("ProtectionShield", Tag.TAG_FLOAT)
            ? Math.clamp(tag.getFloat("ProtectionShield"), 0.0F, GameplayConfig.PROTECTION_MAX_SHIELD)
            : GameplayConfig.PROTECTION_MAX_SHIELD;
        protectionCooldownTicks = Math.clamp(
            tag.getInt("ProtectionCooldown"),
            0,
            GameplayConfig.PROTECTION_COOLDOWN_TICKS
        );
        protectionRecoveryDelayTicks = Math.clamp(
            tag.getInt("ProtectionRecoveryDelay"),
            0,
            GameplayConfig.PROTECTION_RECOVERY_DELAY_TICKS
        );
        selectedPage = Math.clamp(tag.getInt("SelectedPage"), 0, pages.size() - 1);
        if (tag.contains("CorruptedServants", Tag.TAG_LIST)) {
            ListTag serializedServants = tag.getList("CorruptedServants", Tag.TAG_COMPOUND);
            for (int index = 0; index < serializedServants.size(); index++) {
                CompoundTag recordTag = serializedServants.getCompound(index);
                if (!recordTag.hasUUID("RosterId") || !recordTag.contains("Entity", Tag.TAG_COMPOUND)) {
                    continue;
                }
                corruptedServants.add(new CorruptedServantRecord(
                    recordTag.getUUID("RosterId"),
                    recordTag.getCompound("Entity").copy(),
                    recordTag.hasUUID("ActiveEntity") ? recordTag.getUUID("ActiveEntity") : null
                ));
            }
        }
        if (tag.contains("SelectedCorruptedServants", Tag.TAG_LIST)) {
            ListTag serializedSelection = tag.getList("SelectedCorruptedServants", Tag.TAG_COMPOUND);
            for (int index = 0; index < serializedSelection.size(); index++) {
                CompoundTag selectedTag = serializedSelection.getCompound(index);
                if (selectedTag.hasUUID("RosterId")) {
                    selectedCorruptedServants.add(selectedTag.getUUID("RosterId"));
                }
            }
            selectedCorruptedServants.retainAll(
                corruptedServants.stream().map(CorruptedServantRecord::rosterId).toList()
            );
        } else {
            for (CorruptedServantRecord record : corruptedServants) {
                selectedCorruptedServants.add(record.rosterId);
            }
        }
    }

    private void writeToNetwork(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(GameplayConfig.DATA_VERSION);
        buffer.writeBoolean(learned);
        buffer.writeBoolean(imaginaryAffinity);
        buffer.writeBoolean(crestWormAssimilated);
        buffer.writeBoolean(crestWormExpelled);
        buffer.writeBoolean(grailWormAscended);
        buffer.writeBoolean(sakuraImaginaryEffects);
        buffer.writeBoolean(forbiddenMagicUnlocked);
        buffer.writeEnum(shadowArtState);
        buffer.writeEnum(shadowArtMode);
        for (int ticks : shadowArtRespawnTicks) buffer.writeVarInt(ticks);
        buffer.writeVarInt(grailErosion);
        buffer.writeBoolean(grailErosionPending());
        buffer.writeEnum(cursedArmorState);
        buffer.writeVarLong(cursedArmorStageStartTick);
        buffer.writeEnum(magicMode);
        buffer.writeEnum(blackMudSummonMode);
        buffer.writeEnum(selectedShadowCommandMode);
        buffer.writeEnum(activeShadowCommandMode);
        buffer.writeBoolean(shadowAttackAround);
        buffer.writeInt(shadowDismissalGeneration);
        buffer.writeBoolean(protectionEnabled);
        buffer.writeVarInt(protectionTicksRemaining);
        buffer.writeFloat(protectionShield);
        buffer.writeVarInt(protectionCooldownTicks);
        buffer.writeVarInt(pages.size());
        buffer.writeVarInt(selectedPage);
    }

    private static ImaginarySpaceData readFromNetwork(RegistryFriendlyByteBuf buffer) {
        buffer.readVarInt(); // Reserved data-format version for future migrations.
        ImaginarySpaceData data = new ImaginarySpaceData();
        data.learned = buffer.readBoolean();
        data.imaginaryAffinity = buffer.readBoolean();
        data.crestWormAssimilated = buffer.readBoolean();
        data.crestWormExpelled = buffer.readBoolean();
        data.grailWormAscended = buffer.readBoolean();
        data.sakuraImaginaryEffects = buffer.readBoolean();
        data.forbiddenMagicUnlocked = buffer.readBoolean();
        data.shadowArtState = buffer.readEnum(ShadowArtState.class);
        data.shadowArtMode = buffer.readEnum(ShadowArtMode.class);
        for (int i = 0; i < data.shadowArtRespawnTicks.length; i++) {
            data.shadowArtRespawnTicks[i] = Math.clamp(buffer.readVarInt(), 0, GameplayConfig.SHADOW_ART_RIBBON_RESPAWN_TICKS);
        }
        data.grailErosion = Math.clamp(buffer.readVarInt(), 0, 3);
        data.grailErosionCompletionTick = buffer.readBoolean() ? Long.MAX_VALUE : 0L;
        data.cursedArmorState = buffer.readEnum(CursedArmorState.class);
        data.cursedArmorStageStartTick = buffer.readVarLong();
        data.magicMode = buffer.readEnum(MagicMode.class);
        data.blackMudSummonMode = buffer.readEnum(BlackMudSummonMode.class);
        data.selectedShadowCommandMode = buffer.readEnum(ShadowCommandMode.class);
        data.activeShadowCommandMode = buffer.readEnum(ShadowCommandMode.class);
        data.shadowAttackAround = buffer.readBoolean();
        data.shadowDismissalGeneration = buffer.readInt();
        data.protectionEnabled = buffer.readBoolean();
        data.protectionTicksRemaining = Math.clamp(
            buffer.readVarInt(),
            0,
            GameplayConfig.PROTECTION_DURATION_TICKS
        );
        data.protectionShield = Math.clamp(
            buffer.readFloat(),
            0.0F,
            GameplayConfig.PROTECTION_MAX_SHIELD
        );
        data.protectionCooldownTicks = Math.clamp(
            buffer.readVarInt(),
            0,
            GameplayConfig.PROTECTION_COOLDOWN_TICKS
        );
        int encodedPages = buffer.readVarInt();
        if (encodedPages < 1 || encodedPages > MAX_DECODED_PAGES) {
            throw new IllegalArgumentException("Invalid Imaginary Space page count: " + encodedPages);
        }
        data.pages.clear();
        for (int page = 0; page < encodedPages; page++) {
            data.pages.add(createPage());
        }
        data.selectedPage = Math.clamp(buffer.readVarInt(), 0, encodedPages - 1);
        return data;
    }

    private List<NonNullList<ItemStack>> copyPages() {
        List<NonNullList<ItemStack>> result = new ArrayList<>(pages.size());
        for (List<ItemStack> page : pages) {
            NonNullList<ItemStack> copy = createPage();
            for (int slot = 0; slot < page.size(); slot++) {
                copy.set(slot, page.get(slot).copy());
            }
            result.add(copy);
        }
        return result;
    }

    public static final class CorruptedServantRecord {
        private final UUID rosterId;
        private final CompoundTag entitySnapshot;
        @Nullable
        private UUID activeEntityId;

        private CorruptedServantRecord(UUID rosterId, CompoundTag entitySnapshot, @Nullable UUID activeEntityId) {
            this.rosterId = rosterId;
            this.entitySnapshot = entitySnapshot;
            this.activeEntityId = activeEntityId;
        }

        public UUID rosterId() {
            return rosterId;
        }

        public CompoundTag entitySnapshot() {
            return entitySnapshot.copy();
        }

        @Nullable
        public UUID activeEntityId() {
            return activeEntityId;
        }
    }

    public record GrailPowerSnapshot(
        double mana,
        double maximumMana,
        double manaRegen,
        double restoreSpeed,
        boolean waterAttribute,
        boolean magus
    ) {
    }

    private static void insertIntoEmptySlots(List<NonNullList<ItemStack>> targetPages, ItemStack remainder) {
        for (NonNullList<ItemStack> page : targetPages) {
            for (int slot = 0; slot < page.size() && !remainder.isEmpty(); slot++) {
                if (page.get(slot).isEmpty()) {
                    int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
                    page.set(slot, remainder.copyWithCount(moved));
                    remainder.shrink(moved);
                }
            }
        }
    }

    private static NonNullList<ItemStack> createPage() {
        return NonNullList.withSize(GameplayConfig.MENU_CAPACITY, ItemStack.EMPTY);
    }

    private int maximumPages() {
        return Math.max(pages.size(), GameplayConfig.MAX_SPACE_PAGES.get());
    }

    private boolean validPage(int page) {
        return page >= 0 && page < pages.size();
    }

    private boolean validSlot(int slot) {
        return slot >= 0 && slot < GameplayConfig.MENU_CAPACITY;
    }
}


