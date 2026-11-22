package in.littlelist;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/admin")
class AdminApi {
 private final JdbcTemplate db; private final AdminAccess access;
 AdminApi(JdbcTemplate db,AdminAccess access){this.db=db;this.access=access;}
 record Page(List<Map<String,Object>> items,long total,int page,int size){}
 private void validate(int page,int size,String q){if(page<0||page>100000||size<1||size>100||q.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination or search.");}
 private Map<String,Object> one(String sql,UUID id){var rows=db.queryForList(sql,id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found.");return rows.getFirst();}
 @GetMapping("/overview") Map<String,Object> overview(Authentication auth){access.audit(auth,"overview",null);return db.queryForMap("select (select count(*) from app_user) users,(select count(*) from wishlist) registries,(select count(*) from gift where not archived) gifts");}
 @GetMapping("/users") Page users(Authentication auth,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="true") boolean hasRegistries){
  access.require(auth);validate(page,size,q);access.audit(auth,"users",null);
  String where=" where (position(lower(?) in lower(coalesce(u.display_name,'')||' '||coalesce(u.email,'')))>0)"+(hasRegistries?" and exists(select 1 from wishlist w where w.owner_subject=u.owner_subject)":"");
  long total=db.queryForObject("select count(*) from app_user u"+where,Long.class,q);
  return new Page(db.queryForList("select u.id,u.display_name,u.email,u.first_seen,u.last_login,(select count(*) from wishlist w where w.owner_subject=u.owner_subject) registry_count from app_user u"+where+" order by u.first_seen desc,u.id limit ? offset ?",q,size,(long)page*size),total,page,size);
 }
 @GetMapping("/users/{id}") Map<String,Object> user(Authentication auth,@PathVariable UUID id){access.audit(auth,"user",id);return one("select id,display_name,email,first_seen,last_login from app_user where id=?",id);}
 @GetMapping("/users/{id}/registries") Page registries(Authentication auth,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
  access.require(auth);validate(page,size,"");access.audit(auth,"registries",id);one("select id from app_user where id=?",id);
  String from=" from wishlist w join app_user u on u.owner_subject=w.owner_subject where u.id=?";
  return new Page(db.queryForList("select w.id,w.title,w.occasion,w.created_at,(w.share_id is not null) published,(select count(*) from gift g where g.wishlist_id=w.id and not g.archived) gift_count"+from+" order by w.created_at desc,w.id limit ? offset ?",id,size,(long)page*size),db.queryForObject("select count(*)"+from,Long.class,id),page,size);
 }
 @GetMapping("/registries/{id}") Map<String,Object> registry(Authentication auth,@PathVariable UUID id){access.audit(auth,"registry",id);return one("select w.id,w.title,w.occasion,w.greeting,w.created_at,(w.share_id is not null) published,u.id user_id from wishlist w left join app_user u on u.owner_subject=w.owner_subject where w.id=?",id);}
 @GetMapping("/registries/{id}/items") Page items(Authentication auth,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
  access.require(auth);validate(page,size,"");access.audit(auth,"items",id);one("select id from wishlist where id=?",id);
  return new Page(db.queryForList("select g.id,g.name,g.category,g.variant,g.note,g.retailer_url,g.price_paise,g.quantity,g.priority,(select count(*) from gift_claim c where c.gift_id=g.id and c.status='purchased') purchased,(select count(*) from gift_claim c where c.gift_id=g.id and c.status='held' and c.held_until>now()) reserved from gift g where g.wishlist_id=? and not g.archived order by g.created_at,g.id limit ? offset ?",id,size,(long)page*size),db.queryForObject("select count(*) from gift where wishlist_id=? and not archived",Long.class,id),page,size);
 }
 @GetMapping("/registries/{id}/checklist") Page checklist(Authentication auth,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
  access.require(auth);validate(page,size,"");access.audit(auth,"checklist",id);one("select id from wishlist where id=?",id);
  return new Page(db.queryForList("select item_key,state from checklist_entry where wishlist_id=? order by item_key limit ? offset ?",id,size,(long)page*size),db.queryForObject("select count(*) from checklist_entry where wishlist_id=?",Long.class,id),page,size);
 }
}
