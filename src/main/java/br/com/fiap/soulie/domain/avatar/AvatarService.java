package br.com.fiap.soulie.domain.avatar;

import br.com.fiap.soulie.domain.avatar.dto.AvatarRequest;
import br.com.fiap.soulie.domain.avatar.dto.AvatarResponse;
import br.com.fiap.soulie.domain.avatar.dto.UpdateAvatar;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvatarService {

    @Autowired
    private AvatarRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvatarMapper mapper;


    // Get
    public AvatarResponse avatarDetails(Long userId){
        var avatar = repository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não possui Avatar"));
        return mapper.toDetail(avatar);
    }

    @Transactional
    public AvatarResponse createMyAvatar(AvatarRequest avatarRequest, Long userId){
        if (repository.existsByUserId(userId)) {
            throw new ValidationException("Este usuário já possui um Avatar");
        }
        var user = userRepository.getReferenceById(userId);
        var avatar = mapper.toEntity(avatarRequest, user);
        repository.save(avatar);
        return mapper.toDetail(avatar);
    }

    // Put
    @Transactional
    public AvatarResponse updateMyAvatar(UpdateAvatar updateAvatar, Long userId){
        var avatar = repository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não possui Avatar"));
        avatar.updateAvatar(updateAvatar);

        return mapper.toDetail(avatar);
    }

}
