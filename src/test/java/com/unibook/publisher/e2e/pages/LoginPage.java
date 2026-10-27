package com.unibook.publisher.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LoginPage {

    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public LoginPage navigate(String baseUrl) {
        page.navigate(baseUrl + "/login");
        assertThat(heading()).hasText("UniBook");
        return this;
    }

    public Locator heading() {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("UniBook"));
    }

    public Locator emailField() {
        return page.getByLabel("Email");
    }

    public Locator passwordField() {
        return page.getByLabel("Пароль");
    }

    public Locator submitButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Увійти"));
    }

    public Locator errorAlert() {
        return page.getByRole(AriaRole.ALERT);
    }

    public void login(String email, String password, String expectedPath) {
        emailField().fill(email);
        passwordField().fill(password);
        submitButton().click();

        assertThat(page).hasURL(expectedPath);
    }

    public void loginExpectingFailure(String email, String password) {
        emailField().fill(email);
        passwordField().fill(password);
        submitButton().click();

        assertThat(errorAlert()).isVisible();
        assertThat(errorAlert()).hasText("Невірний логін або пароль.");
    }
}
