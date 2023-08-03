package xyz.opcal.tools.command.check;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import xyz.opcal.tools.VersionsApplication;

@TestInstance(Lifecycle.PER_CLASS)
class MergetRquestModeTest {

	@BeforeEach
	void restProperties() throws IOException {
		FileUtils.copyFileToDirectory(FileUtils.getFile("./src/test/resources/dependencies.properties"), FileUtils.getFile("./target/test-classes/"));
	}

	@Test
	void test() throws FileNotFoundException {
		assertDoesNotThrow(() -> VersionsApplication.main(new String[] { "check", "./target/test-classes/versions-merge-request.yml" }));
	}
}
