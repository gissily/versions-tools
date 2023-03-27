package xyz.opcal.tools.model.config;

public enum UpdatePolicy {

	/**
	 * update latest major
	 */
	MAJOR,

	/**
	 * update latest minor
	 */
	MINOR,

	/**
	 * update latest incremental
	 */
	INCREMENTAL,

	/**
	 * update to the latest version, version check and update order: <br/>
	 * <p>
	 * MAJOR > MINOR > INCREMENTAL
	 * </p>
	 */
	LATEST,

	/**
	 * update current snapshot version to release which exists
	 */
	SNAPSHOT

}
