import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import other.API;
import other.DateMatcher;
import other.MethodsForTests;
import pages.HomePage;
import pages.ProjectBuildPage;
import pages.ProjectPage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;


/**
 * Created by tetiana.sviatska on 7/14/2015.
 */
public class ProjectTests extends BaseTests {

    private static final API api = API.getAPIUsingDefaultCredentials();
    private static final Random random = new Random();

    @ClassRule
    public static TestRule setUpDriverAndCleanUpUsersRule = (base, d) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            driver = MethodsForTests.getDriver();
            try {
                base.evaluate();
            } finally {
                // driver.quit();
            }
        }
    };

    @Nullable
    private String getRandomExistingProjectName() {
        JSONArray jobs = api.request(HomePage.HOME_PAGE_URL).optJSONArray("jobs");
        return jobs != null ? jobs.getJSONObject(random.nextInt(jobs.length())).getString("name") : null;
    }

    private int getRandomBuildNumberFor(@NotNull String projectName) {
        JSONArray builds = api.request(String.format(ProjectPage.PROJECT_PAGE_URL, MethodsForTests.encode(projectName))).optJSONArray("builds");
        return builds != null ? builds.getJSONObject(random.nextInt(builds.length())).getInt("number") : 0;
    }

    private Instant getBuildTime(@NotNull String projectName, int buildNumber) {
        return Instant.ofEpochMilli(api.request(String.format(ProjectBuildPage.PROJECT_BUILD_PAGE_URL, MethodsForTests.encode(projectName), buildNumber)).optLong("timestamp"));
    }

    @Test
    public void compareWithJsonDate() {
        String selectedProject = getRandomExistingProjectName();
        Assume.assumeNotNull(selectedProject);
        int buildNumber = getRandomBuildNumberFor(selectedProject);
        LocalDateTime expected = LocalDateTime.ofInstant(getBuildTime(selectedProject, buildNumber), ZoneId.systemDefault());

        ProjectBuildPage page = new ProjectBuildPage(driver, selectedProject, buildNumber).get();

        LocalDateTime actual = LocalDateTime.from(
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").parse(page.getBuildTime()));

        Assert.assertThat(actual, DateMatcher.equals(expected, ChronoUnit.SECONDS));
    }
}
