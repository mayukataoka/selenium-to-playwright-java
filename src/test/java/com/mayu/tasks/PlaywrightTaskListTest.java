package com.mayu.tasks;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Playwright version of the same five scenarios.
 *
 * <p>Two structural differences from the Selenium test: the browser is launched once for the whole
 * class and each test gets a fresh isolated BrowserContext (cheap, so isolation costs nothing), and
 * there are no explicit waits - every assertThat() retries until it passes or times out.
 */
class PlaywrightTaskListTest {

  private static final String BASE = "http://127.0.0.1:5173";

  private static Playwright playwright;
  private static Browser browser;

  private BrowserContext context;
  private Page page;

  @BeforeAll
  static void launchBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
  }

  @AfterAll
  static void closeBrowser() {
    playwright.close();
  }

  @BeforeEach
  void newContext() {
    // A fresh context per test: isolated cookies and storage, milliseconds to create.
    context = browser.newContext();
    page = context.newPage();
    page.navigate(BASE + "/");
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  @DisplayName("starts empty")
  void startsEmpty() {
    assertThat(page.getByTestId("empty-state")).isVisible();
    assertThat(page.getByTestId("task-item")).hasCount(0);
    assertThat(page.getByTestId("counter")).containsText("0 remaining");
  }

  @Test
  @DisplayName("adds a task")
  void addsATask() {
    page.locator("#new-task-input").fill("buy milk");
    page.getByTestId("add-button").click();

    // No explicit wait: the assertion itself retries.
    assertThat(page.getByTestId("task-item")).hasCount(1);
    assertThat(page.getByTestId("task-title")).hasText("buy milk");
    assertThat(page.getByTestId("counter")).containsText("1 remaining");
  }

  @Test
  @DisplayName("rejects an empty task")
  void rejectsAnEmptyTask() {
    page.getByTestId("add-button").click();
    assertThat(page.getByTestId("task-item")).hasCount(0);
  }

  @Test
  @DisplayName("marks a task done and decrements the counter")
  void marksATaskDone() {
    page.locator("#new-task-input").fill("write tests");
    page.locator("#new-task-input").press("Enter");

    page.getByTestId("toggle").check();

    assertThat(page.getByTestId("task-item").first()).hasClass(Pattern.compile("done"));
    assertThat(page.getByTestId("counter")).containsText("0 remaining");
  }

  @Test
  @DisplayName("deletes a task")
  void deletesATask() {
    page.locator("#new-task-input").fill("temporary");
    page.locator("#new-task-input").press("Enter");
    assertThat(page.getByTestId("task-item")).hasCount(1);

    page.getByTestId("delete").click();

    assertThat(page.getByTestId("task-item")).hasCount(0);
    assertThat(page.getByTestId("empty-state")).isVisible();
  }
}
