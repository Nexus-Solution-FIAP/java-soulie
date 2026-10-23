package br.com.fiap.soulie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Confere que o contexto do Spring sobe inteiro: todos os beans, os mappers do
 * MapStruct e as cadeias de validators.
 *
 * <p>O {@code @ActiveProfiles("test")} é obrigatório. Sem ele o teste usaria o
 * perfil padrão ({@code dev}) e exigiria um PostgreSQL de verdade, mais as
 * variáveis {@code DB_*} e {@code SECRET_JWT} no ambiente.
 */
@SpringBootTest
@ActiveProfiles("test")
class SoulieApplicationTests {

    @Test
    void contextLoads() {
    }

}
