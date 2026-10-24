package br.com.fiap.soulie.domain.mission;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@Entity
@Table(name = "daily_missions")
public class DailyMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String emoji;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String tag;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private int xp;

    @Column(name = "co2_saved_kg", nullable = false)
    private double co2SavedKg;

    @Column(name = "water_saved_liters", nullable = false)
    private double waterSavedLiters;

    @Getter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MissionPeriod period = MissionPeriod.DAILY;

    public DailyMission() {
    }

    public MissionPeriod getPeriod() {
        return period == null ? MissionPeriod.DAILY : period;
    }
}
