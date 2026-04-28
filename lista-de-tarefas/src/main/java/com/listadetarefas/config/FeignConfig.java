package com.listadetarefas.config; // Ajuste conforme seu pacote

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = getServletRequestAttributes();

            String authHeader = attributes.getRequest().getHeader("Authorization");
            
            if (authHeader == null || authHeader.isBlank()) {
                throw new IllegalArgumentException(
                        "[Feign Client Error] Cabeçalho 'Authorization' ausente ou vazio na requisição de origem. " +
                                "É impossível realizar a chamada interna segura para outro microsserviço sem o token do usuário."
                );
            }
            
            requestTemplate.header("Authorization", authHeader);
        };
    }

    private static ServletRequestAttributes getServletRequestAttributes() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new IllegalStateException(
                    "[Feign Client Error] Contexto de requisição web não encontrado. " +
                            "Isso geralmente ocorre ao tentar usar o FeignClient dentro de uma thread assíncrona " +
                            "(@Async, Listener do RabbitMQ ou @Scheduled) que não possui os dados da requisição original."
            );
        }
        return attributes;
    }
}
