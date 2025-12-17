# Google Cloud Platform Deployment

This directory contains Terraform configuration for deploying Finbot on GCP.

## Architecture Options

### Option 1: Compute Engine (VM-based)
- **GCE Instance**: Ubuntu 22.04 with Docker
- **Memorystore for Redis**: Managed Redis instance
- **Artifact Registry**: Private Docker registry
- **VPC Network**: Isolated network with firewall rules

### Option 2: Cloud Run (Serverless)
- **Cloud Run Services**: Fully managed containers
- **Memorystore for Redis**: Shared Redis instance
- **VPC Access Connector**: Private connectivity
- **Auto-scaling**: Built-in horizontal scaling

## Prerequisites

1. GCP account with billing enabled
2. Project created in GCP Console
3. Terraform installed (>= 1.0)
4. gcloud CLI installed and authenticated
5. Docker images built

## Deployment Steps

### 1. Authenticate with GCP

```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
```

### 2. Enable Required APIs

```bash
gcloud services enable compute.googleapis.com
gcloud services enable redis.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable vpcaccess.googleapis.com
```

### 3. Configure Variables

Create `terraform.tfvars`:

```hcl
project_id      = "your-gcp-project-id"
region          = "us-central1"
zone            = "us-central1-a"
machine_type    = "e2-standard-2"
polygon_api_key = "LkgydUcNGAFPthknFLbtkvshslkuSNqU"
ssh_cidr        = "YOUR_IP/32"
environment     = "production"
```

### 4. Initialize Terraform

```bash
terraform init
```

### 5. Plan Deployment

```bash
terraform plan
```

### 6. Apply Configuration

```bash
terraform apply
```

### 7. Build and Push Docker Images

```bash
# Configure Docker for Artifact Registry
gcloud auth configure-docker us-central1-docker.pkg.dev

# Get registry URL
REGISTRY=$(terraform output -raw artifact_registry_url)

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

### 8. Access Your Application

#### Compute Engine Deployment

```bash
# Get VM external IP
terraform output vm_external_ip

# Dashboard
http://<VM_IP>:3000

# WebSocket API
ws://<VM_IP>:8080/ws/market/{symbol}
```

#### Cloud Run Deployment

```bash
# Get Cloud Run URLs
terraform output cloud_run_websocket_url
terraform output cloud_run_ingestion_url
terraform output cloud_run_analytics_url
```

## Outputs

- `vm_external_ip`: External IP of VM (GCE deployment)
- `redis_host`: Memorystore Redis host
- `artifact_registry_url`: Docker registry URL
- `cloud_run_websocket_url`: Cloud Run WebSocket service URL
- `dashboard_url`: Dashboard URL (GCE deployment)

## Deployment Comparison

| Feature | Compute Engine | Cloud Run |
|---------|---------------|-----------|
| **Cost** | Fixed (always running) | Pay per request |
| **Scaling** | Manual/Auto Scaling Groups | Automatic (0 to N) |
| **Management** | More control | Fully managed |
| **WebSocket** | Full support | Limited (HTTP/2) |
| **Best For** | Stateful, long connections | Stateless, HTTP APIs |

**Recommendation**: Use **Compute Engine** for this application due to WebSocket requirements.

## Security Considerations

1. **Firewall Rules**: Restrict SSH access to your IP
2. **Service Account**: Minimal permissions (least privilege)
3. **Redis**: Private VPC connectivity only
4. **Secrets**: Use Secret Manager for sensitive data
5. **SSL/TLS**: Add Cloud Load Balancer with SSL certificate

## Monitoring and Logging

### Cloud Logging

```bash
# View logs
gcloud logging read "resource.type=gce_instance" --limit 50

# Stream logs
gcloud logging tail "resource.type=gce_instance"
```

### Cloud Monitoring

1. Navigate to Cloud Console > Monitoring
2. Create dashboards for:
   - CPU/Memory usage
   - Network traffic
   - Redis operations
   - Application metrics

### SSH into VM

```bash
gcloud compute ssh finbot-vm --zone=us-central1-a
```

### Check Services

```bash
cd /opt/finbot
docker-compose ps
docker-compose logs -f
```

## Scaling

### Vertical Scaling (GCE)

Change machine type:

```bash
gcloud compute instances stop finbot-vm --zone=us-central1-a
gcloud compute instances set-machine-type finbot-vm \
  --machine-type=e2-standard-4 \
  --zone=us-central1-a
gcloud compute instances start finbot-vm --zone=us-central1-a
```

### Horizontal Scaling (Cloud Run)

Cloud Run auto-scales. Configure max instances:

```hcl
metadata {
  annotations = {
    "autoscaling.knative.dev/maxScale" = "20"
    "autoscaling.knative.dev/minScale" = "1"
  }
}
```

### Managed Instance Groups (GCE)

For multiple VMs behind load balancer:

1. Create instance template
2. Create managed instance group
3. Add load balancer
4. Configure auto-scaling policies

## Cost Optimization

1. **Committed Use Discounts**: 1 or 3-year commitments (up to 57% off)
2. **Preemptible VMs**: For non-critical workloads (up to 80% off)
3. **Cloud Run**: Pay only for actual usage
4. **Right-sizing**: Use Cloud Monitoring to optimize machine types
5. **Budget Alerts**: Set up billing alerts

### Estimated Monthly Costs (us-central1)

**Compute Engine Deployment:**
- e2-standard-2 VM: ~$50/month
- Memorystore Redis (1GB): ~$50/month
- Network egress: ~$10-30/month
- **Total**: ~$110-130/month

**Cloud Run Deployment:**
- Cloud Run services: ~$20-50/month (depends on traffic)
- Memorystore Redis: ~$50/month
- VPC Connector: ~$10/month
- **Total**: ~$80-110/month

## Backup and Disaster Recovery

### Redis Backup

```bash
# Enable automatic backups
gcloud redis instances update finbot-redis \
  --region=us-central1 \
  --enable-auth \
  --backup-schedule-frequency=DAILY
```

### VM Snapshots

```bash
# Create snapshot
gcloud compute disks snapshot finbot-vm \
  --snapshot-names=finbot-backup-$(date +%Y%m%d) \
  --zone=us-central1-a
```

## Cleanup

```bash
terraform destroy
```

**Warning**: This will delete all resources including Redis data.

## Troubleshooting

### Cannot Connect to VM

1. Check firewall rules: `gcloud compute firewall-rules list`
2. Verify VM is running: `gcloud compute instances list`
3. Check external IP: `terraform output vm_external_ip`

### Services Not Starting

1. SSH into VM: `gcloud compute ssh finbot-vm`
2. Check Docker: `docker ps -a`
3. View logs: `cd /opt/finbot && docker-compose logs`

### Redis Connection Issues

1. Verify VPC connectivity
2. Check Redis instance status: `gcloud redis instances list`
3. Test connection from VM: `redis-cli -h <REDIS_HOST>`

### Cloud Run Issues

1. Check service status: `gcloud run services list`
2. View logs: `gcloud run services logs read finbot-websocket-api`
3. Verify VPC connector: `gcloud compute networks vpc-access connectors list`

## Production Checklist

- [ ] Enable Cloud Armor (DDoS protection)
- [ ] Set up Cloud CDN for dashboard
- [ ] Configure Cloud Load Balancer with SSL
- [ ] Enable Cloud Logging and Monitoring
- [ ] Set up alerting policies
- [ ] Configure backup schedules
- [ ] Implement CI/CD pipeline (Cloud Build)
- [ ] Use Secret Manager for API keys
- [ ] Enable VPC Service Controls
- [ ] Configure IAM roles properly

## Additional Resources

- [GCP Compute Engine Documentation](https://cloud.google.com/compute/docs)
- [Memorystore for Redis](https://cloud.google.com/memorystore/docs/redis)
- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Artifact Registry](https://cloud.google.com/artifact-registry/docs)
- [Best Practices for GCP](https://cloud.google.com/architecture/framework)
