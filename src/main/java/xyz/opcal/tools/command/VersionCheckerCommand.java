package xyz.opcal.tools.command;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import xyz.opcal.tools.command.subcommands.MergeRequestHandler;
import xyz.opcal.tools.command.subcommands.VersionCheckHandler;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Component
@Command(mixinStandardHelpOptions = true, description = "versions updates check command", versionProvider = CommandVersionProvider.class, subcommands = {
		MergeRequestHandler.class })
public class VersionCheckerCommand {

	static final String UPDATE_FLAG_FILE = "versionUpdate";
	static final String PARENT_FLAG_FILE = "parentUpdate";

	public static File getUpdateFlagFile() {
		return FileUtils.getFile(FileUtils.getTempDirectory(), UPDATE_FLAG_FILE);
	}

	public static File getParentFlagFile() {
		return FileUtils.getFile(FileUtils.getTempDirectory(), PARENT_FLAG_FILE);
	}

	@Spec
	CommandSpec spec;

	private @Autowired VersionCheckHandler versionCheckHandler;

	@SneakyThrows
	@Command(name = "check", description = "version update checking")
	public void check(@Parameters(index = "0", description = "version update checking config file") File config) {
		if (config == null || !config.exists()) {
			throw new ParameterException(spec.commandLine(), "file does not exist");
		}
		versionCheckHandler.check(config);
	}

}
