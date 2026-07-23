package net.xxxjk.typemoonworld.api;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Client-side storage. The server payload is the authority; local datapack reloads are not enough. */
public final class DefinitionSnapshotStore {
   private static final AtomicReference<DefinitionSnapshot> CURRENT = new AtomicReference<>(new DefinitionSnapshot(0L, Map.of()));
   private DefinitionSnapshotStore() { }
   public static DefinitionSnapshot current() { return CURRENT.get(); }
   public static void replace(DefinitionSnapshot snapshot) { if (snapshot != null) CURRENT.set(snapshot); }
   public static boolean has(String section, String id) { return current().section(section).contains("\"" + id + "\""); }
}
