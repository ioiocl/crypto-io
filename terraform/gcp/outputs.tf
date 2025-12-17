output "vm_external_ip" {
  description = "External IP of the VM instance"
  value       = google_compute_instance.finbot_vm.network_interface[0].access_config[0].nat_ip
}

output "redis_host" {
  description = "Redis host"
  value       = google_redis_instance.finbot_redis.host
}

output "redis_port" {
  description = "Redis port"
  value       = google_redis_instance.finbot_redis.port
}

output "artifact_registry_url" {
  description = "Artifact Registry URL"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/finbot"
}

output "dashboard_url" {
  description = "Dashboard URL (VM deployment)"
  value       = "http://${google_compute_instance.finbot_vm.network_interface[0].access_config[0].nat_ip}:3000"
}

output "websocket_api_url" {
  description = "WebSocket API URL (VM deployment)"
  value       = "ws://${google_compute_instance.finbot_vm.network_interface[0].access_config[0].nat_ip}:8080"
}

output "cloud_run_websocket_url" {
  description = "Cloud Run WebSocket API URL"
  value       = google_cloud_run_service.websocket_api.status[0].url
}

output "cloud_run_ingestion_url" {
  description = "Cloud Run Ingestion Service URL"
  value       = google_cloud_run_service.ingestion_service.status[0].url
}

output "cloud_run_analytics_url" {
  description = "Cloud Run Analytics Service URL"
  value       = google_cloud_run_service.analytics_service.status[0].url
}
