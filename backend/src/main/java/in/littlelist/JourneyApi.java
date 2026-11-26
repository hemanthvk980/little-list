package in.littlelist;
import java.time.LocalDate;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/wishlists/{id}/journey")
class JourneyApi {
 private final JdbcTemplate db;private final WishlistService lists;
 JourneyApi(JdbcTemplate db,WishlistService lists){this.db=db;this.lists=lists;}
 record Journey(@Size(max=40)String chapter,LocalDate arrivalDate,boolean multiple,
  @Pattern(regexp="|first|experienced|skip")String experience,
  @Pattern(regexp="|ready|guided|exploring")String feeling,boolean countdownEnabled){}
 @GetMapping @Transactional Journey get(Authentication auth,@PathVariable UUID id){
  lists.get(Api.owner(auth),id);var rows=db.queryForList("select * from registry_journey where wishlist_id=?",id);
  if(rows.isEmpty())return new Journey("",null,false,"","",true);
  var r=rows.getFirst();return new Journey((String)r.get("chapter"),r.get("arrival_date")==null?null:((java.sql.Date)r.get("arrival_date")).toLocalDate(),(Boolean)r.get("multiple"),(String)r.get("experience"),(String)r.get("feeling"),(Boolean)r.get("countdown_enabled"));
 }
 @PutMapping @Transactional Journey put(Authentication auth,@PathVariable UUID id,@Valid @RequestBody Journey j){
  lists.get(Api.owner(auth),id);save(id,j);return j;
 }
 void save(UUID id,Journey j){
  if(j.arrivalDate()!=null&&(j.arrivalDate().isBefore(LocalDate.now().minusYears(5))||j.arrivalDate().isAfter(LocalDate.now().plusYears(3))))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Choose a date within the next three years or past five years.");
  db.update("insert into registry_journey values(?,?,?,?,?,?,?) on conflict(wishlist_id) do update set chapter=excluded.chapter,arrival_date=excluded.arrival_date,multiple=excluded.multiple,experience=excluded.experience,feeling=excluded.feeling,countdown_enabled=excluded.countdown_enabled",id,j.chapter()==null?"":j.chapter(),j.arrivalDate(),j.multiple(),j.experience()==null?"":j.experience(),j.feeling()==null?"":j.feeling(),j.countdownEnabled());
 }
}
