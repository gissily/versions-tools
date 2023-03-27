package xyz.opcal.tools.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import lombok.SneakyThrows;
import xyz.opcal.tools.model.config.PropertyVersionInfo;
import xyz.opcal.tools.model.reporting.ReportPropertyInfo;

@Service
public class VersionCheckerService {

	static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

	private @Autowired VersionConfigService versionConfigService;
	private @Autowired ReportParseService reportParseService;

	@SneakyThrows
	public void check(File config) {
		var versionConfig = versionConfigService.load(config);

		var dependenciesFile = ResourceUtils.getFile(versionConfig.getDependencies());
		var dependencies = loadProperties(dependenciesFile);

		var reports = reportParseService.parse(ResourceUtils.getFile(versionConfig.getUpdateReport()));

		versionConfig.getVersionRegisters().stream().filter(pvinfo -> Objects.nonNull(reports.get(pvinfo.getPropertyName())))
				.map(pvinfo -> checkVersion(pvinfo, reports.get(pvinfo.getPropertyName()))).filter(triple -> StringUtils.isNoneBlank(triple.getRight()))
				.forEach(triple -> updateDependenciesProperties(dependencies, triple));

		updateDependencies(dependencies, dependenciesFile);
	}

	void updateDependenciesProperties(Properties dependencies, Triple<String, String, String> triple) {
		System.out.println(String.format("update new version key: [%s]\t\t[ %s -> %s ]", triple.getLeft(), triple.getMiddle(), triple.getRight()));
		dependencies.setProperty(triple.getLeft(), triple.getRight());
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
