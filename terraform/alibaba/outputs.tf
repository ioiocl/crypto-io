output "ecs_public_ip" {
  description = "Public IP of the ECS instance"
  value       = alicloud_eip.finbot_eip.ip_address
}

output "redis_connection_domain" {
  description = "Redis connection domain"
  value       = alicloud_kvstore_instance.finbot_redis.connection_domain
}

output "redis_port" {
  description = "Redis port"
  value       = alicloud_kvstore_instance.finbot_redis.port
}

output "container_registry_url" {
  description = "Container Registry URL"
  value       = "registry.${var.region}.aliyuncs.com/${alicloud_cr_namespace.finbot_namespace.name}"
}

output "dashboard_url" {
  description = "Dashboard URL"
  value       = "http://${alicloud_eip.finbot_eip.ip_address}:3000"
}

output "websocket_api_url" {
  description = "WebSocket API URL"
  value       = "ws://${alicloud_eip.finbot_eip.ip_address}:8080"
}
