package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.action.ActionService;
import br.com.fiap.soulie.domain.action.dto.ActionRequest;
import br.com.fiap.soulie.domain.action.dto.ActionResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/action")
@SecurityRequirement(name = "bearer-key")
public class ActionController {

    @Autowired
    private ActionService service;

    @GetMapping
    public ResponseEntity<Page<ActionResponse>> listActions(
            @ParameterObject @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.listActions(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActionResponse> actionDetails(@PathVariable Long id) {
        return ResponseEntity.ok(service.actionDetails(id));
    }

    @PostMapping
    public ResponseEntity<ActionResponse> createAction(@RequestBody @Valid ActionRequest request,
                                                       UriComponentsBuilder uriBuilder) {
        var detail = service.createAction(request);
        var uri = uriBuilder.path("/action/{id}").buildAndExpand(detail.id()).toUri();
        return ResponseEntity.created(uri).body(detail);
    }
}
