package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    // These can be moved to config
    private static final int MAX_TICKETS_PER_PURCHASE = 20;
    private static final int INFANT_TICKET_PRICE = 0;
    private static final int CHILD_TICKET_PRICE = 10;
    private static final int ADULT_TICKET_PRICE = 20;

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    private int numAdultTickets = 0;
    private int numChildTickets = 0;
    private int numInfantTickets = 0;

    public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        if (ticketTypeRequests == null) {
            throw new InvalidPurchaseException("TicketTypeRequest is null.");
        }

        List<TicketType> ticketTypes = ticketTypeRequests.getTicketType();

        if (ticketTypes == null || ticketTypes.isEmpty()) {
            throw new InvalidPurchaseException("No tickets requested.");
        }

        if (ticketTypes.size() > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException("Maximum of " + MAX_TICKETS_PER_PURCHASE + " tickets allowed per purchase.");
        }

        setNumOfTickets(ticketTypes);

        validateTickets();

        makePayment(account);

        reserveSeats();

        userConfirmation();
    }

    protected void setNumOfTickets(ticketTypes) {
        for (TicketType type : ticketTypes) {
            switch (type) {
                case ADULT:
                    this.numAdultTickets++;
                    break;
                case CHILD:
                    this.numChildTickets++;
                    break;
                case INFANT:
                    this.numInfantTickets++;
                    break;
                default:
                    throw new TicketPurchaseException("Invalid ticket type requested: " + type);
            }
        }
    }

    protected void validateTickets() {
        if (this.numAdultTickets == 0) {
            throw new TicketPurchaseException("At least one adult ticket must be purchased.");
        }

        if (this.numInfantTickets > this.numAdultTickets) {
            throw new TicketPurchaseException("Cannot purchase more infant tickets than adult tickets.");
        }
    }

    protected void makePayment(Account account) {
        int totalTicketPrice = (numAdultTickets * ADULT_TICKET_PRICE) + (numChildTickets * CHILD_TICKET_PRICE);

        if (totalTicketPrice > accountId.getBalance()) {
            throw new TicketPurchaseException("Insufficient funds to purchase tickets.");
        }

        try {
            paymentService.makePayment(totalTicketPrice, account);
        } catch (PaymentException e) {
            throw new TicketPurchaseException("Error making payment: " + e.getMessage());
        }
    }

    protected void reserveSeats() {
        List<Seat> seatsToReserve = new ArrayList<>();
        for (int i = 0; i < numAdultTickets; i++) {
            seatsToReserve.add(reservationService.reserveSeat());
        }
        for (int i = 0; i < numChildTickets; i++) {
            seatsToReserve.add(reservationService.reserveSeat());
        }
    }

    protected void userConfirmation() {
        System.out.println("Tickets purchased successfully.");
        System.out.println("Adult tickets: " + numAdultTickets);
        System.out.println("Child tickets: " + numChildTickets);
        System.out.println("Infant tickets: " + numInfantTickets);
        System.out.println("Total cost: £" + totalTicketPrice);
    }
}
