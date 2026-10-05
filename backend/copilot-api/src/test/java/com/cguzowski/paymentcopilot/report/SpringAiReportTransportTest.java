package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

class SpringAiReportTransportTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> request = new AtomicReference<>();
    private final CountDownLatch connected = new CountDownLatch(1);
    private final CountDownLatch disconnected = new CountDownLatch(1);

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackageClasses = SpringAiReportModel.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(Configuration.class))
    static class ClientConfiguration {}

    private void start(String response, int status, boolean stall) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/api/chat", exchange -> {
            calls.incrementAndGet();
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().set("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(status, 0);
            connected.countDown();
            try (var output = exchange.getResponseBody()) {
                do {
                    output.write(response.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    if (stall) Thread.sleep(25);
                } while (stall);
            } catch (IOException exception) {
                disconnected.countDown();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private ApplicationContextRunner context() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebClientAutoConfiguration.class))
                .withUserConfiguration(ClientConfiguration.class);
    }

    private SpringAiReportModel model(WebClient.Builder builder) {
        OllamaApi api = OllamaApi.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .webClientBuilder(builder)
                .build();
        return new SpringAiReportModel(
                Optional.of(OllamaChatModel.builder().ollamaApi(api).build()),
                "test-report-model",
                new ReportPromptFactory(json),
                35,
                128);
    }

    private static String chunk(String content, boolean done) {
        return "{\"model\":\"test-report-model\",\"created_at\":\"2026-10-04T12:00:00Z\","
                + "\"message\":{\"role\":\"assistant\",\"content\":\"" + content + "\"},"
                + "\"done\":" + done
                + (done ? ",\"done_reason\":\"stop\",\"prompt_eval_count\":10,\"eval_count\":2" : "") + "}\n";
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsCompletePromptWithExplicitContextAndNoTruncationOrShift() throws Exception {
        start(chunk("first", false) + chunk("second", true), 200, false);
        String prompt = "start " + "synthetic evidence ".repeat(1200) + " final instruction";
        context().run(context -> {
            ReportModelResponse response =
                    model(context.getBean(WebClient.Builder.class)).generate(prompt, "{}");
            assertThat(response.output()).isEqualTo("firstsecond");
        });
        var body = json.readTree(request.get());
        assertThat(body.path("messages").get(0).path("content").asString()).isEqualTo(prompt);
        assertThat(body.path("stream").asBoolean()).isTrue();
        assertThat(body.path("truncate").asBoolean(true)).isFalse();
        assertThat(body.path("shift").asBoolean(true)).isFalse();
        assertThat(body.path("options").path("num_ctx").asInt()).isEqualTo(8192);
        assertThat(body.path("options").path("num_batch").asInt()).isEqualTo(128);
        assertThat(body.path("options").path("num_gpu").asInt()).isEqualTo(35);
        assertThat(body.path("options").path("num_predict").asInt()).isEqualTo(1536);
        assertThat(body.path("options").path("temperature").asDouble()).isZero();
        assertThat(body.path("think").asBoolean(true)).isFalse();
        assertThat(body.path("format").isObject()).isTrue();
        assertThat(body.path("tools").isMissingNode() || body.path("tools").isEmpty())
                .isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void deadlineClosesActualHttpStreamAndDiscardsPartialOutput() throws Exception {
        start(chunk("partial", false), 200, true);
        context().run(context -> {
            SpringAiReportModel model = model(context.getBean(WebClient.Builder.class));
            assertThatThrownBy(() -> new ReportModelCallExecutor(Duration.ofSeconds(1)).generate(model, "prompt", "{}"))
                    .isInstanceOf(ReportModelTimedOutException.class);
        });
        assertThat(connected.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(disconnected.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void callerInterruptionClosesActualHttpStream() throws Exception {
        start(chunk("partial", false), 200, true);
        context().run(context -> {
            SpringAiReportModel model = model(context.getBean(WebClient.Builder.class));
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread caller = Thread.ofVirtual().start(() -> {
                try {
                    new ReportModelCallExecutor(Duration.ofSeconds(10)).generate(model, "prompt", "{}");
                } catch (Throwable exception) {
                    failure.set(exception);
                }
            });
            assertThat(connected.await(3, TimeUnit.SECONDS)).isTrue();
            caller.interrupt();
            caller.join(2000);
            assertThat(caller.isAlive()).isFalse();
            assertThat(failure.get()).isInstanceOf(ReportModelUnavailableException.class);
        });
        assertThat(disconnected.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void oversizedProviderRejectionIsUnavailableWithoutRetry() throws Exception {
        start("{\"error\":\"input length exceeds context length\"}", 400, false);
        context()
                .run(context -> assertThatThrownBy(() -> model(context.getBean(WebClient.Builder.class))
                                .generate("oversized synthetic prompt", "{}"))
                        .isInstanceOf(ReportModelUnavailableException.class)
                        .hasMessage(null));
        assertThat(calls).hasValue(1);
    }

    @Test
    void incompleteHttpResponseNeverReturnsPartialOutput() throws Exception {
        start(chunk("partial", false), 200, false);
        context()
                .run(context -> assertThatThrownBy(() ->
                                model(context.getBean(WebClient.Builder.class)).generate("prompt", "{}"))
                        .isInstanceOf(ReportModelUnavailableException.class)
                        .hasMessage(null));
        assertThat(calls).hasValue(1);
    }
}
