package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.community.CommunityApiService;
import br.com.fiap.soulie.domain.garden.GardenService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommunityController.class)
class CommunityControllerTest extends ControllerTestBase {

    @MockitoBean
    private CommunityApiService communityApiService;

    @MockitoBean
    private GardenService gardenService;

    @Test
    void deleteOwnPostReturns204() throws Exception {
        doNothing().when(communityApiService).deletePost(loggedUser, 8L);

        mvc.perform(delete("/v1/community/feed/8").with(logged()))
                .andExpect(status().isNoContent());
        verify(communityApiService).deletePost(loggedUser, 8L);
    }

    @Test
    void deleteWithoutTokenReturns401() throws Exception {
        mvc.perform(delete("/v1/community/feed/8"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(communityApiService);
    }

    @Test
    void deleteSomeoneElsesPostReturns403() throws Exception {
        doThrow(new AccessDeniedException("Você só pode apagar as suas publicações"))
                .when(communityApiService).deletePost(loggedUser, 8L);

        mvc.perform(delete("/v1/community/feed/8").with(logged()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUnknownPostReturns404() throws Exception {
        doThrow(new EntityNotFoundException("Publicação não encontrada"))
                .when(communityApiService).deletePost(loggedUser, 99L);

        mvc.perform(delete("/v1/community/feed/99").with(logged()))
                .andExpect(status().isNotFound());
    }
}
