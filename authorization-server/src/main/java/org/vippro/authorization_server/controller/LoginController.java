package org.vippro.authorization_server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.vippro.authorization_server.config.AuthorizationServerProperties;

@Controller
public class LoginController {

    private final AuthorizationServerProperties properties;

    public LoginController(AuthorizationServerProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/login")
    public String login(CsrfToken csrfToken) {
        csrfToken.getToken();
        return "forward:/login.html";
    }

    @GetMapping("/home")
    public String home() {
        return "redirect:" + spaRoot();
    }

    @GetMapping("/register")
    public String register() {
        String registrationUrl = UriComponentsBuilder.fromUriString(spaRoot())
                .replacePath("/")
                .queryParam("register", true)
                .build()
                .toUriString();
        return "redirect:" + registrationUrl;
    }

    private String spaRoot() {
        return UriComponentsBuilder.fromUriString(properties.spa().origin())
                .replacePath("/")
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}
