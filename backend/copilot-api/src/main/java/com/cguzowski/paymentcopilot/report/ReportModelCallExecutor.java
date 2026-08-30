package com.cguzowski.paymentcopilot.report;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class ReportModelCallExecutor {

    private final Duration timeout;

    ReportModelCallExecutor(@Value("${app.report.generation-timeout:2m}") Duration timeout) {
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("The report generation timeout must be positive.");
        }
        this.timeout = timeout;
    }

    ReportModelResponse generate(ReportModel model, String prompt) {
        FutureTask<ReportModelResponse> invocation = new FutureTask<>(() -> model.generate(prompt));
        Thread.ofVirtual().name("report-model-call-", 0).start(invocation);
        try {
            return invocation.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException exception) {
            invocation.cancel(true);
            throw new ReportModelTimedOutException(exception);
        } catch (InterruptedException exception) {
            invocation.cancel(true);
            Thread.currentThread().interrupt();
            throw new ReportModelUnavailableException(exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new ReportModelUnavailableException(exception);
        }
    }
}
