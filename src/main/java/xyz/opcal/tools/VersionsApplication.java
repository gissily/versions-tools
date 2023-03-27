package xyz.opcal.tools;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

	public static void main(String[] args) {
		SpringApplication.run(VersionsApplication.class, args);
	}

	private @Autowired VersionCheckerService versionCheckerService;

	@Override
	public void run(String... args) throws Exception {
		if (ArrayUtils.isEmpty(args)) {
			return;
		}
		var configFile = ResourceUtils.getFile(args[0]);
		if (!configFile.exists()) {
			System.out.println(String.format("%s does not exist", args[0]));
			return;
		}
		versionCheckerService.check(configFile);
	}

}
