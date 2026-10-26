package com.unibook.publisher.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class ManuscriptFormPage {

    private final Page page;

    public ManuscriptFormPage(Page page) {
        this.page = page;
    }

    public Locator heading() {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Подати новий рукопис"));
    }

    public Locator titleField() {
        return page.locator("#title");
    }

    public Locator annotationField() {
        return page.locator("#annotation");
    }

    public Locator draftFileUrlField() {
        return page.locator("#draftFileUrl");
    }

    public Locator submitButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Подати"));
    }

    public Locator genreCheckbox(String genreName) {
        return page.getByLabel(genreName);
    }

    public Locator genreError() {
        return page.getByText("Оберіть хоча б один жанр");
    }

    public ManuscriptFormPage fillTitle(String title) {
        titleField().fill(title);
        return this;
    }

    public ManuscriptFormPage fillAnnotation(String annotation) {
        annotationField().fill(annotation);
        return this;
    }

    public ManuscriptFormPage fillDraftFileUrl(String draftFileUrl) {
        draftFileUrlField().fill(draftFileUrl);
        return this;
    }

    public ManuscriptFormPage checkGenre(String genreName) {
        genreCheckbox(genreName).check();
        return this;
    }

    public void submit() {
        submitButton().click();
    }
}
