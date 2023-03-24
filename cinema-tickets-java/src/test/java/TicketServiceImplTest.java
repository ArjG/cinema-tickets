import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import thirdparty.*;

import java.util.Arrays;
import java.util.Collections;

public class TicketServiceImplTest {

    @Mock
    private TicketPaymentService paymentService;

    @Mock
    private SeatReservationService reservationService;

    private TicketServiceImpl ticketService;

    private Account account;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
        account = new Account(1, 100);
    }

    @Test
    public void purchaseTickets_InvalidRequest_NullTicketTypeRequest() {
        Assertions.assertThrows(TicketPurchaseException.class, () -> ticketService.purchaseTickets(null, account));
    }

    @Test
    public void purchaseTickets_InvalidRequest_EmptyTicketTypeList() {
        TicketTypeRequest request = new TicketTypeRequest(Collections.emptyList());
        Assertions.assertThrows(TicketPurchaseException.class, () -> ticketService.purchaseTickets(request, account));
    }

    @Test
    public void purchaseTickets_InvalidRequest_TooManyTickets() {
        TicketTypeRequest request = new TicketTypeRequest(Collections.nCopies(21, TicketType.ADULT));
        Assertions.assertThrows(TicketPurchaseException.class, () -> ticketService.purchaseTickets(request, account));
    }

    @Test
    public void purchaseTickets_InvalidRequest_NoAdultTickets() {
        TicketTypeRequest request = new TicketTypeRequest(Collections.nCopies(3, TicketType.CHILD));
        Assertions.assertThrows(TicketPurchaseException.class, () -> ticketService.purchaseTickets(request, account));
    }

    @Test
    public void purchaseTickets_InvalidRequest_TooManyInfantTickets() {
        TicketTypeRequest request = new TicketTypeRequest(Arrays.asList(TicketType.ADULT, TicketType.ADULT, TicketType.INFANT, TicketType.INFANT));
        Assertions.assertThrows(TicketPurchaseException.class, () -> ticketService.purchaseTickets(request, account));
    }

    @Test
    public void purchaseTickets_InvalidRequest_InvalidTicketType() {
        TicketTypeRequest request = new TicketTypeRequest(Arrays.asList(TicketType.ADULT, TicketType.ADULT, null));
        Assertions.assertThrows(TicketPurchaseException.class, () -> ticketService.purchaseTickets(request, account));
    }

    @Test
    public void purchaseTickets_InvalidRequest_InsufficientFunds() throws PaymentException {
        Mockito.doThrow(PaymentException.class).when(paymentService).makePayment(Mockito.anyInt(), Mockito.any(Account.class));
        TicketTypeRequest request = new TicketTypeRequest(Arrays.asList(TicketType.ADULT, TicketType.CHILD));
        Assertions.assertThrows(TicketPurchaseException.class, () -> ticketService.purchaseTickets(request, account));
    }

    @Test
    public void purchaseTickets_ValidRequest() throws PaymentException {
        Mockito.doNothing().when(paymentService).makePayment(Mockito.anyInt(), Mockito.any(Account.class));
        Mockito.when(reservationService.reserveSeat()).thenReturn(new Seat());
        TicketTypeRequest request = new TicketTypeRequest(Arrays.asList(TicketType.ADULT, TicketType.CHILD, TicketType.INFANT));
        ticketService.purchaseTickets(request, account);
        Mockito.verify(paymentService, Mockito.times(1)).makePayment(30, account);
        Mockito.verify(reservationService, Mockito.times(2)).reserveSeat();
    }
}
