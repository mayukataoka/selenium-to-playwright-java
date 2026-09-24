package com.mayu.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mayu.tasks.pages.SeleniumTaskListPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/** Selenium version, driven through a page object. */
class SeleniumTaskListTest {

  private static final String BASE = "http://127.0.0.1:5173";

  private WebDriver driver;
  private SeleniumTaskListPage tasks;

  @BeforeEach
  void setUp() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    driver = new ChromeDriver(options);
    tasks = new SeleniumTaskListPage(driver).open(BASE);
  }

  @AfterEach
  void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  @Test
  @DisplayName("starts empty")
  void startsEmpty() {
    assertTrue(tasks.emptyStateIsVisible());
    assertEquals(0, tasks.taskCount());
    assertTrue(tasks.counterText().contains("0 remaining"));
  }

  @Test
  @DisplayName("adds a task")
  void addsATask() {
    tasks.addTask("buy milk");

    tasks.waitForTaskCount(1);
    assertEquals(1, tasks.taskCount());
    assertEquals("buy milk", tasks.firstTaskTitle());
    assertTrue(tasks.counterText().contains("1 remaining"));
  }

  @Test
  @DisplayName("rejects an empty task")
  void rejectsAnEmptyTask() {
    tasks.submitEmptyTask();

    assertEquals(0, tasks.taskCount());
  }

  @Test
  @DisplayName("marks a task done and decrements the counter")
  void marksATaskDone() {
    tasks.addTask("write tests");
    tasks.waitForTaskCount(1);

    tasks.toggleFirstTask();

    tasks.waitForFirstTaskDone();
    assertTrue(tasks.firstTaskIsDone());
    assertTrue(tasks.counterText().contains("0 remaining"));
  }

  @Test
  @DisplayName("deletes a task")
  void deletesATask() {
    tasks.addTask("temporary");
    tasks.waitForTaskCount(1);

    tasks.deleteFirstTask();

    tasks.waitForTaskCount(0);
    assertEquals(0, tasks.taskCount());
    assertTrue(tasks.emptyStateIsVisible());
  }
}
