# Finbot Deployment Checklist

Use this checklist to ensure successful deployment of Finbot.

## Pre-Deployment

### Local Testing
- [ ] Docker Desktop installed and running
- [ ] Minimum 8GB RAM allocated to Docker
- [ ] Ports 3000, 6379, 8080, 8081, 8082 available
- [ ] `.env` file created from `.env.example`
- [ ] Polygon API key configured in `.env`
- [ ] Run `docker compose up --build` successfully
- [ ] Dashboard accessible at http://localhost:3000
- [ ] Data appears within 30 seconds
- [ ] All 5 containers running (redis, ingestion, analytics, websocket-api, dashboard)
- [ ] No errors in logs: `docker compose logs`
- [ ] WebSocket connection working (check browser console)
- [ ] Charts updating in real-time

### Code Review
- [ ] All services build without errors
- [ ] No hardcoded credentials
- [ ] Environment variables properly configured
- [ ] Logging levels appropriate
- [ ] Error handling in place
- [ ] Health check endpoints working

## Alibaba Cloud Deployment

### Prerequisites
- [ ] Alibaba Cloud account created
- [ ] Billing enabled
- [ ] Access Key and Secret Key obtained
- [ ] Terraform installed (>= 1.0)
- [ ] Docker images built locally
- [ ] Region selected (e.g., us-west-1)

### Terraform Configuration
- [ ] Navigate to `terraform/alibaba/`
- [ ] Run `terraform init`
- [ ] Create `terraform.tfvars` with:
  - [ ] `access_key`
  - [ ] `secret_key`
  - [ ] `region`
  - [ ] `redis_password` (strong password)
  - [ ] `polygon_api_key`
  - [ ] `ssh_cidr` (your IP only)
- [ ] Run `terraform plan` and review
- [ ] Run `terraform apply` and confirm
- [ ] Note outputs (ECS IP, Redis host, Registry URL)

### Container Registry
- [ ] Login to Alibaba Container Registry
- [ ] Get registry URL from Terraform output
- [ ] Build and tag images:
  ```bash
  docker build -t <REGISTRY>/ingestion-service:latest -f ingestion-service/Dockerfile .
  docker build -t <REGISTRY>/analytics-service:latest -f analytics-service/Dockerfile .
  docker build -t <REGISTRY>/websocket-api:latest -f websocket-api/Dockerfile .
  docker build -t <REGISTRY>/dashboard:latest -f dashboard/Dockerfile ./dashboard
  ```
- [ ] Push all images to registry
- [ ] Verify images in ACR console

### Verification
- [ ] SSH into ECS instance: `ssh ubuntu@<PUBLIC_IP>`
- [ ] Check Docker is running: `docker ps`
- [ ] Check services are up: `cd /opt/finbot && docker-compose ps`
- [ ] View logs: `docker-compose logs -f`
- [ ] Test Redis connection
- [ ] Access dashboard: `http://<PUBLIC_IP>:3000`
- [ ] Test WebSocket: `ws://<PUBLIC_IP>:8080/ws/market/AAPL`

### Security
- [ ] Security group rules reviewed
- [ ] SSH access restricted to your IP
- [ ] Redis password set
- [ ] API keys not exposed
- [ ] HTTPS/WSS configured (if production)

## GCP Deployment

### Prerequisites
- [ ] GCP account created
- [ ] Project created in GCP Console
- [ ] Billing enabled
- [ ] gcloud CLI installed
- [ ] Authenticated: `gcloud auth login`
- [ ] Project set: `gcloud config set project <PROJECT_ID>`
- [ ] Terraform installed (>= 1.0)

### Enable APIs
- [ ] Compute Engine API
- [ ] Redis API (Memorystore)
- [ ] Artifact Registry API
- [ ] Cloud Run API (if using serverless)
- [ ] VPC Access API (if using Cloud Run)

```bash
gcloud services enable compute.googleapis.com redis.googleapis.com artifactregistry.googleapis.com run.googleapis.com vpcaccess.googleapis.com
```

### Terraform Configuration
- [ ] Navigate to `terraform/gcp/`
- [ ] Run `terraform init`
- [ ] Create `terraform.tfvars` with:
  - [ ] `project_id`
  - [ ] `region` (e.g., us-central1)
  - [ ] `zone` (e.g., us-central1-a)
  - [ ] `polygon_api_key`
  - [ ] `ssh_cidr` (your IP only)
- [ ] Run `terraform plan` and review
- [ ] Run `terraform apply` and confirm
- [ ] Note outputs (VM IP, Redis host, Registry URL)

### Artifact Registry
- [ ] Configure Docker: `gcloud auth configure-docker <REGION>-docker.pkg.dev`
- [ ] Get registry URL from Terraform output
- [ ] Build and tag images:
  ```bash
  docker build -t <REGISTRY>/ingestion-service:latest -f ingestion-service/Dockerfile .
  docker build -t <REGISTRY>/analytics-service:latest -f analytics-service/Dockerfile .
  docker build -t <REGISTRY>/websocket-api:latest -f websocket-api/Dockerfile .
  docker build -t <REGISTRY>/dashboard:latest -f dashboard/Dockerfile ./dashboard
  ```
- [ ] Push all images to registry
- [ ] Verify images in Artifact Registry console

### Verification (Compute Engine)
- [ ] SSH into VM: `gcloud compute ssh finbot-vm --zone=<ZONE>`
- [ ] Check Docker is running: `docker ps`
- [ ] Check services: `cd /opt/finbot && docker-compose ps`
- [ ] View logs: `docker-compose logs -f`
- [ ] Test Redis connection
- [ ] Access dashboard: `http://<VM_IP>:3000`
- [ ] Test WebSocket: `ws://<VM_IP>:8080/ws/market/AAPL`

### Verification (Cloud Run)
- [ ] Check services: `gcloud run services list`
- [ ] View logs: `gcloud run services logs read finbot-websocket-api`
- [ ] Test endpoints from Terraform outputs
- [ ] Verify VPC connector working
- [ ] Check auto-scaling behavior

### Security
- [ ] Firewall rules reviewed
- [ ] SSH access restricted
- [ ] Service account permissions minimal
- [ ] Redis in private VPC
- [ ] API keys in Secret Manager (optional)
- [ ] HTTPS/WSS configured (if production)

## Post-Deployment

### Monitoring Setup
- [ ] Cloud logging configured
- [ ] Metrics collection enabled
- [ ] Alerts configured:
  - [ ] High CPU usage
  - [ ] High memory usage
  - [ ] Service down
  - [ ] Error rate threshold
- [ ] Dashboard for monitoring created

### Backup Configuration
- [ ] Redis backup schedule set
- [ ] VM/instance snapshots enabled
- [ ] Configuration files backed up
- [ ] Terraform state backed up

### Performance Testing
- [ ] Load test WebSocket connections
- [ ] Verify analytics performance
- [ ] Check memory usage under load
- [ ] Test auto-scaling (if configured)
- [ ] Measure latency end-to-end

### Documentation
- [ ] Deployment notes documented
- [ ] Access credentials stored securely
- [ ] Runbook created for operations
- [ ] Incident response plan documented
- [ ] Team trained on system

## Production Readiness

### High Availability
- [ ] Multi-AZ deployment (if required)
- [ ] Load balancer configured
- [ ] Auto-scaling policies set
- [ ] Health checks configured
- [ ] Failover tested

### Security Hardening
- [ ] SSL/TLS certificates installed
- [ ] DDoS protection enabled (Cloud Armor/Anti-DDoS)
- [ ] WAF configured (if applicable)
- [ ] Secrets rotated
- [ ] Audit logging enabled
- [ ] Compliance requirements met

### Performance Optimization
- [ ] CDN configured for dashboard
- [ ] Redis memory optimized
- [ ] Connection pooling tuned
- [ ] Caching strategy implemented
- [ ] Database indexes optimized (if applicable)

### Cost Optimization
- [ ] Right-sized instances
- [ ] Committed use discounts applied (if long-term)
- [ ] Budget alerts configured
- [ ] Cost monitoring dashboard created
- [ ] Unused resources cleaned up

## Operational Checklist

### Daily
- [ ] Check service health
- [ ] Review error logs
- [ ] Monitor resource usage
- [ ] Verify data freshness

### Weekly
- [ ] Review performance metrics
- [ ] Check backup status
- [ ] Update dependencies (if needed)
- [ ] Review security alerts

### Monthly
- [ ] Review costs
- [ ] Update documentation
- [ ] Test disaster recovery
- [ ] Review and optimize

## Rollback Plan

### If Deployment Fails
1. [ ] Note error messages
2. [ ] Check logs: `docker compose logs` or cloud logs
3. [ ] Verify configuration
4. [ ] Rollback Terraform: `terraform destroy`
5. [ ] Fix issues locally first
6. [ ] Redeploy

### If Services Fail
1. [ ] Check service status
2. [ ] Review logs
3. [ ] Restart services: `docker-compose restart`
4. [ ] If persistent, rollback to previous version
5. [ ] Investigate root cause

## Success Criteria

### Local Deployment
- ✅ All services running
- ✅ Dashboard accessible
- ✅ Real-time data flowing
- ✅ No errors in logs
- ✅ WebSocket connections stable

### Cloud Deployment
- ✅ Infrastructure provisioned
- ✅ Services deployed and running
- ✅ Public access working
- ✅ Monitoring configured
- ✅ Backups enabled
- ✅ Security hardened
- ✅ Performance acceptable
- ✅ Costs within budget

## Troubleshooting Reference

### Common Issues

**Issue**: Services won't start
- Check Docker memory allocation
- Verify ports are available
- Review environment variables
- Check logs for specific errors

**Issue**: No data in dashboard
- Verify Polygon API key
- Check ingestion service logs
- Verify Redis connectivity
- Check WebSocket connection

**Issue**: High latency
- Check network connectivity
- Review Redis performance
- Optimize analytics window size
- Scale services horizontally

**Issue**: Out of memory
- Increase Docker/VM memory
- Reduce window size
- Reduce number of symbols
- Optimize analytics algorithms

## Contact Information

- **Documentation**: See README.md, QUICKSTART.md, ARCHITECTURE.md
- **Logs**: `docker compose logs -f` or cloud logging console
- **Health**: Check `/q/health` endpoints
- **Metrics**: Check `/q/metrics` endpoints

---

**Deployment Date**: _________________

**Deployed By**: _________________

**Environment**: [ ] Local [ ] Alibaba Cloud [ ] GCP

**Status**: [ ] Success [ ] Failed [ ] In Progress

**Notes**: _________________________________________________
