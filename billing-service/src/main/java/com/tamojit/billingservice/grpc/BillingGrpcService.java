package com.tamojit.billingservice.grpc;

import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    @Override
    public void createBillingAccount(
        billing.BillingRequest billingRequest,
        StreamObserver<billing.BillingResponse> responseObserver // streaming messages b/w gRPC client & server, back-n-forth
    ) {
        log.info("createBillingAccount request received : {}", billingRequest.toString());

        // <-- any business logic -->

        // a mock-up Billing Account streamed back be gRPC server
        BillingResponse response = BillingResponse.newBuilder()
            .setAccountId("123445")
            .setStatus("ACTIVE")
            .build();

        responseObserver.onNext(response); // sending response from gRPC server --> gRPC client
        responseObserver.onCompleted(); // ending the current cycle of gRPC response [multiple responses can be sent in a single cycle using onNext()]
    }
}
