#!/bin/bash
set -e # Stops the script if any command fails

# Delete stack if it exists
aws --endpoint-url=http://localhost:4566 cloudformation delete-stack \
    --stack-name patient-management

# Wait for deletion to complete before redeploying
aws --endpoint-url=http://localhost:4566 cloudformation wait stack-delete-complete \
    --stack-name patient-management

aws --endpoint-url=http://localhost:4566 cloudformation deploy \
    --stack-name patient-management \
    --template-file "./cdk.out/localstack.template.json"

aws --endpoint-url=http://localhost:4566 elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName" --output text