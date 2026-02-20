package com.example.secrets_manager.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      @NonNull HttpServletRequest request,
      HttpServletResponse response,
      @NonNull AuthenticationException authException)
      throws IOException, ServletException {
    // This is invoked when a user tries to access a secured REST resource without supplying any
    // credentials
    // We should just send a 401 Unauthorized response because there is no 'login page' to redirect
    // to
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
  }
}
