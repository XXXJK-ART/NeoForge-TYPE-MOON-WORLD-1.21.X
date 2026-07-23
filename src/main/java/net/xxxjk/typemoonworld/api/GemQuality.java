package net.xxxjk.typemoonworld.api;

public enum GemQuality {
   POOR(50), NORMAL(100), HIGH(200);
   private final int capacity;
   GemQuality(int capacity) { this.capacity = capacity; }
   public int capacity() { return this.capacity; }
}
