package xyz.opcal.tools.command.mr;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.SneakyThrows;

@SpringBootTest(args = { "mr", "parent", "opcal-commons-build.version" })
class MergeRequestParentTest {

	static {
		restProperties();
	}

	@SneakyThrows
	static void restProperties() {
		FileUtils.copyFileToDirectory(FileUtils.getFile("./src/test/resources/versionUpdate"), FileUtils.getTempDirectory());
	}

	@Test
	void test() {
		assertTrue(FileUtils.getFile(FileUtils.getTempDirectory(), "versionUpdate").exists());
	}
}
