package com.example.TestAuthorization;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


import org.junit.*;
import static org.junit.Assert.*;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class GameTestClass {
  private static String HOST = "http://127.0.0.1";
  //path for online server
  //private static String HOST = "http://ruswizard.ddns.net";
  private static String SESSION = "IAMSESSION";
  private static String PATH_TO_CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
  private static String PATH_TO_CHROMEDRIVER = "/Users/karinamavletova/Downloads/chromedriver-mac-arm64/chromedriver";
  private WebDriver driver;
  private String baseUrl;
  private boolean acceptNextAlert = true;
  private StringBuffer verificationErrors = new StringBuffer();
  JavascriptExecutor js;
  Duration timeOut;
  WebDriverWait wait;

  @Before
  public void setUp() throws Exception {
    ChromeDriverService cds = ChromeDriverService.createDefaultService();
    cds.setExecutable(PATH_TO_CHROMEDRIVER);
    ChromeOptions cdo = new ChromeOptions();
    cdo.setBinary(PATH_TO_CHROME);
    cdo.addArguments("--no-sandbox");

    driver = new ChromeDriver(cds, cdo);

    baseUrl = "https://www.google.com/";
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(60));
    js = (JavascriptExecutor) driver;

    timeOut = Duration.ofSeconds(10);
    wait = new WebDriverWait(driver, timeOut);
  }


    /*
    Данный метод тестирует следующий сценарий:
    Если пользователь не осуществлял действий на протяжении 5 минут то
    сессия автоматически прекращается (Logout).
     */
    @Test
    public void testAuthorization () throws Exception {
      Thread.sleep(300000);
      //Thread.sleep(3000);

      // Эмулируем, что прошло 5 минут, изменив время
     // String script = "Date.prototype.getTime = function() { return new Date().getTime() + 300000; };";
      // Выполняем скрипт
      //js.executeScript(script);

      // Ждём 10 секунд появления элемента
      WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-btn")));
      assertNotNull(element);
    }



    /*
    Данный метод проверяет, может ли корабль выходить за указанные границы [-20, 20].
    Проверка производится именно при движении влево.
     */
    @Test
    public void testBorder(){
      driver.get("http://ruswizard.ddns.net:8091");
      driver.findElement(By.id("newsess-btn")).click();
      WebElement sessionIdInput = driver.findElement(By.id("sessionId"));
      String sessionId = sessionIdInput.getAttribute("value");
      driver.get("http://ruswizard.ddns.net:8091/?session=" + sessionId);
      driver.findElement(By.id("login-btn")).click();


      // Производим 100 кликов влево.
      WebElement locationElement;
      for (int i = 0; i < 100; i++) {

        wait.until(ExpectedConditions.elementToBeClickable(By.id("arrowLeft"))).click();

      }
      try {
          Thread.sleep(3000);
      } catch (InterruptedException e) {
          throw new RuntimeException(e);
      }

      // Находим текущую координату.
      locationElement= driver.findElement(By.id("place"));
      String locationText = locationElement.getText();
      int coordinates = getCoordinates(locationText);
      // Проверяем, вышли ли за границу.
      boolean result = coordinates >=-20;
      assertTrue(result);
    }


    /*
      Данный метод проверяет, отображается ли имя моего корабля первым в случае нескольких
      кораблей на 1 клетке.
     */
    @Test
    public void testName(){
      driver.get("http://ruswizard.ddns.net:8091");
      driver.findElement(By.id("newsess-btn")).click();
      WebElement sessionIdInput = driver.findElement(By.id("sessionId"));
      String sessionId = sessionIdInput.getAttribute("value");
      driver.get("http://ruswizard.ddns.net:8091/?session=" + sessionId);
      driver.findElement(By.id("login-btn")).click();


      String direction = "left";
      // Двигаемся циклом по допустимой области [-20, 20]
      while(true){
        if(direction.equals("left")){
          wait.until(ExpectedConditions.elementToBeClickable(By.id("arrowLeft"))).click();
        }
        else{
          wait.until(ExpectedConditions.elementToBeClickable(By.id("arrowRight"))).click();
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        // На каждом шаге извлекаем верхнее имя.
        // И проверяем, содержит ли это имя символы [].
        WebElement divModel =  driver.findElement(By.cssSelector(".sea.navbar-nav.flex-row.mb-2"));
        List<WebElement> seaObjects = divModel.findElements(By.cssSelector("span.mapItem"));
        WebElement myShipPlace = seaObjects.get(6);
        WebElement mapOverImage = myShipPlace.findElement(By.cssSelector("span.mapOverImage"));
        String mainName = mapOverImage.findElement(By.cssSelector("span.namePanel")).getText();
        System.out.println(mainName);
        assertTrue(mainName.contains("[") && mainName.contains("]"));

        // Если вдруг при движении влево не наткнулись на чужой корабль, идём назад проверять правую границу.
        if(getCoordinates(driver.findElement(By.id("place")).getText()) == -19){
          direction = "right";
        }

      }
    }


    /*
    В требованиях 3 раздела сказано, что при покупке товаров у порта, цена покупки растёт.
    Также стоимость товара со временем (не реже чем раз в минуту) восстанавливается к исходным значениям.
    Здесь проверяется сценарий возвращения цены к исходному состоянию.
     */
    @Test
    public void returnOfPrices(){
      driver.get("http://ruswizard.ddns.net:8091");
      driver.findElement(By.id("newsess-btn")).click();
      WebElement sessionIdInput = driver.findElement(By.id("sessionId"));
      String sessionId = sessionIdInput.getAttribute("value");
      driver.get("http://ruswizard.ddns.net:8091/?session=" + sessionId);
      driver.findElement(By.id("login-btn")).click();

      WebElement element = driver.findElement(By.id("act-0-0"));

      // Прокрутка к элементу "Зайти в порт"
      js.executeScript("arguments[0].scrollIntoView(true);", element);

      try {
            Thread.sleep(1000);
      } catch (InterruptedException e) {
            throw new RuntimeException(e);
      }
      wait.until(ExpectedConditions.elementToBeClickable(By.id("act-0-0"))).click();
      // Совершаем две покупки.
      driver.findElement(By.id("item1008buy")).click();
      driver.findElement(By.id("item1008buy")).click();
      // Запоминаем состояние цены после 2 покупок.
      float firstPrice = getPriceToBuy();

      // Ждём ровно минуту, чтобы проверить начала ли цена падать
      try {
        Thread.sleep(60000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      // Смотрим, какая цена стала спутся минуту
      float secondPrice = getPriceToBuy();
      // Проверяем, стала ли она опускаться
      assertTrue(secondPrice < firstPrice);

    }

    /*
    В требованиях 3 раздела сказано следующее:
    "Продажа товара порту приводит к снижению стоимости покупки и повышению стоимости продажи."
    Проверим данное требование тестом ниже
     */
    @Test
    public void testPrices(){
      driver.get("http://ruswizard.ddns.net:8091");
      driver.findElement(By.id("newsess-btn")).click();
      WebElement sessionIdInput = driver.findElement(By.id("sessionId"));
      String sessionId = sessionIdInput.getAttribute("value");
      driver.get("http://ruswizard.ddns.net:8091/?session=" + sessionId);
      driver.findElement(By.id("login-btn")).click();

      WebElement element = driver.findElement(By.id("act-0-0"));
      js.executeScript("arguments[0].scrollIntoView(true);", element);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      wait.until(ExpectedConditions.elementToBeClickable(By.id("act-0-0"))).click();

      // Запоминаем цену до продажи.
      ArrayList<Float> firstPrices = getTwoPrices();
      // Продаём товар
      driver.findElement(By.id("item1008sell")).click();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      // Смотрим, как изменилась цена после продажи.
      ArrayList<Float> secondPrices = getTwoPrices();
      assertTrue((secondPrices.get(0) < firstPrices.get(0)) && secondPrices.get(1) > firstPrices.get(1));

    }

    /*
    Метод предназначен для извлечения координат
     */
    private int getCoordinates(String text){
      String coordinateStr = "";
      boolean flag = false;
      for (int i = 0; i < text.length(); i++) {
        if(text.charAt(i) == ']'){
          break;
        }
        if(text.charAt(i) == '['){
          flag = true;
          continue;
        }
        if(flag){
          coordinateStr+=text.charAt(i);
        }
      }
      return Integer.parseInt(coordinateStr);
    }

  /*
    Метод предназначен для получения цены покупки товара из порта
  */
    private float getPriceToBuy(){
      WebElement tradeTable = driver.findElement(By.id("tradeTable"));
      WebElement firstRowGood = tradeTable.findElement(By.cssSelector("tr"));
      List<WebElement> cells = firstRowGood.findElements(By.cssSelector("td"));
      return Float.parseFloat(cells.get(3).getText().split("/")[0]);

    }


    /*
      Метод предназначен для получения двух цен: покупки и продажи
    */
    private ArrayList<Float> getTwoPrices(){
      WebElement tradeTable = driver.findElement(By.id("tradeTable"));
      WebElement firstRowGood = tradeTable.findElement(By.cssSelector("tr"));
      List<WebElement> cells = firstRowGood.findElements(By.cssSelector("td"));
      ArrayList<Float> prices = new ArrayList<>();
      prices.add(Float.parseFloat(cells.get(3).getText().split("/")[0]));
      prices.add(Float.parseFloat(cells.get(3).getText().split("/")[1]));
      return prices;
    }

    @After
    public void tearDown () throws Exception {
      driver.quit();
      String verificationErrorString = verificationErrors.toString();
      if (!"".equals(verificationErrorString)) {
        fail(verificationErrorString);
      }
    }

    private boolean isElementPresent (By by){
      try {
        driver.findElement(by);
        return true;
      } catch (NoSuchElementException e) {
        return false;
      }
    }

    private boolean isAlertPresent () {
      try {
        driver.switchTo().alert();
        return true;
      } catch (NoAlertPresentException e) {
        return false;
      }
    }

    private String closeAlertAndGetItsText () {
      try {
        Alert alert = driver.switchTo().alert();
        String alertText = alert.getText();
        if (acceptNextAlert) {
          alert.accept();
        } else {
          alert.dismiss();
        }
        return alertText;
      } finally {
        acceptNextAlert = true;
      }
    }
}


