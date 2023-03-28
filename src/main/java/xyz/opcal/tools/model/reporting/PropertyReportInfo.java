package xyz.opcal.tools.model.reporting;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PropertyReportInfo {

	private String propertyName;
	private String currentVersion;
	private String latestSubincremental;
	private String latestIncremental;
	private String latestMinor;
	private String latestMajor;

}
