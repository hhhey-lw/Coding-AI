package com.coding;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@MapperScan("com.coding.admin.mapper")
public class SpringAiWorkflowApplication {

    public static void main(String[] args) throws UnknownHostException {
        // 1. 启动 Spring Boot 应用，并获取应用上下文
        ConfigurableApplicationContext context = SpringApplication.run(SpringAiWorkflowApplication.class, args);

        // 2. 获取实际使用的端口
        int port = 8080; // 默认值
        String contextPath = ""; // 默认无上下文路径

        if (context instanceof ServletWebServerApplicationContext webContext) {
            port = webContext.getWebServer().getPort(); // 获取实际端口，比如 8080 或你配置的 server.port
        }

        // 3. 获取 context-path（如果有，比如 /api）
        contextPath = context.getEnvironment().getProperty("server.servlet.context-path", "");
        if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        // 4. 获取本机 IP 地址
        String hostAddress = InetAddress.getLocalHost().getHostAddress(); // 例如 192.168.1.100
        // 如果你想显示主机名，也可以用 getHostName()

        // 5. 拼接 Swagger UI 和 OpenAPI JSON 的访问地址
        String swaggerUiUrl = String.format("http://%s:%d%s/swagger-ui/index.html", hostAddress, port, contextPath);
        String openApiJsonUrl = String.format("http://%s:%d%s/v3/api-docs", hostAddress, port, contextPath);

        // 6. 打印到控制台
        System.out.println("\n========================================");
        System.out.println("✅ Swagger UI 可访问地址: " + swaggerUiUrl);
        System.out.println("✅ OpenAPI JSON 文档: " + openApiJsonUrl);
        System.out.println("========================================\n");
    }

}
