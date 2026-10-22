package br.com.fiap.soulie.infra.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.UUID;

/**
 * Tratamento central de erros da API.
 *
 * <p>Os formatos de resposta das exceções de negócio foram mantidos como
 * estavam, porque o app mobile já os consome. Três coisas mudaram:
 *
 * <ol>
 *   <li>O tratamento genérico de {@code Exception} não devolve mais
 *       {@code ex.getLocalizedMessage()}, que vazava SQL, nome de tabela e
 *       caminho de arquivo para quem provocasse um erro inesperado.</li>
 *   <li>Conta desativada passou a responder igual a senha errada, fechando um
 *       oráculo de enumeração — ver {@link #treatmentErrorBadCredentials}.</li>
 *   <li>A classe estende {@link ResponseEntityExceptionHandler} para que as
 *       exceções padrão do MVC voltem a responder o status certo. Antes o
 *       catch-all de {@code Exception} as engolia: 404, 405 e 400 viravam 500
 *       com stack no log, então uma varredura de caminhos ({@code GET /.env},
 *       {@code GET /actuator/...}) gerava um ERROR por requisição e afogava a
 *       monitoração.</li>
 * </ol>
 */
@RestControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> treatmentNotFound() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Erro de validação de corpo da requisição.
     *
     * <p>Mantém o formato {@code [{field, message}]} que o app já consome, em vez
     * do {@code ProblemDetail} que a classe-mãe devolveria.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        var errors = ex.getFieldErrors();
        return ResponseEntity.badRequest().body(errors.stream().map(ErrorValidationData::new).toList());
    }

    /**
     * Credenciais inválidas — e também conta desativada.
     *
     * <p>{@link AccountStatusException} (de que {@code DisabledException} é
     * subclasse) é tratada aqui **com o mesmo corpo e status** de propósito. Ela
     * acontece nos {@code preAuthenticationChecks}, antes da conferência de
     * senha, então um corpo diferente revelaria que a conta existe e foi
     * desativada — bastava tentar logar com qualquer senha para descobrir quem
     * já teve conta e pediu exclusão.
     */
    @ExceptionHandler({BadCredentialsException.class, AccountStatusException.class})
    public ResponseEntity<String> treatmentErrorBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> treatmentErrorAuthentication() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Falha na autenticação");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> treatmentErrorAcessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado");
    }

    // Regra de negócio violada: a mensagem é nossa, escrita para o usuário final,
    // então pode ir no corpo da resposta.
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<String> treatmentValidationError(ValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> treatmentBusinessError(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    /**
     * Violação de restrição do banco — tipicamente e-mail ou handle duplicado.
     *
     * <p>Sem este tratamento, trocar o e-mail para um já cadastrado caía no
     * catch-all e virava 500. É conflito de estado, não erro do servidor.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> treatmentDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Não foi possível concluir: já existe um registro com esses dados.");
    }

    // As exceções padrão do MVC que já carregam o próprio status — 404
    // (NoResourceFoundException), 405 (HttpRequestMethodNotSupportedException),
    // 400 (HttpMessageNotReadableException), 415 — são tratadas pela
    // ResponseEntityExceptionHandler, que esta classe estende. Não declare
    // @ExceptionHandler para ErrorResponseException aqui: o método handleException
    // da classe-mãe é final e já cobre esse tipo, e declarar outro faz o Spring
    // recusar a subida com "Ambiguous @ExceptionHandler method mapped".

    /**
     * Rede de proteção para o que não foi previsto.
     *
     * <p>O detalhe do erro vai para o log do servidor com um identificador de
     * correlação; o cliente recebe só esse identificador. Assim dá para
     * investigar um caso específico sem expor o funcionamento interno.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorData> treatmentInternalServerError(Exception ex) {
        var correlationId = UUID.randomUUID().toString();
        log.error("Erro inesperado [{}]", correlationId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorData(
                        "Não foi possível concluir a operação. Tente novamente.",
                        correlationId));
    }

    public record ErrorData(String message, String correlationId) {
    }

    private record ErrorValidationData(String field, String message) {

        public ErrorValidationData(FieldError error) {
            this(error.getField(), error.getDefaultMessage());
        }
    }
}
