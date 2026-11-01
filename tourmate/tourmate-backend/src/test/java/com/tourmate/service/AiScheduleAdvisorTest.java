package com.tourmate.service;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class AiScheduleAdvisorTest {
 HttpServer server;ObjectMapper json=new ObjectMapper();JsonNode sent;String content;String doneReason="stop";int status=200;
 @BeforeEach void setup()throws Exception{server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/api/chat",e->{sent=json.readTree(e.getRequestBody());byte[] bytes=json.writeValueAsBytes(Map.of("model","test","message",Map.of("role","assistant","content",content,"thinking",""),"done",true,"done_reason",doneReason,"prompt_eval_count",20,"eval_count",20));if(status!=200)bytes="private-provider-details".getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(status,bytes.length);try(var out=e.getResponseBody()){out.write(bytes);}});server.start();}
 @AfterEach void close(){server.stop(0);}
 AiScheduleAdvisor advisor(){var api=OllamaApi.builder().baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).build();var model=OllamaChatModel.builder().ollamaApi(api).defaultOptions(OllamaChatOptions.builder().model("test").temperature(0.2).format("json").build()).retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).build();return new AiScheduleAdvisor(ChatClient.builder(model),json,"test");}
 @Test void callsRealSpringAiOllamaAdapterAndConvertsStructuredOutput(){content="{\"visits\":[{\"candidateId\":\"garden\",\"durationMinutes\":180,\"reason\":\"Allow a relaxed exploration of gardens.\"}],\"assumptions\":[\"Hours unknown\"]}";var result=advisor().advise(Map.of("candidates",List.of(Map.of("candidateId","garden","description","Large botanical garden")),"pace","RELAXED"));assertEquals(180,result.visits().getFirst().durationMinutes());assertEquals("test",sent.path("model").asText());assertFalse(sent.path("stream").asBoolean());assertTrue(sent.path("format").path("properties").has("visits"));assertFalse(sent.path("think").asBoolean(true));String prompt=sent.path("messages").toString();assertTrue(prompt.contains("Large botanical garden"));assertTrue(prompt.contains("Never invent"));assertTrue(prompt.contains("durationMinutes"));}
 @Test void invalidJsonAndProviderErrorsAreSanitized(){content="This is not JSON";var ex=assertThrows(ResponseStatusException.class,()->advisor().advise(Map.of()));assertEquals(503,ex.getStatusCode().value());status=500;assertFalse(assertThrows(ResponseStatusException.class,()->advisor().advise(Map.of())).getReason().contains("private-provider"));}
 @Test void truncatedOutputExplainsTheLengthLimit(){doneReason="length";content="{}";var ex=assertThrows(ResponseStatusException.class,()->advisor().advise(Map.of()));assertEquals(502,ex.getStatusCode().value());assertTrue(ex.getReason().contains("length limit"),ex.getReason());}
 @Test void emptyVisitsAreRejectedWithoutSuggestingModelDownload(){content="{\"visits\":[],\"assumptions\":[]}";var ex=assertThrows(ResponseStatusException.class,()->advisor().advise(Map.of()));assertEquals(502,ex.getStatusCode().value());assertFalse(ex.getReason().contains("pull"));}

}
