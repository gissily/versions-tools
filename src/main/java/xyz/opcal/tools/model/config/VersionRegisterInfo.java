package xyz.opcal.tools.model.config;

import java.io.Serializable;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class VersionRegisterInfo implements Serializable {

	private static final long serialVersionUID = -6804825073204253140L;

	private String propertyName;
	private Boolean enable = true;
	private UpdatePolicy updatePolicy;
	
	private String groupId;
	private String artifactId;

}
