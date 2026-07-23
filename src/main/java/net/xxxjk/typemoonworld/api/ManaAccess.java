package net.xxxjk.typemoonworld.api;

public interface ManaAccess {
   double current();
   double maximum();
   boolean tryConsume(double amount);
   void add(double amount);
}
