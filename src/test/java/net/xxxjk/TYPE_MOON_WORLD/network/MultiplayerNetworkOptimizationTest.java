package net.xxxjk.TYPE_MOON_WORLD.network;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MultiplayerNetworkOptimizationTest {
   @Test
   void fullPlayerVariableAndManaSyncSkipUnchangedSnapshots() throws IOException {
      String source = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/network/TypeMoonWorldModVariables.java"));

      assertTrue(source.contains("forceSyncPlayerVariables(Entity entity)"));
      assertTrue(source.contains("syncPlayerVariables(entity, true)"));
      assertTrue(source.contains("fullSyncSnapshotSent"));
      assertTrue(source.contains("fullSyncSnapshotHash"));
      assertTrue(source.contains("snapshot.hashCode()"));
      assertTrue(source.contains("manaSyncSnapshotSent"));
      assertTrue(source.contains("manaSyncSnapshotHash"));
      assertTrue(source.contains("payload.hashCode()"));
      assertTrue(source.contains("resetSyncSnapshots()"));
   }

   @Test
   void highFrequencyInputPacketsUseCompactDirectionEncodingAndServerGuards() throws IOException {
      String paleRider = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/network/PaleRiderPossessionInputMessage.java"));
      String servantFlight = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/network/ServantCardFlightMessage.java"));
      String manaBurst = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/network/ManaBurstInputMessage.java"));

      assertTrue(paleRider.contains("PacketFlow.SERVERBOUND"));
      assertTrue(paleRider.contains("ServerPacketRateLimiter.allow(player, \"pale_rider_possession_input\", 2)"));
      assertTrue(paleRider.contains("buf.writeByte(encodeUnit(msg.forward))"));
      assertTrue(paleRider.contains("decodeUnit(buf.readByte())"));

      assertTrue(servantFlight.contains("buffer.writeByte(encodeUnit(message.forward))"));
      assertTrue(servantFlight.contains("decodeUnit(buffer.readByte())"));

      assertTrue(manaBurst.contains("buffer.writeByte(encodeUnit(message.forward))"));
      assertTrue(manaBurst.contains("decodeUnit(buffer.readByte())"));
   }

   @Test
   void clientInputSyncAvoidsRepeatedUnchangedPackets() throws IOException {
      String source = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/TypeMoonWorldModKeyMappings.java"));

      assertTrue(source.contains("lastPaleRiderYaw"));
      assertTrue(source.contains("Mth.degreesDifference(lastPaleRiderYaw, yaw)"));
      assertTrue(source.contains("paleRiderInputKeepaliveChecks"));
      assertTrue(source.contains("keepaliveChecks = moving ? 2 : 10"));
      assertTrue(source.contains("clearPaleRiderInputState()"));

      assertTrue(source.contains("lastManaBurstForward"));
      assertTrue(source.contains("manaBurstInputKeepaliveChecks"));
      assertTrue(source.contains("++manaBurstInputKeepaliveChecks >= 3"));
      assertTrue(source.contains("clearManaBurstInputState()"));
   }
}
