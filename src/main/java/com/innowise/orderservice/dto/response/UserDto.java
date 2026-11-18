package com.innowise.orderservice.dto.response;

import java.io.Serializable;

public record UserDto(Long id, String name, String surname, String email) implements Serializable {
}
