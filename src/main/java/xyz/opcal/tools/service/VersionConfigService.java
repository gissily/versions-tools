package xyz.opcal.tools.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import lombok.SneakyThrows;
import xyz.opcal.tools.expression.ConfigEnviranmentParser;
import xyz.opcal.tools.model.config.VersionConfig;

@Service
public class VersionConfigService {

	private final YAMLMapper yamlMapper;
	private final ConfigurableEnvironment environment;
	private final ConfigEnviranmentParser configEnviranmentParser;

	public VersionConfigService(ConfigurableEnvironment environment) {
		this.environment = environment;
		this.configEnviranmentParser = new ConfigEnviranmentParser(this.environment);
		this.yamlMapper = new YAMLMapper();
		this.yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@SneakyThrows
	public VersionConfig load(File config) {
		return yamlMapper.readValue(loadConfigWithEnv(config), VersionConfig.class);
	}

	@SneakyThrows
	String loadConfigWithEnv(File config) {
		try (InputStream input = new FileInputStream(config)) {
			return configEnviranmentParser.parser(IOUtils.toString(input, StandardCharsets.UTF_8));
		}
	}
}
