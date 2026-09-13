package net.xxxjk.TYPE_MOON_WORLD.chain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HeavenChainMountedBindingTest {
    private static final Path BINDING_SERVICE = Path.of(
        "src/main/java/net/xxxjk/TYPE_MOON_WORLD/chain/service/BindingService.java"
    );

    @Test
    void mountedGroupKeepsItsPassengerRelationshipWhileBound() throws IOException {
        String source = Files.readString(BINDING_SERVICE);
        String holdTarget = methodBody(source, "private static void holdTarget");

        assertTrue(source.contains("Entity root = target.getRootVehicle();"));
        assertTrue(source.contains("addLivingPassengers(owner, livingRoot, result, seen);"));
        assertTrue(holdTarget.contains("if (passenger)"));
        assertTrue(holdTarget.indexOf("if (passenger)") < holdTarget.indexOf("target.setPos("));
        assertFalse(holdTarget.contains("target.stopRiding();"));
    }

    @Test
    void customMountMovementStopsWhileBound() throws IOException {
        for (String mount : new String[] {
            "entity/IskandarMountEntity.java",
            "entity/ZhaoYunHakuryuEntity.java",
            "entity/MedusaPegasusEntity.java",
            "entity/GordiusWheelEntity.java"
        }) {
            String source = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD", mount));
            assertTrue(source.contains("BindingService.isBound"), mount);
        }
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        int nextMethod = source.indexOf("\n    private static ", start + signature.length());
        return source.substring(start, nextMethod);
    }
}
