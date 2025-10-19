package org.acme;

import java.util.Collections;
import java.util.Map;

import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class PubSubEmulatorTestResource implements QuarkusTestResourceLifecycleManager {

    private final PubSubEmulatorContainer emulator = new PubSubEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:latest"))
            .withExposedPorts(8085);

    @Override
    public Map<String, String> start() {
        emulator.start();
        return Collections.singletonMap("quarkus.google.cloud.pubsub.emulator-host", emulator.getEmulatorEndpoint());
    }

    @Override
    public void stop() {
        emulator.stop();
    }
}
