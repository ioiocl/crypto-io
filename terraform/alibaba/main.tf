terraform {
  required_providers {
    alicloud = {
      source  = "aliyun/alicloud"
      version = "~> 1.220"
    }
  }
  required_version = ">= 1.0"
}

provider "alicloud" {
  access_key = var.access_key
  secret_key = var.secret_key
  region     = var.region
}

# VPC and Network
resource "alicloud_vpc" "finbot_vpc" {
  vpc_name   = "finbot-vpc"
  cidr_block = "10.0.0.0/16"
}

resource "alicloud_vswitch" "finbot_vswitch" {
  vpc_id     = alicloud_vpc.finbot_vpc.id
  cidr_block = "10.0.1.0/24"
  zone_id    = var.zone_id
}

# Security Group
resource "alicloud_security_group" "finbot_sg" {
  name        = "finbot-security-group"
  description = "Security group for Finbot application"
  vpc_id      = alicloud_vpc.finbot_vpc.id
}

# Allow HTTP
resource "alicloud_security_group_rule" "allow_http" {
  type              = "ingress"
  ip_protocol       = "tcp"
  port_range        = "80/80"
  security_group_id = alicloud_security_group.finbot_sg.id
  cidr_ip           = "0.0.0.0/0"
}

# Allow HTTPS
resource "alicloud_security_group_rule" "allow_https" {
  type              = "ingress"
  ip_protocol       = "tcp"
  port_range        = "443/443"
  security_group_id = alicloud_security_group.finbot_sg.id
  cidr_ip           = "0.0.0.0/0"
}

# Allow WebSocket API
resource "alicloud_security_group_rule" "allow_websocket" {
  type              = "ingress"
  ip_protocol       = "tcp"
  port_range        = "8080/8080"
  security_group_id = alicloud_security_group.finbot_sg.id
  cidr_ip           = "0.0.0.0/0"
}

# Allow Dashboard
resource "alicloud_security_group_rule" "allow_dashboard" {
  type              = "ingress"
  ip_protocol       = "tcp"
  port_range        = "3000/3000"
  security_group_id = alicloud_security_group.finbot_sg.id
  cidr_ip           = "0.0.0.0/0"
}

# Allow SSH
resource "alicloud_security_group_rule" "allow_ssh" {
  type              = "ingress"
  ip_protocol       = "tcp"
  port_range        = "22/22"
  security_group_id = alicloud_security_group.finbot_sg.id
  cidr_ip           = var.ssh_cidr
}

# Redis Instance (ApsaraDB for Redis)
resource "alicloud_kvstore_instance" "finbot_redis" {
  instance_name  = "finbot-redis"
  instance_class = "redis.master.small.default"
  instance_type  = "Redis"
  engine_version = "7.0"
  vswitch_id     = alicloud_vswitch.finbot_vswitch.id
  password       = var.redis_password
  security_ips   = ["10.0.0.0/16"]
}

# ECS Instance
resource "alicloud_instance" "finbot_ecs" {
  instance_name              = "finbot-ecs"
  instance_type              = var.instance_type
  system_disk_category       = "cloud_essd"
  system_disk_size           = 40
  image_id                   = var.image_id
  vswitch_id                 = alicloud_vswitch.finbot_vswitch.id
  security_groups            = [alicloud_security_group.finbot_sg.id]
  internet_max_bandwidth_out = 100
  
  user_data = templatefile("${path.module}/user_data.sh", {
    redis_host        = alicloud_kvstore_instance.finbot_redis.connection_domain
    redis_password    = var.redis_password
    polygon_api_key   = var.polygon_api_key
    docker_registry   = var.docker_registry
  })

  tags = {
    Name        = "finbot-ecs"
    Environment = var.environment
    Project     = "finbot"
  }
}

# Elastic IP
resource "alicloud_eip" "finbot_eip" {
  bandwidth            = "100"
  internet_charge_type = "PayByTraffic"
}

resource "alicloud_eip_association" "finbot_eip_assoc" {
  allocation_id = alicloud_eip.finbot_eip.id
  instance_id   = alicloud_instance.finbot_ecs.id
}

# Container Registry
resource "alicloud_cr_namespace" "finbot_namespace" {
  name               = "finbot"
  auto_create        = false
  default_visibility = "PRIVATE"
}

resource "alicloud_cr_repo" "ingestion_service" {
  namespace = alicloud_cr_namespace.finbot_namespace.name
  name      = "ingestion-service"
  summary   = "Finbot Ingestion Service"
  repo_type = "PRIVATE"
}

resource "alicloud_cr_repo" "analytics_service" {
  namespace = alicloud_cr_namespace.finbot_namespace.name
  name      = "analytics-service"
  summary   = "Finbot Analytics Service"
  repo_type = "PRIVATE"
}

resource "alicloud_cr_repo" "websocket_api" {
  namespace = alicloud_cr_namespace.finbot_namespace.name
  name      = "websocket-api"
  summary   = "Finbot WebSocket API"
  repo_type = "PRIVATE"
}

resource "alicloud_cr_repo" "dashboard" {
  namespace = alicloud_cr_namespace.finbot_namespace.name
  name      = "dashboard"
  summary   = "Finbot Dashboard"
  repo_type = "PRIVATE"
}
