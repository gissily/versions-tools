package xyz.opcal.tools.hints;

import org.apache.commons.configuration2.builder.fluent.FileBasedBuilderParameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import xyz.opcal.tools.model.config.VersionConfig;
import xyz.opcal.tools.model.config.VersionRegisterInfo;
import xyz.opcal.tools.model.reporting.MergeRequestInfo;

public class AppRuntimeHints implements RuntimeHintsRegistrar {

	private BindingReflectionHintsRegistrar bindingReflectionHintsRegistrar = new BindingReflectionHintsRegistrar();

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		hints.reflection().registerType(java.util.Calendar[].class).registerType(java.util.Date[].class).registerType(java.sql.Date[].class)
				.registerType(java.sql.Time[].class).registerType(java.sql.Timestamp[].class).registerType(java.net.URL[].class)
				.registerType(org.apache.commons.configuration2.PropertiesConfiguration.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
						MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS);

		hints.proxies().registerJdkProxy(PropertiesBuilderParameters.class, FileBasedBuilderParameters.class);

		// version config
		hints.serialization().registerType(VersionConfig.class).registerType(VersionRegisterInfo.class).registerType(MergeRequestInfo.class);

		// jackson annotation register
		bindingReflectionHintsRegistrar.registerReflectionHints(hints.reflection(), VersionConfig.class, VersionRegisterInfo.class, MergeRequestInfo.class);
	}

}
