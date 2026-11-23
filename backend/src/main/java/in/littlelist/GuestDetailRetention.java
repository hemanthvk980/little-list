package in.littlelist;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
@Configuration
@EnableScheduling
class GuestDetailRetention {
    private final JdbcTemplate db;
    GuestDetailRetention(JdbcTemplate db){this.db=db;}
    @Scheduled(fixedDelay=3600000) void eraseExpired(){
        db.update("update claim_request set guest_name=null,guest_email=null,gift_message=null,purchase_store=null where expires_at<clock_timestamp() and guest_email is not null");
        db.update("update gift_claim set guest_name=null,guest_email=null,gift_message=null,purchase_store=null where guest_email is not null and (purchased_at<clock_timestamp()-interval '365 days' or (status<>'purchased' and held_until<clock_timestamp()-interval '30 days'))");
    }
}
