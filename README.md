# Configure Docker-Compose
```yaml
services:
  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"
    command: -config.file=/etc/loki/local-config.yaml
    networks:
      - grafana

  grafana:
    environment:
      - GF_PATHS_PROVISIONING=/etc/grafana/provisioning
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
    entrypoint:
      - sh
      - -euc
      - |
        mkdir -p /etc/grafana/provisioning/datasources
        cat <<EOF > /etc/grafana/provisioning/datasources/ds.yaml
        apiVersion: 1
        datasources:
        - name: Loki
          type: loki
          access: proxy
          orgId: 1
          url: http://loki:3100
          basicAuth: false
          isDefault: true
          version: 1
          editable: false
        - name: Prometheus
          type: prometheus
          access: proxy
          url: http://prometheus:9090
          isDefault: false
        EOF
        /run.sh
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    networks:
      - grafana

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - "./prometheus.yml:/etc/prometheus/prometheus.yml"
    networks:
      - grafana

networks:
  grafana:
    driver: bridge
```

# Add Loki Dependency
1. Add to `pom.xml`
```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender</artifactId>
    <version>1.6.0</version>
</dependency>
```

# Create logback-spring.xml
1. under `src/main/resources` create the file `logback-spring.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty name="appName" source="spring.application.name"/>

    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http>
            <url>http://localhost:3100/loki/api/v1/push</url>
        </http>
        <format>
            <label>
                <pattern>app=${appName},host=${HOSTNAME},level=%level</pattern>
                <readMarkers>true</readMarkers>
            </label>
            <message>
                <pattern>
                    {
                    "level":"%level",
                    "class":"%logger{36}",
                    "thread":"%thread",
                    "message": "%message",
                    "requestId": "%X{X-Request-ID}"
                    }
                </pattern>
            </message>
        </format>
    </appender>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="LOKI" />
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

# Navigate to Grafana and start using it
1. Visit http://localhost:3000 and since Loki is configured as a Datasource in the docker-compose.yml file go to explore and query your logs with `{host="LAPTOP-LQBHLF3H"} |~ "Hello|World" | detected_level = "INFO"` or `{app="grafanalokidemo"} |~ "Hello|World" | detected_level = "INFO"`

# Now for metrics adding Prometheus
1. Add dependencies
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```
2. Add configuration to `application.properties`. This will configure our Spring Boot application to expose the Prometheus metrics endpoint `/actuator/prometheus`
```bash
management.endpoints.web.exposure.include=*
management.prometheus.metrics.export.enabled=true
management.prometheus.metrics.export.pushgateway.enabled=true
```
3. Create in the root a `prometheus.yml` file and add the following configuration. Otherwise, add the configuration in `/etc/prometheus/prometheus.yml` within the grafana container.
```yaml
scrape_configs:
  - job_name: 'spring-boot-application'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s # This can be adjusted based on our needs
    static_configs:
      - targets: ['host.docker.internal:8080']
```
4. Configure Prometheus datasource directly in Grafana service in `docker-compose.yaml`
```yaml
    entrypoint:
      - sh
      - -euc
      - |
        mkdir -p /etc/grafana/provisioning/datasources
        cat <<EOF > /etc/grafana/provisioning/datasources/ds.yaml
        apiVersion: 1
        datasources:
        - name: Loki
          type: loki
          access: proxy
          orgId: 1
          url: http://loki:3100
          basicAuth: false
          isDefault: true
          version: 1
          editable: false
        - name: Prometheus
          type: prometheus
          access: proxy
          url: http://prometheus:9090
          isDefault: false
        EOF
        /run.sh
    image: grafana/grafana:latest
```
5. With the docker-compose.yml configuration on point 4 it's not necessary anymore to add the manually the datasource. Otherwise, Add Prometheus as a Datasource in Grafana in `Connections > DataSources`. Prometheus is running on http://prometheus:9090
6. Now it's time to use `PromQL` to build Dashboards. For example, to reveal the avg response time of HTTP requests, we would use a query like `rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])`