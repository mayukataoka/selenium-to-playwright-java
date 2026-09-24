# Selenium → Playwright in Java: the same five tests, twice

One small task-list app (`app/index.html`), covered by an identical suite of five
scenarios written twice in Java + JUnit 5 — once with Selenium WebDriver, once
with Playwright — so the migration differences are visible line by line.

    SeleniumTaskListTest     5 passed
    PlaywrightTaskListTest   5 passed

## Run them

    brew install maven                  # requires JDK 21 and Chrome
    mvn exec:java -D exec.mainClass=com.microsoft.playwright.CLI \
                  -D exec.classpathScope=test -D exec.args="install chromium"

    npx http-server app -p 5173 --silent &   # serve the app under test
    mvn test

## Measured on this machine

| Run | Selenium | Playwright |
|---|---|---|
| Cold (first run) | 12.1s | 27.5s |
| Warm | 5.3s | **2.5s** |

Playwright is roughly twice as fast warm, but pays a one-time driver bootstrap
cost that makes a single cold run look worse. Worth knowing before quoting
either number: measure warm, and measure more than once.

## Where to look

Two files carry most of what this repository is trying to show.

**The test bodies.** Open `SeleniumTaskListTest` and `PlaywrightTaskListTest`
side by side. The lines that drive the app are identical; the lines that changed
are all assertions. Every `WebDriverWait` / `ExpectedConditions` on the left has
no counterpart on the right — it was absorbed into a retrying `assertThat(locator)`.

**The lifecycle.** Compare the setup and teardown. Selenium constructs a
`ChromeDriver` in `@BeforeEach` and must `quit()` it in `@AfterEach`, because a
driver *is* a browser process. Playwright splits that into a hierarchy —
`Playwright` → `Browser` → `BrowserContext` → `Page` — and launches the browser
once per class in `@BeforeAll`, handing each test a fresh `BrowserContext`
instead. A context is an isolated cookie jar and storage partition that costs
milliseconds, so per-test isolation stops being something a suite has to trade
away for speed. That is the reason for the split: the expensive object is shared,
the isolating one is not.

## Line-by-line mapping

| Intent | Selenium | Playwright |
|---|---|---|
| open a page | `driver.get(url)` | `page.navigate(url)` |
| find by css | `driver.findElement(By.cssSelector(s))` | `page.locator(s)` |
| find by test id | `By.cssSelector("[data-testid=x]")` | `page.getByTestId("x")` |
| find many | `driver.findElements(...)` | the same locator — it is a collection |
| type | `.sendKeys("text")` | `.fill("text")` |
| press a key | `.sendKeys("text\n")` | `.press("Enter")` |
| click | `.click()` | `.click()` |
| check a box | `.click()` on the input | `.check()` (verifies the end state) |
| read text | `.getText()` | `assertThat(loc).hasText(...)` |
| count | `findElements(...).size()` | `assertThat(loc).hasCount(n)` |
| visible | `.isDisplayed()` | `assertThat(loc).isVisible()` |
| class | `.getAttribute("class").contains(...)` | `assertThat(loc).hasClass(Pattern...)` |
| wait | `new WebDriverWait(driver, ...).until(ExpectedConditions...)` | **none — assertions retry** |
| setup | `new ChromeDriver(options)` per test | `browser.newContext()` per test |
| teardown | `driver.quit()` — mandatory | `context.close()` |

## What actually changes in a migration

- **Waiting.** This is the whole point. Selenium needs an explicit
  `WebDriverWait` before any assertion that follows a state change; Playwright's
  `assertThat()` retries until it passes or times out. Most of the flakiness in
  a mature Selenium suite lives in the waits, and most of the deleted lines in a
  migration are waits.
- **Isolation cost.** Selenium launches a whole browser per test (`@BeforeEach`),
  which is why suites drift toward sharing one driver and leaking state between
  tests. Playwright launches the browser once per class and gives each test a
  fresh `BrowserContext` in milliseconds, so isolation stops being a tradeoff.
- **Lifecycle.** A forgotten `driver.quit()` leaks a browser process per test.
  Playwright's contexts are cheap and closing one cannot leak a browser.
- **Assertion style.** Selenium reads values and asserts on them; Playwright
  asserts on the locator and lets the assertion do the polling. Porting
  `assertEquals(expected, el.getText())` to `assertThat(loc).hasText(expected)`
  is not cosmetic — it is what removes the race.

## Keeping the Page Object Model across the migration

Both suites are driven through a page object (`pages/SeleniumTaskListPage`,
`pages/PlaywrightTaskListPage`). Keeping that layer is what makes a migration
incremental rather than a rewrite: the page object is the only thing that knows
which driver is underneath, so it absorbs the change.

Measured on these five tests, after introducing the page objects:

| | Selenium test | Playwright test |
|---|---|---|
| Action calls (`addTask`, `deleteFirstTask`, …) | identical | identical |
| Explicit waits in the test body | 5 | **0** |

The action API is deliberately the same on both page objects, so the lines that
*drive* the app survived the migration untouched. What changed is the assertions.

### The trap: a page object that returns values

The obvious way to write a page object is to have it return resolved values —
`String firstTaskTitle()`, `int taskCount()`. That works in Selenium, and it is
exactly what to avoid in Playwright:

```java
// Selenium page object - returns a value, so the test must wait first
public int taskCount() { return driver.findElements(TASK_ITEM).size(); }

// Playwright page object - returns a Locator, so the assertion can retry
public Locator tasks() { return page.getByTestId("task-item"); }
```

A resolved value is read once, at the moment it is called. If the page object
hands the test a `String`, the test can only assert on a snapshot, and the
auto-waiting the migration was *for* is thrown away — you end up reintroducing
explicit waits in Playwright and wondering why it is still flaky.

So the page object layer survives the migration, but its **state-reading half
has to be rewritten to return `Locator`s** while its action half stays as it is.
That asymmetry is the part that does not show up in a syntax mapping table.
