
package com.coding.graph.core.agent.flow.node;


import com.coding.graph.core.agent.BaseAgent;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.node.action.AsyncEdgeAction;
import com.coding.graph.core.state.OverAllState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 路由边行为
 */
@Slf4j
public class RoutingEdgeAction implements AsyncEdgeAction {

	private ChatClient chatClient;

	private String taskKey;

	public RoutingEdgeAction(ChatModel chatModel, BaseAgent current, List<BaseAgent> subAgents) {
		StringBuilder sb = new StringBuilder();
		sb.append("You are responsible for task routing in a graph-based AI system.\n");

		if (current instanceof ReactAgent reactAgent) {
			sb.append("The instruction that you should follow is to finish this task is: ");
			sb.append(StringUtils.isEmpty(reactAgent.instruction()) ? reactAgent.description()
					: reactAgent.instruction());
		}
		else {
			sb.append("Your role seen by the user is: ");
			sb.append(current.description());
		}

		sb.append("\n\n");
		sb.append(
				"There're a few agents that can handle this task, you can delegate the task to one of the following.");
		sb.append("The agents ability are listed in a 'name:description' format as below:\n");
		for (BaseAgent agent : subAgents) {
			sb.append("- ").append(agent.name()).append(": ").append(agent.description()).append("\n");
		}
		sb.append("\n\n");
		sb.append("Return the agent name to delegate the task to.");
		sb.append("\n\n");
		sb.append(
				"It should be emphasized that the returned result only requires the agent name and no other content.");
		sb.append("\n\n");
		sb.append(
				"For example, if you want to delegate the task to the agent named 'agent1', you should return 'agent1'.");

		this.chatClient = ChatClient.builder(chatModel).defaultSystem(sb.toString()).build();
		this.taskKey = current.outputKey();
	}

	public RoutingEdgeAction(ChatModel chatModel, String roleDescription, String outputKey, List<BaseAgent> subAgents) {
		StringBuilder sb = new StringBuilder();
		sb.append("You are responsible for task routing in a graph-based AI system.\n");

		sb.append("Your role seen by the user is: ");
		sb.append(roleDescription);

		sb.append("\n\n");
		sb.append(
				"There're a few agents that can handle this task, you can delegate the task to one of the following.");
		sb.append("The agents ability are listed in a 'name:description' format as below:\n");
		for (BaseAgent agent : subAgents) {
			sb.append("- ").append(agent.name()).append(": ").append(agent.description()).append("\n");
		}
		sb.append("\n\n");
		sb.append("Return the agent name to delegate the task to.");
		sb.append("\n\n");
		sb.append(
				"It should be emphasized that the returned result only requires the agent name and no other content.");
		sb.append("\n\n");
		sb.append(
				"For example, if you want to delegate the task to the agent named 'agent1', you should return 'agent1'.");

		this.chatClient = ChatClient.builder(chatModel).defaultSystem(sb.toString()).build();
		this.taskKey = outputKey;
	}

	@Override
	public CompletableFuture<String> apply(OverAllState state) {
		CompletableFuture<String> result = new CompletableFuture<>();
		try {
			String taskDetail;
			Object value = state.value(taskKey, null).get();
			if (value != null && value instanceof String str) {
				taskDetail = str;
			} else if (value != null && value instanceof List list) {
				taskDetail = ((Message)list.get(list.size() - 1)).getText();
			} else {
				taskDetail = "请你给出最佳Agent名称来完成该任务。";
			}
			result.complete(this.chatClient.prompt(taskDetail).call().content());
			log.info("路由边决策结果: {}", result.get());
		}
		catch (Exception e) {
			result.completeExceptionally(e);
		}
		return result;
	}

}
