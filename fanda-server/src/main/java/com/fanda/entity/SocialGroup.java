package com.fanda.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fd_social_group")
public class SocialGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "creator_name", length = 50)
    private String creatorName;

    @Column(name = "avatar", length = 20)
    private String avatar;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "meal_time", length = 10)
    private String mealTime;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "max_people")
    private Integer maxPeople;

    @Column(name = "current_people")
    private Integer currentPeople;

    @Column(name = "status", length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "socialGroup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SocialTag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "socialGroup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SocialCandidate> candidates = new ArrayList<>();

    @OneToMany(mappedBy = "socialGroup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SocialMember> members = new ArrayList<>();
}
