package br.com.fiap.soulie.domain.action;

import br.com.fiap.soulie.domain.action.dto.ActionRequest;
import br.com.fiap.soulie.domain.action.dto.ActionResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ActionService {

    @Autowired
    private ActionRepository actionRepository;

    public Page<ActionResponse> listActions(Pageable pageable) {
        return actionRepository.findAll(pageable).map(ActionResponse::new);
    }

    public ActionResponse actionDetails(Long id) {
        var action = actionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ação não encontrada"));
        return new ActionResponse(action);
    }

    public ActionResponse createAction(ActionRequest actionRequest) {
        var action = new Action(actionRequest);
        actionRepository.save(action);
        return new ActionResponse(action);
    }
}
