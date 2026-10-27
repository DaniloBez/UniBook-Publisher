package com.unibook.publisher.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Базовий клас для Playwright UI тестів.
 *
 * <p>Chromium запускається один раз на клас у headless-режимі (можна вимкнути
 * через {@code -Dplaywright.headless=false}). Для кожного тесту створюється
 * окремий {@link BrowserContext} із записом trace. Скріншот при падінні та
 * trace зберігаються у {@code build/reports/playwright/<class>-<test>/}.</p>
 */
@ExtendWith(BasePlaywrightTest.PlaywrightFailureExtension.class)
public abstract class BasePlaywrightTest {

    private static final Path REPORTS_DIR = Path.of("build", "reports", "playwright");

    protected static Playwright playwright;
    protected static Browser browser;

    @LocalServerPort
    protected int port;

    protected BrowserContext context;
    protected Page page;

    private Path testReportDir;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(isHeadless()));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void openPage(TestInfo testInfo) throws IOException {
        testReportDir = REPORTS_DIR.resolve(reportName(testInfo));
        Files.createDirectories(testReportDir);

        context = browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(baseUrl()));
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true)
                .setTitle(testInfo.getDisplayName()));

        page = context.newPage();
        page.setDefaultTimeout(10_000);
        page.setDefaultNavigationTimeout(15_000);
    }

    @AfterEach
    void closePage() throws IOException {
        if (context == null) {
            return;
        }

        try {
            context.tracing().stop(new Tracing.StopOptions()
                    .setPath(testReportDir.resolve("trace.zip")));
        } catch (RuntimeException ignored) {
            // trace вже зупинено або браузер аварійно закрився — не маскуємо результат тесту
        } finally {
            context.close();
            context = null;
        }
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected void open(String path) {
        page.navigate(baseUrl() + path);
    }

    private void saveFailureScreenshot() {
        if (page == null || testReportDir == null) {
            return;
        }

        try {
            Files.createDirectories(testReportDir);
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(testReportDir.resolve("failure.png"))
                    .setFullPage(true));
        } catch (RuntimeException | IOException exception) {
            System.err.println("Не вдалося зберегти скріншот Playwright: " + exception.getMessage());
        }
    }

    private static boolean isHeadless() {
        return Boolean.parseBoolean(System.getProperty("playwright.headless", "true"));
    }

    private static String reportName(TestInfo testInfo) {
        String className = testInfo.getTestClass()
                .map(Class::getSimpleName)
                .orElse("UnknownTest");
        String testName = testInfo.getTestMethod()
                .map(method -> method.getName())
                .orElseGet(testInfo::getDisplayName);
        return sanitize(className) + "-" + sanitize(testName);
    }

    private static String sanitize(String value) {
        String sanitized = value.replaceAll("[^a-zA-Z0-9._-]+", "_");
        return sanitized.length() > 120 ? sanitized.substring(0, 120) : sanitized;
    }

    /**
     * JUnit викликає {@link AfterTestExecutionCallback} до {@code @AfterEach},
     * тому сторінка ще відкрита і скріншот справді відображає момент падіння.
     */
    public static final class PlaywrightFailureExtension implements AfterTestExecutionCallback {

        @Override
        public void afterTestExecution(ExtensionContext extensionContext) {
            if (extensionContext.getExecutionException().isEmpty()) {
                return;
            }

            Object testInstance = extensionContext.getRequiredTestInstance();
            if (testInstance instanceof BasePlaywrightTest browserTest) {
                browserTest.saveFailureScreenshot();
            }
        }
    }
}
