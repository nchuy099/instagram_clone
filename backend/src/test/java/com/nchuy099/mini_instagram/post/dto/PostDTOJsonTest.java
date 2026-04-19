package com.nchuy099.mini_instagram.post.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostDTOJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void serialize_WhenBooleanFlagsPresent_ShouldExposeIsLikedAndIsSavedProperties() throws Exception {
        PostDTO dto = PostDTO.builder()
                .liked(true)
                .saved(true)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"isLiked\":true");
        assertThat(json).contains("\"isSaved\":true");
        assertThat(json).doesNotContain("\"liked\":");
        assertThat(json).doesNotContain("\"saved\":");
    }
}
