package br.com.fiap.soulie.infra.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatKnowledgeTest {

    @Test
    void carregaPersonaConhecimentoEExemplos() {
        String prompt = new ChatKnowledge().systemPrompt();

        assertThat(prompt).contains("Você é **Alma**");
        assertThat(prompt).contains("SoulUp");
        assertThat(prompt).contains("Missões, XP, sequência e conquistas");
        assertThat(prompt).contains("Navegação e uso do aplicativo");
        assertThat(prompt).contains("Descarte, compostagem e hábitos");
        assertThat(prompt).contains("propostas ainda não confirmadas");
        assertThat(prompt).contains("Diálogos ilustrativos");
        assertThat(prompt).contains("FATOS obtido da conta autenticada");
    }

    @Test
    void naoCarregaGabaritoDoReadme() {
        String prompt = new ChatKnowledge().systemPrompt();

        assertThat(prompt).doesNotContain("A01");
        assertThat(prompt).doesNotContain("Critérios de aceite");
        assertThat(prompt).doesNotContain("30 perguntas e cenários");
        assertThat(prompt).doesNotContain("gsk_");
    }
}
