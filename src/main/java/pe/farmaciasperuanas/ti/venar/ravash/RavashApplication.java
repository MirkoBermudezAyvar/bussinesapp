package pe.farmaciasperuanas.ti.venar.ravash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main class for running Spring Boot framework.<br/>
 * <b>Class</b>: Application<br/>
 * <b>Copyright</b>: 2025 Farmacias Peruanas.<br/>
 * <b>Company</b>: Farmacias Peruanas.<br/>
 *

 * <u>Developed by</u>: <br/>
 * <ul>
 * <li>Mirko Bermudez</li>
 * </ul>
 * <u>Changes</u>:<br/>
 * <ul>
 * <li>Jul 23, 2025 RavashApplication Class.</li>
 * </ul>
 * @version 1.0
 */

@SpringBootApplication
public class RavashApplication {

  /**
   * Main method.
   */
  public static void main(String[] args) {
    new SpringApplication(RavashApplication.class).run(args);
  }
}
