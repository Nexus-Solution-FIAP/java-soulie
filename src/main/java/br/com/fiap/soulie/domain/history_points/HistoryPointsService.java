package br.com.fiap.soulie.domain.history_points;

import br.com.fiap.soulie.domain.action.Action;
import br.com.fiap.soulie.domain.history_points.dto.HistoryPointsResponse;
import br.com.fiap.soulie.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistoryPointsService {

    @Autowired
    private HistoryPointsRepository historyPointsRepository;

    // Get
    public List<HistoryPointsResponse> myHistory(Long userId) {
        return historyPointsRepository.findAllByUserIdOrderByDateDesc(userId).stream()
                .map(HistoryPointsResponse::new)
                .toList();
    }

    // Chamado quando uma missão é concluída
    @Transactional
    public void register(User user, Action action, int points) {
        historyPointsRepository.save(new HistoryPoints(user, action, points));
    }

}
