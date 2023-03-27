package xyz.opcal.tools.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(properties = { "logging.level.ROOT=info" })
class VersionConfigServiceTest {

	@Autowired
	VersionConfigService versionConfigService;

	@Test
	void test() throws FileNotFoundException {
		var ymlFile = ResourceUtils.getFile("./target/test-classes/versions.yml");
		var versionConfig = versionConfigService.load(ymlFile);
		assertNotNull(versionConfig);
		log.info("info:\n{}", versionConfig);
	}

}
