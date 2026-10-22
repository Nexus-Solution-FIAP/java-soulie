package br.com.fiap.soulie.domain.garden;

import br.com.fiap.soulie.api.dto.CommunityGardenResponse;
import br.com.fiap.soulie.domain.avatar.AvatarMapper;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.avatar.dto.AvatarResponse;
import br.com.fiap.soulie.domain.garden.dto.AddUserToGardenData;
import br.com.fiap.soulie.domain.garden.dto.GardenRequest;
import br.com.fiap.soulie.domain.garden.dto.GardenResponse;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class GardenService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

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
        garden.setInviteCode(newCode());
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

    @Transactional
    public Optional<Garden> findMine(User user) {
        return repository.findAllByUsersIdOrderByIdAsc(user.getId()).stream().findFirst();
    }

    @Transactional
    public Optional<CommunityGardenResponse> current(User user) {
        return findMine(user).map(this::toCommunity);
    }

    @Transactional
    public List<User> members(User user) {
        return findMine(user)
                .map(this::activeMembers)
                .orElseGet(() -> List.of(user));
    }

    @Transactional
    public CommunityGardenResponse openMine(User user) {
        return findMine(user)
                .map(this::toCommunity)
                .orElseGet(() -> toCommunity(createPersonal(user)));
    }

    @Transactional
    public Garden requireGarden(User user) {
        return findMine(user).orElseGet(() -> createPersonal(user));
    }

    @Transactional
    public CommunityGardenResponse join(User user, String rawCode) {
        String code = normalizeInviteCode(rawCode);
        if (!code.matches("[A-Z0-9]{4,12}")) {
            throw new ValidationException("Código inválido");
        }
        Garden target = repository.findByInviteCodeIgnoreCase(code)
                .orElseGet(() -> repository.findByInviteCode(code).orElse(null));
        if (target == null) {
            throw new ValidationException("Código de jardim não encontrado");
        }
        User managed = userRepository.getReferenceById(user.getId());
        leaveAllExcept(managed, target.getId());
        target.addUserToGarden(managed);
        Garden saved = repository.saveAndFlush(target);
        return toCommunity(saved);
    }

    @Transactional
    public void leave(User user) {
        if (findMine(user).isEmpty()) {
            throw new ValidationException("Você não está em um jardim");
        }
        leaveAllExcept(user, null);
    }

    private Garden createPersonal(User user) {
        var garden = new Garden();
        garden.setName(gardenName(user));
        garden.setInviteCode(newCode());
        garden.addUserToGarden(userRepository.getReferenceById(user.getId()));
        return repository.save(garden);
    }

    private void leaveAllExcept(User user, Long keepId) {
        var gardens = new ArrayList<>(repository.findAllByUsersIdOrderByIdAsc(user.getId()));
        for (Garden garden : gardens) {
            if (keepId != null && garden.getId().equals(keepId)) {
                continue;
            }
            garden.getUsers().removeIf(member -> member.getId().equals(user.getId()));
            repository.saveAndFlush(garden);
            if (garden.getUsers().isEmpty()) {
                repository.delete(garden);
            }
        }
    }

    private List<User> activeMembers(Garden garden) {
        return garden.getUsers().stream()
                .filter(member -> member.getActive() == null || member.getActive())
                .sorted(Comparator.comparingInt(User::getPoints).reversed().thenComparing(User::getId))
                .toList();
    }

    private CommunityGardenResponse toCommunity(Garden garden) {
        return new CommunityGardenResponse(
                garden.getId(),
                garden.getName(),
                garden.getInviteCode(),
                activeMembers(garden).size()
        );
    }

    private String normalizeInviteCode(String rawCode) {
        if (rawCode == null) {
            return "";
        }
        return rawCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String gardenName(User user) {
        String name = user.getName() == null ? "" : user.getName().trim();
        String first = name.isBlank() ? "Soulie" : name.split(" ")[0];
        String titled = "Jardim de " + first;
        return titled.length() > 100 ? titled.substring(0, 100) : titled;
    }

    private String newCode() {
        for (int attempt = 0; attempt < 8; attempt++) {
            var code = new StringBuilder(6);
            for (int index = 0; index < 6; index++) {
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            if (!repository.existsByInviteCode(code.toString())) {
                return code.toString();
            }
        }
        throw new ValidationException("Não foi possível gerar o código do jardim");
    }
}
