package com.mayu.tasks.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Page object for the task list, Playwright implementation.
 *
 * <p>The action methods have the same names and signatures as the Selenium page object, which is
 * what let the test bodies survive the migration unchanged.
 *
 * <p>The state methods deliberately do NOT. They return {@link Locator}s rather than resolved
 * values, so the tests can assert on them with Playwright's retrying assertions. A page object that
 * returns a String or an int forces an immediate read, and an immediate read throws away the very
 * auto-waiting the migration was for - you end up reintroducing explicit waits in Playwright.
 */
public class PlaywrightTaskListPage {

  private final Page page;

  public PlaywrightTaskListPage(Page page) {
    this.page = page;
  }

  public PlaywrightTaskListPage open(String baseUrl) {
    page.navigate(baseUrl + "/");
    return this;
  }

  // --- actions: same names and signatures as the Selenium page object -------

  public void addTask(String title) {
    page.locator("#new-task-input").fill(title);
    page.getByTestId("add-button").click();
  }

  public void submitEmptyTask() {
    page.getByTestId("add-button").click();
  }

  public void toggleFirstTask() {
    page.getByTestId("toggle").check();
  }

  public void deleteFirstTask() {
    page.getByTestId("delete").click();
  }

  // --- state: Locators, not values, so assertions can retry -----------------

  public Locator tasks() {
    return page.getByTestId("task-item");
  }

  public Locator firstTaskTitle() {
    return page.getByTestId("task-title");
  }

  public Locator firstTask() {
    return tasks().first();
  }

  public Locator emptyState() {
    return page.getByTestId("empty-state");
  }

  public Locator counter() {
    return page.getByTestId("counter");
  }
}
