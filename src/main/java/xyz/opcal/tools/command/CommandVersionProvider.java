package xyz.opcal.tools.command;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import picocli.CommandLine.IVersionProvider;

@Component
public class CommandVersionProvider implements IVersionProvider {

	@Value("${info.app.version}")
	private String version;
	
	@Override
	public String[] getVersion() throws Exception {
		return new String[] { version };
	}

}
