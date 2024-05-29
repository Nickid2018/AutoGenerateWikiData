package net.minecraft.server.level;

public class Ticket<T> implements Comparable<Ticket<T>> {

    @Override
    public int compareTo(Ticket<T> o) {
        throw new RuntimeException();
    }
}
