package com.task_flow.backend.engine;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SampleWorkflow {

    private final WorkflowRegistry registry;
  
    public SampleWorkflow(WorkflowRegistry registry) {  
        this.registry = registry; 
    }

    @PostConstruct 
    public void init() {
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

            // Step 3: Run Fraud Check (Runs in parallel with Step 2 after ingest)

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

            builder.step("run-fraud-check", context -> {
                Map<String, Object> order = (Map<String, Object>) context.get("ingest-order");
                System.out.println("[Pipeline] Step 3: Running ML fraud analysis for User " + order.get("userId"));
                return Map.of("riskScore", 0.02, "status", "APPROVED");
            }, 3, List.of("ingest-order"));
        });

    }
}

