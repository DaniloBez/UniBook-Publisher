package com.unibook.publisher.production.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "genres")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "genre_id")
    private UUID genreId;

    @Column(nullable = false, unique = true, name = "genre_name")
    private String genreName;
}
