package xyz.opcal.tools.service;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.BuilderParameters;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.PropertiesBuilderParametersImpl;
import org.apache.commons.configuration2.builder.fluent.FileBasedBuilderParameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.stereotype.Service;

import lombok.SneakyThrows;

@Service
public class DependenciesPropertiesService {

	static final Class<?>[] interfaces = new Class<?>[] { PropertiesBuilderParameters.class, FileBasedBuilderParameters.class };

	@SneakyThrows
	public FileBasedConfigurationBuilder<PropertiesConfiguration> loadConfigurationBuilder(File file) {
		return new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class).configure(builderProperties().setFile(file));
	}

	@SneakyThrows
	public void updatePropertiesFile(FileBasedConfigurationBuilder<PropertiesConfiguration> builder) {
		builder.save();
	}

	/**
	 * native support
	 * 
	 * @return
	 */
	static PropertiesBuilderParameters builderProperties() {
		ClassLoader classLoader = ProxyFactory.class.getClassLoader();
		InvocationHandler handler = new ParametersIfcInvocationHandler(new PropertiesBuilderParametersImpl());
		var newProxyInstance = (PropertiesBuilderParameters) Proxy.newProxyInstance(classLoader, interfaces, handler);
		new org.apache.commons.configuration2.builder.fluent.Parameters().getDefaultParametersManager().initializeParameters(newProxyInstance);
		return newProxyInstance;
	}

	/**
	 * native support
	 */
	private static class ParametersIfcInvocationHandler implements InvocationHandler {
		private final Object target;

		public ParametersIfcInvocationHandler(final Object targetObj) {
			target = targetObj;
		}

		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			final Object result = method.invoke(target, args);
			return isFluentResult(method) ? proxy : result;
		}

		private static boolean isFluentResult(final Method method) {
			final Class<?> declaringClass = method.getDeclaringClass();
			return declaringClass.isInterface() && !declaringClass.equals(BuilderParameters.class);
		}
	}
}
