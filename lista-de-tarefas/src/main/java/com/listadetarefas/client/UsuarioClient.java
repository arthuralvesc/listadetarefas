package com.listadetarefas.client;

import com.listadetarefas.config.FeignConfig;
import com.listadetarefas.dto.UsuarioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "usuarios", url = "${API_USUARIOS_URL}", configuration = FeignConfig.class)
public interface UsuarioClient {

    @GetMapping("/usuarios/{id}")
    UsuarioDTO buscarUsuarioPorId(@PathVariable("id") Long id);
}