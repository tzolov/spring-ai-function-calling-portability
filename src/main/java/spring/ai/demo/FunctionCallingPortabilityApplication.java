package spring.ai.demo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import reactor.core.publisher.Flux;

import org.springframework.ai.anthropic.AnthropicChatClient;
import org.springframework.ai.azure.openai.AzureOpenAiChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mistralai.MistralAiChatClient;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

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
	// public Function<List<Transaction>, List<Status>> paymentStatus() {
	// 	return transactions -> transactions.stream().map(t -> DATASET.get(t)).toList();
	// }
	public Function<Transaction, Status> paymentStatus() {
		return transaction -> DATASET.get(transaction);
	}


	// @Bean
	// @Description("Get the list statuses of a list of payment transactions")
	// public Function<Transactions, Statuses> paymentStatus() {
	// 	return transactions -> {
	// 		return new Statuses(transactions.transactions().stream().map(t -> DATASET.get(t)).toList());
	// 	};
	// }

	@Bean
	ApplicationRunner applicationRunner(
			MistralAiChatClient mistralAi,
			VertexAiGeminiChatClient vertexAiGemini,
			OpenAiChatClient openAi,
			AzureOpenAiChatClient azureOpenAi,
			AnthropicChatClient anthropicChatClient) {

		return args -> {

			// String prompt = "What is the status of my payment transaction 003 and transaction 001?";
			String prompt = "What is the statuses of the following payment transactions 003, 001, 002?";

			System.out.println("OPEN_AI: " + openAi.call(prompt));

			System.out.println("AZURE_OPEN_AI: " + azureOpenAi.call(prompt));

			System.out.println("MISTRAL_AI: " + mistralAi.call(prompt));

			System.out.println("VERTEX_AI_GEMINI: " + vertexAiGemini.call(prompt));

			Flux<ChatResponse> geminiStream = vertexAiGemini.stream(new Prompt(prompt));
			geminiStream.collectList().block().stream().findFirst().ifPresent(resp -> {
				System.out.println("VERTEX_AI_GEMINI (Streaming): " + resp.getResult().getOutput().getContent());
			});

			System.out.println("ANTHROPIC: " + anthropicChatClient.call(prompt));
		};
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(FunctionCallingPortabilityApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
