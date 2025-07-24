package pe.farmaciasperuanas.ti.venar.ravash.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableR2dbcRepositories(basePackages = "pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository")
public class R2dbcConfig {
    // Spring Boot auto-configura todo basándose en application.yml
    // No necesitamos definir beans manualmente
}