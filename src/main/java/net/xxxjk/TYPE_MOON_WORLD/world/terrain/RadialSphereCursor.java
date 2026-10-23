package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/** Merges radial columns so each voxel is visited once, nearest to the center first. */
final class RadialSphereCursor {
   record Offset(int x, int y, int z) {
      long distanceSqr() { return (long)x * x + (long)y * y + (long)z * z; }
   }

   private static final class Column {
      final int x, y;
      int z;
      Column(int x, int y) { this.x = x; this.y = y; }
      long distanceSqr() { return (long)x * x + (long)y * y + (long)z * z; }
      void advance() { z = z > 0 ? -z : 1 - z; }
   }

   private final long radiusSqr;
   private final PriorityQueue<Column> columns = new PriorityQueue<>(Comparator
      .comparingLong(Column::distanceSqr).thenComparingInt(c -> c.x).thenComparingInt(c -> c.y));

   RadialSphereCursor(int radius) {
      if (radius < 0) throw new IllegalArgumentException("Negative sphere radius");
      radiusSqr = (long)radius * radius;
      // Keep one frontier per column, rather than allocating/sorting the whole sphere.
      for (int x = -radius; x <= radius; x++) {
         for (int y = -radius; y <= radius; y++) {
            if ((long)x * x + (long)y * y <= radiusSqr) columns.add(new Column(x, y));
         }
      }
   }

   boolean hasNext() { return !columns.isEmpty(); }

   Offset next() {
      Column column = columns.poll();
      if (column == null) throw new NoSuchElementException();
      Offset offset = new Offset(column.x, column.y, column.z);
      column.advance();
      if (column.distanceSqr() <= radiusSqr) columns.add(column);
      return offset;
   }
}
