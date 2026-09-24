package com.mayu.tasks.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page object for the task list, Selenium implementation.
 *
 * <p>Every wait the tests would otherwise need lives in here. That is the point: the tests describe
 * behaviour, the page object owns how to talk to the browser.
 */
public class SeleniumTaskListPage {

  private static final By INPUT = By.id("new-task-input");
  private static final By ADD_BUTTON = By.cssSelector("[data-testid=add-button]");
  private static final By TASK_ITEM = By.cssSelector("[data-testid=task-item]");
  private static final By TASK_TITLE = By.cssSelector("[data-testid=task-title]");
  private static final By TOGGLE = By.cssSelector("[data-testid=toggle]");
  private static final By DELETE = By.cssSelector("[data-testid=delete]");
  private static final By EMPTY_STATE = By.cssSelector("[data-testid=empty-state]");
  private static final By COUNTER = By.cssSelector("[data-testid=counter]");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public SeleniumTaskListPage(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(5));
  }

  public SeleniumTaskListPage open(String baseUrl) {
    driver.get(baseUrl + "/");
    return this;
  }

  // --- actions -------------------------------------------------------------

  public void addTask(String title) {
    driver.findElement(INPUT).sendKeys(title);
    driver.findElement(ADD_BUTTON).click();
  }

  public void submitEmptyTask() {
    driver.findElement(ADD_BUTTON).click();
  }

  public void toggleFirstTask() {
    driver.findElement(TOGGLE).click();
  }

  public void deleteFirstTask() {
    driver.findElement(DELETE).click();
  }

  // --- state, with the waits the assertions would otherwise race against ----

  public int taskCount() {
    return driver.findElements(TASK_ITEM).size();
  }

  public void waitForTaskCount(int expected) {
    wait.until(ExpectedConditions.numberOfElementsToBe(TASK_ITEM, expected));
  }

  public void waitForFirstTaskDone() {
    wait.until(ExpectedConditions.attributeContains(TASK_ITEM, "class", "done"));
  }

  public String firstTaskTitle() {
    return driver.findElement(TASK_TITLE).getText();
  }

  public boolean firstTaskIsDone() {
    return driver.findElement(TASK_ITEM).getAttribute("class").contains("done");
  }

  public boolean emptyStateIsVisible() {
    return driver.findElement(EMPTY_STATE).isDisplayed();
  }

  public String counterText() {
    return driver.findElement(COUNTER).getText();
  }
}
