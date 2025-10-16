// ...existing code...
package com.project.pawn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String credentialId; // base64 or url-safe base64

    @Lob
    private String publicKey; // stored attestation / public key (for demo we store raw data)

    private Long signCount;
}
// ...existing code...
