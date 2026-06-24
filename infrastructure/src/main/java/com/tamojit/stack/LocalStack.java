package com.tamojit.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

public class LocalStack extends Stack {
    private final Vpc vpc;

    // Stack configuration & resources provisioning
    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = createVpc();

        DatabaseInstance authServiceDb = createDatabase("AuthServiceDB", "auth-service-db");
        DatabaseInstance patientServiceDb = createDatabase("PatientServiceDB", "patient-service-db");

        CfnHealthCheck authDbHealthCheck = createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");
        CfnHealthCheck patientDbHealthCheck = createDbHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");
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
