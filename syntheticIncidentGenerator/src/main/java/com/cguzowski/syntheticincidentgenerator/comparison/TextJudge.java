package com.cguzowski.syntheticincidentgenerator.comparison;

interface TextJudge {
    Reply evaluate(String prompt);

    record Reply(String content, String providerResponse) {}
}
