package com.mayu.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Selenium and shadow DOM.
 *
 * <p>These tests deliberately do NOT go through a page object. What is being demonstrated is the
 * selector layer itself, and a page object would hide the exact thing under comparison.
 */
class SeleniumShadowDomTest {

  private static final String BASE = "http://127.0.0.1:5173";

  private WebDriver driver;

  @BeforeEach
  void setUp() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    driver = new ChromeDriver(options);
    driver.get(BASE + "/shadow.html");
  }

  @AfterEach
  void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  @Test
  @DisplayName("an ordinary selector cannot see into a shadow root")
  void ordinarySelectorCannotSeeIn() {
    // Proof, not commentary: CSS does not cross a shadow boundary.
    assertEquals(0, driver.findElements(By.cssSelector("[data-testid=card-title]")).size());
  }

  @Test
  @DisplayName("getShadowRoot() is the way in")
  void getShadowRootIsTheWayIn() {
    WebElement host = driver.findElement(By.cssSelector("task-card"));
    SearchContext shadow = host.getShadowRoot();

    WebElement title = shadow.findElement(By.cssSelector("[data-testid=card-title]"));
    assertEquals("buy milk", title.getText());
  }

  @Test
  @DisplayName("interacting with a control inside a shadow root")
  void interactInsideShadowRoot() {
    SearchContext shadow = driver.findElement(By.cssSelector("task-card")).getShadowRoot();

    shadow.findElement(By.cssSelector("[data-testid=card-action]")).click();

    assertEquals(
        "archived", shadow.findElement(By.cssSelector("[data-testid=card-status]")).getText());
  }

  @Test
  @DisplayName("nested roots need one getShadowRoot() per boundary")
  void nestedRootsNeedAHopPerBoundary() {
    SearchContext panel = driver.findElement(By.cssSelector("task-panel")).getShadowRoot();
    SearchContext card = panel.findElement(By.cssSelector("task-card")).getShadowRoot();

    assertEquals("nested task", card.findElement(By.cssSelector("[data-testid=card-title]")).getText());
  }

  @Test
  @DisplayName("XPath does not work inside a shadow root - CSS only")
  void xpathDoesNotWorkInsideAShadowRoot() {
    SearchContext shadow = driver.findElement(By.cssSelector("task-card")).getShadowRoot();

    // A ShadowRoot is not a Document, so there is no XPath context to evaluate against.
    assertThrows(
        Exception.class,
        () -> shadow.findElement(By.xpath("//span[@data-testid='card-title']")));
  }

  @Test
  @DisplayName("light DOM is unaffected")
  void lightDomIsUnaffected() {
    assertTrue(
        driver
            .findElement(By.cssSelector("[data-testid=light-dom-text]"))
            .getText()
            .contains("main document"));
  }

  @Test
  @DisplayName("a closed shadow root IS reachable from Selenium - verified, not assumed")
  void closedShadowRootIsReachableFromSelenium() {
    WebElement host = driver.findElement(By.cssSelector("secure-note"));

    // In-page JavaScript sees null here, because that is what the spec says a
    // closed root exposes:
    Object fromPageScript =
        ((JavascriptExecutor) driver)
            .executeScript("return document.querySelector('secure-note').shadowRoot;");
    assertNull(fromPageScript);

    // Selenium does not go through element.shadowRoot. It goes through the
    // WebDriver protocol, which the browser grants privileged access to - so it
    // reaches in anyway:
    SearchContext closedRoot = host.getShadowRoot();
    assertEquals(
        "hidden from every automation tool",
        closedRoot.findElement(By.cssSelector("[data-testid=secret]")).getText());

    // The Playwright suite asserts the opposite for the same page: its locators
    // do not pierce a closed root. "Closed means unreachable" is therefore true
    // of the DOM API, and false of Selenium.
  }
}
