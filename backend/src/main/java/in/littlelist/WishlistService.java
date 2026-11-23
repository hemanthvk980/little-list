package in.littlelist;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WishlistService {
    private final JdbcTemplate db;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final EmailDelivery mail;
    private final String webUrl, from;
    public WishlistService(JdbcTemplate db, EmailDelivery mail, com.fasterxml.jackson.databind.ObjectMapper json, @Value("${app.web-url}") String webUrl, @Value("${app.mail-from}") String from) {
        this.db=db; this.json=json; this.mail=mail; this.webUrl=webUrl; this.from=from;
    }
    public record Header(UUID id, String title, String occasion, UUID shareId) {}
    public record Gift(UUID id, String name, String category, String note, String retailerUrl, Integer pricePaise, int quantity, int held, int purchased, int available, String variant, String priority, String merchant, String captureSource, OffsetDateTime capturedAt, String imageUrl) {}
    public record Wishlist(UUID id, String title, String occasion, UUID shareId, List<Gift> items, String greeting, String shortCode, UUID photoVersion) {}
    public record Shared(String title, String occasion, List<Gift> items, String greeting, UUID photoVersion) {}
    public record Claim(UUID id, String name, String retailerUrl, String status, OffsetDateTime heldUntil, UUID giftId, UUID shareId) {}
    public record Delivery(String email, String token, String intent) { @Override public String toString() { return "Delivery[redacted]"; } }
    private static ResponseStatusException error(int status, String message) { return new ResponseStatusException(HttpStatus.valueOf(status), message); }
    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String secret() { byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes); return HexFormat.of().formatHex(bytes); }
    private Map<String,Object> row(String sql, Object... args) { var rows=db.queryForList(sql,args); if(rows.isEmpty()) throw error(404,"This list or gift is no longer available."); return rows.getFirst(); }
    private void lockList(UUID id) { row("select id from wishlist where id=? for update",id); }
    private void owner(String owner, UUID id) { row("select id from wishlist where id=? and owner_subject=? for update",id,owner); }
    private Header header(Map<String,Object> r) { return new Header((UUID)r.get("id"),(String)r.get("title"),(String)r.get("occasion"),(UUID)r.get("share_id")); }
    public List<Header> lists(String owner) { return db.queryForList("select * from wishlist where owner_subject=? order by created_at desc limit 100",owner).stream().map(this::header).toList(); }
    private List<Gift> gifts(UUID id) {
        return db.query("select g.*, count(c.id) filter(where c.status='held' and c.held_until>clock_timestamp()) held, count(c.id) filter(where c.status='purchased') purchased from gift g left join gift_claim c on c.gift_id=g.id where g.wishlist_id=? and not g.archived group by g.id order by g.created_at,g.id", (r,n)-> {
            int held=r.getInt("held"), purchased=r.getInt("purchased"), quantity=r.getInt("quantity");
            return new Gift(r.getObject("id",UUID.class),r.getString("name"),r.getString("category"),r.getString("note"),r.getString("retailer_url"),r.getObject("price_paise",Integer.class),quantity,held,purchased,quantity-held-purchased,r.getString("variant"),r.getString("priority"),Retailer.merchant(r.getString("retailer_url")),r.getString("capture_source"),r.getObject("created_at",OffsetDateTime.class),r.getString("image_url"));
        },id);
    }
    private Wishlist view(UUID id) { Header h=header(row("select * from wishlist where id=?",id)); return new Wishlist(id,h.title(),h.occasion(),h.shareId(),gifts(id),(String)row("select greeting from wishlist where id=?",id).get("greeting"),db.queryForObject("select (select code from registry_link where share_id=w.share_id) from wishlist w where id=?",String.class,id),(UUID)row("select photo_version from wishlist where id=?",id).get("photo_version")); }
    @Transactional public Wishlist get(String owner,UUID id) { owner(owner,id); return view(id); }
    @Transactional public Wishlist create(String owner,Api.ListInput input) {
        // Serialize per-owner creation to enforce the starter bound under concurrent requests.
        db.queryForList("select pg_advisory_xact_lock(hashtext(?))",owner);
        if(lists(owner).size()>=100) throw error(409,"You can have up to 100 lists.");
        UUID id=UUID.randomUUID(); db.update("insert into wishlist(id,owner_subject,title,occasion,greeting) values(?,?,?,?,?)",id,owner,input.title().trim(),input.occasion(),input.greeting()); return view(id);
    }
    @Transactional public Wishlist update(String owner,UUID id,Api.ListInput input) { owner(owner,id); db.update("update wishlist set title=?,occasion=?,greeting=? where id=?",input.title().trim(),input.occasion(),input.greeting(),id); return view(id); }
    @Transactional public void delete(String owner,UUID id) { owner(owner,id); db.update("delete from wishlist where id=?",id); }
    @Transactional public void deleteAccount(String owner) { db.queryForList("select pg_advisory_xact_lock(hashtext(?))",owner); db.queryForList("select id from wishlist where owner_subject=? order by id for update",owner); db.update("delete from wishlist where owner_subject=?",owner); db.update("delete from onboarding where owner_subject=?",owner); db.update("delete from onboarding_result where owner_subject=?",owner); db.update("delete from app_user where owner_subject=?",owner); }
    @Transactional public Wishlist publish(String owner,UUID id,boolean enabled) { owner(owner,id); db.update("update wishlist set share_id="+(enabled?"coalesce(share_id,gen_random_uuid())":"null")+" where id=?",id); if(enabled) { byte[] bytes=new byte[16]; new SecureRandom().nextBytes(bytes); String code=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); db.update("insert into registry_link(code,wishlist_id,share_id) select ?,id,share_id from wishlist where id=? on conflict(share_id) do nothing",code,id); } return view(id); }
    private void url(String value) {
        if(value==null || value.isBlank()) return;
        try { URI uri=URI.create(value); if(!Set.of("https","http").contains(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null) throw new IllegalArgumentException(); }
        catch(IllegalArgumentException e) { throw error(400,"Use a full http or https store URL without embedded credentials."); }
    }
    private int occupied(UUID id) { return db.queryForObject("select count(*) from gift_claim where gift_id=? and (status='purchased' or (status='held' and held_until>clock_timestamp()))",Integer.class,id); }
    @Transactional public Wishlist saveGift(String owner,UUID id,UUID gift,Api.GiftInput input) {
        owner(owner,id); url(input.retailerUrl());
        if(input.imageUrl()!=null && !input.imageUrl().isBlank() && ProductPreview.image(input.imageUrl()).isEmpty()) throw error(400,"Choose an HTTPS image from a supported retailer image host.");
        String fingerprint=hash(json.valueToTree(input).toString());
        if(gift==null && input.requestId()!=null) {
            var prior=db.queryForList("select body_hash from gift_save where wishlist_id=? and request_id=?",id,input.requestId());
            if(!prior.isEmpty()) { if(!fingerprint.equals(prior.getFirst().get("body_hash"))) throw error(409,"This save request was already used. Start a new save."); return view(id); }
        }
        UUID saved=gift==null?UUID.randomUUID():gift;
        if(gift==null) {
            if(gifts(id).size()>=100) throw error(409,"A list can contain up to 100 gifts.");
            db.update("insert into gift(id,wishlist_id,name,category,note,retailer_url,price_paise,quantity) values(?,?,?,?,?,?,?,?)",saved,id,input.name().trim(),input.category().trim(),input.note(),input.retailerUrl(),input.pricePaise(),input.quantity());
        } else {
            row("select id from gift where id=? and wishlist_id=? and not archived for update",gift,id);
            if(input.quantity()<occupied(gift)) throw error(409,"Quantity cannot be lower than purchased and actively held units.");
            db.update("update gift set name=?,category=?,note=?,retailer_url=?,price_paise=?,quantity=? where id=?",input.name().trim(),input.category().trim(),input.note(),input.retailerUrl(),input.pricePaise(),input.quantity(),gift);
        }
        db.update("update gift set variant=?,priority=?,capture_source=?,image_url=? where id=?",input.variant(),input.priority()==null?"nice-to-have":input.priority(),input.captureSource()==null?"manual":input.captureSource(),input.imageUrl(),saved);
        if(gift==null && input.requestId()!=null) db.update("insert into gift_save(wishlist_id,request_id,body_hash) values(?,?,?)",id,input.requestId(),fingerprint);
        return view(id);
    }
    @Transactional public Wishlist removeGift(String owner,UUID id,UUID gift) {
        owner(owner,id); row("select id from gift where id=? and wishlist_id=? and not archived for update",gift,id);
        if(occupied(gift)>0) throw error(409,"A gift with active holds or purchases cannot be removed.");
        db.update("update gift set archived=true where id=?",gift); return view(id);
    }
    public Shared shared(UUID share) { var r=row("select id,title,occasion,greeting,photo_version from wishlist where share_id=?",share); return new Shared((String)r.get("title"),(String)r.get("occasion"),gifts((UUID)r.get("id")),(String)r.get("greeting"),(UUID)r.get("photo_version")); }
    @Transactional public Delivery requestClaim(UUID share,UUID gift,String email) { return requestClaim(share,gift,email,"reserve"); }
    @Transactional public Delivery requestClaim(UUID share,UUID gift,String email,String intent) {
        return requestClaim(share,gift,email,intent,null,null,null);
    }
    @Transactional public Delivery requestClaim(UUID share,UUID gift,String email,String intent,String name,String message,String store) {
        if(!Set.of("reserve","purchase").contains(intent)) throw error(400,"Choose reserve or purchase.");
        String recipient=hash(email.trim().toLowerCase(Locale.ROOT));
        db.queryForList("select pg_advisory_xact_lock(hashtext(?))",recipient);
        var list=row("select id from wishlist where share_id=? for update",share);
        row("select id from gift where id=? and wishlist_id=? and not archived",gift,list.get("id"));
        if(occupied(gift)>=(Integer)row("select quantity from gift where id=?",gift).get("quantity"))throw error(409,"All units are already reserved or purchased.");
        int count=db.queryForObject("select count(*) from claim_request where created_at>clock_timestamp()-interval '15 minutes' and (recipient_hash=? or gift_id=?)",Integer.class,recipient,gift);
        if(count>=5) throw error(429,"Please wait 15 minutes before requesting another verification email.");
        String token=secret(); db.update("insert into claim_request(token_hash,gift_id,share_id,recipient_hash,intent,expires_at) values(?,?,?,?,?,clock_timestamp()+interval '30 minutes')",hash(token),gift,share,recipient,intent);
        db.update("update claim_request set guest_name=?,guest_email=?,gift_message=?,purchase_store=? where token_hash=?",name,email.trim(),message,store,hash(token));
        return new Delivery(email,token,intent);
    }
    public void sendRequest(Delivery delivery) {
        SimpleMailMessage message=new SimpleMailMessage(); message.setFrom(from); message.setTo(delivery.email()); message.setSubject("Confirm your Little List gift");
        message.setText((delivery.intent().equals("purchase")?"Open this link, then confirm you purchased one unit. This records your report, not a retailer-verified order.":"Open this link, then explicitly confirm to hold one unit for 48 hours.")+" Opening the link changes nothing.\n\n"+webUrl+"/confirm?intent="+delivery.intent()+"#token="+delivery.token()+"\n\nThis verification link expires in 30 minutes.");
        try { mail.send(message,"claim-"+hash(delivery.token())); } catch(org.springframework.mail.MailException e) { throw error(503,"We could not send your email. Please try again shortly."); }
    }
    @Transactional public Claim confirm(String token,String management) {
        var initial=row("select r.gift_id,g.wishlist_id from claim_request r join gift g on g.id=r.gift_id where r.token_hash=?",hash(token));
        lockList((UUID)initial.get("wishlist_id"));
        var gift=row("select * from gift where id=? for update",initial.get("gift_id"));
        var req=row("select *,expires_at>clock_timestamp() valid from claim_request where token_hash=? for update",hash(token));
        if(req.get("claim_id")!=null) {
            row("select id from gift_claim where id=? and token_hash=?",req.get("claim_id"),hash(management)); return claim(management);
        }
        if(!Boolean.TRUE.equals(req.get("valid"))) throw error(410,"This verification link expired. Request a new email.");
        row("select id from wishlist where id=? and share_id=?",initial.get("wishlist_id"),req.get("share_id"));
        if(Boolean.TRUE.equals(gift.get("archived"))) throw error(404,"This gift is no longer available.");
        if(occupied((UUID)gift.get("id")) >= (Integer)gift.get("quantity")) throw error(409,"Someone else just claimed the last one. Choose another gift.");
        if(!db.queryForList("select id from gift_claim where token_hash=?",hash(management)).isEmpty()) throw error(409,"Use a new management credential for this gift.");
        UUID id=UUID.randomUUID();
        db.update("insert into gift_claim(id,gift_id,token_hash,status,held_until) values(?,?,?,?,clock_timestamp()+interval '48 hours')",id,gift.get("id"),hash(management),"purchase".equals(req.get("intent"))?"purchased":"held");
        db.update("update gift_claim set guest_name=?,guest_email=?,gift_message=?,purchase_store=?,purchased_at=case when status='purchased' then clock_timestamp() else null end where id=?",req.get("guest_name"),req.get("guest_email"),req.get("gift_message"),req.get("purchase_store"),id);
        db.update("update claim_request set guest_name=null,guest_email=null,gift_message=null,purchase_store=null where token_hash=?",hash(token));
        db.update("update claim_request set consumed_at=clock_timestamp(),claim_id=? where token_hash=?",id,hash(token)); return claim(management);
    }
    public Claim claim(String token) {
        var claims=db.query("select c.*,g.name,g.retailer_url,w.share_id,case when c.status='held' and c.held_until<=clock_timestamp() then 'expired' else c.status end visible_status from gift_claim c join gift g on g.id=c.gift_id join wishlist w on w.id=g.wishlist_id where c.token_hash=?",(r,n)->new Claim(r.getObject("id",UUID.class),r.getString("name"),r.getString("retailer_url"),r.getString("visible_status"),r.getObject("held_until",OffsetDateTime.class),r.getObject("gift_id",UUID.class),r.getObject("share_id",UUID.class)),hash(token));
        if(claims.isEmpty()) throw error(404,"This claim is no longer available."); return claims.getFirst();
    }
    @Transactional public Claim record(String token,String status) {
        var r=row("select c.gift_id,g.wishlist_id from gift_claim c join gift g on g.id=c.gift_id where c.token_hash=?",hash(token));
        lockList((UUID)r.get("wishlist_id")); row("select id from gift where id=? for update",r.get("gift_id"));
        Claim c=claim(token); if(c.status().equals(status)) return c;
        if(c.status().equals("expired")) throw error(410,"Your hold expired. Open the wishlist to request a new hold, or contact the host if you already bought it.");
        if(!c.status().equals("held")) throw error(409,"This claim is already final and cannot be changed.");
        db.update("update gift_claim set status=?,purchased_at=case when ?='purchased' then clock_timestamp() else purchased_at end where id=?",status,status,c.id()); return claim(token);
    }
}
