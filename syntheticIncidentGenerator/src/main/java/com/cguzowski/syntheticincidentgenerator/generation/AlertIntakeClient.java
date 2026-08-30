package com.cguzowski.syntheticincidentgenerator.generation;

@FunctionalInterface
public interface AlertIntakeClient {

    AlertIntakeResponse submit(AlertIntakeRequest request);
}
