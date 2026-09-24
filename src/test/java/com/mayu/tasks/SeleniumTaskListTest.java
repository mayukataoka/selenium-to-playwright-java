package com.mayu.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** The Selenium version. Note the manual driver lifecycle and the explicit waits. */
class SeleniumTaskListTest {

  private static final String BASE = "http://127.0.0.1:5173";

  private WebDriver driver;
  private WebDriverWait wait;

  @BeforeEach
  void setUp() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    driver = new ChromeDriver(options);
    // Selenium has no auto-waiting assertions: every wait is explicit.
    wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    driver.get(BASE + "/");
  }

  @AfterEach
  void tearDown() {
    // Forget this and you leak a browser process per test.
    if (driver != null) {
      driver.quit();
    }
  }

  @Test
  @DisplayName("starts empty")
  void startsEmpty() {
    WebElement empty = driver.findElement(By.cssSelector("[data-testid=empty-state]"));
    assertTrue(empty.isDisplayed());
    assertEquals(0, driver.findElements(By.cssSelector("[data-testid=task-item]")).size());
    assertTrue(
        driver.findElement(By.cssSelector("[data-testid=counter]")).getText().contains("0 remaining"));
  }

  @Test
  @DisplayName("adds a task")
  void addsATask() {
    driver.findElement(By.id("new-task-input")).sendKeys("buy milk");
    driver.findElement(By.cssSelector("[data-testid=add-button]")).click();

    // Without this wait the assertion below is a race.
    wait.until(
        ExpectedConditions.numberOfElementsToBe(By.cssSelector("[data-testid=task-item]"), 1));

    List<WebElement> items = driver.findElements(By.cssSelector("[data-testid=task-item]"));
    assertEquals(1, items.size());
    assertEquals(
        "buy milk", driver.findElement(By.cssSelector("[data-testid=task-title]")).getText());
    assertTrue(
        driver.findElement(By.cssSelector("[data-testid=counter]")).getText().contains("1 remaining"));
  }

  @Test
  @DisplayName("rejects an empty task")
  void rejectsAnEmptyTask() {
    driver.findElement(By.cssSelector("[data-testid=add-button]")).click();
    assertEquals(0, driver.findElements(By.cssSelector("[data-testid=task-item]")).size());
  }

  @Test
  @DisplayName("marks a task done and decrements the counter")
  void marksATaskDone() {
    driver.findElement(By.id("new-task-input")).sendKeys("write tests\n");
    wait.until(
        ExpectedConditions.numberOfElementsToBe(By.cssSelector("[data-testid=task-item]"), 1));

    driver.findElement(By.cssSelector("[data-testid=toggle]")).click();

    wait.until(
        ExpectedConditions.attributeContains(
            By.cssSelector("[data-testid=task-item]"), "class", "done"));
    assertTrue(
        driver.findElement(By.cssSelector("[data-testid=counter]")).getText().contains("0 remaining"));
  }

  @Test
  @DisplayName("deletes a task")
  void deletesATask() {
    driver.findElement(By.id("new-task-input")).sendKeys("temporary\n");
    wait.until(
        ExpectedConditions.numberOfElementsToBe(By.cssSelector("[data-testid=task-item]"), 1));

    driver.findElement(By.cssSelector("[data-testid=delete]")).click();

    wait.until(
        ExpectedConditions.numberOfElementsToBe(By.cssSelector("[data-testid=task-item]"), 0));
    assertTrue(driver.findElement(By.cssSelector("[data-testid=empty-state]")).isDisplayed());
  }
}
