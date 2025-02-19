// springmicrometerundertow/SpringMicrometerUndertowApplication.java

package projecttest.springmicrometerundertow;

import io.micrometer.core.instrument.MeterRegistry;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.handlers.MetricsHandler;
import io.undertow.servlet.api.MetricsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringMicrometerUndertowApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringMicrometerUndertowApplication.class, args);
    }

    @Bean
    UndertowDeploymentInfoCustomizer undertowDeploymentInfoCustomizer(UndertowMetricsHandlerWrapper undertowMetricsHandlerWrapper) {

        return deploymentInfo -> deploymentInfo.addOuterHandlerChainWrapper(undertowMetricsHandlerWrapper);
        //return deploymentInfo -> deploymentInfo.addOuterHandlerChainWrapper(MetricsHandler.WRAPPER);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> micrometerMeterRegistryCustomizer(@Value("${spring.application.name}") String applicationName) {
        return registry -> registry.config().commonTags("application.name", applicationName);
    }

    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public String hello(@RequestParam(name = "name", required = true) String name) {
        return "Hello " + name + "!";
    }

}


// springmicrometerundertow/UndertowMeterBinder.java

package projecttest.springmicrometerundertow;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.undertow.server.handlers.MetricsHandler;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

@Component
public class UndertowMeterBinder implements ApplicationListener<ApplicationReadyEvent> {

    private final UndertowMetricsHandlerWrapper undertowMetricsHandlerWrapper;

    public UndertowMeterBinder(UndertowMetricsHandlerWrapper undertowMetricsHandlerWrapper) {
        this.undertowMetricsHandlerWrapper = undertowMetricsHandlerWrapper;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        bindTo(applicationReadyEvent.getApplicationContext().getBean(MeterRegistry.class));
    }

    public void bindTo(MeterRegistry meterRegistry) {
        bind(meterRegistry, undertowMetricsHandlerWrapper.getMetricsHandler());
    }

    public void bind(MeterRegistry registry, MetricsHandler metricsHandler) {
        bindTimer(registry, "undertow.requests", "Number of requests", metricsHandler,
                m -> m.getMetrics().getTotalRequests(), m2 -> m2.getMetrics().getMinRequestTime());
        bindTimeGauge(registry, "undertow.request.time.max", "The longest request duration in time", metricsHandler,
                m -> m.getMetrics().getMaxRequestTime());
        bindTimeGauge(registry, "undertow.request.time.min", "The shortest request duration in time", metricsHandler,
                m -> m.getMetrics().getMinRequestTime());
        bindCounter(registry, "undertow.request.errors", "Total number of error requests ", metricsHandler,
                m -> m.getMetrics().getTotalErrors());

    }

    private void bindTimer(MeterRegistry registry, String name, String desc, MetricsHandler metricsHandler,
                           ToLongFunction<MetricsHandler> countFunc, ToDoubleFunction<MetricsHandler> consumer) {
        FunctionTimer.builder(name, metricsHandler, countFunc, consumer, TimeUnit.MILLISECONDS)
                .description(desc).register(registry);
    }

    private void bindTimeGauge(MeterRegistry registry, String name, String desc, MetricsHandler metricResult,
                               ToDoubleFunction<MetricsHandler> consumer) {
        TimeGauge.builder(name, metricResult, TimeUnit.MILLISECONDS, consumer).description(desc)
                .register(registry);
    }

    private void bindCounter(MeterRegistry registry, String name, String desc, MetricsHandler metricsHandler,
                             ToDoubleFunction<MetricsHandler> consumer) {
        FunctionCounter.builder(name, metricsHandler, consumer).description(desc)
                .register(registry);
    }
}

// springmicrometerundertow/UndertowMetricsHandlerWrapper.java

package projecttest.springmicrometerundertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.MetricsHandler;
import org.springframework.stereotype.Component;

@Component
public class UndertowMetricsHandlerWrapper implements HandlerWrapper {

    private MetricsHandler metricsHandler;

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        metricsHandler = new MetricsHandler(handler);
        return metricsHandler;
    }

    public MetricsHandler getMetricsHandler() {
        return metricsHandler;
    }
}


