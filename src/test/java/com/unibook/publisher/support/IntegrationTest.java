package com.unibook.publisher.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("integration")
@SpringBootTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "file-storage.type=LOCAL",
                "file-storage.location=build/test-uploads",
                "security.jwt.secret=e2e-test-secret-key-that-is-long-enough-for-hs256-signatures",
                "security.jwt.expiration.access=1800000",
                "security.jwt.expiration.refresh=604800000",
                "s3.endpoint=http://localhost:9000",
                "s3.region=us-east-1",
                "s3.bucket=unibook-test",
                "s3.access-key=test-access-key",
                "s3.secret-key=test-secret-key"
        }
)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {
}
