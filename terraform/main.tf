terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "4.51.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

resource "google_project_service" "run_api" {
  service = "run.googleapis.com"
}

resource "google_project_service" "artifactregistry_api" {
  service = "artifactregistry.googleapis.com"
}

resource "google_project_service" "pubsub_api" {
  service = "pubsub.googleapis.com"
}

resource "google_pubsub_topic" "incoming" {
  name       = "incoming-tasks-topic"
  depends_on = [google_project_service.pubsub_api]
}

resource "google_pubsub_topic" "completed" {
  name       = "completed-tasks-topic"
  depends_on = [google_project_service.pubsub_api]
}

resource "google_cloud_run_v2_service" "default" {
  name     = "quarkus-pubsub-processor"
  location = var.region

  template {
    containers {
      image = "ghcr.io/${var.github_owner}/quarkus-pubsub-processor:${var.image_tag}"
    }
    service_account = google_service_account.run_sa.email
  }

  depends_on = [google_project_service.run_api]
}

resource "google_pubsub_subscription" "push" {
  name  = "incoming-tasks-subscription"
  topic = google_pubsub_topic.incoming.name

  push_config {
    push_endpoint = google_cloud_run_v2_service.default.uri
    oidc_token {
      service_account_email = google_service_account.push_sa.email
    }
  }

  ack_deadline_seconds = 20
}

resource "google_service_account" "run_sa" {
  account_id   = "quarkus-pubsub-processor-run"
  display_name = "Quarkus Pub/Sub Processor Cloud Run SA"
}

resource "google_project_iam_member" "run_sa_pubsub_publisher" {
  project = var.project_id
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.run_sa.email}"
}

resource "google_service_account" "push_sa" {
  account_id   = "quarkus-pubsub-processor-push"
  display_name = "Quarkus Pub/Sub Processor Push SA"
}

resource "google_cloud_run_service_iam_member" "push_sa_run_invoker" {
  location = google_cloud_run_v2_service.default.location
  project  = google_cloud_run_v2_service.default.project
  service  = google_cloud_run_v2_service.default.name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.push_sa.email}"
}
