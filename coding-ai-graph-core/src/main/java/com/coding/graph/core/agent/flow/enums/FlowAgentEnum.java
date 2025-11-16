package com.coding.graph.core.agent.flow.enums;

public enum FlowAgentEnum {

	CONDITIONAL("CONDITIONAL"),
	SEQUENTIAL("SEQUENTIAL"),
	ROUTING("ROUTING")
	;

	private final String type;

	FlowAgentEnum(final String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

}
