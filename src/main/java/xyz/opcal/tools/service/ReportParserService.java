package xyz.opcal.tools.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.SneakyThrows;
import xyz.opcal.tools.model.reporting.ParentReportInfo;
import xyz.opcal.tools.model.reporting.PropertyReportInfo;

@Service
public class ReportParserService {

	private static final int PROPERTY_ROW_SIZE = 7;
	private static final int PARENT_ROW_SIZE = 11;

	@SneakyThrows
	public Map<String, PropertyReportInfo> parsePropertyReport(File file) {
		var document = Jsoup.parse(file);
		var versionTable = document.getElementsByTag("table").get(1);

		var rows = versionTable.getElementsByTag("tr");

		Map<String, PropertyReportInfo> infos = new HashMap<>();
		for (Element row : rows) {
			if (CollectionUtils.isEmpty(row.getElementsByTag("td"))) {
				continue;
			}
			parseProperty(row.getElementsByTag("td")).ifPresent(i -> infos.put(i.getPropertyName(), i));
		}
		return infos;
	}

	private Optional<PropertyReportInfo> parseProperty(Elements dataColumns) {
		if (CollectionUtils.isEmpty(dataColumns) || dataColumns.size() < PROPERTY_ROW_SIZE) {
			return Optional.empty();
		}
		PropertyReportInfo propertyInfo = new PropertyReportInfo();
		propertyInfo.setPropertyName(StringUtils.substringBefore(StringUtils.substringAfter(dataColumns.get(1).text(), "${"), "}"));
		propertyInfo.setCurrentVersion(dataColumns.get(2).text());
		propertyInfo.setLatestSubincremental(dataColumns.get(3).text());
		propertyInfo.setLatestIncremental(dataColumns.get(4).text());
		propertyInfo.setLatestMinor(dataColumns.get(5).text());
		propertyInfo.setLatestMajor(dataColumns.get(6).text());
		return Optional.of(propertyInfo);
	}

	@SneakyThrows
	public List<ParentReportInfo> parseParentReport(File file) {
		var document = Jsoup.parse(file);
		var versionTable = document.getElementsByTag("table").get(0);
		List<ParentReportInfo> infos = new ArrayList<>();

		var rows = versionTable.getElementsByTag("tr");

		for (Element row : rows) {
			if (CollectionUtils.isEmpty(row.getElementsByTag("td"))) {
				continue;
			}
			parseParent(row.getElementsByTag("td")).ifPresent(infos::add);
		}

		return infos;
	}

	private Optional<ParentReportInfo> parseParent(Elements dataColumns) {

		if (CollectionUtils.isEmpty(dataColumns) || dataColumns.size() < PARENT_ROW_SIZE) {
			return Optional.empty();
		}
		ParentReportInfo info = new ParentReportInfo();
		info.setGroupId(dataColumns.get(1).text());
		info.setArtifactId(dataColumns.get(2).text());
		info.setCurrentVersion(dataColumns.get(3).text());
		info.setLatestSubincremental(dataColumns.get(7).text());
		info.setLatestIncremental(dataColumns.get(8).text());
		info.setLatestMinor(dataColumns.get(9).text());
		info.setLatestMajor(dataColumns.get(10).text());
		return Optional.of(info);
	}

}