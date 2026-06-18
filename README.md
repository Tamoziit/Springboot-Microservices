# Springboot-Microservices
A microservices implementation with Springboot

### gRPC
An open-source RPC framework that leverages HTTP/2 connections b/w microservices using **"Protocol Buffer"**, allowing low-latency communication b/w services.
In other words:
- **REST**: for client-server communication using `JSON` format.
- **gRPC**: for inter-service communication in a microservice architecture using `Protobuf` format for high throughput & low-latency data transfer.

NB: Both gRPC & REST use HTTP under the hood.
![description](system-design/gRPC.png)

*Our workflow:*
- When a user creates an account in the `patient-service` -->
- The `patient-service` creates the user record in the DB -->
- It then fires a `gRPC` request via its `gRPC Client` to the `gRPC Server` of `billing-service`.
- The `billing-service` then creates a billing account but itself.
- `billing_service.proto`: A `Protobuf` file used to generate the gRPC client & server corresponding to a particular microservice (here, `billing-service`). Any changes to its corresponding microservice is translated to other microservices extending it, thus scaling perfectly in a microservices architecture.