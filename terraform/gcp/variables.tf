variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "us-central1"
}

variable "zone" {
  description = "GCP Zone"
  type        = string
  default     = "us-central1-a"
}

variable "machine_type" {
  description = "GCE Machine Type"
  type        = string
  default     = "e2-standard-2"
}

variable "polygon_api_key" {
  description = "Polygon/Massive API Key"
  type        = string
  sensitive   = true
  default     = "LkgydUcNGAFPthknFLbtkvshslkuSNqU"
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
