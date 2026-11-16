package in.littlelist;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
class ShortLinks {
    private final JdbcTemplate db;
    ShortLinks(JdbcTemplate db) { this.db=db; }
    @Transactional
    public UUID resolve(String code, boolean count, String source) {
        if(!code.matches("[A-Za-z0-9_-]{22}")) throw missing();
        var rows=db.queryForList("select w.share_id from wishlist w join registry_link l on l.wishlist_id=w.id and l.share_id=w.share_id where l.code=? for update of w",code);
        if(rows.isEmpty()) throw missing();
        if(count) db.update("insert into registry_link_day(code,day,channel,opens) values(?,(clock_timestamp() at time zone 'Asia/Kolkata')::date,?,1) on conflict(code,day,channel) do update set opens=registry_link_day.opens+1",code,"qr".equals(source)?"qr":"whatsapp".equals(source)?"whatsapp":"direct");
        return (UUID)rows.getFirst().get("share_id");
    }
    public Map<String,Object> stats(String owner, UUID id) {
        if(db.queryForList("select id from wishlist where id=? and owner_subject=?",id,owner).isEmpty()) throw missing();
        var days=db.queryForList("select d.day,d.channel,sum(d.opens)::bigint opens from registry_link_day d join registry_link l on l.code=d.code where l.wishlist_id=? and d.day >= (current_timestamp at time zone 'Asia/Kolkata')::date-29 group by d.day,d.channel order by d.day desc,d.channel",id);
        Long total=db.queryForObject("select coalesce(sum(d.opens),0)::bigint from registry_link_day d join registry_link l on l.code=d.code where l.wishlist_id=?",Long.class,id);
        return Map.of("totalOpens",total,"days",days);
    }
    static boolean count(HttpServletRequest request) {
        String ua=Optional.ofNullable(request.getHeader("User-Agent")).orElse("").toLowerCase(Locale.ROOT);
        return request.getMethod().equals("GET") && request.getHeader("Purpose")==null && request.getHeader("Sec-Purpose")==null && !ua.matches(".*(bot|crawler|spider|preview|facebookexternalhit|whatsapp|slack|telegram|linkedin).* ".trim());
    }
    private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"This registry link is no longer available.");}
}

@RestController
class ShortLinkApi {
    private final ShortLinks links;
    ShortLinkApi(ShortLinks links){this.links=links;}
    @RequestMapping(value="/r/{code}",method={RequestMethod.GET,RequestMethod.HEAD})
    ResponseEntity<Void> open(@PathVariable String code,@RequestParam(required=false) String via,HttpServletRequest request){
        UUID share=links.resolve(code,ShortLinks.count(request),via);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/shared/"+share)).cacheControl(CacheControl.noStore()).header("X-Robots-Tag","noindex, nofollow").header("Referrer-Policy","no-referrer").build();
    }
    @GetMapping("/api/v1/wishlists/{id}/link-stats")
    Map<String,Object> stats(Authentication auth,@PathVariable UUID id){return links.stats(Api.owner(auth),id);}
}
