package com.coding.graph.core.node;

import com.coding.graph.core.state.OverAllState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

import static com.coding.graph.core.common.NodeCodeConstants.END;
import static com.coding.graph.core.common.NodeCodeConstants.START;

@Getter
@AllArgsConstructor
public class NodeOutput {
    private final String node;
    private final OverAllState state;

    public boolean isSTART() {
        return Objects.equals(getNode(), START);
    }
    public boolean isEND() {
        return Objects.equals(getNode(), END);
    }

    public static NodeOutput of(String node, OverAllState state) {
        return new NodeOutput(node, state);
    }
}
