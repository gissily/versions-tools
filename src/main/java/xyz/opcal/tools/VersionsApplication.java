package xyz.opcal.tools;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import xyz.opcal.tools.command.VersionCheckerCommand;
import xyz.opcal.tools.hints.AppRuntimeHints;

@ImportRuntimeHints({ AppRuntimeHints.class })
@SpringBootApplication
public class VersionsApplication implements CommandLineRunner, ExitCodeGenerator {

	public static void main(String[] args) {
		SpringApplication.run(VersionsApplication.class, args);
	}

	private int exitCode;
	private IFactory factory;
	private VersionCheckerCommand versionCheckerCommand;

	public VersionsApplication(IFactory factory, VersionCheckerCommand versionCheckerCommand) {
		this.factory = factory;
		this.versionCheckerCommand = versionCheckerCommand;
	}

	@Override
	public void run(String... args) throws Exception {
		exitCode = new CommandLine(versionCheckerCommand, factory).execute(args);
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
