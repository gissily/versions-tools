package xyz.opcal.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

@SpringBootTest(args = { "version" })
class VersionTest {

	@Test
	void test() throws FileNotFoundException {
		assertTrue(ResourceUtils.getFile("./target/test-classes/versions.yml").exists());
	}
}
