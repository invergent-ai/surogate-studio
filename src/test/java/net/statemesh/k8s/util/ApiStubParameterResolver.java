package net.statemesh.k8s.util;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 ParameterResolver that injects a fully mocked ApiStub instance using ApiStubTestFactory.
 * Usage:
 *  @ExtendWith(ApiStubParameterResolver.class)
 *  void testSomething(ApiStub apiStub) { ... }
 */
public class ApiStubParameterResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(ApiStub.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return ApiStubTestFactory.builder().build();
    }
}

