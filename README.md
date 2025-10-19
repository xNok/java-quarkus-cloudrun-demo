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
*   Docker (for Testcontainers)

### Running Tests

The integration tests use **Testcontainers** to automatically start a Google Cloud Pub/Sub emulator in a Docker container. No manual setup required!

```bash
mvn test
```

The `PubSubEmulatorTestResource` manages the emulator lifecycle automatically for each test run.

### Running in Dev Mode

For local development with Quarkus dev mode:

```bash
mvn quarkus:dev
```

**Note:** Dev mode runs the app but doesn't automatically start the Pub/Sub emulator. To test Pub/Sub interactions:

#### Option 1: Run tests (Recommended)
Tests automatically start and manage the Pub/Sub emulator via Testcontainers:
```bash
mvn test
```

#### Option 2: Manual emulator setup
For interactive testing, you can manually start the emulator and publish messages:

1. **Start the Pub/Sub emulator** (requires gcloud SDK):
   ```bash
   gcloud beta emulators pubsub start --project=test-project
   ```

2. **Update `application.properties`** to use the emulator:
   ```properties
   %dev.quarkus.google.cloud.project-id=test-project
   %dev.quarkus.google.cloud.pubsub.emulator-host=localhost:8085
   %dev.incoming-topic=incoming-tasks-topic
   %dev.completed-topic=completed-tasks-topic
   ```

3. **Run the app in dev mode**:
   ```bash
   mvn quarkus:dev
   ```

4. **Publish test messages** using the provided script:
   ```bash
   export PUBSUB_EMULATOR_HOST=localhost:8085
   ./scripts/publish-test-message.sh
   ```
   
   Or manually with gcloud:
   ```bash
   export PUBSUB_EMULATOR_HOST=localhost:8085
   gcloud pubsub topics publish incoming-tasks-topic \
     --project=test-project \
     --message='{"taskId":"test-123","taskType":"GENERATE_REPORT","status":"PENDING","processedTimestamp":null}'
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
