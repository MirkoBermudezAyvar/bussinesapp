package pe.farmaciasperuanas.ti.venar.ravash.infrastructure.inbound.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador principal que expone el servicio a trav&eacute;s de HTTP/Rest para
 * las operaciones del recurso Ravash<br/>
 * <b>Class</b>: RavashController<br/>
 * <b>Copyright</b>: 2025 Farmacias Peruanas.<br/>
 * <b>Company</b>:Farmacias Peruanas.<br/>
 *
 * <u>Developed by</u>: <br/>
 * <ul>
 * <li>Mirko Bermudez</li>
 * </ul>
 * <u>Changes</u>:<br/>
 * <ul>
 * <li>Jul 23, 2025 Creaci&oacute;n de Clase.</li>
 * </ul>
 * @version 1.0
 */
@Slf4j
@RestController
public class RavashRest {

  @GetMapping(value = {"/health"})
  public String health() {
    return "It's running";
  }

}