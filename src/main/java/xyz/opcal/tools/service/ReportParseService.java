package xyz.opcal.tools.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.SneakyThrows;
import xyz.opcal.tools.model.reporting.ReportPropertyInfo;

@Service
public class ReportParseService {

	private static final int DATA_ROW_SIZE = 7;

	@SneakyThrows
	public Map<String, ReportPropertyInfo> parse(File file) {
		var document = Jsoup.parse(file);
		var versionTable = document.getElementsByTag("table").get(1);

		var rows = versionTable.getElementsByTag("tr");

		Map<String, ReportPropertyInfo> infos = new HashMap<>();
		for (Element row : rows) {
			if (CollectionUtils.isEmpty(row.getElementsByTag("td"))) {
				continue;
			}
			parsePropertyInfo(row.getElementsByTag("td")).ifPresent(i -> infos.put(i.getPropertyName(), i));
		}
		return infos;
	}

	private Optional<ReportPropertyInfo> parsePropertyInfo(Elements dataColumns) {
		if (CollectionUtils.isEmpty(dataColumns) || dataColumns.size() < DATA_ROW_SIZE) {
			return Optional.empty();
		}
		ReportPropertyInfo propertyInfo = new ReportPropertyInfo();
		propertyInfo.setPropertyName(StringUtils.substringBefore(StringUtils.substringAfter(dataColumns.get(1).text(), "${"), "}"));
		propertyInfo.setCurrentVersion(dataColumns.get(2).text());
		propertyInfo.setLatestSubincremental(dataColumns.get(3).text());
		propertyInfo.setLatestIncremental(dataColumns.get(4).text());
		propertyInfo.setLatestMinor(dataColumns.get(5).text());
		propertyInfo.setLatestMinor(dataColumns.get(6).text());
		return Optional.of(propertyInfo);
	}

}
