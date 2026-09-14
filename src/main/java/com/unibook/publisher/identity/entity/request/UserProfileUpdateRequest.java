package com.unibook.publisher.identity.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UserProfileUpdateRequest(
        @NotBlank
        @Size(min = 2, max = 100)
        @JsonProperty("display_name")
        String displayName,

        @Size(max = 1000)
        String bio,

        @Size(max = 2048)
        @URL
        @JsonProperty("avatar_url")
        String avatarUrl,

        @Size(max = 5)
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Формат: 'uk' або 'uk-UA'")
        @JsonProperty("preferred_locale")
        String preferredLocale
) {
}
