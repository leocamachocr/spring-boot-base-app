package dev.leocamacho.demo.tests.api;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "dev.leocamacho.demo.tests.api.steps",
        plugin = {"pretty", "html:target/cucumber-reports.html"}
)
public class CucumberTestRunner {
}