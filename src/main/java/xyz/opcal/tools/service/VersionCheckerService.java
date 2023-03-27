package xyz.opcal.tools.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

import lombok.SneakyThrows;
import xyz.opcal.tools.model.config.PropertyVersionInfo;
import xyz.opcal.tools.model.reporting.ReportPropertyInfo;

@Service
public class VersionCheckerService {

	static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
	static final String UPDATE_FLAG_FILE = "versionUpdate";

	private @Autowired VersionConfigService versionConfigService;
	private @Autowired ReportParseService reportParseService;

	@SneakyThrows
	public void check(File config) {
		if (getUpdateFlagFile().exists()) {
			FileUtils.delete(getUpdateFlagFile());
		}
		var versionConfig = versionConfigService.load(config);

		var dependenciesFile = ResourceUtils.getFile(versionConfig.getDependencies());
		var dependencies = loadProperties(dependenciesFile);

		var reports = reportParseService.parse(ResourceUtils.getFile(versionConfig.getUpdateReport()));

		var list = versionConfig.getVersionRegisters().stream().filter(pvinfo -> Objects.nonNull(reports.get(pvinfo.getPropertyName())))
				.map(pvinfo -> checkVersion(pvinfo, reports.get(pvinfo.getPropertyName()))).filter(triple -> StringUtils.isNoneBlank(triple.getRight()))
				.toList();

		StringBuilder updateMessage = new StringBuilder();
		updateMessage.append("updating new versions: \n ");

		list.forEach(triple -> updateMessage.append(updateDependenciesProperties(dependencies, triple)));
		if (!CollectionUtils.isEmpty(list)) {
			updateDependencies(dependencies, dependenciesFile);
			FileUtils.write(getUpdateFlagFile(), updateMessage, StandardCharsets.UTF_8);
			System.out.println(updateMessage.toString());
			System.out.println("update flag file: " + getUpdateFlagFile());
		}
	}

	File getUpdateFlagFile() {
		return FileUtils.getFile(FileUtils.getTempDirectory(), UPDATE_FLAG_FILE);
	}

	String updateDependenciesProperties(Properties dependencies, Triple<String, String, String> triple) {
		dependencies.setProperty(triple.getLeft(), triple.getRight());
		return String.format("[%s]\t\t[%s -> %s]%n ", triple.getLeft(), triple.getMiddle(), triple.getRight());
	}

	private Triple<String, String, String> checkVersion(PropertyVersionInfo propertyVersionInfo, ReportPropertyInfo reportPropertyInfo) {

		return switch (propertyVersionInfo.getUpdatePolicy()) {
		case MAJOR -> Triple.of(propertyVersionInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestMajor());
		case MINOR -> Triple.of(propertyVersionInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestMinor());
		case INCREMENTAL -> Triple.of(propertyVersionInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestIncremental());
		case LATEST -> Triple.of(propertyVersionInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), latestVersion(reportPropertyInfo));
		case SNAPSHOT -> Triple.of(propertyVersionInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), reportPropertyInfo.getLatestSubincremental());
		default -> Triple.of(propertyVersionInfo.getPropertyName(), reportPropertyInfo.getCurrentVersion(), StringUtils.EMPTY);
		};
	}

	private String latestVersion(ReportPropertyInfo reportPropertyInfo) {
		if (StringUtils.isNoneBlank(reportPropertyInfo.getLatestMajor())) {
			return reportPropertyInfo.getLatestMajor();
		}
		if (StringUtils.isNoneBlank(reportPropertyInfo.getLatestMinor())) {
			return reportPropertyInfo.getLatestMinor();
		}
		if (StringUtils.isNoneBlank(reportPropertyInfo.getLatestIncremental())) {
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
