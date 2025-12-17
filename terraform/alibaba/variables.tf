variable "access_key" {
  description = "Alibaba Cloud Access Key"
  type        = string
  sensitive   = true
}

variable "secret_key" {
  description = "Alibaba Cloud Secret Key"
  type        = string
  sensitive   = true
}

variable "region" {
  description = "Alibaba Cloud Region"
  type        = string
  default     = "us-west-1"
}

variable "zone_id" {
  description = "Availability Zone ID"
  type        = string
  default     = "us-west-1a"
}

variable "instance_type" {
  description = "ECS Instance Type"
  type        = string
  default     = "ecs.c6.large"
}

variable "image_id" {
  description = "ECS Image ID (Ubuntu 22.04 recommended)"
  type        = string
  default     = "ubuntu_22_04_x64_20G_alibase_20231221.vhd"
}

variable "redis_password" {
  description = "Redis Password"
  type        = string
  sensitive   = true
}

variable "polygon_api_key" {
  description = "Polygon/Massive API Key"
  type        = string
  sensitive   = true
  default     = "LkgydUcNGAFPthknFLbtkvshslkuSNqU"
}

variable "docker_registry" {
  description = "Docker Registry URL"
  type        = string
  default     = "registry.us-west-1.aliyuncs.com/finbot"
}

variable "ssh_cidr" {
  description = "CIDR block for SSH access"
  type        = string
  default     = "0.0.0.0/0"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}
