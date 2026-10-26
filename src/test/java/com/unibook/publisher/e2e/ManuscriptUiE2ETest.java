package com.unibook.publisher.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.unibook.publisher.e2e.pages.LoginPage;
import com.unibook.publisher.e2e.pages.ManuscriptFormPage;
import com.unibook.publisher.e2e.pages.ManuscriptListPage;
import com.unibook.publisher.support.E2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@E2ETest
class ManuscriptUiE2ETest extends BasePlaywrightTest {

    private static final String PASSWORD = "SuperSecretPassword123!";
    private static final String AUTHOR_EMAIL = "author1@unibook-demo.local";
    private static final String CHIEF_EDITOR_EMAIL = "chief.editor@unibook-demo.local";

    @Test
    @DisplayName("Автор подає рукопис через форму та бачить його у своєму списку")
    void authorCanSubmitManuscriptThroughTheForm() {
        LoginPage loginPage = new LoginPage(page).navigate(baseUrl());
        loginPage.login(AUTHOR_EMAIL, PASSWORD, baseUrl() + "/manuscripts/my");

        ManuscriptListPage listPage = new ManuscriptListPage(page);
        assertThat(listPage.heading()).isVisible();
        assertThat(listPage.submitManuscriptLink()).isVisible();

        String title = "UI manuscript " + System.nanoTime();

        listPage.openSubmissionForm();

        ManuscriptFormPage formPage = new ManuscriptFormPage(page);
        assertThat(formPage.heading()).isVisible();

        formPage
                .fillTitle(title)
                .fillAnnotation("Рукопис, поданий через браузерний інтерфейс під час e2e-тесту.")
                .fillDraftFileUrl("https://storage.example.com/drafts/ui-e2e.docx")
                .checkGenre("Фантастика")
                .submit();

        assertThat(page).hasURL(baseUrl() + "/manuscripts/my");
        assertThat(listPage.successAlert()).isVisible();
        assertThat(listPage.successAlert()).hasText("Рукопис успішно подано.");

        assertThat(listPage.manuscriptRow(title)).isVisible();
        assertThat(listPage.manuscriptRow(title)).containsText(title);
        assertThat(listPage.manuscriptRow(title)).containsText("SUBMITTED");
    }

    @Test
    @DisplayName("Форма показує помилку, якщо не обрано жодного жанру")
    void formShowsValidationErrorWhenNoGenreSelected() {
        LoginPage loginPage = new LoginPage(page).navigate(baseUrl());
        loginPage.login(AUTHOR_EMAIL, PASSWORD, baseUrl() + "/manuscripts/my");

        new ManuscriptListPage(page).openSubmissionForm();

        ManuscriptFormPage formPage = new ManuscriptFormPage(page);
        formPage
                .fillTitle("UI validation manuscript")
                .fillAnnotation("Цей рукопис не має жанру, тому форма має повернути помилку.")
                .fillDraftFileUrl("https://storage.example.com/drafts/ui-validation.docx")
                .submit();

        assertThat(page).hasURL(baseUrl() + "/manuscripts/new");
        assertThat(formPage.genreError()).isVisible();
        assertThat(formPage.titleField()).hasValue("UI validation manuscript");
    }

    @Test
    @DisplayName("Головний редактор бачить усі рукописи, але не бачить кнопку подання")
    void chiefEditorSeesAllManuscriptsWithoutSubmissionLink() {
        LoginPage loginPage = new LoginPage(page).navigate(baseUrl());
        loginPage.login(CHIEF_EDITOR_EMAIL, PASSWORD, baseUrl() + "/manuscripts");

        ManuscriptListPage listPage = new ManuscriptListPage(page);
        assertThat(listPage.allManuscriptsHeading()).isVisible();
        assertThat(listPage.manuscriptRow("SUBMITTED проєкт")).isVisible();
        assertThat(listPage.submitManuscriptLink()).hasCount(0);
    }

    @Test
    @DisplayName("Автору заборонено переглядати загальний список рукописів")
    void authorCannotOpenAllManuscriptsPage() {
        LoginPage loginPage = new LoginPage(page).navigate(baseUrl());
        loginPage.login(AUTHOR_EMAIL, PASSWORD, baseUrl() + "/manuscripts/my");

        open("/manuscripts");

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName("Доступ заборонено"))).isVisible();
        assertThat(page.getByText("У вас немає достатніх прав для перегляду цієї сторінки.")).isVisible();
    }

    @Test
    @DisplayName("Неправильний пароль показує повідомлення про помилку входу")
    void wrongPasswordShowsLoginError() {
        LoginPage loginPage = new LoginPage(page).navigate(baseUrl());
        loginPage.loginExpectingFailure(AUTHOR_EMAIL, "DefinitelyWrongPassword!");

        assertThat(page).hasURL(baseUrl() + "/login?error");
        assertThat(loginPage.errorAlert()).hasText("Невірний логін або пароль.");
    }
}
