package xyz.opcal.tools.command.mr;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import xyz.opcal.tools.VersionsApplication;

@TestInstance(Lifecycle.PER_CLASS)
class MergeRequestCurrentTest {

	@BeforeEach
	void restProperties() throws IOException {
		FileUtils.copyFileToDirectory(FileUtils.getFile("./src/test/resources/versionUpdate"), FileUtils.getTempDirectory());
	}

	@Test
	void test() {
		assertDoesNotThrow(() -> VersionsApplication.main(new String[] { "mr", "current", "spring-boot.version" }));
	}
}
