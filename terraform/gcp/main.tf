terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
  required_version = ">= 1.0"
}

provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}

# VPC Network
resource "google_compute_network" "finbot_network" {
  name                    = "finbot-network"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "finbot_subnet" {
  name          = "finbot-subnet"
  ip_cidr_range = "10.0.0.0/24"
  region        = var.region
  network       = google_compute_network.finbot_network.id
}

# Firewall Rules
resource "google_compute_firewall" "allow_http" {
  name    = "finbot-allow-http"
  network = google_compute_network.finbot_network.name

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["finbot-web"]
}

resource "google_compute_firewall" "allow_websocket" {
  name    = "finbot-allow-websocket"
  network = google_compute_network.finbot_network.name

  allow {
    protocol = "tcp"
    ports    = ["8080"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["finbot-web"]
}

resource "google_compute_firewall" "allow_dashboard" {
  name    = "finbot-allow-dashboard"
  network = google_compute_network.finbot_network.name

  allow {
    protocol = "tcp"
    ports    = ["3000"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["finbot-web"]
}

resource "google_compute_firewall" "allow_ssh" {
  name    = "finbot-allow-ssh"
  network = google_compute_network.finbot_network.name

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = [var.ssh_cidr]
  target_tags   = ["finbot-web"]
}

# Redis Instance (Memorystore)
resource "google_redis_instance" "finbot_redis" {
  name           = "finbot-redis"
  tier           = "BASIC"
  memory_size_gb = 1
  region         = var.region

  authorized_network = google_compute_network.finbot_network.id
  redis_version      = "REDIS_7_0"

  display_name = "Finbot Redis Instance"
}

# Service Account for GCE
resource "google_service_account" "finbot_sa" {
  account_id   = "finbot-service-account"
  display_name = "Finbot Service Account"
}

resource "google_project_iam_member" "finbot_sa_roles" {
  for_each = toset([
    "roles/logging.logWriter",
    "roles/monitoring.metricWriter",
    "roles/artifactregistry.reader"
  ])

  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.finbot_sa.email}"
}

# Compute Engine Instance
resource "google_compute_instance" "finbot_vm" {
  name         = "finbot-vm"
  machine_type = var.machine_type
  zone         = var.zone

  tags = ["finbot-web"]

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2204-lts"
      size  = 40
      type  = "pd-standard"
    }
  }

  network_interface {
    subnetwork = google_compute_subnetwork.finbot_subnet.id

    access_config {
      // Ephemeral public IP
    }
  }

  metadata_startup_script = templatefile("${path.module}/startup_script.sh", {
    redis_host      = google_redis_instance.finbot_redis.host
    redis_port      = google_redis_instance.finbot_redis.port
    polygon_api_key = var.polygon_api_key
    project_id      = var.project_id
    region          = var.region
  })

  service_account {
    email  = google_service_account.finbot_sa.email
    scopes = ["cloud-platform"]
  }

  labels = {
    environment = var.environment
    project     = "finbot"
  }
}

# Artifact Registry
resource "google_artifact_registry_repository" "finbot_repo" {
  location      = var.region
  repository_id = "finbot"
  description   = "Finbot Docker images"
  format        = "DOCKER"
}

# Cloud Run Services (Alternative deployment)
resource "google_cloud_run_service" "ingestion_service" {
  name     = "finbot-ingestion"
  location = var.region

  template {
    spec {
      containers {
        image = "${var.region}-docker.pkg.dev/${var.project_id}/finbot/ingestion-service:latest"

        env {
          name  = "POLYGON_API_KEY"
          value = var.polygon_api_key
        }
        env {
          name  = "REDIS_HOST"
          value = google_redis_instance.finbot_redis.host
        }
        env {
          name  = "REDIS_PORT"
          value = tostring(google_redis_instance.finbot_redis.port)
        }
      }
    }

    metadata {
      annotations = {
        "autoscaling.knative.dev/maxScale" = "10"
        "run.googleapis.com/vpc-access-connector" = google_vpc_access_connector.finbot_connector.id
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }
}

resource "google_cloud_run_service" "analytics_service" {
  name     = "finbot-analytics"
  location = var.region

  template {
    spec {
      containers {
        image = "${var.region}-docker.pkg.dev/${var.project_id}/finbot/analytics-service:latest"

        env {
          name  = "REDIS_HOST"
          value = google_redis_instance.finbot_redis.host
        }
        env {
          name  = "REDIS_PORT"
          value = tostring(google_redis_instance.finbot_redis.port)
        }
      }
    }

    metadata {
      annotations = {
        "autoscaling.knative.dev/maxScale" = "10"
        "run.googleapis.com/vpc-access-connector" = google_vpc_access_connector.finbot_connector.id
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }
}

resource "google_cloud_run_service" "websocket_api" {
  name     = "finbot-websocket-api"
  location = var.region

  template {
    spec {
      containers {
        image = "${var.region}-docker.pkg.dev/${var.project_id}/finbot/websocket-api:latest"

        ports {
          container_port = 8080
        }

        env {
          name  = "REDIS_HOST"
          value = google_redis_instance.finbot_redis.host
        }
        env {
          name  = "REDIS_PORT"
          value = tostring(google_redis_instance.finbot_redis.port)
        }
      }
    }

    metadata {
      annotations = {
        "autoscaling.knative.dev/maxScale" = "10"
        "run.googleapis.com/vpc-access-connector" = google_vpc_access_connector.finbot_connector.id
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }
}

# VPC Access Connector for Cloud Run
resource "google_vpc_access_connector" "finbot_connector" {
  name          = "finbot-connector"
  region        = var.region
  network       = google_compute_network.finbot_network.name
  ip_cidr_range = "10.8.0.0/28"
}

# IAM for Cloud Run (allow unauthenticated access)
resource "google_cloud_run_service_iam_member" "websocket_api_public" {
  service  = google_cloud_run_service.websocket_api.name
  location = google_cloud_run_service.websocket_api.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}
