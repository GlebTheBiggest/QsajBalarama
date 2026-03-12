package com.qsaj.balarama.v1.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private CategoryGroup group;
}