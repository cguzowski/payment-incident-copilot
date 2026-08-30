package com.cguzowski.paymentcopilot.report;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

class ReportModelCallExecutorTest {

    @Test
    void returnsSuccessfulOutputBeforeTheDeadline() {
        ReportModelResponse expected = new ReportModelResponse("{\"result\":true}", "request-1");
        ReportModel model = modelReturning(expected);

        ReportModelResponse actual = new ReportModelCallExecutor(Duration.ofSeconds(1)).generate(model, "prompt");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void preservesProviderFailureClassificationBeforeTheDeadline() {
        ReportModelUnavailableException unavailable = new ReportModelUnavailableException();
        ReportModelTimedOutException timedOut = new ReportModelTimedOutException();

        assertThatThrownBy(() -> new ReportModelCallExecutor(Duration.ofSeconds(1))
                        .generate(modelThrowing(unavailable), "prompt"))
                .isSameAs(unavailable);
        assertThatThrownBy(() ->
                        new ReportModelCallExecutor(Duration.ofSeconds(1)).generate(modelThrowing(timedOut), "prompt"))
                .isSameAs(timedOut);
    }

    @Test
    void cancelsAStalledModelCallAtTheTotalDeadline() throws InterruptedException {
        CountDownLatch interrupted = new CountDownLatch(1);
        ReportModel stalled = new ReportModel() {
            @Override
            public String modelId() {
                return "model";
            }

            @Override
            public ReportModelResponse generate(String prompt) {
                try {
                    new CountDownLatch(1).await();
                    throw new AssertionError("The stalled model should not complete normally.");
                } catch (InterruptedException exception) {
                    interrupted.countDown();
                    Thread.currentThread().interrupt();
                    throw new ReportModelUnavailableException(exception);
                }
            }
        };

        assertThatThrownBy(() -> new ReportModelCallExecutor(Duration.ofMillis(100)).generate(stalled, "prompt"))
                .isInstanceOf(ReportModelTimedOutException.class);
        assertThat(interrupted.await(1, SECONDS)).isTrue();
    }

    private static ReportModel modelReturning(ReportModelResponse response) {
        return new ReportModel() {
            @Override
            public String modelId() {
                return "model";
            }

            @Override
            public ReportModelResponse generate(String prompt) {
                return response;
            }
        };
    }

    private static ReportModel modelThrowing(RuntimeException failure) {
        return new ReportModel() {
            @Override
            public String modelId() {
                return "model";
            }

            @Override
            public ReportModelResponse generate(String prompt) {
                throw failure;
            }
        };
    }
}
