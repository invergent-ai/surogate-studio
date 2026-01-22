package net.statemesh.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ShutdownService implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(@NotNull ApplicationContext context) {
        this.context = context;
    }

    public void shutdown(int exitCode) {
        ((ConfigurableApplicationContext) context).close();
        System.exit(exitCode);
    }
}
