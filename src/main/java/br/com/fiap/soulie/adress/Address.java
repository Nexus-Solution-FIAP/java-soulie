package br.com.fiap.soulie.adress;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    private String street;
    private String number;
    private String neighborhood;
    private String city;
    private String state;

    public Address(AddressData adressData){
        this.street = adressData.street();
        this.number = adressData.number();
        this.neighborhood = adressData.neighborhood();
        this.city = adressData.city();
        this.state = adressData.state();
    }
}
