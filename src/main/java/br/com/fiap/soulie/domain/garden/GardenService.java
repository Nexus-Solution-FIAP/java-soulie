package br.com.fiap.soulie.domain.garden;

import br.com.fiap.soulie.domain.avatar.AvatarMapper;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.avatar.dto.AvatarResponse;
import br.com.fiap.soulie.domain.garden.dto.AddUserToGardenData;
import br.com.fiap.soulie.domain.garden.dto.GardenRequest;
import br.com.fiap.soulie.domain.garden.dto.GardenResponse;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GardenService {

    @Autowired
    private GardenRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private GardenMapper mapper;

    @Autowired
    private AvatarMapper avatarMapper;

    public List<GardenResponse> myGardens(Long userId) {
        return repository.findAllByUsersId(userId).stream().map(mapper::toGardenResponse).toList();
    }

    public GardenResponse gardenDetails(Long gardenId, Long userId) {
        var garden = repository.findByIdAndUsersId(gardenId, userId).orElseThrow(() ->
                new EntityNotFoundException("Jardim não encontrado"));
        return mapper.toGardenResponse(garden);
    }

    public List<AvatarResponse> gardenAvatarDetails(Long gardenId, Long userId) {
        if (!repository.existsByIdAndUsersId(gardenId, userId)) {
            throw new EntityNotFoundException("Jardim não encontrado");
        }
        return avatarRepository.findAllByUserGardensId(gardenId)
                .stream()
                .map(avatarMapper::toDetail)
                .toList();
    }

    @Transactional
    public GardenResponse createGarden(GardenRequest request, Long userId) {
        var garden = mapper.toGarden(request);
        var creator = userRepository.getReferenceById(userId);
        garden.addUserToGarden(creator);
        if (request.usersId() != null && !request.usersId().isEmpty()) {
            userRepository.findAllById(request.usersId()).forEach(garden::addUserToGarden);
        }
        repository.save(garden);
        return mapper.toGardenResponse(garden);
    }

    @Transactional
    public GardenResponse addUserToGarden(AddUserToGardenData data, Long loggedUserId) {
        var garden = repository.findByIdAndUsersId(data.gardenId(), loggedUserId).orElseThrow(() ->
                new EntityNotFoundException("Jardim não encontrado"));
        var users = userRepository.findAllById(data.usersId());
        users.forEach(garden::addUserToGarden);
        return mapper.toGardenResponse(garden);
    }


}
