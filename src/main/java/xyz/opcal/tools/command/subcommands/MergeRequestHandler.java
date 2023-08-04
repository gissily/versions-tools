package xyz.opcal.tools.command.subcommands;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import xyz.opcal.tools.command.VersionCheckerCommand;
import xyz.opcal.tools.model.reporting.MergeRequestInfo;

@Command(name = "mr", description = "merge request info")
@Component
public class MergeRequestHandler {

	private final ObjectMapper objectMapper;

	public MergeRequestHandler() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	MergeRequestInfo[] loadInfos() throws IOException {
		return objectMapper.readValue(VersionCheckerCommand.getUpdateFlagFile(), MergeRequestInfo[].class);
	}

	@Command(name = "keys", description = "list all merget request info propertyName")
	public void keys(@Option(names = "--line", defaultValue = "false") boolean line) throws IOException {
		var lineSeparator = line ? "\n" : " ";
		StringBuilder stringBuilder = new StringBuilder();
		Arrays.stream(loadInfos()).map(MergeRequestInfo::getPropertyName).forEach(property -> stringBuilder.append(property).append(lineSeparator));
		System.out.println(stringBuilder.toString());
	}

	@Command(name = "current", description = "get current version by property name")
	public void currentVersion(@Parameters(index = "0", description = "merget request info property name") String propertyName) throws IOException {
		mergeRequestInfoHandle(propertyName, mergeRequestInfo -> System.out.println(mergeRequestInfo.getCurrentVersion()));
	}

	@Command(name = "new", description = "get new version by property name")
	public void newVersion(@Parameters(index = "0", description = "merget request info property name") String propertyName) throws IOException {
		mergeRequestInfoHandle(propertyName, mergeRequestInfo -> System.out.println(mergeRequestInfo.getNewVersion()));
	}

	@Command(name = "parent", description = "get parent state by property name")
	public void parentState(@Parameters(index = "0", description = "merget request info property name") String propertyName) throws IOException {
		mergeRequestInfoHandle(propertyName, mergeRequestInfo -> System.out.println(mergeRequestInfo.isParent()));
	}

	void mergeRequestInfoHandle(String propertyName, Consumer<MergeRequestInfo> action) throws IOException {
		// @formatter:off
		Arrays.stream(loadInfos()) 
				.filter(mergeRequestInfo -> StringUtils.equals(mergeRequestInfo.getPropertyName(), propertyName)) 
				.findFirst() 
				.ifPresent(action);
		// @formatter:on
	}

}
