package spring.ai.demo;

import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.azure.openai.AzureOpenAiChatClient;
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
			MistralAiChatClient mistralAi,
			VertexAiGeminiChatClient vertexAiGemini,
			OpenAiChatClient openAi,
			AzureOpenAiChatClient azureOpenAi) {

		return args -> {

			String prompt = "What is the status of my payment transaction 003?";

			System.out.println("MISTRAL_AI: " + mistralAi.call(prompt));

			System.out.println("VERTEX_AI_GEMINI: " + vertexAiGemini.call(prompt));

			System.out.println("OPEN_AI: " + openAi.call(prompt));

			System.out.println("AZURE_OPEN_AI: " + azureOpenAi.call(prompt));
		};
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(FunctionCallingPortabilityApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
