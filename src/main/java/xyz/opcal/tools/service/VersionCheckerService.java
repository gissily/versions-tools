package xyz.opcal.tools.service;

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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

import com.google.common.collect.ImmutableList;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import xyz.opcal.tools.model.config.VersionRegisterInfo;
import xyz.opcal.tools.model.reporting.ParentReportInfo;
import xyz.opcal.tools.model.reporting.PropertyReportInfo;
import xyz.opcal.tools.service.report.IReportParser;

@Slf4j
@Service
public class VersionCheckerService {

	static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
	static final String UPDATE_FLAG_FILE = "versionUpdate";
	static final String PARENT_FLAG_FILE = "parentUpdate";

	private @Autowired VersionConfigService versionConfigService;
	private @Autowired ReportParserService reportParseService;
	private @Autowired DependenciesPropertiesService dependenciesPropertiesService;

	@SneakyThrows
	public void check(File config) {
		deleteFlagFiles();

		var versionConfig = versionConfigService.load(config);
		var dependenciesFile = ResourceUtils.getFile(versionConfig.getDependencies());
		var builder = dependenciesPropertiesService.loadConfigurationBuilder(dependenciesFile);
		var dependencies = builder.getConfiguration();

		List<Triple<String, String, String>> list = new ArrayList<>();
		var versionRegisters = mergeVersioinInfo(versionConfig.getVersionRegisters(), dependencies);

		list.addAll(parseReport(versionConfig.getUpdateReports(), versionRegisters, propertyReportParser()));

		var parentVersions = parseReport(versionConfig.getParentReports(), versionRegisters, parentReportParser());
		list.addAll(parentVersions);

		StringBuilder updateMessage = new StringBuilder();
		updateMessage.append("updating new versions: \n ");

		list.forEach(triple -> updateMessage.append(updateDependenciesProperties(dependencies, triple)));
		if (!CollectionUtils.isEmpty(list)) {
			dependencies.getLayout().setGlobalSeparator("=");
			dependencies.getLayout().setFooterComment(null);
			dependenciesPropertiesService.updatePropertiesFile(builder);
			FileUtils.write(getUpdateFlagFile(), updateMessage, StandardCharsets.UTF_8);
			System.out.println(updateMessage.toString());
			System.out.println("update flag file: " + getUpdateFlagFile());
			if (!parentVersions.isEmpty()) {
				FileUtils.touch(getParentFlagFile());
				System.out.println("parent update flag file: " + getParentFlagFile());
			}
		}
	}

	private void deleteFlagFiles() throws IOException {
		if (getUpdateFlagFile().exists()) {
			FileUtils.delete(getUpdateFlagFile());
		}
		if (getParentFlagFile().exists()) {
			FileUtils.delete(getParentFlagFile());
		}
	}

	List<VersionRegisterInfo> mergeVersioinInfo(List<VersionRegisterInfo> versionRegisterInfos, PropertiesConfiguration dependencies) {
		if (CollectionUtils.isEmpty(versionRegisterInfos)) {
			return new ArrayList<>();
		}
		versionRegisterInfos.forEach(info -> info.setCurrentVersion(dependencies.getString(info.getPropertyName())));
		return ImmutableList.copyOf(versionRegisterInfos);
	}

	File getUpdateFlagFile() {
		return FileUtils.getFile(FileUtils.getTempDirectory(), UPDATE_FLAG_FILE);
	}

	File getParentFlagFile() {
		return FileUtils.getFile(FileUtils.getTempDirectory(), PARENT_FLAG_FILE);
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
			return versionRegisters.stream().filter(VersionRegisterInfo::getEnable)
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
			return versionRegisters.stream().filter(VersionRegisterInfo::getEnable)
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
		return StringUtils.equals(report.getGroupId(), versionRegisterInfo.getGroupId())
				&& StringUtils.equals(report.getArtifactId(), versionRegisterInfo.getArtifactId());
	}

	private Triple<String, String, String> checkVersion(VersionRegisterInfo registerInfo, PropertyReportInfo reportPropertyInfo) {
		var newVersion = switch (registerInfo.getUpdatePolicy()) {
		case MAJOR -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestMajor());
		case MINOR -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestMinor());
		case INCREMENTAL -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), getIncrementalVersion(reportPropertyInfo));
		case LATEST -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), latestVersion(reportPropertyInfo));
		case SNAPSHOT -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestSubincremental());
		default -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), StringUtils.EMPTY);
		};
		if (StringUtils.equals(newVersion.getRight(), registerInfo.getCurrentVersion())) {
			return Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), StringUtils.EMPTY);
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
		return StringUtils.EMPTY;
	}

}
