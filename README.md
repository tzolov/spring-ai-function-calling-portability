# Spring AI Function Calling Portability

Demonstrate `Function Calling` code portability across 4 AI Models: OpenAI, AzureOpenAI, VertexAI Gemini and Mistral AI.

Use Case: Suppose we want the AI model to respond with information that it does not have.
For example the status of your recent payment transactions.
Users can ask questions about current status for certain payment transactions and use function calling to answer them.

For example, let's consider a sample dataset and a function that retrieves the payment status given a transaction:

```java
	record Transaction(String id) {
	}

	record Status(String name) {
	}

	private static final Map<Transaction, Status> DATASET =
		Map.of(
			new Transaction("001"), new Status("pending"),
			new Transaction("002"), new Status("approved"),
			new Transaction("003"), new Status("rejected"));

	@Bean
	@Description("Get the status of a payment transaction")
	public Function<Transaction, Status> paymentStatus() {
		return transaction -> DATASET.get(transaction);
	}
```

Function is registered as `@Bean` and uses the `@Description` annotation to define function description.
Spring AI greatly simplifies code you need to write to support function invocation.
It brokers the function invocation conversation for you.
You simply provide your function definition as a `@Bean` and then provide the bean name of the function in your prompt options.

Lets add the boot starters for 4 AI Models that support function calling:

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.ai</groupId>
	<artifactId>spring-ai-mistral-ai-spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.ai</groupId>
	<artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.ai</groupId>
	<artifactId>spring-ai-vertex-ai-gemini-spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.ai</groupId>
	<artifactId>spring-ai-azure-openai-spring-boot-starter</artifactId>
</dependency>
```

and configure them in `application.properties`:

```
# MistralAI
spring.ai.mistralai.api-key=${MISTRAL_AI_API_KEY}
spring.ai.mistralai.chat.options.model=mistral-small-latest
spring.ai.mistralai.chat.options.functions=paymentStatus

# OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.functions=paymentStatus

# Google VertexAI Gemini
spring.ai.vertex.ai.gemini.project-id=${VERTEX_AI_GEMINI_PROJECT_ID}
spring.ai.vertex.ai.gemini.location=${VERTEX_AI_GEMINI_LOCATION}
spring.ai.vertex.ai.gemini.chat.options.model=gemini-pro
spring.ai.vertex.ai.gemini.chat.options..functions=paymentStatus

# Microsoft Azure OpenAI
spring.ai.azure.openai.api-key=${AZURE_OPENAI_API_KEY}
spring.ai.azure.openai.endpoint=${AZURE_OPENAI_ENDPOINT}
# This name is acutally the model deployment name in the Azure OpenAI platform.
spring.ai.azure.openai.chat.options.model=gpt-4-0125-preview
spring.ai.azure.openai.chat.options.functions=paymentStatus
```

Now you can test them with the same prompt:

```java
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
```

The output would look something like:

```
MISTRAL_AI: The status of your payment transaction 003 is rejected.
VERTEX_AI_GEMINI: Your transaction has been rejected.
OPEN_AI: The status of your payment transaction 003 is rejected.
AZURE_OPEN_AI: The status of your payment transaction 003 is "rejected".
```

If you change the question, slightly, you can see the limitations of some models.
For example lets ask form multiple transactions (e.g. activate the Parallel Function calling):
`String prompt = "What is the status of my payment transactions 003 and 001?"
Then the result would look something like:

```
MISTRAL_AI: To check the status of multiple payment transactions, I would need to call the "paymentStatus" function for each transaction ID separately as the function currently only accepts one transaction ID at a time. Here are the requests:
1. For transaction ID 003:
``
[{"name": "paymentStatus", "arguments": {"id": "003"}}]
``

2. For transaction ID 001:
``
[{"name": "paymentStatus", "arguments": {"id": "001"}}]
``
Please send these requests one by one to get the status of your transactions.

VERTEX_AI_GEMINI: OK. The status of the payment transaction with the ID `003` is `rejected` and the status of the payment transaction with the ID `001` is `pending`.

OPEN_AI: The status of payment transaction 003 is rejected and the status of payment transaction 001 is pending.

AZURE_OPEN_AI: The status of your payment transactions is as follows:
- Transaction 003: Rejected
- Transaction 001: Pending
```

As you can see, currently Mistral AI doesn't support parallel function calling.


## Related [Spring AI](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/) documentation:

* [Spring AI OpenAI](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/openai-chat.html) and [Function Calling](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/functions/openai-chat-functions.html)

* [Spring AI Azure OpenAI](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/azure-openai-chat.html) and [Function Calling](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/functions/azure-open-ai-chat-functions.html)

* [Spring AI Google VertexAI Gemini](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/vertexai-gemini-chat.html) and [Function Calling](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/functions/vertexai-gemini-chat-functions.html)

* [Spring AI Mistral AI](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/mistralai-chat.html) and [Function Calling](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/functions/mistralai-chat-functions.html)

## Native (GraalVM) Build

You can build this as a native executable.

First maker sure you are using GraalVM 21 JDK. For example:

```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-21.0.2+13.1/Contents/Home
```

Then build:

```
./mvnw clean install -Pnative native:compile
```

Run the native executable:

```
./target/function-calling-portability
```
