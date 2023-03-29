package xyz.opcal.tools.model.config;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class VersionConfig implements Serializable {

	private static final long serialVersionUID = -870189158014674398L;

	/**
	 * dependencies.properties file path
	 */
	private String dependencies;

	/**
	 * property-updates-report.html file path
	 */
	private List<String> updateReports;

	/**
	 * parent-updates-report.html file path
	 */
	private List<String> parentReports;

	/**
	 * register version property need to check and update
	 */
	private List<VersionRegisterInfo> versionRegisters;
}
