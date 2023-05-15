package xyz.opcal.tools.expression;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class ConfigEnviranmentParser {

	private final ConfigurableEnvironment environment;

	public ConfigEnviranmentParser(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	Map<String, Object> getEnvironmentVariables() {
		Map<String, Object> variables = new HashMap<>();
		variables.putAll(environment.getSystemEnvironment());
		variables.putAll(environment.getSystemProperties());

		// @formatter:off
		StreamSupport.stream(environment.getPropertySources().spliterator(), false) 
				.filter(EnumerablePropertySource.class::isInstance)
				.map(source -> ((EnumerablePropertySource<?>) source).getPropertyNames())
				.flatMap(Arrays::stream)
				.forEach(property -> variables.put(property, environment.getProperty(property)));
		return variables;
		// @formatter:on
	}

	public String parser(String template) {
		Expression expression = new SpelExpressionParser().parseExpression(template, new TemplateParserContext());
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariables(getEnvironmentVariables());
		return expression.getValue(context, String.class);
	}

}
