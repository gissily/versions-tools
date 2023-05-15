package xyz.opcal.tools.service.report;

import java.io.File;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.commons.lang3.tuple.Triple;

import xyz.opcal.tools.model.config.VersionRegisterInfo;

@FunctionalInterface
public interface IReportParser extends BiFunction<File, List<VersionRegisterInfo>, List<Triple<String, String, String>>> {

	default List<Triple<String, String, String>> parse(File reportFile, List<VersionRegisterInfo> versionRegisters) {
		return apply(reportFile, versionRegisters);
	}
}
