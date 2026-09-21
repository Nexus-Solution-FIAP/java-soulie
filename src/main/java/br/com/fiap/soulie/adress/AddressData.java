package br.com.fiap.soulie.adress;

public record AddressData(
        String street,
        String number,
        String neighborhood,
        String city,
        String state
) {
}
