package com.regularization;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class RegularizationService {
    private static List<String> reasons = Arrays.asList("I had some personal work.", "Left early.",
            "Came late.", "I got stuck in traffic.");
    private static com.regularization.Config config = new com.regularization.Config();
    private static WebDriver driver = null;
    private static int MIN_WORKING_HOURS = 0;
    private static int MAX_WORKING_HOURS = 9;
    private static int CHECK_FOR_LAST_MONTH = 5;
    private static Boolean IS_FORGOT_SWIPE_OUT_REGULARIZATION_ALLOWED = false;

    public static void main(String... args) {
        if (args.length == 2) {
            String userId = args[0];
            String password = args[1];
            regularize(userId, password);
        }
    }

    public static void regularize(String userId, String password) {
        try {
            Boolean configLoaded = config.init();
            if (configLoaded) {
                System.out.println("-----------------------");
                System.out.println("Start: " + new Date());
                init();
                openGreytHRLoginPage();
                login(userId, password);
                openAttendanceInfoPage();
                setDates();
                ArrayList<Date> regularizeDates = getRegularizeDates();
                System.out.println("regularizeDates: " + regularizeDates);
                applyRegularization(regularizeDates);
                driver.close();
                System.out.println("End: " + new Date());
                System.out.println("-----------------------");
            }
        } catch (Exception exception) {
            System.out.println("Exception: " + exception.getMessage());
            System.out.println("Stacktrace: " + exception.getStackTrace().toString());
            exception.printStackTrace();
        }
    }

    public static void init() {
        System.setProperty("webdriver.chrome.driver", config.get("chromeDriverPath"));
        ChromeOptions chromeOptions = new ChromeOptions();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        driver = new ChromeDriver(chromeOptions);
    }

    public static void openGreytHRLoginPage() {
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        driver.get(config.get("pageUrl"));
        //  driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        //  driver.manage().window().maximize();
    }

    public static void login(String userId, String password) {
        driver.findElement(By.id(config.get("loginUserNameHtmlId"))).sendKeys(userId);
        driver.findElement(By.id(config.get("loginUserPasswordHtmlId"))).sendKeys(password);
        driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
        driver.findElement(By.id(config.get("loginButtonHtmlId"))).click();
        WebDriverWait myWaitVar = new WebDriverWait(driver, 10);
        String homePageLastElementLoad = config.get("homePageLastElementLoad");
        myWaitVar.until(ExpectedConditions.invisibilityOfElementLocated(By.id(homePageLastElementLoad)));
    }

    public static void openAttendanceInfoPage() {
        driver.get(config.get("attendanceInfoPageUrl"));
    }

    public static void setDates() {
        Date currentDate = new Date();
        int currentDay = getNormalizedDateFromEpochMills(currentDate.getTime()).get("day");
        WebDriverWait myWaitVar = new WebDriverWait(driver, 10);
        WebElement tableCalendar = driver.findElement(By.xpath("//*[@id=\"mainDiv\"]/div[1]/div/div[2]/div/button[2]"));
        tableCalendar.click();
        myWaitVar.until(ExpectedConditions.elementToBeClickable(By.name("fromdate")));
        if (currentDay > 0 && currentDay <= CHECK_FOR_LAST_MONTH) {
            WebElement selectDate = driver.findElement(By.name("fromdate"));
            selectDate.click();
            myWaitVar.until(ExpectedConditions.elementToBeClickable(By.xpath(config.get("fromDateDiv"))));
            WebElement prevButton = driver.findElement(By.xpath(config.get("prevButtonXPath")));
            prevButton.click();
            Boolean isValueFound = false;
            int lastMonthDay = 1;
            for (int j = 1; j <= 5; j++) {
                for (int k = 1; k <= 7; k++) {
                    WebElement dayInWeb = driver.findElement(By.xpath( config.get("fromDateRowXpath") + j + "]/td[" + k + "]"));
                    if (dayInWeb.getText().trim() != null && !dayInWeb.getText().trim().equals("")) {
                        WebElement dayEmelement = driver.findElement(By.xpath(config.get("fromDateRowXpath") + j + "]/td[" + k + "]/a"));
                        if (dayEmelement.getText().trim().equals(Integer.toString(lastMonthDay))) {
                            dayEmelement.click();
                            isValueFound = true;
                        }
                    }
                    if (isValueFound) {
                        break;
                    }
                }
                if (isValueFound) {
                    break;
                }
            }

            WebElement showButton = driver.findElement(By.xpath(config.get("showButtonXPath")));
            showButton.click();
            myWaitVar.until(ExpectedConditions.elementToBeClickable(By.xpath(config.get("columnsWaitXpath") + (58 - lastMonthDay) + "]/td[2]")));
        }
    }

    public static ArrayList<Date> getRegularizeDates() {
        ArrayList<Date> regularizeDates = new ArrayList<Date>();
        try {
            List rows = driver.findElements(By.xpath(config.get("attendanceDateRow")));
            for (int i = 0; i < rows.size() - 1; i++) {
                String firstInDate = driver.findElement(By.xpath(config.get("attendanceTableInfoRow") + (i + 1) + "]/td[2]")).getText().trim();
                String lastOutDate = driver.findElement(By.xpath(config.get("attendanceTableInfoRow") + (i + 1) + "]/td[3]")).getText().trim();
                String dateString = driver.findElement( By.xpath(config.get("attendanceTableInfoRow") + (i + 1) + "]/td[1]")).getText().trim();
                String numberOfHours = driver.findElement(By.xpath(config.get("attendanceTableInfoRow")  + (i + 1) + "]/td[5]")).getText().trim();
                int numberOfHoursWork = Integer.parseInt(numberOfHours.split(":")[0].trim());
                int numberOfMinuteWorks = Integer.parseInt(numberOfHours.split(":")[1].trim());
                SimpleDateFormat formatter2 = new SimpleDateFormat("dd MMM yyyy");
                Date date = formatter2.parse(dateString);
                if (firstInDate != null && firstInDate.length() != 0 && lastOutDate != null && lastOutDate.length() != 0) {
                    if (firstInDate != lastOutDate) {
                        if (numberOfHoursWork > MIN_WORKING_HOURS && numberOfHoursWork < MAX_WORKING_HOURS) {
                            regularizeDates.add(date);
                        } else if (numberOfHoursWork == MIN_WORKING_HOURS && numberOfMinuteWorks >= 0) {
                            regularizeDates.add(date);
                        }
                    } else if (IS_FORGOT_SWIPE_OUT_REGULARIZATION_ALLOWED) {
                        regularizeDates.add(date);
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return regularizeDates;
    }

    public static void applyRegularization(ArrayList<Date> regularizeDates) {
        if (regularizeDates.size() > 0) {
            WebDriverWait myWaitVar = new WebDriverWait(driver, 10);
            driver.get(config.get("regularizationPage"));
            myWaitVar.until(ExpectedConditions.elementToBeClickable(By.name("dateField")));
            List initialRegularizationDetailsRow = driver.findElements(By.xpath(config.get("regularizationTable")));
            for (int i = 0; i < initialRegularizationDetailsRow.size() - 1; i++) {
                WebElement deleteIcon = driver.findElement(By.xpath(config.get("regularizationTableRow") + (i + 1) + "]/td[6]/i"));
                deleteIcon.click();
            }
            Date currentDate = new Date();
            Map<String, Integer> currentDateMap = getNormalizedDateFromEpochMills(currentDate.getTime());
            int currentDay = currentDateMap.get("day");
            int currentMonth = currentDateMap.get("month");
            myWaitVar.until(ExpectedConditions.visibilityOf(driver.findElement(By.name("dateField"))));
            WebElement selectDate = driver.findElement(By.name("dateField"));
            selectDate.click();
            for (int i = 0; i < regularizeDates.size(); i++) {
                Date regularizeDate = regularizeDates.get(i);
                Map<String, Integer> regularizeDateMap = getNormalizedDateFromEpochMills(regularizeDate.getTime());
                int regularizeDay = regularizeDateMap.get("day");
                int regularizeMonth = regularizeDateMap.get("month");
                boolean isValueFound = false;
                for (int j = 1; j <= 5; j++) {
                    for (int k = 1; k <= 7; k++) {
                        WebElement dayInWeb = driver.findElement(By.xpath(config.get("regularizationDatePickerRow") + j + "]/td[" + k + "]"));
                        if (dayInWeb.getText().trim() != null && !dayInWeb.getText().trim().equals("")) {
                            WebElement dayEmelement = driver.findElement(By.xpath( config.get("regularizationDatePickerRow")  + j + "]/td[" + k + "]/a"));
                            String dayInTheCalendar = dayEmelement.getText().trim();
                            if (dayEmelement.getText().trim().equals(Integer.toString(regularizeDay))) {
                                dayEmelement.click();
                                isValueFound = true;
                            }
                        }
                        if (isValueFound) {
                            break;
                        }
                    }
                    if (isValueFound) {
                        break;
                    }
                }
            }

            myWaitVar.until(ExpectedConditions.elementToBeClickable(By.xpath(" //*[@id=\"reason\"]")));
            List regularizationDetailsRow = driver.findElements(By.xpath("//*[@id=\"gts-employee-apply-attendanceRegularization\"]/table/tbody/tr"));
            for (int i = 0; i < regularizationDetailsRow.size() - 1; i++) {
                Random rand = new Random();
                int reasonIndex = rand.nextInt(reasons.size());
                WebElement reason = driver.findElement(By.xpath("//*[@id=\"gts-employee-apply-attendanceRegularization\"]/table/tbody/tr[" + (i + 1) + "]/td[5]"));
                reason.findElement(By.id("reason")).sendKeys(reasons.get(reasonIndex));
            }
            driver.findElement(By.xpath(config.get("submitButton"))).click();
        }
    }

    static HashMap<String, Integer> getNormalizedDateFromEpochMills(Long epochMills) {
        HashMap<String, Integer> normalizedDate = null;
        if (epochMills != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            String dateString = dateFormat.format(new Date(epochMills));
            normalizedDate = getNormalizedDate(dateString, epochMills / 1000);
        }
        return normalizedDate;
    }

    public static HashMap getNormalizedDate(String dateString, Long epochSeconds) {
        HashMap<String, Integer> normalizedDate = new HashMap();
        String[] splitArray = dateString.split("/");
        if (splitArray.length == 1)
            splitArray = dateString.substring(0, 10).split("-");
        try {
            normalizedDate.put("year", Integer.parseInt(splitArray[0]));
            normalizedDate.put("month", Integer.parseInt(splitArray[1]));
            normalizedDate.put("day", Integer.parseInt(splitArray[2]));
        } catch (Exception e) {
            e.printStackTrace();
            normalizedDate = null;
        }
        return normalizedDate;
    }
}