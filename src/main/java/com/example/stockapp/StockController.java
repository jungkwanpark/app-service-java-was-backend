package com.example.stockapp;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.azure.messaging.eventhubs.*;
import com.azure.identity.DefaultAzureCredentialBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class StockController {
    @Autowired 
    private UserRepository userRepository;

    @Value("${python.agent.url}")
    private String pythonAgentUrl;

    // 어제 사용했던 네임스페이스 호스트 설정 사용
    @Value("${azure.eventhub.namespace-host}")
    private String namespaceHost;

    @Value("${azure.eventhub.name}")
    private String eventHubName;

    private EventHubProducerClient producer;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        try {
            // [어제 성공했던 방식] Connection String 대신 Namespace와 Credential 사용
            this.producer = new EventHubClientBuilder()
                .fullyQualifiedNamespace(namespaceHost)
                .eventHubName(eventHubName)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildProducerClient();
            System.out.println(">>> Event Hub 초기화 성공 (Namespace 방식)");
        } catch (Exception e) {
            System.err.println(">>> Event Hub 초기화 실패: " + e.getMessage());
        }
    }

    @GetMapping("/")
    public String loginPage() { return "login"; }

    @GetMapping("/signup")
    public String signupPage() { return "signup"; }

    @PostMapping("/signup")
    public String processSignup(@RequestParam String userId, @RequestParam String userPw) {
        User user = new User();
        user.setUserId(userId);
        user.setUserPw(userPw);
        userRepository.save(user);
        return "redirect:/";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String userId, @RequestParam String userPw, HttpServletResponse response) {
        return userRepository.findById(userId)
            .filter(u -> u.getUserPw().equals(userPw))
            .map(u -> {
                String cookieValue = UUID.randomUUID().toString();
                u.setCookie(cookieValue);
                userRepository.save(u);
                Cookie cookie = new Cookie("AUTH_TOKEN", cookieValue);
                response.addCookie(cookie);
                return "redirect:/main";
            }).orElse("redirect:/?error");
    }

    @GetMapping("/main")
    public String mainPage(@CookieValue(value = "AUTH_TOKEN", required = false) String token, Model model) {
        if (token == null) return "redirect:/";
        return "main";
    }

    @PostMapping("/send-ticker")
    public String saveTicker(@CookieValue("AUTH_TOKEN") String token, @RequestParam String stockTicker, Model model) {
        User user = userRepository.findAll().stream()
            .filter(u -> token.equals(u.getCookie())).findFirst().orElse(null);

        if (user != null) {
            try {
                // 1. Python API 호출
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("userId", user.getUserId());
                requestBody.put("stockTicker", stockTicker);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
                Map response = restTemplate.postForObject(pythonAgentUrl, requestEntity, Map.class);

                if (response != null) {
                    // 2. Event Hub 전송
                    sendToEventHub(user.getUserId(), stockTicker);

                    model.addAttribute("ticker", response.get("stockTicker"));
                    model.addAttribute("score", response.get("score"));
                    model.addAttribute("reply", response.get("LlmReply"));
                    model.addAttribute("source", response.get("source"));
                    return "result"; 
                }
            } catch (Exception e) {
                model.addAttribute("error", "분석 실패: " + e.getMessage());
                return "main"; 
            }
        }
        return "redirect:/";
    }

    private void sendToEventHub(String userId, String ticker) {
        try {
            if (producer != null) {
                EventDataBatch batch = producer.createBatch();
                String jsonMessage = String.format("{\"userId\":\"%s\", \"ticker\":\"%s\"}", userId, ticker);
                batch.tryAdd(new EventData(jsonMessage));
                producer.send(batch);
                System.out.println(">>> 전송 완료: " + ticker);
            }
        } catch (Exception e) {
            System.err.println(">>> 전송 실패: " + e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        if (producer != null) producer.close();
    }
}

