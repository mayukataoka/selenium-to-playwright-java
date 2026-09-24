package com.mayu.tasks;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Playwright and shadow DOM.
 *
 * <p>There is no shadow-specific API used below, on purpose. Playwright's css and text engines
 * pierce OPEN shadow roots at any depth, so these selectors are written exactly as they would be
 * for light DOM. Compare the line count with the Selenium version.
 */
class PlaywrightShadowDomTest {

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
    context = browser.newContext();
    page = context.newPage();
    page.navigate(BASE + "/shadow.html");
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  @DisplayName("an ordinary selector reaches into a shadow root")
  void ordinarySelectorReachesIn() {
    assertThat(page.getByTestId("card-title").first()).hasText("buy milk");
    assertThat(page.getByTestId("card-status").first()).hasText("active");
  }

  @Test
  @DisplayName("interacting with a control inside a shadow root")
  void interactInsideShadowRoot() {
    page.getByTestId("card-action").first().click();

    assertThat(page.getByTestId("card-status").first()).hasText("archived");
  }

  @Test
  @DisplayName("nested roots need no extra syntax at all")
  void nestedRootsNeedNoExtraSyntax() {
    assertThat(page.locator("task-panel").getByTestId("card-title")).hasText("nested task");
    assertThat(page.getByTestId("panel-heading")).hasText("This week");
  }

  @Test
  @DisplayName("light DOM is unaffected")
  void lightDomIsUnaffected() {
    assertThat(page.getByTestId("light-dom-text")).containsText("main document");
  }

  @Test
  @DisplayName("a closed shadow root is NOT reachable - unlike Selenium")
  void closedShadowRootIsNotReachable() {
    // Playwright's locators do not pierce a closed root. The Selenium suite
    // asserts the opposite for this same element: getShadowRoot() reaches in
    // over the WebDriver protocol. Worth knowing before claiming that a closed
    // root is "unreachable by any tool" - that is true of the DOM API only.
    assertThat(page.getByTestId("secret")).hasCount(0);
  }
}
