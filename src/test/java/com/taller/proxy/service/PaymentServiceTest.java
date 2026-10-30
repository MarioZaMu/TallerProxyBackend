package com.taller.proxy.service;

import com.taller.proxy.config.SimulationProperties;
import com.taller.proxy.exception.ConflictException;
import com.taller.proxy.exception.InvalidParameterException;
import com.taller.proxy.exception.PaymentFailedException;
import com.taller.proxy.exception.ResourceNotFoundException;
import com.taller.proxy.model.Payment;
import com.taller.proxy.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentServiceTest {

    private final PaymentService reliable = new PaymentService(new SimulationProperties(0, 0, 0));

    @Test
    void processGetAndRefundPayment() {
        Payment payment = (Payment) reliable.execute("processPayment",
                Map.of("orderId", "ORD-1001", "amount", 150000, "method", "PSE"));
        assertEquals(PaymentStatus.COMPLETED, payment.status());
        assertEquals(payment, reliable.execute("getPaymentStatus", Map.of("paymentId", payment.paymentId())));

        Payment refunded = (Payment) reliable.execute("refundPayment", Map.of("paymentId", payment.paymentId()));
        assertEquals(PaymentStatus.REFUNDED, refunded.status());
        assertThrows(ConflictException.class,
                () -> reliable.execute("refundPayment", Map.of("paymentId", payment.paymentId())));
    }

    @Test
    void validatesInput() {
        assertThrows(InvalidParameterException.class,
                () -> reliable.execute("processPayment", Map.of("orderId", "ORD-1", "amount", 0)));
        assertThrows(InvalidParameterException.class,
                () -> reliable.execute("processPayment", Map.of("orderId", "ORD-1", "amount", 5, "method", "BITCOIN")));
        assertThrows(ResourceNotFoundException.class,
                () -> reliable.execute("getPaymentStatus", Map.of("paymentId", "PAY-0")));
    }

    @Test
    void failureRateOfOneAlwaysFails() {
        PaymentService broken = new PaymentService(new SimulationProperties(1.0, 0, 0));
        for (int i = 0; i < 20; i++) {
            assertThrows(PaymentFailedException.class,
                    () -> broken.execute("processPayment", Map.of("orderId", "ORD-1", "amount", 10)));
        }
    }

    @Test
    void unknownOperationIsNotAffectedByRandomFailures() {
        PaymentService broken = new PaymentService(new SimulationProperties(1.0, 0, 0));
        assertThrows(com.taller.proxy.exception.UnknownOperationException.class,
                () -> broken.execute("nope", Map.of()));
    }

    @Test
    void defaultFailureRateIsAboutTenPercent() {
        PaymentService service = new PaymentService(new SimulationProperties(0.10, 0, 0));
        int failures = 0;
        int calls = 4000;
        for (int i = 0; i < calls; i++) {
            try {
                service.execute("processPayment", Map.of("orderId", "ORD-1", "amount", 10));
            } catch (PaymentFailedException e) {
                failures++;
            }
        }
        double rate = failures / (double) calls;
        assertTrue(rate > 0.07 && rate < 0.13, "observed failure rate " + rate);
    }
}
