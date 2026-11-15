package com.captain.servant.lancelot;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KnightArmoryRulesTest {
    @Test
    void aircraftExpiresEvenDuringLaunchGrace() {
        long deadline = 1000L + KnightArmoryRules.A10_DURATION_TICKS;
        assertFalse(KnightArmoryRules.shouldExplode(1299, deadline, 299, false, false));
        assertTrue(KnightArmoryRules.shouldExplode(1300, deadline, 1, false, false));
    }

    @Test
    void launchGraceOnlySuppressesCollisionsUntilItsBoundary() {
        assertFalse(KnightArmoryRules.shouldExplode(10, 1200, 19, true, true));
        assertTrue(KnightArmoryRules.shouldExplode(20, 1200, 20, true, false));
        assertTrue(KnightArmoryRules.shouldExplode(20, 1200, 20, false, true));
        assertFalse(KnightArmoryRules.shouldExplode(20, 1200, 20, false, false));
    }

    @Test
    void menuIncludesAircraftWithoutReusingAnotherSkillSlot() {
        assertSame(KnightArmoryEntry.A10, KnightArmoryEntry.byNetworkId(5));
        assertNull(KnightArmoryEntry.byNetworkId(-1));
        assertNull(KnightArmoryEntry.A10.itemId());
        assertEquals(450.0, KnightArmoryEntry.A10.manaCost());
        assertEquals(3600, KnightArmoryEntry.A10.cooldownTicks());
    }

    @Test
    void npcMinigunAndAircraftRequireHalfHealthAndBothHaveThreeMinuteCooldown() {
        assertFalse(KnightArmoryRules.npcCanUseAtHealth(KnightArmoryEntry.MINIGUN, 11.0F, 20.0F));
        assertTrue(KnightArmoryRules.npcCanUseAtHealth(KnightArmoryEntry.MINIGUN, 10.0F, 20.0F));
        assertFalse(KnightArmoryRules.npcCanUseAtHealth(KnightArmoryEntry.A10, 11.0F, 20.0F));
        assertTrue(KnightArmoryRules.npcCanUseAtHealth(KnightArmoryEntry.A10, 10.0F, 20.0F));
        assertTrue(KnightArmoryRules.npcCanUseAtHealth(KnightArmoryEntry.IRON_ROD, 20.0F, 20.0F));
        assertTrue(KnightArmoryRules.npcCanUseAtHealth(KnightArmoryEntry.MP5, 20.0F, 20.0F));
        assertEquals(3600, KnightArmoryEntry.MINIGUN.cooldownTicks());
        assertEquals(3600, KnightArmoryEntry.A10.cooldownTicks());
    }

    @Test
    void npcArmoryChoiceKeepsIronRodAsTheDefaultAndSeparatesHeavyWeapons() {
        assertSame(KnightArmoryEntry.IRON_ROD,
                KnightArmoryRules.chooseNpcArmoryEntry(false, true, true, 0.0F));
        assertSame(KnightArmoryEntry.A10,
                KnightArmoryRules.chooseNpcArmoryEntry(true, true, true, 0.10F));
        assertSame(KnightArmoryEntry.MINIGUN,
                KnightArmoryRules.chooseNpcArmoryEntry(true, true, true, 0.35F));
        assertSame(KnightArmoryEntry.IRON_ROD,
                KnightArmoryRules.chooseNpcArmoryEntry(true, true, true, 0.90F));
    }

    @Test
    void npcArmoryChoiceFallsBackWhenOnlyOneHeavyWeaponIsAvailable() {
        assertSame(KnightArmoryEntry.A10,
                KnightArmoryRules.chooseNpcArmoryEntry(true, true, false, 0.10F));
        assertSame(KnightArmoryEntry.MINIGUN,
                KnightArmoryRules.chooseNpcArmoryEntry(true, false, true, 0.10F));
        assertSame(KnightArmoryEntry.IRON_ROD,
                KnightArmoryRules.chooseNpcArmoryEntry(true, false, false, 0.10F));
    }

    @Test
    void npcCannotStackAircraftAndMinigunOrReuseHeavyWeaponDuringLock() {
        assertFalse(KnightArmoryRules.npcCanChooseHeavyWeapon(true, false, 100, 0));
        assertFalse(KnightArmoryRules.npcCanChooseHeavyWeapon(false, true, 100, 0));
        assertFalse(KnightArmoryRules.npcCanChooseHeavyWeapon(false, false, 100, 101));
        assertTrue(KnightArmoryRules.npcCanChooseHeavyWeapon(false, false, 101, 101));
    }
}
