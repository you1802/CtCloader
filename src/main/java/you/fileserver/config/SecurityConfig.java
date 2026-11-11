package you.fileserver.config;

import jakarta.servlet.DispatcherType;
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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static you.fileserver.config.Constants.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final ObjectMapper objectMapper;
    private final CsrfTokenRepository csrfTokenRepository;
    private final XorCsrfTokenRequestAttributeHandler tokenHandler;
    private final RequestMappingHandlerMapping handlerMapping;

    public SecurityConfig(ObjectMapper objectMapper,
                          CsrfTokenRepository csrfTokenRepository,
                          XorCsrfTokenRequestAttributeHandler tokenHandler,
                          RequestMappingHandlerMapping handlerMapping) {
        this.objectMapper = objectMapper;
        this.csrfTokenRepository = csrfTokenRepository;
        this.tokenHandler = tokenHandler;
        this.handlerMapping = handlerMapping;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll() //サーバー内部で発生する転送(FORWARD),エラー処理(ERROR)はすべて許可
                        .requestMatchers("/CtCloader/**", "/api/**", CSS_PATH, JS_PATH, IMG_PATH).permitAll() //すべての人がアクセス可能な領域
                        .requestMatchers(ADMIN_LOGIN_PATH).anonymous() //非ログイン中のみアクセス可能
                        .requestMatchers("/user/api/**").hasRole(AUTH_USER) //ユーザーでログイン中のみアクセス可能なAPI
                        .requestMatchers("/admin/**").hasRole(AUTH_ADMIN) //ADMINのみアクセス可能な領域
                        .requestMatchers("/h2-console/**").permitAll() //H2コンソール(開発用)
                        .anyRequest().denyAll()) //残りはすべてアクセス不可
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

                            // クライアントに200 OKと新しいトークンを返す
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json;charset=UTF-8");
                            objectMapper.writeValue(response.getWriter(), Map.of(
                                    "code", 200,
                                    "csrfToken", csrfToken.getToken()));
                        })
                        //失敗時
                .failureHandler((_, response, _) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(), Map.of("code", 401));
                }))

                //例外処理
                .exceptionHandling(exception -> { exception
                        .authenticationEntryPoint((request, response, _) -> { //認可チェックではじかれた時の処理
                            if (request.getRequestURI().equals(ADMIN_HOME_PATH)) { //管理者用のトップページの場合は管理者用ログイン画面にリダイレクト
                                response.sendRedirect(ADMIN_LOGIN_PATH);
                            } else { //それ以外の場合
                                boolean isPageExists = false; //アクセスしようとしたページが存在するか

                                try {
                                    if (handlerMapping.getHandler(request) != null) { //リクエストされたURLがマッピングリストに存在するかのチェック
                                        isPageExists = true; //存在する場合はページが存在する
                                    }
                                } catch (Exception _) {} //エラー時は存在しないものとして扱う

                                if (isPageExists) {
                                    response.sendError(HttpServletResponse.SC_NOT_FOUND); //ページがない場合は404
                                } else {
                                    response.sendError(HttpServletResponse.SC_FORBIDDEN); //ページがある場合は403
                                }
                            }
                        });
                });
        return http.build();
    }

    //パスワードエンコーダー
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
