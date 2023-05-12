package wrm.flunkydom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    System.setProperty("ai.djl.default_engine","PyTorch");
    SpringApplication.run(Application.class, args);
  }

}
