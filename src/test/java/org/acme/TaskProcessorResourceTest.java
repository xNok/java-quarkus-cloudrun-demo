package org.acme;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PushConfig;

import io.quarkiverse.googlecloudservices.pubsub.QuarkusPubSub;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(PubSubEmulatorTestResource.class)
public class TaskProcessorResourceTest {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TopicAdminClient topicAdminClient;

    @Inject
    SubscriptionAdminClient subscriptionAdminClient;

    @Inject
    QuarkusPubSub pubSub;

    private Subscriber subscriber;

    @BeforeEach
    public void setup() throws IOException {
        ProjectTopicName topicName = ProjectTopicName.of("test-project", "completed-tasks-topic");
        topicAdminClient.createTopic(topicName);
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of("test-project", "test-subscription");
        subscriptionAdminClient.createSubscription(subscriptionName, topicName, PushConfig.getDefaultInstance(), 10);
    }

    @AfterEach
    public void teardown() throws IOException {
        if (subscriber != null) {
            subscriber.stopAsync();
        }
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of("test-project", "test-subscription");
        subscriptionAdminClient.deleteSubscription(subscriptionName);
        ProjectTopicName topicName = ProjectTopicName.of("test-project", "completed-tasks-topic");
        topicAdminClient.deleteTopic(topicName);
    }

    @Test
    public void testReceiveMessage() throws IOException {
        // Create a test task
        Task task = new Task("a-123-xyz", "GENERATE_REPORT", "PENDING", null);

        // Create a Pub/Sub message
        Message message = new Message(
                Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(task)),
                Collections.emptyMap(),
                "123",
                "2023-10-27T10:00:00Z");
        PubSubMessage pubSubMessage = new PubSubMessage(message);

        // Start a subscriber to listen for the completed task
        final var receivedMessages = new java.util.ArrayList<String>();
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of("test-project", "test-subscription");
        MessageReceiver receiver = (msg, consumer) -> {
            receivedMessages.add(msg.getData().toStringUtf8());
            consumer.ack();
        };
        subscriber = (Subscriber) pubSub.subscriber(subscriptionName.getSubscription(), receiver);
        subscriber.startAsync().awaitRunning();

        // Send the message to the endpoint
        given()
                .contentType("application/json")
                .body(pubSubMessage)
                .when()
                .post("/receive-message")
                .then()
                .statusCode(200);

        // Wait for the message to be processed and published
        await().atMost(5, TimeUnit.SECONDS).until(() -> receivedMessages.size() == 1);

        // Verify the received message
        String receivedData = receivedMessages.get(0);
        Task completedTask = objectMapper.readValue(receivedData, Task.class);

        assertEquals("a-123-xyz", completedTask.getTaskId());
        assertEquals("GENERATE_REPORT", completedTask.getTaskType());
        assertEquals("COMPLETE", completedTask.getStatus());
        assertNotNull(completedTask.getProcessedTimestamp());
    }
}
