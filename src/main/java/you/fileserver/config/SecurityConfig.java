package you.fileserver.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static you.fileserver.config.Constants.LOGIN_PATH;
import static you.fileserver.config.Constants.LOGOUT_PATH;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final ObjectMapper objectMapper;
    private final CsrfTokenRepository csrfTokenRepository;
    private final XorCsrfTokenRequestAttributeHandler tokenHandler;

    public SecurityConfig(ObjectMapper objectMapper, CsrfTokenRepository csrfTokenRepository, XorCsrfTokenRequestAttributeHandler tokenHandler) {
        this.objectMapper = objectMapper;
        this.csrfTokenRepository = csrfTokenRepository;
        this.tokenHandler = tokenHandler;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll()) //仮置き(すべて許可)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")) //h2コンソール用(開発用)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) //h2コンソール用(開発用)

                //ログアウトをAPI向けにカスタム
                .logout(logout -> logout
                        .logoutUrl(LOGOUT_PATH)
                        .logoutSuccessHandler((request, response, _) -> {
                            //csrfトークンを生成
                            CsrfToken rawCsrfToken = csrfTokenRepository.generateToken(request);
                            csrfTokenRepository.saveToken(rawCsrfToken, request, response);

                            tokenHandler.handle(request, response, () -> rawCsrfToken); //ハンドラを使いリクエストにトークンを紐づけ

                            CsrfToken encodeCsrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName()); //リクエストからトークンを取得

                            // クライアントに200 OKと新しいトークンを返す
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json;charset=UTF-8");
                            objectMapper.writeValue(response.getWriter(), Map.of(
                                    "code", 200,
                                    "csrfToken", encodeCsrfToken.getToken()
                            ));
                        }))
                //ログインフォームをAPI向けにカスタム
                .formLogin(login -> login
                        .loginProcessingUrl(LOGIN_PATH)
                        .successHandler((request, response, _) -> {
                            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            objectMapper.writeValue(response.getWriter(), Map.of("code", 200, "csrfToken", csrfToken.getToken()));
                        })
                        //失敗時
                .failureHandler((_, response, _) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(), Map.of("code", 401));
                }));
        return http.build();
    }

    //パスワードエンコーダー
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
