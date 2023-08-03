package xyz.opcal.tools.command.check;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import lombok.SneakyThrows;

@SpringBootTest(args = { "check", "./target/test-classes/versions-merge-request.yml" })
class MergetRquestModeTest {

	static {
		restProperties();
	}

	@SneakyThrows
	static void restProperties() {
		FileUtils.copyFileToDirectory(FileUtils.getFile("./src/test/resources/dependencies.properties"), FileUtils.getFile("./target/test-classes/"));
	}

	@Test
	void test() throws FileNotFoundException {
		assertTrue(ResourceUtils.getFile("./target/test-classes/versions-merge-request.yml").exists());
	}
}
