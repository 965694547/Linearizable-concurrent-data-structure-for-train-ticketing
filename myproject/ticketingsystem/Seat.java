package ticketingsystem;

public interface Seat{
    public int buyTicket(int departure, int arrival);
    public boolean refundTicket(Ticket ticket, int i);
    public int inquery(int departure, int arrival);
}