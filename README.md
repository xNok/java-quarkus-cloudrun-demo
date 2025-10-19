# Java Quarkus Pub/Sub Processor for Cloud Run

This project is a containerized Java Quarkus application that processes messages from a Google Cloud Pub/Sub topic and publishes a corresponding update to a second topic. The entire solution is deployable via Terraform and integrated with a GitHub Actions CI/CD pipeline for building and publishing the container image.

## Use Case: Task Status Processor

The application acts as a simple task status processor.

*   **Receives a Message:** The app listens for messages on an `incoming-tasks-topic`. A push subscription sends these messages to the app's endpoint.
*   **Processes the Message:** The application's core logic changes the status from `PENDING` to `COMPLETE` and adds a `processedTimestamp`.
*   **Publishes an Update:** The app publishes the modified message to a `completed-tasks-topic`.

## Architecture

The project consists of the following components:

*   **Java Quarkus Application:** A REST endpoint that receives and processes Pub/Sub messages.
*   **Google Cloud Pub/Sub:** Two topics (`incoming-tasks-topic` and `completed-tasks-topic`) for messaging.
*   **Google Cloud Run:** A serverless platform to run the containerized application.
*   **GitHub Container Registry:** A private Docker registry to store the container image.
*   **Terraform:** Infrastructure as Code (IaC) to provision the Google Cloud resources.
*   **GitHub Actions:** A CI/CD pipeline to build and push the container image.

## Local Development

To run the application and tests locally, you'll need the following:

*   Java 21+
*   Maven 3.8.x
*   Docker

1.  **Run the Pub/Sub emulator:**

    ```bash
    gcloud beta emulators pubsub start --project=test-project
    ```

2.  **Run the application in dev mode:**

    ```bash
    mvn quarkus:dev
    ```

3.  **Run the integration tests:**

    ```bash
    mvn test
    ```

## Configuration

The application requires the following environment variables:

*   `GOOGLE_CLOUD_PROJECT` - The Google Cloud project ID.
*   `INCOMING_TOPIC` - The name of the incoming Pub/Sub topic.
*   `COMPLETED_TOPIC` - The name of the completed Pub/Sub topic.

## Deployment

To deploy the application and infrastructure, you'll need the following:

*   Terraform
*   Google Cloud SDK

1.  **Authenticate to Google Cloud:**

    ```bash
    gcloud auth login
    gcloud auth application-default login
    ```

2.  **Initialize Terraform:**

    ```bash
    cd terraform
    terraform init
    ```

3.  **Apply the Terraform configuration:**

    ```bash
    terraform apply -var="project_id=<your-project-id>" -var="github_owner=<your-github-owner>" -var="image_tag=<your-image-tag>"
    ```

## CI/CD

The project includes a GitHub Actions CI/CD pipeline that builds and pushes the container image to GitHub Container Registry. The pipeline is triggered on every push to the `main` branch.
