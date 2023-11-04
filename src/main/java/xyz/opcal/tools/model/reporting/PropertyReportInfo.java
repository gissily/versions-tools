package xyz.opcal.tools.model.reporting;

import java.util.Objects;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class PropertyReportInfo implements Comparable<PropertyReportInfo> {

	private String propertyName;
	private String currentVersion;
	private String latestSubincremental;
	private String latestIncremental;
	private String latestMinor;
	private String latestMajor;

	@Override
	public int compareTo(PropertyReportInfo o) {
		if (Objects.isNull(o)) {
			return -1;
		}
		return currentVersion.compareTo(o.currentVersion);
	}

}
