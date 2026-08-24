package com.task_flow.backend.engine;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class SampleWorkflow {

    private final WorkflowRegistry registry;
  
    public SampleWorkflow(WorkflowRegistry registry) {  
        this.registry = registry; 
    }

    @PostConstruct 
    public void init() {
        // Shared counter to simulate a transient failure on the first execution
        AtomicInteger fraudAttemptCounter = new AtomicInteger(0);

        registry.register("sample-workflow", builder -> {
            
            // Step 1: Ingest the raw order payload
            builder.step("ingest-order", context -> {
                System.out.println("[Pipeline] Step 1: Ingesting raw order payload...");
                Map<String, Object> orderData = Map.of("orderId", "ORD-9988", "amount", 149.99, "userId", "user_123");
                return orderData;
            });

            // Step 2: Check inventory (Runs after ingest)
            builder.step("check-inventory", context -> {
                Map<String, Object> order = (Map<String, Object>) context.get("ingest-order");
                System.out.println("[Pipeline] Step 2: Checking warehouse inventory for Order " + order.get("orderId"));
                return Map.of("inventoryReserved", true, "warehouseId", "WH-East");
            }, 3, List.of("ingest-order"));

            // Step 3: Flaky Fraud Check (Fails on 1st attempt, passes on 2nd to test retry logic)
            builder.step("run-fraud-check", context -> {
                Map<String, Object> order = (Map<String, Object>) context.get("ingest-order");
                int attempt = fraudAttemptCounter.incrementAndGet();
                
                System.out.println("[Pipeline] Step 3: Running ML fraud analysis for User " + order.get("userId") + " (Attempt " + attempt + ")");
                
                if (attempt == 1) {
                    throw new RuntimeException("Transient connection timeout reaching external Fraud Microservice API");
                }
                
                return Map.of("riskScore", 0.02, "status", "APPROVED");
            }, 3, List.of("ingest-order"));

            // Step 4: Charge Payment (FAN-IN: Waits for inventory AND fraud check to both succeed)
            builder.step("charge-payment", context -> {
                Map<String, Object> inventory = (Map<String, Object>) context.get("check-inventory");
                Map<String, Object> fraud = (Map<String, Object>) context.get("run-fraud-check");
                
                System.out.println("[Pipeline] Step 4: Inventory confirmed (" + inventory.get("warehouseId") + ") & Fraud score clean (" + fraud.get("riskScore") + "). Charging card...");
                return Map.of("transactionId", "TXN-776655", "charged", true);
            }, 3, List.of("check-inventory", "run-fraud-check"));

            // Step 5: Ship Order (Final step, depends on successful payment)
            builder.step("ship-order", context -> {
                Map<String, Object> payment = (Map<String, Object>) context.get("charge-payment");
                System.out.println("[Pipeline] Step 5: Payment verified (" + payment.get("transactionId") + "). Generating shipping label...");
                return Map.of("trackingNumber", "TRK-XYZ-888999");
            }, 3, List.of("charge-payment"));
        });

        registry.register("delay-stress-test", builder -> {
            
            // Step 1: Initial starting point (Immediate trigger)
            builder.step("stress-step-1", context -> {
                System.out.println("[STRESS TEST] Step 1 executed.");
                return Map.of("data", "init");
            });

            // Step 2: Branch A with a 1-second delay (Depends on Step 1)
            builder.stepWithDelay("stress-step-2A", Duration.ofSeconds(1), context -> {
                System.out.println("[STRESS TEST] Step 2A executed (after 1s delay).");
                return Map.of("branchA", "done");
            }, 3, List.of("stress-step-1"));

            // Step 3: Branch B with a 2-second delay (Depends on Step 1)
            builder.stepWithDelay("stress-step-2B", Duration.ofSeconds(2), context -> {
                System.out.println("[STRESS TEST] Step 2B executed (after 2s delay).");
                return Map.of("branchB", "done");
            }, 3, List.of("stress-step-1"));

            // Step 4: Fan-in step combining both branches with an additional 1-second delay
            builder.stepWithDelay("stress-step-3-fanin", Duration.ofSeconds(1), context -> {
                Map<String, Object> a = (Map<String, Object>) context.get("stress-step-2A");
                Map<String, Object> b = (Map<String, Object>) context.get("stress-step-2B");
                System.out.println("[STRESS TEST] Step 3 (Fan-in) executed (after 1s delay) with A=" + a.get("branchA") + " and B=" + b.get("branchB"));
                return Map.of("status", "merged");
            }, 3, List.of("stress-step-2A", "stress-step-2B"));

            // Step 5: Final completion step dependencies met
            builder.step("stress-step-final", context -> {
                System.out.println("[STRESS TEST] Final step succeeded. Stress test completed!");
                return Map.of("result", "completed successfully");
            }, 3, List.of("stress-step-3-fanin"));
        });

        // ============================================
        // ULTIMATE STRESS TEST: Tests Everything
        // ============================================
        // Features tested:
        // - Retries with failures
        // - Parallel execution
        // - Delays/Timers
        // - Child workflows (nested and parallel)
        // - Fan-in synchronization
        // - Complex DAG dependencies
        
        AtomicInteger paymentRetryCounter = new AtomicInteger(0);
        AtomicInteger notificationRetryCounter = new AtomicInteger(0);
        
        // Child Workflow 1: Payment Processing (with retry)
        registry.register("payment-child-workflow", builder -> {
            builder.step("validate-payment", context -> {
                String orderId = (String) context.get("orderId");
                Double amount = (Double) context.get("amount");
                System.out.println("[PAYMENT-CHILD] Validating payment for order " + orderId + " ($" + amount + ")");
                return Map.of("valid", true);
            });
            
            builder.stepWithDelay("charge-with-delay", Duration.ofMillis(500), context -> {
                int attempt = paymentRetryCounter.incrementAndGet();
                System.out.println("[PAYMENT-CHILD] Attempting to charge card (Attempt " + attempt + ")");
                
                if (attempt == 1) {
                    throw new RuntimeException("Payment gateway timeout - simulated failure");
                }
                
                String orderId = (String) context.get("orderId");
                return Map.of("transactionId", "TXN-" + UUID.randomUUID().toString().substring(0, 8), "orderId", orderId);
            }, 3, List.of("validate-payment"));
            
            builder.step("send-payment-receipt", context -> {
                Map<String, Object> charge = (Map<String, Object>) context.get("charge-with-delay");
                System.out.println("[PAYMENT-CHILD] Payment successful! TxnID: " + charge.get("transactionId"));
                return Map.of("receiptSent", true, "transactionId", charge.get("transactionId"));
            }, 3, List.of("charge-with-delay"));
        });
        
        // Child Workflow 2: Inventory Check (with delay)
        registry.register("inventory-child-workflow", builder -> {
            builder.stepWithDelay("check-warehouse", Duration.ofMillis(300), context -> {
                String orderId = (String) context.get("orderId");
                System.out.println("[INVENTORY-CHILD] Checking inventory for order " + orderId);
                return Map.of("available", true, "warehouse", "WH-Central");
            }, 3, List.of());
            
            builder.step("reserve-items", context -> {
                System.out.println("[INVENTORY-CHILD] Reserving items in warehouse");
                return Map.of("reserved", true, "reservationId", "RES-" + UUID.randomUUID().toString().substring(0, 8));
            }, 3, List.of("check-warehouse"));
        });
        
        // Child Workflow 3: Shipping Label Generation
        registry.register("shipping-child-workflow", builder -> {
            builder.step("generate-label", context -> {
                String orderId = (String) context.get("orderId");
                System.out.println("[SHIPPING-CHILD] Generating shipping label for order " + orderId);
                return Map.of("trackingNumber", "TRACK-" + UUID.randomUUID().toString().substring(0, 8));
            });
            
            builder.stepWithDelay("notify-carrier", Duration.ofMillis(400), context -> {
                int attempt = notificationRetryCounter.incrementAndGet();
                System.out.println("[SHIPPING-CHILD] Notifying carrier (Attempt " + attempt + ")");
                
                if (attempt == 1) {
                    throw new RuntimeException("Carrier API unreachable - simulated failure");
                }
                
                return Map.of("carrierNotified", true);
            }, 3, List.of("generate-label"));
        });
        
        // Parent Workflow: Ultimate Stress Test
        registry.register("ultimate-stress-test", builder -> {
            // Step 1: Initialize order
            builder.step("create-order", context -> {
                String customerId = (String) context.get("customerId");
                if (customerId == null) {
                        customerId = "CUST-DEFAULT-999"; // Fallback or throw explicit error
                    }
                System.out.println("[ULTIMATE-STRESS] Creating order for customer: " + customerId);
                return Map.of(
                    "orderId", "ORD-" + UUID.randomUUID().toString().substring(0, 8),
                    "amount", 299.99,
                    "customerId", customerId
                );
            });
            
            // Step 2: Validate customer (with delay)
            builder.stepWithDelay("validate-customer", Duration.ofMillis(200), context -> {
                Map<String, Object> order = (Map<String, Object>) context.get("create-order");
                System.out.println("[ULTIMATE-STRESS] Validating customer for order " + order.get("orderId"));
                return Map.of("customerValid", true, "riskLevel", "low");
            }, 3, List.of("create-order"));
            
            // Parallel Branch A: Payment child workflow
            builder.childWorkflow(
                "process-payment-child",
                "payment-child-workflow",
                parentContext -> {
                    Map<String, Object> order = (Map<String, Object>) parentContext.get("create-order");
                    return Map.of(
                        "orderId", order.get("orderId"),
                        "amount", order.get("amount")
                    );
                },
                List.of("validate-customer")
            );
            
            // Parallel Branch B: Inventory child workflow
            builder.childWorkflow(
                "check-inventory-child",
                "inventory-child-workflow",
                parentContext -> {
                    Map<String, Object> order = (Map<String, Object>) parentContext.get("create-order");
                    return Map.of("orderId", order.get("orderId"));
                },
                List.of("validate-customer")
            );
            
            // Step 3: Fraud check (runs in parallel with children, depends on validate-customer)
            builder.stepWithDelay("run-fraud-analysis", Duration.ofMillis(600), context -> {
                Map<String, Object> order = (Map<String, Object>) context.get("create-order");
                System.out.println("[ULTIMATE-STRESS] Running fraud analysis for order " + order.get("orderId"));
                return Map.of("fraudScore", 0.05, "approved", true);
            }, 3, List.of("validate-customer"));
            
            // Step 4: Fan-in checkpoint (waits for payment, inventory, and fraud check)
            builder.step("verify-order-ready", context -> {
                Map<String, Object> payment = (Map<String, Object>) context.get("process-payment-child");
                Map<String, Object> inventory = (Map<String, Object>) context.get("check-inventory-child");
                Map<String, Object> fraud = (Map<String, Object>) context.get("run-fraud-analysis");
                
                System.out.println("[ULTIMATE-STRESS] All checks passed! Payment: " + payment.get("transactionId") + 
                                   ", Inventory: " + inventory.get("reserved") + 
                                   ", Fraud: " + fraud.get("approved"));
                return Map.of("orderVerified", true);
            }, 3, List.of("process-payment-child", "check-inventory-child", "run-fraud-analysis"));
            
            // Step 5: Spawn shipping child workflow (after verification)
            builder.childWorkflow(
                "arrange-shipping-child",
                "shipping-child-workflow",
                parentContext -> {
                    Map<String, Object> order = (Map<String, Object>) parentContext.get("create-order");
                    return Map.of("orderId", order.get("orderId"));
                },
                List.of("verify-order-ready")
            );
            
            // Step 6: Final confirmation with delay
            builder.stepWithDelay("send-confirmation-email", Duration.ofMillis(300), context -> {
                Map<String, Object> shipping = (Map<String, Object>) context.get("arrange-shipping-child");
                Map<String, Object> payment = (Map<String, Object>) context.get("process-payment-child");
                
                System.out.println("[ULTIMATE-STRESS] ✓ ORDER COMPLETE! Tracking: " + shipping.get("trackingNumber") + 
                                   ", Transaction: " + payment.get("transactionId"));
                return Map.of("emailSent", true, "orderStatus", "completed");
            }, 3, List.of("arrange-shipping-child"));
        });
    }
}
