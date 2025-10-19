package org.acme;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.googlecloudservices.pubsub.QuarkusPubSub;
import io.quarkus.runtime.annotations.RegisterForReflection;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Path("/")
public class TaskProcessorResource {

    @Inject
    QuarkusPubSub pubSub;

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Path("/receive-message")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receiveMessage(PubSubMessage message) throws IOException {
        // Decode the message data
        String decodedData = new String(Base64.getDecoder().decode(message.getMessage().getData()));

        // Process the message
        Task task = objectMapper.readValue(decodedData, Task.class);
        Task completedTask = new Task(task.getTaskId(), task.getTaskType(), "COMPLETE", Instant.now().toString());

        // Publish the completed task
        com.google.pubsub.v1.PubsubMessage pubsubMessage = com.google.pubsub.v1.PubsubMessage.newBuilder()
                .setData(com.google.protobuf.ByteString.copyFrom(objectMapper.writeValueAsBytes(completedTask)))
                .build();
        pubSub.publisher("completed-tasks-topic").publish(pubsubMessage);

        return Response.ok().build();
    }
}

// Helper classes for Pub/Sub message structure
@RegisterForReflection
class PubSubMessage {
    private final Message message;

    @JsonCreator
    public PubSubMessage(@JsonProperty("message") Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}

@RegisterForReflection
class Message {
    private final String data;
    private final Map<String, String> attributes;
    private final String messageId;
    private final String publishTime;

    @JsonCreator
    public Message(
            @JsonProperty("data") String data,
            @JsonProperty("attributes") Map<String, String> attributes,
            @JsonProperty("messageId") String messageId,
            @JsonProperty("publishTime") String publishTime) {
        this.data = data;
        this.attributes = attributes;
        this.messageId = messageId;
        this.publishTime = publishTime;
    }

    public String getData() {
        return data;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getPublishTime() {
        return publishTime;
    }
}


@RegisterForReflection
class Task {
    private String taskId;
    private String taskType;
    private String status;
    private String processedTimestamp;

    public Task() {
    }

    @JsonCreator
    public Task(
            @JsonProperty("taskId") String taskId,
            @JsonProperty("taskType") String taskType,
            @JsonProperty("status") String status,
            @JsonProperty("processedTimestamp") String processedTimestamp) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.status = status;
        this.processedTimestamp = processedTimestamp;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getStatus() {
        return status;
    }

    public String getProcessedTimestamp() {
        return processedTimestamp;
    }
}
