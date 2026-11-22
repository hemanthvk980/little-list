package in.littlelist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude=org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration.class)
public class LittleList {
    public static void main(String[] args) {
        SpringApplication.run(LittleList.class, args);
    }
}
