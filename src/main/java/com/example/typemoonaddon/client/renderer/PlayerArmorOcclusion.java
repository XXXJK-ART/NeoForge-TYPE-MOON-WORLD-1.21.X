package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.data.ImaginarySpaceData.CursedArmorState;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonItems;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;

/** Temporarily hides only the player-skin surfaces covered by a full-body Geo wearable. */
public final class PlayerArmorOcclusion {
    private static final Map<Player, VisibilitySnapshot> SNAPSHOTS = new IdentityHashMap<>();

    public static void apply(Player player, PlayerRenderer renderer) {
        restore(player);
        OcclusionMask mask = mask(player);
        if (mask == OcclusionMask.NONE) {
            return;
        }

        PlayerModel<?> model = renderer.getModel();
        SNAPSHOTS.put(player, VisibilitySnapshot.capture(model));
        if (mask.hideBody) {
            hide(model.body);
        }
        if (mask.hideJacket) {
            hide(model.jacket);
        }
        if (mask.hideRightArm) {
            hide(model.rightArm);
            hide(model.rightSleeve);
        }
        if (mask.hideLeftArm) {
            hide(model.leftArm);
            hide(model.leftSleeve);
        }
        if (mask.hideRightLeg) {
            hide(model.rightLeg);
            hide(model.rightPants);
        }
        if (mask.hideLeftLeg) {
            hide(model.leftLeg);
            hide(model.leftPants);
        }
    }

    public static void restore(Player player) {
        VisibilitySnapshot snapshot = SNAPSHOTS.remove(player);
        if (snapshot != null) {
            snapshot.restore();
        }
    }

    private static OcclusionMask mask(Player player) {
        CursedArmorState cursedState = player.getData(AddonAttachments.CURSED_ARMOR_VIEW.get()).state();
        if (cursedState == CursedArmorState.ACTIVE) {
            return OcclusionMask.CURSED_ARMOR;
        }
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(AddonItems.VOID_RING_REGALIA.get())) {
            return OcclusionMask.VOID_RING;
        }
        return OcclusionMask.NONE;
    }

    private static void hide(ModelPart part) {
        part.visible = false;
    }

    private enum OcclusionMask {
        NONE(false, false, false, false, false, false),
        VOID_RING(false, true, false, true, false, false),
        CURSED_ARMOR(true, true, true, true, false, false);

        private final boolean hideBody;
        private final boolean hideJacket;
        private final boolean hideRightArm;
        private final boolean hideLeftArm;
        private final boolean hideRightLeg;
        private final boolean hideLeftLeg;

        OcclusionMask(
            boolean hideBody,
            boolean hideJacket,
            boolean hideRightArm,
            boolean hideLeftArm,
            boolean hideRightLeg,
            boolean hideLeftLeg
        ) {
            this.hideBody = hideBody;
            this.hideJacket = hideJacket;
            this.hideRightArm = hideRightArm;
            this.hideLeftArm = hideLeftArm;
            this.hideRightLeg = hideRightLeg;
            this.hideLeftLeg = hideLeftLeg;
        }
    }

    private record VisibilitySnapshot(
        ModelPart body,
        boolean bodyVisible,
        ModelPart jacket,
        boolean jacketVisible,
        ModelPart rightArm,
        boolean rightArmVisible,
        ModelPart rightSleeve,
        boolean rightSleeveVisible,
        ModelPart leftArm,
        boolean leftArmVisible,
        ModelPart leftSleeve,
        boolean leftSleeveVisible,
        ModelPart rightLeg,
        boolean rightLegVisible,
        ModelPart rightPants,
        boolean rightPantsVisible,
        ModelPart leftLeg,
        boolean leftLegVisible,
        ModelPart leftPants,
        boolean leftPantsVisible
    ) {
        private static VisibilitySnapshot capture(PlayerModel<?> model) {
            return new VisibilitySnapshot(
                model.body,
                model.body.visible,
                model.jacket,
                model.jacket.visible,
                model.rightArm,
                model.rightArm.visible,
                model.rightSleeve,
                model.rightSleeve.visible,
                model.leftArm,
                model.leftArm.visible,
                model.leftSleeve,
                model.leftSleeve.visible,
                model.rightLeg,
                model.rightLeg.visible,
                model.rightPants,
                model.rightPants.visible,
                model.leftLeg,
                model.leftLeg.visible,
                model.leftPants,
                model.leftPants.visible
            );
        }

        private void restore() {
            body.visible = bodyVisible;
            jacket.visible = jacketVisible;
            rightArm.visible = rightArmVisible;
            rightSleeve.visible = rightSleeveVisible;
            leftArm.visible = leftArmVisible;
            leftSleeve.visible = leftSleeveVisible;
            rightLeg.visible = rightLegVisible;
            rightPants.visible = rightPantsVisible;
            leftLeg.visible = leftLegVisible;
            leftPants.visible = leftPantsVisible;
        }
    }

    private PlayerArmorOcclusion() {
    }
}
