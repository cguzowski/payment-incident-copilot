package com.cguzowski.paymentcopilot.report;

final class ReportModelTimedOutException extends RuntimeException {

    ReportModelTimedOutException() {}

    ReportModelTimedOutException(Throwable cause) {
        super(cause);
    }
}
