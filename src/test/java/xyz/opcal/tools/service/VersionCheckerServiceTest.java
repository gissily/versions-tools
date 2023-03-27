package xyz.opcal.tools.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

@SpringBootTest(properties = { "logging.level.ROOT=info" })
class VersionCheckerServiceTest {

	@Autowired
	VersionCheckerService versionCheckerService;

	@Test
	void test() throws FileNotFoundException {
		var configFile = ResourceUtils.getFile("./target/test-classes/versions.yml");
		assertDoesNotThrow(() -> versionCheckerService.check(configFile));
	}

}
