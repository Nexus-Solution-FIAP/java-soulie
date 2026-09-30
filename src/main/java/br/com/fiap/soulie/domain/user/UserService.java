package br.com.fiap.soulie.domain.user;


import br.com.fiap.soulie.domain.user.record.UpdateUserData;
import br.com.fiap.soulie.domain.user.record.UserData;
import br.com.fiap.soulie.domain.user.record.UserDataDetail;
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

    // Get
    public Page<UserDataDetail> listUsersDetails(Pageable pageable) {
        return repository.findAllByActiveTrue(pageable).map(UserDataDetail::new);
    }

    public UserDataDetail userDetails(Long id){
        var user = repository.getReferenceById(id);
        return new UserDataDetail(user);
    }

    // Post
    @Transactional
    public UserDataDetail registerUser(UserData userData) {
        var user = new User(userData);
        repository.save(user);
        user.encryptPassword(passwordEncoder);
        return new UserDataDetail(user);
    }

    // Put
    @Transactional
    public UserDataDetail updateUser(UpdateUserData updateUserData) {
        var user = repository.getReferenceById(updateUserData.id());
        user.updateData(updateUserData);
        return new UserDataDetail(user);
    }

    // Patch
    @Transactional
    public UserDataDetail activeUser(Long id){
        var user = repository.getReferenceById(id);
        user.activateUser();
        return new UserDataDetail(user);
    }

    // Delete
    @Transactional
    public UserDataDetail excludeUser(Long id) {
        var user = repository.getReferenceById(id);
        user.deleteUser();
        return new UserDataDetail(user);
    }
}
