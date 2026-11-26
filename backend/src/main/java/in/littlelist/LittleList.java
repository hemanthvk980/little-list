package in.littlelist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication(exclude=org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration.class)
public class LittleList {
    public static void main(String[] args) {
        SpringApplication.run(LittleList.class, args);
    }
}
