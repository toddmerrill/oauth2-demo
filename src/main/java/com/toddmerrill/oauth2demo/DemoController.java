package com.toddmerrill.oauth2demo;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.ModelAndView;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Controller
public class DemoController {
    private static final String AUTH_NONCE = "auth_nonce";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String USER_PROFILE = "userProfile";

    private final WebClient oauthClient;
    private final WebClient resourceClient;
    private final HttpSession session;

    @Value("${callback.url}")
    private String callbackUrl;

    @Value("${oauth.authorize.url}")
    private String authorizationUrl;

    @Value("${oauth.token.path}")
    private String oauthTokenPath;

    @Value("${client.id}")
    private String clientId;

    @Value("${client.secret}")
    private String clientSecret;

    public DemoController(@Qualifier("oauthClient") WebClient oauthClient,
                          @Qualifier("resourceClient") WebClient resourceClient,
                          HttpSession session) {
        this.oauthClient = oauthClient;
        this.resourceClient = resourceClient;
        this.session = session;
    }

    @GetMapping("/")
    public String index() {
        log.debug("processing index");
        return "index";
    }

    @GetMapping("/authenticate")
    public ModelAndView redirectToAuthenticator(ModelMap model, HttpSession session) {
        log.debug("processing authenticate");
        if (session.getAttribute(ACCESS_TOKEN) != null) {
            log.debug("Already Authenticated!");
            return loggedInResponse(model);
        }
        String authNonce = UUID.randomUUID().toString();
        session.setAttribute(AUTH_NONCE, authNonce);
        model.addAttribute("client_id", clientId);
        model.addAttribute("redirect_uri", callbackUrl);
        model.addAttribute("scope", "user");
        model.addAttribute("state", authNonce);  // state can contain any information you wish, but here we're using it only as an assertion in the callback
        return new ModelAndView("redirect:" + authorizationUrl, model);
    }

    @GetMapping("/callback")
    public ModelAndView callback(@RequestParam String code, @RequestParam String state, ModelMap model) {
        log.debug("processing callback");
        if(!state.equals(session.getAttribute(AUTH_NONCE))) {
            throw new RuntimeException("Invalid callback state check!");
        }

        AccessToken response = exchangeForAccessToken(code);

        assert response != null;
        session.setAttribute(ACCESS_TOKEN, response);
        log.debug("Authorization successful!");

        UserProfile userProfile = retrieveGitHubProfile(response);

        assert userProfile != null;

        log.debug("User name: {}", userProfile.getName());
        session.setAttribute(USER_PROFILE, userProfile);

        return loggedInResponse(model);
    }

    @GetMapping("/logout")
    public String logout() {
        log.debug("processing logout");
        session.invalidate();
        return "index";
    }

    private ModelAndView loggedInResponse(ModelMap model) {
        UserProfile userProfile = (UserProfile) session.getAttribute(USER_PROFILE);
        assert userProfile != null;

        model.addAttribute("name", userProfile.getName());
        return new ModelAndView("index", model);
    }

    private AccessToken exchangeForAccessToken(String code) {
        return oauthClient.get()
                .uri(uriBuilder -> uriBuilder.path(oauthTokenPath)
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .queryParam("redirect_uri", callbackUrl)
                        .queryParam("code", code)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(AccessToken.class)
                .onErrorResume(e -> Mono.empty())
                .block();
    }

    private UserProfile retrieveGitHubProfile(AccessToken response) {
        // All user profile data is requested, but only the 'name' field
        // in the result is mapped to the UserProfile entity
        return resourceClient.get()
                .uri(uriBuilder -> uriBuilder.path("/user").build())
                .header("Authorization", "Bearer " + response.getAccessToken())
                .retrieve()
                .bodyToMono(UserProfile.class)
                .onErrorResume(e -> Mono.empty())
                .block();
    }
}
