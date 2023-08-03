package xyz.opcal.tools.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(properties = { "logging.level.ROOT=info" }, args = { "--version" })
class ReportParserServiceTest {

	@Autowired
	ReportParserService reportParseService;

	@Test
	void parsePropertyReport() throws FileNotFoundException {
		var reportFile = ResourceUtils.getFile("./target/test-classes/property-updates-report.html");
		var reports = reportParseService.parsePropertyReport(reportFile);
		assertNotNull(reports);
		assertFalse(CollectionUtils.isEmpty(reports));
		log.info("report:\n{}", reports);
	}

	@Test
	void parseParentReport() throws FileNotFoundException {
		var reportFile = ResourceUtils.getFile("./target/test-classes/parent-updates-report.html");
		var reports = reportParseService.parseParentReport(reportFile);
		assertNotNull(reports);
		assertFalse(CollectionUtils.isEmpty(reports));
		log.info("report:\n{}", reports);
	}

}
