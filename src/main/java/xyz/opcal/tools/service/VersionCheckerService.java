package xyz.opcal.tools.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

import lombok.SneakyThrows;
import xyz.opcal.tools.model.config.VersionRegisterInfo;
import xyz.opcal.tools.model.reporting.ParentReportInfo;
import xyz.opcal.tools.model.reporting.PropertyReportInfo;

@Service
public class VersionCheckerService {

	static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
	static final String UPDATE_FLAG_FILE = "versionUpdate";
	static final String PARENT_FLAG_FILE = "parentUpdate";

	private @Autowired VersionConfigService versionConfigService;
	private @Autowired ReportParserService reportParseService;

	private void deleteFlagFiles() throws IOException {
		if (getUpdateFlagFile().exists()) {
			FileUtils.delete(getUpdateFlagFile());
		}
		if (getParentFlagFile().exists()) {
			FileUtils.delete(getParentFlagFile());
		}

	}

	@SneakyThrows
	public void check(File config) {
		deleteFlagFiles();

		var versionConfig = versionConfigService.load(config);

		var dependenciesFile = ResourceUtils.getFile(versionConfig.getDependencies());
		var dependencies = loadProperties(dependenciesFile);

		List<Triple<String, String, String>> list = new ArrayList<>();

		var updateReportPath = versionConfig.getUpdateReport();
		if (StringUtils.isNotBlank(updateReportPath) && ResourceUtils.getFile(versionConfig.getUpdateReport()).exists()) {
			list.addAll(parsePropertyUpdateReport(ResourceUtils.getFile(versionConfig.getUpdateReport()), versionConfig.getVersionRegisters()));
		}

		boolean updateParent = false;

		var parentReportPath = versionConfig.getParentReport();
		if (StringUtils.isNotBlank(parentReportPath) && ResourceUtils.getFile(versionConfig.getParentReport()).exists()) {
			var parentVersions = parseParentUpdateReport(ResourceUtils.getFile(versionConfig.getParentReport()), versionConfig.getVersionRegisters());
			list.addAll(parentVersions);
			updateParent = !parentVersions.isEmpty();
		}

		StringBuilder updateMessage = new StringBuilder();
		updateMessage.append("updating new versions: \n ");

		list.forEach(triple -> updateMessage.append(updateDependenciesProperties(dependencies, triple)));
		if (!CollectionUtils.isEmpty(list)) {
			updateDependencies(dependencies, dependenciesFile);
			FileUtils.write(getUpdateFlagFile(), updateMessage, StandardCharsets.UTF_8);
			System.out.println(updateMessage.toString());
			System.out.println("update flag file: " + getUpdateFlagFile());
			if (updateParent) {
				FileUtils.touch(getParentFlagFile());
				System.out.println("parent update flag file: " + getParentFlagFile());
			}
		}
	}

	List<Triple<String, String, String>> parsePropertyUpdateReport(File reportFile, List<VersionRegisterInfo> versionRegisters) {
		var reports = reportParseService.parsePropertyReport(reportFile);
		return versionRegisters.stream().filter(VersionRegisterInfo::getEnable)
				.filter(registerInfo -> Objects.nonNull(reports.get(registerInfo.getPropertyName())))
				.map(registerInfo -> checkVersion(registerInfo, reports.get(registerInfo.getPropertyName())))
				.filter(triple -> StringUtils.isNotBlank(triple.getRight())).toList();
	}

	List<Triple<String, String, String>> parseParentUpdateReport(File reportFile, List<VersionRegisterInfo> versionRegisters) {
		var reports = reportParseService.parseParentReport(reportFile);
		return versionRegisters.stream().filter(VersionRegisterInfo::getEnable)
				.filter(registerInfo -> StringUtils.isNoneBlank(registerInfo.getGroupId(), registerInfo.getArtifactId()))
				.map(registerInfo -> getParenetReport(registerInfo, reports)).filter(Optional::isPresent).map(op -> parentToProperty(op.get()))
				.map(pair -> checkVersion(pair.getLeft(), pair.getRight())).filter(triple -> StringUtils.isNotBlank(triple.getRight())).toList();
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
		return parentReports.stream()
				.filter(report -> StringUtils.equals(report.getGroupId(), versionRegisterInfo.getGroupId())
						&& StringUtils.equals(report.getArtifactId(), versionRegisterInfo.getArtifactId()))
				.map(report -> Pair.of(versionRegisterInfo, report)).findFirst();

	}

	File getUpdateFlagFile() {
		return FileUtils.getFile(FileUtils.getTempDirectory(), UPDATE_FLAG_FILE);
	}

	File getParentFlagFile() {
		return FileUtils.getFile(FileUtils.getTempDirectory(), PARENT_FLAG_FILE);
	}

	String updateDependenciesProperties(Properties dependencies, Triple<String, String, String> triple) {
		dependencies.setProperty(triple.getLeft(), triple.getRight());
		return String.format("[%s]\t\t[%s -> %s]%n ", triple.getLeft(), triple.getMiddle(), triple.getRight());
	}

	private Triple<String, String, String> checkVersion(VersionRegisterInfo registerInfo, PropertyReportInfo reportPropertyInfo) {

		return switch (registerInfo.getUpdatePolicy()) {
		case MAJOR -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestMajor());
		case MINOR -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestMinor());
		case INCREMENTAL -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestIncremental());
		case LATEST -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), latestVersion(reportPropertyInfo));
		case SNAPSHOT -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestSubincremental());
		default -> Triple.of(registerInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), StringUtils.EMPTY);
		};
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

	@SneakyThrows
	private void updateDependencies(Properties dependencies, File file) {
		try (var outputStream = new FileOutputStream(file)) {
			dependencies.store(outputStream, null);
		}
	}

	@SneakyThrows
	private Properties loadProperties(File file) {
		Properties properties = new Properties();
		try (var inputStream = new FileInputStream(file)) {
			properties.load(inputStream);
		}
		return properties;
	}

}
