package com.tamojit.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalStack extends Stack {
    private final Vpc vpc;
    private final Cluster ecsCluster;

    // Stack configuration & resources provisioning
    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = createVpc();

        DatabaseInstance authServiceDb = createDatabase("AuthServiceDB", "auth-service-db");
        DatabaseInstance patientServiceDb = createDatabase("PatientServiceDB", "patient-service-db");

        CfnHealthCheck authDbHealthCheck = createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");
        CfnHealthCheck patientDbHealthCheck = createDbHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");

        CfnCluster mskCluster = createMskCluster();

        this.ecsCluster = createEcsCluster();

        FargateService authService = createFargateService(
            "AuthService",
            "auth-service",
            List.of(4005),
            authServiceDb,
            Map.of("JWT_SECRET", "jiboncholchenaaarshojapothetaiaajohaanshikonomotey")
        );
        authService.getNode().addDependency(authServiceDb);
        authService.getNode().addDependency(authDbHealthCheck);

        FargateService billingService = createFargateService(
            "BillingService",
            "billing-service",
            List.of(4001, 9001),
            null,
            null
        );

        FargateService analyticsService = createFargateService(
            "AnalyticsService",
            "analytics-service",
            List.of(4002),
            null,
            null
        );
        analyticsService.getNode().addDependency(mskCluster);

        FargateService patientService = createFargateService(
            "PatientService",
            "patient-service",
            List.of(4000),
            patientServiceDb,
            Map.of(
                "BILLING_SERVICE_ADDRESS", "host.docker.internal",
                "BILLING_SERVICE_GRPC_PORT", "9001"
            )
        );
        patientService.getNode().addDependency(patientServiceDb);
        patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);
    }

    private Vpc createVpc() {
        return Vpc.Builder
            .create(this, "PatientManagementVPC") // VPC created for the current scoped Stack
            .vpcName("PatientManagementVPC")
            .maxAzs(2)
            .build();
    }

    private DatabaseInstance createDatabase(String id, String dbName) {
        return DatabaseInstance.Builder
            .create(this, id)
            .engine(DatabaseInstanceEngine.postgres(
                PostgresInstanceEngineProps.builder()
                    .version(PostgresEngineVersion.VER_17_2)
                    .build()
            ))
            .vpc(vpc) // created in current VPC
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
            .allocatedStorage(20)
            .credentials(Credentials.fromGeneratedSecret("admin_user"))
            .databaseName(dbName)
            .removalPolicy(RemovalPolicy.DESTROY) // on removing the Stack, DB Storage is also destroyed
            .build();
    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
        return CfnHealthCheck.Builder
            .create(this, id)
            .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty
                .builder()
                .type("TCP") // TCP conn. to check DB entrypoint
                .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                .ipAddress(db.getDbInstanceEndpointAddress())
                .requestInterval(30) // Health Checks at 30 sec interval
                .failureThreshold(3) // report after 3 failures
                .build())
            .build();
    }

    private CfnCluster createMskCluster() {
        return CfnCluster.Builder
            .create(this, "MskCluster")
            .clusterName("kafka-cluster")
            .kafkaVersion("2.8.0")
            .numberOfBrokerNodes(1)
            .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty
                .builder()
                .instanceType("kafka.m5.xlarge")
                .clientSubnets(vpc.getPrivateSubnets()
                    .stream()
                    .map(ISubnet::getSubnetId)
                    .collect(Collectors.toList()))
                .brokerAzDistribution("DEFAULT") // which broker belongs to which AZ
                .build()
            )
            .build();
    }

    // each individual service inside this cluster can be referenced by <service-name>.<cluster-name>
    // eg: auth-service.patient-management.local
    private Cluster createEcsCluster() {
        return Cluster.Builder
            .create(this, "PatientManagementCluster")
            .vpc(vpc)
            .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                .name("patient-management.local")
                .build())
            .build();
    }

    // ECS task to run a container
    private FargateService createFargateService(
        String id,
        String imageName,
        List<Integer> ports,
        DatabaseInstance db,
        Map<String, String> additionalEnvVars
    ) {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
            .create(this, id + "Task")
            .cpu(256)
            .memoryLimitMiB(512)
            .build();

        ContainerDefinitionOptions.Builder containerOptions = ContainerDefinitionOptions.builder()
            .image(ContainerImage.fromRegistry(imageName))
            .portMappings(ports.stream()
                .map(port -> PortMapping.builder()
                    .containerPort(port)
                    .hostPort(port)
                    .protocol(Protocol.TCP)
                    .build())
                .toList())
            .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(LogGroup.Builder // Logging group
                    .create(this, id + "LogGroup")
                    .logGroupName("/ecs/" + imageName)
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .retention(RetentionDays.ONE_DAY) // log retention timeline
                    .build())
                .streamPrefix(imageName)
                .build()));

        // Kafka server plugging for all containers
        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

        if (additionalEnvVars != null) {
            envVars.putAll(additionalEnvVars);
        }

        if (db != null) {
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s:%s-db".formatted(
                db.getDbInstanceEndpointAddress(),
                db.getDbInstanceEndpointPort(),
                imageName
            ));

            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
            envVars.put("SPRING_DATASOURCE_PASSWORD", db.getSecret().secretValueFromJson("password").toString()); // localstack creates a db password for RDS by default in AWS Secret Manager
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000"); // Retrying to connect to DB before failure
        }

        // attaching env to container
        containerOptions.environment(envVars);

        // combining Fargate task with ECS container
        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        // attaching Fargate task to ECS cluster
        return FargateService.Builder
            .create(this, id)
            .cluster(ecsCluster)
            .taskDefinition(taskDefinition)
            .assignPublicIp(false) // private (internal) ECS container
            .serviceName(imageName)
            .build();
    }

    public static void main(final String[] args) {
        System.out.println("App Synthesizing in progress...");

        // CloudFormation Template out dir.
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        // Synthesizer - converts Java IaC --> CloudFormation Template
        StackProps props = StackProps.builder()
            .synthesizer(new BootstraplessSynthesizer()) // skip initial bootstrapping of AWS CDK env for LocalStack
            .build();

        new LocalStack(app, "localstack", props);

        app.synth(); // synthesizing CDK into Template
        System.out.println("App Synthesizing completed");
    }
}
