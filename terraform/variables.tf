variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region"
  type        = string
  default     = "us-central1"
}

variable "github_owner" {
  description = "The GitHub owner"
  type        = string
}

variable "image_tag" {
  description = "The Docker image tag"
  type        = string
}
