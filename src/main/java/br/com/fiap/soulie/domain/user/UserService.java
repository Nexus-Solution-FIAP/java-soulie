package br.com.fiap.soulie.domain.user;


import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.user.dto.UpdateUserData;
import br.com.fiap.soulie.domain.user.dto.UserRequest;
import br.com.fiap.soulie.domain.user.dto.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper mapper;

    @Autowired
    private AvatarRepository avatarRepository;

    // Get
    public Page<UserResponse> listUsersDetails(Pageable pageable) {
        return repository.findAllByActiveTrue(pageable).map(mapper::toDetail);
    }

    public UserResponse userDetails(Long id){
        var user = repository.getReferenceById(id);
        return mapper.toDetail(user);
    }

    // Post
    @Transactional
    public UserResponse registerUser(UserRequest userRequest) {
        var user = mapper.toEntity(userRequest);
        user.assignHandle(uniqueHandle(userRequest.email()));
        repository.save(user);
        user.encryptPassword(passwordEncoder);
        avatarRepository.save(Avatar.starter(user));
        return mapper.toDetail(user);
    }

    // Put
    @Transactional
    public UserResponse updateUser(UpdateUserData updateUserData, Long id) {
        var user = repository.getReferenceById(id);
        user.updateData(updateUserData);
        return mapper.toDetail(user);
    }

    // Patch
    @Transactional
    public UserResponse activeUser(Long id){
        var user = repository.getReferenceById(id);
        user.activateUser();
        return mapper.toDetail(user);
    }

    // Delete
    @Transactional
    public UserResponse excludeUser(Long id) {
        var user = repository.getReferenceById(id);
        user.deleteUser();
        return mapper.toDetail(user);
    }

    private String uniqueHandle(String email) {
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String base = local.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (base.isBlank()) {
            base = "soulie";
        }
        String candidate = "@" + base;
        int suffix = 1;
        while (repository.existsByHandle(candidate)) {
            candidate = "@" + base + suffix++;
        }
        return candidate;
    }
}
