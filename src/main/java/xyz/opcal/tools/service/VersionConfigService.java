package xyz.opcal.tools.service;

import java.io.File;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import lombok.SneakyThrows;
import xyz.opcal.tools.model.config.VersionConfig;

@Service
public class VersionConfigService {

	private final YAMLMapper yamlMapper;

	public VersionConfigService() {
		yamlMapper = new YAMLMapper();
		yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@SneakyThrows
	public VersionConfig load(File config) {
		return yamlMapper.readValue(config, VersionConfig.class);
	}
}
