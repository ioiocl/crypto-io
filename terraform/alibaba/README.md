# Alibaba Cloud Deployment

This directory contains Terraform configuration for deploying Finbot on Alibaba Cloud.

## Architecture

- **ECS Instance**: Ubuntu 22.04 with Docker
- **ApsaraDB for Redis**: Managed Redis instance
- **Container Registry**: Private Docker registry
- **VPC**: Isolated network with security groups
- **Elastic IP**: Public IP for external access

## Prerequisites

1. Alibaba Cloud account
2. Access Key and Secret Key
3. Terraform installed (>= 1.0)
4. Docker images built and ready to push

## Deployment Steps

### 1. Configure Variables

Create `terraform.tfvars`:

```hcl
access_key      = "YOUR_ACCESS_KEY"
secret_key      = "YOUR_SECRET_KEY"
region          = "us-west-1"
zone_id         = "us-west-1a"
redis_password  = "YourSecurePassword123!"
polygon_api_key = "LkgydUcNGAFPthknFLbtkvshslkuSNqU"
ssh_cidr        = "YOUR_IP/32"  # Restrict SSH access
environment     = "production"
```

### 2. Initialize Terraform

```bash
terraform init
```

### 3. Plan Deployment

```bash
terraform plan
```

### 4. Apply Configuration

```bash
terraform apply
```

### 5. Build and Push Docker Images

```bash
# Get registry URL
REGISTRY=$(terraform output -raw container_registry_url)

# Login to Alibaba Container Registry
docker login --username=YOUR_USERNAME registry.us-west-1.aliyuncs.com

# Build and push images
cd ../..

docker build -t $REGISTRY/ingestion-service:latest -f ingestion-service/Dockerfile .
docker push $REGISTRY/ingestion-service:latest

docker build -t $REGISTRY/analytics-service:latest -f analytics-service/Dockerfile .
docker push $REGISTRY/analytics-service:latest

docker build -t $REGISTRY/websocket-api:latest -f websocket-api/Dockerfile .
docker push $REGISTRY/websocket-api:latest

docker build -t $REGISTRY/dashboard:latest -f dashboard/Dockerfile ./dashboard
docker push $REGISTRY/dashboard:latest
```

### 6. Access Your Application

```bash
# Get public IP
terraform output ecs_public_ip

# Dashboard
http://<PUBLIC_IP>:3000

# WebSocket API
ws://<PUBLIC_IP>:8080/ws/market/{symbol}
```

## Outputs

- `ecs_public_ip`: Public IP of ECS instance
- `redis_connection_domain`: Redis connection endpoint
- `container_registry_url`: Docker registry URL
- `dashboard_url`: Dashboard access URL
- `websocket_api_url`: WebSocket API URL

## Security Considerations

1. **SSH Access**: Restrict `ssh_cidr` to your IP only
2. **Redis Password**: Use strong password
3. **API Keys**: Store in environment variables
4. **Security Groups**: Review and adjust rules as needed
5. **SSL/TLS**: Add load balancer with SSL certificate for production

## Monitoring

### SSH into ECS Instance

```bash
ssh ubuntu@<PUBLIC_IP>
```

### Check Services

```bash
cd /opt/finbot
docker-compose ps
docker-compose logs -f
```

### Redis Connection

```bash
# From ECS instance
redis-cli -h <REDIS_HOST> -a <REDIS_PASSWORD>
```

## Scaling

### Vertical Scaling

Change `instance_type` in `terraform.tfvars`:

```hcl
instance_type = "ecs.c6.xlarge"  # More CPU/RAM
```

### Horizontal Scaling

Deploy multiple ECS instances behind a load balancer:

1. Create Application Load Balancer (ALB)
2. Add ECS instances to backend pool
3. Configure health checks

## Cost Optimization

1. Use **Pay-As-You-Go** for development
2. Switch to **Subscription** for production (discounts)
3. Enable **Auto Scaling** for ECS
4. Use **Spot Instances** for non-critical workloads
5. Monitor with **CloudMonitor** to optimize resources

## Cleanup

```bash
terraform destroy
```

**Warning**: This will delete all resources including data in Redis.

## Troubleshooting

### Cannot Connect to ECS

1. Check security group rules
2. Verify Elastic IP is attached
3. Check SSH key permissions

### Services Not Starting

1. SSH into ECS: `ssh ubuntu@<PUBLIC_IP>`
2. Check logs: `cd /opt/finbot && docker-compose logs`
3. Verify environment variables in `.env`

### Redis Connection Failed

1. Verify Redis instance is running
2. Check VPC security group allows ECS access
3. Verify password is correct

## Additional Resources

- [Alibaba Cloud ECS Documentation](https://www.alibabacloud.com/help/ecs)
- [ApsaraDB for Redis](https://www.alibabacloud.com/help/redis)
- [Container Registry](https://www.alibabacloud.com/help/container-registry)
