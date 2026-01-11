package xyz.opcal.tools.command.subcommands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import xyz.opcal.tools.command.VersionCheckerCommand;
import xyz.opcal.tools.model.config.VersionRegisterInfo;
import xyz.opcal.tools.model.reporting.MergeRequestInfo;
import xyz.opcal.tools.model.reporting.ParentReportInfo;
import xyz.opcal.tools.model.reporting.PropertyReportInfo;
import xyz.opcal.tools.service.DependenciesPropertiesService;
import xyz.opcal.tools.service.ReportParserService;
import xyz.opcal.tools.service.VersionConfigService;
import xyz.opcal.tools.service.report.IReportParser;

@Slf4j
@Component
public class VersionCheckHandler {

	static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

	private static Logger commandConsole = LoggerFactory.getLogger("COMMAND_CONSOLE");

	private VersionConfigService versionConfigService;
	private ReportParserService reportParseService;
	private DependenciesPropertiesService dependenciesPropertiesService;

	private final ObjectMapper objectMapper;

	public VersionCheckHandler(VersionConfigService versionConfigService, ReportParserService reportParseService,
			DependenciesPropertiesService dependenciesPropertiesService) {
		this.versionConfigService = versionConfigService;
		this.reportParseService = reportParseService;
		this.dependenciesPropertiesService = dependenciesPropertiesService;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@SneakyThrows
	public void check(File config) {

		deleteFlagFiles();

		var versionConfig = versionConfigService.load(config);
		var dependenciesFile = ResourceUtils.getFile(versionConfig.getDependencies());
		var configBuilder = dependenciesPropertiesService.loadConfigurationBuilder(dependenciesFile);
		var dependencies = configBuilder.getConfiguration();

		List<Triple<String, String, String>> list = new ArrayList<>();
		var versionRegisters = mergeVersioinInfo(versionConfig.getVersionRegisters(), dependencies);

		var dependencyVersions = parseReport(versionConfig.getUpdateReports(), versionRegisters, propertyReportParser());
		list.addAll(dependencyVersions);

		var parentVersions = parseReport(versionConfig.getParentReports(), versionRegisters, parentReportParser());
		list.addAll(parentVersions);
		if (!CollectionUtils.isEmpty(list)) {
			switch (versionConfig.getMode()) {
			case MERGE_REQUEST:
				var mergeRequests = new ArrayList<>();
				mergeRequests.addAll(toMergeRequestInfo(dependencyVersions, false));
				mergeRequests.addAll(toMergeRequestInfo(parentVersions, true));
				objectMapper.writeValue(VersionCheckerCommand.getUpdateFlagFile(), mergeRequests);
				commandConsole.info("version checking result in merge request mode");
				break;
			case PUSH:
			default:
				var updateMessage = list.stream()
						.reduce(new StringBuilder(), 
								(message, triple) -> message.append(updateDependenciesProperties(dependencies, triple)), 
								(_, u) -> u)
						.insert(0, "updating new versions: \n ").toString();
				dependencies.getLayout().setGlobalSeparator("=");
				dependencies.getLayout().setFooterComment(null);
				configBuilder.save();
				FileUtils.write(VersionCheckerCommand.getUpdateFlagFile(), updateMessage, StandardCharsets.UTF_8);
				commandConsole.info(updateMessage);
				break;
			}

			commandConsole.info("update flag file: {}", VersionCheckerCommand.getUpdateFlagFile());
			if (!parentVersions.isEmpty()) {
				FileUtils.touch(VersionCheckerCommand.getParentFlagFile());
				commandConsole.info("parent update flag file: {}", VersionCheckerCommand.getParentFlagFile());
			}
		}
	}

	List<MergeRequestInfo> toMergeRequestInfo(List<Triple<String, String, String>> dependencyVersions, boolean isParent) {
		return dependencyVersions.stream().map(triple -> new MergeRequestInfo().setPropertyName(triple.getLeft()).setCurrentVersion(triple.getMiddle())
				.setNewVersion(triple.getRight()).setParent(isParent)).toList();
	}

	private void deleteFlagFiles() throws IOException {
		if (VersionCheckerCommand.getUpdateFlagFile().exists()) {
			FileUtils.delete(VersionCheckerCommand.getUpdateFlagFile());
		}
		if (VersionCheckerCommand.getParentFlagFile().exists()) {
			FileUtils.delete(VersionCheckerCommand.getParentFlagFile());
		}
	}

	List<VersionRegisterInfo> mergeVersioinInfo(List<VersionRegisterInfo> versionRegisterInfos, PropertiesConfiguration dependencies) {
		if (CollectionUtils.isEmpty(versionRegisterInfos)) {
			return new ArrayList<>();
		}
		versionRegisterInfos.forEach(info -> info.setCurrentVersion(dependencies.getString(info.getPropertyName())));
		return ImmutableList.copyOf(versionRegisterInfos);
	}

	String updateDependenciesProperties(PropertiesConfiguration dependencies, Triple<String, String, String> triple) {
		dependencies.setProperty(triple.getLeft(), triple.getRight());
		return String.format("[%s]\t\t[%s -> %s]%n ", triple.getLeft(), triple.getMiddle(), triple.getRight());
	}

	List<Triple<String, String, String>> parseReport(List<String> files, List<VersionRegisterInfo> versionRegisters, IReportParser parser) {
		if (CollectionUtils.isEmpty(files)) {
			return new ArrayList<>();
		}
		// @formatter:off
		return files.stream()
				.map(this::mapFile)
				.filter(file -> Objects.nonNull(file) && file.exists())
				.flatMap(file -> parseReport(file, versionRegisters, parser).stream())
				.distinct()
				.toList();
		// @formatter:on
	}

	File mapFile(String filePath) {
		try {
			return ResourceUtils.getFile(filePath);
		} catch (FileNotFoundException e) {
			log.error("loading file {} error: {}", filePath, e.getMessage());
		}
		return null;
	}

	List<Triple<String, String, String>> parseReport(File reportFile, List<VersionRegisterInfo> versionRegisters, IReportParser parser) {
		return parser.parse(reportFile, versionRegisters);
	}

	IReportParser propertyReportParser() {
		return (reportFile, versionRegisters) -> {
			var reports = reportParseService.parsePropertyReport(reportFile);
			// @formatter:off
			return versionRegisters.stream().filter(VersionRegisterInfo::getEnabled)
					.filter(registerInfo -> Objects.nonNull(reports.get(registerInfo.getPropertyName())))
					.map(registerInfo -> checkVersion(registerInfo, reports.get(registerInfo.getPropertyName())))
					.filter(triple -> StringUtils.isNotBlank(triple.getRight()))
					.toList();
			// @formatter:on
		};
	}

	IReportParser parentReportParser() {
		return (reportFile, versionRegisters) -> {
			var reports = reportParseService.parseParentReport(reportFile);
			// @formatter:off
			return versionRegisters.stream().filter(VersionRegisterInfo::getEnabled)
					.filter(registerInfo -> StringUtils.isNoneBlank(registerInfo.getGroupId(), registerInfo.getArtifactId()))
					.map(registerInfo -> getParenetReport(registerInfo, reports))
					.filter(Optional::isPresent)
					.map(op -> parentToProperty(op.get()))
					.map(pair -> checkVersion(pair.getLeft(), pair.getRight()))
					.filter(triple -> StringUtils.isNotBlank(triple.getRight()))
					.toList();
			// @formatter:on
		};
	}

	Pair<VersionRegisterInfo, PropertyReportInfo> parentToProperty(Pair<VersionRegisterInfo, ParentReportInfo> parentReports) {
		var registerInfo = parentReports.getLeft();
		var report = parentReports.getRight();
		PropertyReportInfo propertyReportInfo = new PropertyReportInfo();
		propertyReportInfo.setPropertyName(registerInfo.getPropertyName());
		propertyReportInfo.setCurrentVersion(report.getCurrentVersion());
		propertyReportInfo.setLatestSubincremental(report.getLatestSubincremental());
		propertyReportInfo.setLatestIncremental(report.getLatestIncremental());
		propertyReportInfo.setLatestMinor(report.getLatestMinor());
		propertyReportInfo.setLatestMajor(report.getLatestMajor());
		return Pair.of(registerInfo, propertyReportInfo);
	}

	Optional<Pair<VersionRegisterInfo, ParentReportInfo>> getParenetReport(VersionRegisterInfo versionRegisterInfo, List<ParentReportInfo> parentReports) {
		// @formatter:off
		return parentReports.stream()
				.filter(report -> isSameArtifact(report, versionRegisterInfo))
				.map(report -> Pair.of(versionRegisterInfo, report))
				.findFirst();
		// @formatter:on
	}

	boolean isSameArtifact(ParentReportInfo report, VersionRegisterInfo versionRegisterInfo) {
		return Strings.CS.equals(report.getGroupId(), versionRegisterInfo.getGroupId())
				&& Strings.CS.equals(report.getArtifactId(), versionRegisterInfo.getArtifactId());
	}

	private Triple<String, String, String> checkVersion(VersionRegisterInfo registerInfo, PropertyReportInfo reportPropertyInfo) {
		var newVersion = switch (registerInfo.getUpdatePolicy()) {
		case MAJOR -> reportPropertyInfo.getLatestMajor();
		case MINOR -> reportPropertyInfo.getLatestMinor();
		case INCREMENTAL -> getIncrementalVersion(reportPropertyInfo);
		case LATEST -> latestVersion(reportPropertyInfo);
		case SNAPSHOT -> reportPropertyInfo.getLatestSubincremental();
		default -> StringUtils.EMPTY;
		};
		if (Strings.CS.equals(newVersion, registerInfo.getCurrentVersion())) {
			newVersion = StringUtils.EMPTY;
		}
		return Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), allowSnapshotVersion(registerInfo, newVersion));
	}

	private String allowSnapshotVersion(VersionRegisterInfo registerInfo, String newVersion) {
		if (Boolean.FALSE.equals(registerInfo.getAllowSnapshot()) && Strings.CI.equals(newVersion, SNAPSHOT_SUFFIX)) {
			return StringUtils.EMPTY;
		}
		return newVersion;
	}

	String getIncrementalVersion(PropertyReportInfo reportPropertyInfo) {
		if (StringUtils.isBlank(reportPropertyInfo.getLatestIncremental())) {
			return reportPropertyInfo.getLatestSubincremental();
		}
		return reportPropertyInfo.getLatestIncremental();
	}

	private String latestVersion(PropertyReportInfo reportPropertyInfo) {
		if (StringUtils.isNotBlank(reportPropertyInfo.getLatestMajor())) {
			return reportPropertyInfo.getLatestMajor();
		}
		if (StringUtils.isNotBlank(reportPropertyInfo.getLatestMinor())) {
			return reportPropertyInfo.getLatestMinor();
		}
		if (StringUtils.isNotBlank(reportPropertyInfo.getLatestIncremental())) {
			return reportPropertyInfo.getLatestIncremental();
		}
		if (StringUtils.isNotBlank(reportPropertyInfo.getLatestSubincremental())) {
			return reportPropertyInfo.getLatestSubincremental();
		}
		return StringUtils.EMPTY;
	}
}
