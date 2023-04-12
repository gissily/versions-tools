package xyz.opcal.tools;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.util.ResourceUtils;

import xyz.opcal.tools.hints.AppRuntimeHints;
import xyz.opcal.tools.service.VersionCheckerService;

@ImportRuntimeHints({ AppRuntimeHints.class })
@SpringBootApplication
public class VersionsApplication implements CommandLineRunner {

	static final String[] VERSION_COMMANDS = new String[] { "version", "-v", "-V", "--version" };

	public static void main(String[] args) {
		SpringApplication.run(VersionsApplication.class, args);
	}

	@Value("${info.app.version}")
	private String version;

	private @Autowired VersionCheckerService versionCheckerService;

	@Override
	public void run(String... args) throws Exception {
		if (ArrayUtils.isEmpty(args)) {
			return;
		}
		if (checkVersion(args[0])) {
			version();
			return;
		}
		var configFile = ResourceUtils.getFile(args[0]);
		if (!configFile.exists()) {
			System.out.println(String.format("%s does not exist", args[0]));
			return;
		}
		versionCheckerService.check(configFile);
	}

	void version() {
		System.out.println(version);
	}

	boolean checkVersion(String command) {
		return ArrayUtils.contains(VERSION_COMMANDS, command);
	}

}
