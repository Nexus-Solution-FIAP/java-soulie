package br.com.fiap.soulie.domain.avatar;

import br.com.fiap.soulie.domain.avatar.record.AvatarData;
import br.com.fiap.soulie.domain.avatar.record.AvatarDataDetail;
import br.com.fiap.soulie.domain.avatar.record.UpdateAvatar;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvatarService {

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private UserRepository userRepository;


    // Get
    public AvatarDataDetail avatarDetails(Long userId){
        var avatar = avatarRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não possui Avatar"));
        return new AvatarDataDetail(avatar);
    }


    // Post
    @Transactional
    public AvatarDataDetail createAvatar(AvatarData avatarData){
        var user = userRepository.getReferenceById(avatarData.userId());
        var avatar = new Avatar(avatarData, user);
        avatarRepository.save(avatar);

        return new AvatarDataDetail(avatar);
    }

    @Transactional
    public AvatarDataDetail createMyAvatar(AvatarData avatarData, Long userId){
        if (avatarRepository.existsByUserId(userId)) {
            throw new ValidationException("Este usuário já possui um Avatar");
        }
        var user = userRepository.getReferenceById(userId);
        var avatar = new Avatar(avatarData, user);
        avatarRepository.save(avatar);
        return new AvatarDataDetail(avatar);
    }

    // Put
    @Transactional
    public AvatarDataDetail updateMyAvatar(UpdateAvatar updateAvatar, Long userId){
        var avatar = avatarRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não possui Avatar"));
        avatar.updateAvatar(updateAvatar);

        return new AvatarDataDetail(avatar);
    }

}
