package com.taller.proxy.service;

import com.taller.proxy.config.SimulationProperties;
import com.taller.proxy.exception.ConflictException;
import com.taller.proxy.exception.InvalidParameterException;
import com.taller.proxy.exception.PaymentFailedException;
import com.taller.proxy.exception.ResourceNotFoundException;
import com.taller.proxy.model.Payment;
import com.taller.proxy.model.PaymentMethod;
import com.taller.proxy.model.PaymentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/** Payments microservice. A configurable share of the calls (10% by default) fails on purpose. */
@Service
public class PaymentService extends SimulatedService {

    public static final String SERVICE_ID = "payments";

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(5000);

    public PaymentService(SimulationProperties properties) {
        super(SERVICE_ID, properties);
        register("processPayment", p -> processPayment(p.requireString("orderId"),
                p.requirePositiveAmount("amount"), p.optionalString("method", PaymentMethod.CARD.name())));
        register("getPaymentStatus", p -> getPaymentStatus(p.requireString("paymentId")));
        register("refundPayment", p -> refundPayment(p.requireString("paymentId")));
    }

    @Override
    protected void beforeExecute(String operation) {
        if (ThreadLocalRandom.current().nextDouble() < properties().paymentFailureRate()) {
            throw new PaymentFailedException("Payment gateway failure while executing '" + operation
                    + "' (simulated random failure)");
        }
    }

    public Payment processPayment(String orderId, BigDecimal amount, String method) {
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterException("'method' must be one of CARD, PSE, TRANSFER, CASH");
        }
        Instant now = Instant.now();
        Payment payment = new Payment("PAY-" + sequence.incrementAndGet(), orderId, amount, paymentMethod,
                PaymentStatus.COMPLETED, now, now);
        payments.put(payment.paymentId(), payment);
        return payment;
    }

    public Payment getPaymentStatus(String paymentId) {
        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found: " + paymentId);
        }
        return payment;
    }

    public Payment refundPayment(String paymentId) {
        return payments.compute(paymentId, (id, current) -> {
            if (current == null) {
                throw new ResourceNotFoundException("Payment not found: " + id);
            }
            if (current.status() == PaymentStatus.REFUNDED) {
                throw new ConflictException("Payment already refunded: " + id);
            }
            return current.withStatus(PaymentStatus.REFUNDED);
        });
    }
}
