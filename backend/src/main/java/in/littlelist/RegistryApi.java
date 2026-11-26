package in.littlelist;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1")
class RegistryApi {
    private final JdbcTemplate db; private final WishlistService service; private final ObjectMapper json; private final JourneyApi journeys;
    RegistryApi(JdbcTemplate db,WishlistService service,ObjectMapper json,JourneyApi journeys){this.db=db;this.service=service;this.json=json;this.journeys=journeys;}
    record Starter(String key,String name,String category,int quantity,List<String> collections) {}
    static final List<Starter> STARTERS=List.of(
        new Starter("swaddles","Cotton swaddles","Sleep",2,List.of("essentials","budget","twins")),
        new Starter("sheets","Fitted cot sheets","Sleep",2,List.of("essentials","twins")),
        new Starter("muslins","Muslin cloths","Feeding",4,List.of("essentials","budget","second","twins")),
        new Starter("bibs","Washable bibs","Feeding",3,List.of("essentials","budget","second")),
        new Starter("clothes","Everyday cotton clothes","Clothing",3,List.of("essentials","budget","second","twins")),
        new Starter("towels","Soft bath towels","Bath & care",2,List.of("essentials","budget","twins")),
        new Starter("mat","Washable changing mat","Everyday essentials",1,List.of("essentials","budget","second")),
        new Starter("bag","Out-and-about bag","Travel",1,List.of("essentials","twins")),
        new Starter("books","Board books","Play",2,List.of("essentials","second")),
        new Starter("diapers","Nappies or diapers","Diapering",1,List.of("essentials","budget","twins")),
        new Starter("wipes","Gentle baby wipes","Diapering",1,List.of("essentials","budget")),
        new Starter("storage","Nursery storage baskets","Nursery & decor",1,List.of("essentials")),
        new Starter("stroller","A stroller for outings","Baby gear",1,List.of("essentials")),
        new Starter("thermometer","Digital thermometer","Health & safety",1,List.of("essentials")),
        new Starter("meal","A home-cooked meal","Help & experiences",1,List.of("budget","second"))
    );
    record Draft(@Min(1) @Max(5) int step,@Size(max=40) String chapter,@Size(max=100) String title,
                 @Size(max=80) String occasion,@Size(max=500) String greeting,@Size(max=20) String collection,
                 @NotNull @Size(max=20) List<@NotBlank @Size(max=40) String> selected, @Valid JourneyApi.Journey journey) {
        Draft(int step,String chapter,String title,String occasion,String greeting,String collection,List<String> selected){this(step,chapter,title,occasion,greeting,collection,selected,null);}
    }
    record Complete(@NotNull UUID requestId,@NotNull @Valid Draft draft) {}
    record Decision(@NotBlank @Pattern(regexp="undecided|need|have|not-needed") String state) {}
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
    @GetMapping("/templates") List<Starter> templates(){return STARTERS;}
    @GetMapping("/onboarding") Object draft(Authentication auth){
        var rows=db.queryForList("select draft from onboarding where owner_subject=?",Api.owner(auth));
        if(rows.isEmpty())return new Draft(1,"","","","","empty",List.of());
        try{return json.readValue((String)rows.getFirst().get("draft"),Draft.class);}catch(Exception e){throw new IllegalStateException(e);}
    }
    @PutMapping("/onboarding") @Transactional Draft save(Authentication auth,@Valid @RequestBody Draft draft){
        String owner=Api.owner(auth);db.queryForList("select pg_advisory_xact_lock(hashtext(?))",owner);
        db.update("insert into onboarding(owner_subject,draft) values(?,?) on conflict(owner_subject) do update set draft=excluded.draft",owner,encode(draft));return draft;
    }
    @PostMapping("/onboarding/complete") @Transactional WishlistService.Wishlist complete(Authentication auth,@Valid @RequestBody Complete input){
        String owner=Api.owner(auth);db.queryForList("select pg_advisory_xact_lock(hashtext(?))",owner);
        String hash=WishlistService.hash(encode(input.draft()));
        var prior=db.queryForList("select * from onboarding_result where owner_subject=? and request_id=?",owner,input.requestId());
        if(!prior.isEmpty()){
            if(!hash.equals(prior.getFirst().get("body_hash")))throw new ResponseStatusException(HttpStatus.CONFLICT,"This setup request was already completed with different details.");
            return service.get(owner,(UUID)prior.getFirst().get("wishlist_id"));
        }
        Draft draft=input.draft();
        if(draft.title()==null || draft.title().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Give your registry a title.");
        Set<String> selected=new HashSet<>(draft.selected());
        if(!STARTERS.stream().map(Starter::key).toList().containsAll(selected))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose items from the starter checklist.");
        var list=service.create(owner,new Api.ListInput(draft.title(),draft.occasion(),draft.greeting()));
        if(draft.journey()!=null)journeys.save(list.id(),draft.journey());
        for(var item:STARTERS)if(selected.contains(item.key()))service.saveGift(owner,list.id(),null,new Api.GiftInput(item.name(),item.category(),"Edit this suggestion to suit your family.",null,null,item.quantity(),"","nice-to-have","template",null));
        db.update("insert into onboarding_result(owner_subject,request_id,body_hash,wishlist_id) values(?,?,?,?)",owner,input.requestId(),hash,list.id());
        db.update("delete from onboarding where owner_subject=?",owner);
        return service.get(owner,list.id());
    }
    @GetMapping("/wishlists/{id}/checklist") @Transactional Map<String,String> checklist(Authentication auth,@PathVariable UUID id){
        service.get(Api.owner(auth),id);Map<String,String> result=new LinkedHashMap<>();STARTERS.forEach(s->result.put(s.key(),"undecided"));
        db.queryForList("select item_key,state from checklist_entry where wishlist_id=?",id).forEach(r->result.put((String)r.get("item_key"),(String)r.get("state")));return result;
    }
    @PatchMapping("/wishlists/{id}/checklist/{key}") @Transactional Map<String,String> decide(Authentication auth,@PathVariable UUID id,@PathVariable String key,@Valid @RequestBody Decision input){
        service.get(Api.owner(auth),id);
        if(STARTERS.stream().noneMatch(s->s.key().equals(key)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Checklist item not found.");
        db.update("insert into checklist_entry(wishlist_id,item_key,state) values(?,?,?) on conflict(wishlist_id,item_key) do update set state=excluded.state",id,key,input.state());return checklist(auth,id);
    }
}
