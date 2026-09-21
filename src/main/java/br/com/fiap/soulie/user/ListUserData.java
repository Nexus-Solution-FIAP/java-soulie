package br.com.fiap.soulie.user;

public record ListUserData(
        String name,
        String email,
        int points
) {
    public ListUserData(User user){
        this(user.getName(), user.getEmail(), user.getPoints());
    }
}
