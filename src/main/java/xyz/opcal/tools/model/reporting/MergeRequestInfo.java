package xyz.opcal.tools.model.reporting;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString
@Accessors(chain = true)
public class MergeRequestInfo implements Serializable {

	private static final long serialVersionUID = -4880259866428838903L;

	private String propertyName;
	private String currentVersion;
	private String newVersion;
	private boolean parent;

}
