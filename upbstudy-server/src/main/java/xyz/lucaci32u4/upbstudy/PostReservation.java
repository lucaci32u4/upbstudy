package xyz.lucaci32u4.upbstudy;

import jakarta.validation.constraints.NotBlank;

public record PostReservation(@NotBlank(message = "dateTime cannot be empty") String dateTime,
                              @NotBlank(message = "apiKey cannot be empty") String apiKey) {
}
