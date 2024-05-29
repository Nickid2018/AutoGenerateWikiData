package net.minecraft.server.level;

import net.minecraft.util.SortedArraySet;

public abstract class DistanceManager {

    public SortedArraySet<Ticket<?>> getTickets(long l2) {
        throw new RuntimeException();
    }

    public void removeTicket(long l, Ticket<?> ticket) {
        throw new RuntimeException();
    }
}
