package com.unibook.publisher.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.List;

/**
 * Список рукописів ({@code /manuscripts} та {@code /manuscripts/my}).
 */
public class ManuscriptListPage {

    private final Page page;

    public ManuscriptListPage(Page page) {
        this.page = page;
    }

    public Locator heading() {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Мої рукописи"));
    }

    public Locator allManuscriptsHeading() {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Усі рукописи"));
    }

    public Locator successAlert() {
        return page.locator(".alert-success");
    }

    public Locator submitManuscriptLink() {
        return page.locator("a[href='/manuscripts/new']");
    }

    public Locator emptyMessage() {
        return page.getByText("Рукописів ще немає.");
    }

    public Locator manuscriptRow(String title) {
        return page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(title));
    }

    public List<String> manuscriptTitles() {
        return page.locator("tbody tr td:nth-child(2) strong").allTextContents();
    }

    public void openSubmissionForm() {
        submitManuscriptLink().click();
    }
}
