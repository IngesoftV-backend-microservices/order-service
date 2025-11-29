package com.selimhorri.app.metrics;

import org.springframework.stereotype.Component;

import com.selimhorri.app.domain.enums.OrderStatus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderBusinessMetrics {

    private final MeterRegistry meterRegistry;
    
    // Contadores
    private Counter ordersCreatedCounter;
    
    // Timer para medir duración de procesamiento
    private Timer orderProcessingTimer;
    
    // Gauge para valor total de órdenes (se actualizará dinámicamente)
    private Double orderValueTotal = 0.0;

    public void initializeMetrics() {
        // Contador de órdenes creadas
        this.ordersCreatedCounter = Counter.builder("ecommerce.orders.created.total")
                .description("Total number of orders created")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        // Timer para duración de procesamiento
        this.orderProcessingTimer = Timer.builder("ecommerce.orders.processing.duration.seconds")
                .description("Time taken to process orders")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        // Gauge para valor total de órdenes
        Gauge.builder("ecommerce.orders.value.total", () -> orderValueTotal)
                .description("Total value of all orders")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        log.info("Order business metrics initialized");
    }

    /**
     * Registra la creación de una nueva orden
     */
    public void recordOrderCreated(OrderStatus status, Double orderFee) {
        if (ordersCreatedCounter == null) {
            initializeMetrics();
        }
        
        ordersCreatedCounter.increment();
        
        Counter.builder("ecommerce.orders.by.status")
                .description("Orders by status")
                .tag("status", status.name())
                .tag("service", "order-service")
                .register(meterRegistry)
                .increment();
        
        if (orderFee != null) {
            synchronized (this) {
                orderValueTotal += orderFee;
            }
        }
        
        log.debug("Recorded order created: status={}, fee={}", status, orderFee);
    }

    /**
     * Registra un cambio de estado de orden
     */
    public void recordOrderStatusChange(OrderStatus oldStatus, OrderStatus newStatus) {
        // Decrementar el estado anterior (solo si no es el primer estado)
        if (oldStatus != null) {
            Counter.builder("ecommerce.orders.by.status")
                    .description("Orders by status")
                    .tag("status", oldStatus.name())
                    .tag("service", "order-service")
                    .register(meterRegistry)
                    .increment(-1);
        }
        
        // Incrementar el nuevo estado
        Counter.builder("ecommerce.orders.by.status")
                .description("Orders by status")
                .tag("status", newStatus.name())
                .tag("service", "order-service")
                .register(meterRegistry)
                .increment();
        
        log.debug("Recorded order status change: {} -> {}", oldStatus, newStatus);
    }

    /**
     * Registra el tiempo de procesamiento de una orden
     */
    public Timer.Sample startOrderProcessingTimer() {
        if (orderProcessingTimer == null) {
            initializeMetrics();
        }
        return Timer.start(meterRegistry);
    }

    /**
     * Detiene el timer y registra la duración
     */
    public void stopOrderProcessingTimer(Timer.Sample sample) {
        if (sample != null && orderProcessingTimer != null) {
            sample.stop(orderProcessingTimer);
        }
    }
}

