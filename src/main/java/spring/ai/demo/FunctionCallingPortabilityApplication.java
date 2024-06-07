package spring.ai.demo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class FunctionCallingPortabilityApplication {

	record Transactions(List<Transaction> transactions) {
	}

	record Statuses(List<Status> statuses) {
	}

	record Transaction(String id) {
	}

	record Status(String name) {
	}

	private static final Map<Transaction, Status> DATASET = Map.of(
			new Transaction("001"), new Status("pending"),
			new Transaction("002"), new Status("approved"),
			new Transaction("003"), new Status("rejected"));

	// The spring.ai.<model>.chat.options.functions=paymentStatus properties
	// are used to register the paymentStatus function with the AI Models
	@Bean
	@Description("Get the status of a payment transaction")
	public Function<Transaction, Status> paymentStatus() {
		return transaction -> DATASET.get(transaction);
	}

	@Bean
	ApplicationRunner applicationRunner(
			MistralAiChatModel mistralAi,
			VertexAiGeminiChatModel vertexAiGemini,
			OpenAiChatModel openAi,
			AzureOpenAiChatModel azureOpenAi,
			AnthropicChatModel anthropicChatClient) {

		return args -> {

			// String prompt = "What is the status of my payment transaction 003 and
			// transaction 001?";
			String prompt = "What is the statuses of the following payment transactions 003, 001, 002? Use multiple funciotn calls if needed.";

			System.out.println("\n OPEN_AI: " + openAi.call(prompt) + "\n");

			System.out.println("\n OPEN AI (Streaming): " + content(openAi.stream(new Prompt(prompt))) + "\n");

			System.out.println("\n AZURE OPEN AI: " + azureOpenAi.call(prompt) + "\n");

			System.out.println("\n AZURE OPEN AI (Streaming): " + content(azureOpenAi.stream(new Prompt(prompt))) + "\n");

			System.out.println("\n MISTRAL AI: " + mistralAi.call(prompt) + "\n");

			System.out.println("\n MISTRAL AI (Streaming): " + content(mistralAi.stream(new Prompt(prompt))) + "\n");

			System.out.println("\n VERTEX_AI_GEMINI: " + vertexAiGemini.call(prompt) + "\n");
			
			System.out.println("\n VERTEX_AI_GEMINI (Streaming): " + content(vertexAiGemini.stream(new Prompt(prompt))) + "\n");

			System.out.println("\n ANTHROPIC: " + anthropicChatClient.call(prompt) + "\n");
		};
	}

	private static String content(Flux<ChatResponse> stream) {
		return stream.collectList().block().stream().findFirst().map(resp -> resp.getResult().getOutput().getContent())
				.orElse("");
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(FunctionCallingPortabilityApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
