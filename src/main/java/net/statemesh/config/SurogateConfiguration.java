package net.statemesh.config;

import net.statemesh.k8s.strategy.ClusterSelectionStrategy;
import net.statemesh.k8s.strategy.RandomClusterSelectionStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Set;

@Configuration
public class SurogateConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean(name = "clusterSelector")
    public ClusterSelectionStrategy getClusterSelector() {
        return new RandomClusterSelectionStrategy();
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        SpringResourceTemplateResolver scriptResolver = new SpringResourceTemplateResolver();
        scriptResolver.setApplicationContext(applicationContext);
        scriptResolver.setPrefix("classpath:/templates/");
        scriptResolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolvers(Set.of(scriptResolver));
        return templateEngine;
    }
}
