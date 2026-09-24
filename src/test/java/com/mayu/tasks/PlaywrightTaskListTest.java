package com.mayu.tasks;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.mayu.tasks.pages.PlaywrightTaskListPage;
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
 * Playwright version, driven through a page object.
 *
 * <p>Compare the action lines with the Selenium test: they are identical, because both page objects
 * expose the same action API. Only the assertions changed - and they changed on purpose, from
 * reading a value to asserting on a Locator, which is what removes the explicit waits.
 */
class PlaywrightTaskListTest {

  private static final String BASE = "http://127.0.0.1:5173";

  private static Playwright playwright;
  private static Browser browser;

  private BrowserContext context;
  private PlaywrightTaskListPage tasks;

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
    context = browser.newContext();
    Page page = context.newPage();
    tasks = new PlaywrightTaskListPage(page).open(BASE);
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  @DisplayName("starts empty")
  void startsEmpty() {
    assertThat(tasks.emptyState()).isVisible();
    assertThat(tasks.tasks()).hasCount(0);
    assertThat(tasks.counter()).containsText("0 remaining");
  }

  @Test
  @DisplayName("adds a task")
  void addsATask() {
    tasks.addTask("buy milk");

    assertThat(tasks.tasks()).hasCount(1);
    assertThat(tasks.firstTaskTitle()).hasText("buy milk");
    assertThat(tasks.counter()).containsText("1 remaining");
  }

  @Test
  @DisplayName("rejects an empty task")
  void rejectsAnEmptyTask() {
    tasks.submitEmptyTask();

    assertThat(tasks.tasks()).hasCount(0);
  }

  @Test
  @DisplayName("marks a task done and decrements the counter")
  void marksATaskDone() {
    tasks.addTask("write tests");

    tasks.toggleFirstTask();

    assertThat(tasks.firstTask()).hasClass(Pattern.compile("done"));
    assertThat(tasks.counter()).containsText("0 remaining");
  }

  @Test
  @DisplayName("deletes a task")
  void deletesATask() {
    tasks.addTask("temporary");
    assertThat(tasks.tasks()).hasCount(1);

    tasks.deleteFirstTask();

    assertThat(tasks.tasks()).hasCount(0);
    assertThat(tasks.emptyState()).isVisible();
  }
}
