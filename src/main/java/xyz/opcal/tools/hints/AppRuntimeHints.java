package xyz.opcal.tools.hints;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import xyz.opcal.tools.model.config.VersionRegisterInfo;
import xyz.opcal.tools.model.config.VersionConfig;

public class AppRuntimeHints implements RuntimeHintsRegistrar {

	private BindingReflectionHintsRegistrar bindingReflectionHintsRegistrar = new BindingReflectionHintsRegistrar();

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		// version config
		hints.serialization()
			.registerType(VersionConfig.class)
			.registerType(VersionRegisterInfo.class);

		// jackson annotation register
		bindingReflectionHintsRegistrar.registerReflectionHints(hints.reflection(), VersionConfig.class, VersionRegisterInfo.class);
	}

}
