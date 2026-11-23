package in.littlelist;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/wishlists/{id}/guests")
class GuestRecords {
    private final JdbcTemplate db; private final WishlistService service;
    GuestRecords(JdbcTemplate db,WishlistService service){this.db=db;this.service=service;}
    @GetMapping ResponseEntity<Map<String,Object>> list(Authentication auth,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        service.get(Api.owner(auth),id);
        if(page<0||page>100000||!Set.of(20,50,100).contains(size))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a valid page and page size.");
        long total=db.queryForObject("select count(*) from gift_claim c join gift g on g.id=c.gift_id where g.wishlist_id=? and c.status='purchased'",Long.class,id);
        var rows=db.queryForList("select c.id,g.name as gift,c.guest_name as name,c.guest_email as email,c.gift_message as message,c.purchase_store as store,c.purchased_at as purchased from gift_claim c join gift g on g.id=c.gift_id where g.wishlist_id=? and c.status='purchased' order by c.purchased_at desc nulls last,c.id limit ? offset ?",id,size,page*size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("items",rows,"total",total,"page",page,"size",size));
    }
    @DeleteMapping("/{claimId}/details") @ResponseStatus(HttpStatus.NO_CONTENT) void erase(Authentication auth,@PathVariable UUID id,@PathVariable UUID claimId){
        service.get(Api.owner(auth),id);
        db.update("update gift_claim set guest_name=null,guest_email=null,gift_message=null,purchase_store=null where id=? and gift_id in(select id from gift where wishlist_id=?)",claimId,id);
    }
}
