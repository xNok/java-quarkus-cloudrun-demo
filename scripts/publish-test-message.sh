#!/bin/bash

# Script to manually publish a test message to the Pub/Sub emulator
# Usage: ./scripts/publish-test-message.sh

set -e

# Configuration
PROJECT_ID="${GOOGLE_CLOUD_PROJECT:-test-project}"
TOPIC_ID="${INCOMING_TOPIC:-incoming-tasks-topic}"
PUBSUB_EMULATOR_HOST="${PUBSUB_EMULATOR_HOST:-localhost:8085}"

# Export for gcloud
export PUBSUB_EMULATOR_HOST

echo "📤 Publishing test message to Pub/Sub emulator..."
echo "   Project: $PROJECT_ID"
echo "   Topic: $TOPIC_ID"
echo "   Emulator: $PUBSUB_EMULATOR_HOST"
echo ""

# Create the topic if it doesn't exist (emulator only)
gcloud pubsub topics create "$TOPIC_ID" \
  --project="$PROJECT_ID" \
  2>/dev/null || echo "ℹ️  Topic already exists"

# Sample task message
TASK_JSON='{
  "taskId": "test-123",
  "taskType": "GENERATE_REPORT",
  "status": "PENDING",
  "processedTimestamp": null
}'

# Publish the message
gcloud pubsub topics publish "$TOPIC_ID" \
  --project="$PROJECT_ID" \
  --message="$TASK_JSON"

echo ""
echo "✅ Message published successfully!"
echo ""
echo "💡 To view this message in your app, make sure:"
echo "   1. Pub/Sub emulator is running (gcloud beta emulators pubsub start)"
echo "   2. Your app is running in dev mode (mvn quarkus:dev)"
echo "   3. Application properties are configured to use the emulator"
