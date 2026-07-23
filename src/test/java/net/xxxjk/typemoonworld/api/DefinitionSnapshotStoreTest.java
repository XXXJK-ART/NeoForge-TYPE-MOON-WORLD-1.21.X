package net.xxxjk.typemoonworld.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefinitionSnapshotStoreTest {
   @Test
   void replacesOnlyWithBoundedImmutableSnapshot() {
      DefinitionSnapshot snapshot = new DefinitionSnapshot(7L, Map.of("magic", "{\"tmw_test_addon:test_magic\":{}}"));
      DefinitionSnapshotStore.replace(snapshot);
      assertEquals(7L, DefinitionSnapshotStore.current().revision());
      assertTrue(DefinitionSnapshotStore.has("magic", "tmw_test_addon:test_magic"));
   }
}
