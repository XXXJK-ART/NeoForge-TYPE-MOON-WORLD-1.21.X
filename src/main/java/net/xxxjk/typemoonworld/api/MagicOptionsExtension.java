package net.xxxjk.typemoonworld.api;

/** Marker for a client-side magic configuration extension. */
@FunctionalInterface
public interface MagicOptionsExtension {
   void open(Object clientScreenContext);
}
