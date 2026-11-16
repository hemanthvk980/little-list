package in.littlelist;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("local")
@Testcontainers
class WishlistServiceTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);
    }
    @Autowired WishlistService service;
    @Autowired JdbcTemplate db;
    @Autowired RegistryApi registry;
    @Autowired ShortLinks shortLinks;
    @Autowired RegistryPhotos photos;
    @Test void directPurchaseRequiresConfirmationAndRetriesOnce() {
        var delivery=service.requestClaim(list.shareId(),gift,"bought@example.test","purchase");
        assertEquals(1,service.shared(list.shareId()).items().getFirst().available());
        String management=secret();var purchased=service.confirm(delivery.token(),management);
        assertEquals("purchased",purchased.status());assertEquals(purchased.id(),service.confirm(delivery.token(),management).id());
        assertEquals(1,service.shared(list.shareId()).items().getFirst().purchased());
        assertEquals(0,service.shared(list.shareId()).items().getFirst().available());
        status(400,()->service.requestClaim(list.shareId(),gift,"invalid@example.test","anything"));
    }
    @Test void directPurchaseAndReservationCompeteForLastUnit() throws Exception {
        String a=service.requestClaim(list.shareId(),gift,"a@example.test","purchase").token(),b=request();
        assertEquals(1,race(()->attempt(()->service.confirm(a,secret())),()->attempt(()->service.confirm(b,secret()))));
        var g=service.get("owner",list.id()).items().getFirst();assertEquals(1,g.held()+g.purchased());
    }
    @Test void photosAreBoundedOwnedAndRevokedWithSharing() throws Exception {
        var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("photo","ignored");
        var stranger=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("stranger","ignored");
        var own=service.create("local|photo",new Api.ListInput("Photo test",""));
        var image=new java.awt.image.BufferedImage(800,600,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        var file=new org.springframework.mock.web.MockMultipartFile("file","family.png","image/png",bytes.toByteArray());
        status(404,()->{try{photos.upload(stranger,own.id(),file);}catch(java.io.IOException e){throw new RuntimeException(e);}});
        var uploaded=photos.upload(auth,own.id(),file);assertNotNull(uploaded.photoVersion());
        byte[] stored=photos.own(auth,own.id()).getBody();var resized=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(stored));assertEquals(512,resized.getWidth());
        var published=service.publish("local|photo",own.id(),true);assertArrayEquals(stored,photos.shared(published.shareId()).getBody());
        service.publish("local|photo",own.id(),false);status(404,()->photos.shared(published.shareId()));
        assertNull(photos.remove(auth,own.id()).photoVersion());status(404,()->photos.own(auth,own.id()));
        status(400,()->RegistryPhotos.normalize("<svg></svg>".getBytes()));
        status(400,()->RegistryPhotos.normalize(new byte[5*1024*1024+1]));
    }
    @Test void shortLinkCountsAreAtomicAndRevocationIsImmediate() throws Exception {
        String code=list.shortCode(); assertTrue(code.matches("[A-Za-z0-9_-]{22}"));
        assertEquals(code,service.publish("owner",list.id(),true).shortCode());
        assertEquals(list.shareId(),shortLinks.resolve(code,false,null));
        assertEquals(0L,shortLinks.stats("owner",list.id()).get("totalOpens"));
        assertEquals(2,race(()->{shortLinks.resolve(code,true,"whatsapp");return true;},()->{shortLinks.resolve(code,true,"other");return true;}));
        assertEquals(2L,shortLinks.stats("owner",list.id()).get("totalOpens"));
        status(404,()->shortLinks.stats("intruder",list.id()));
        service.publish("owner",list.id(),false);status(404,()->shortLinks.resolve(code,true,null));
        var replacement=service.publish("owner",list.id(),true);assertNotEquals(code,replacement.shortCode());
        status(404,()->shortLinks.resolve(code,true,null));
        service.delete("owner",list.id());
        assertEquals(0,db.queryForObject("select count(*) from registry_link_day",Integer.class));
    }
    @Test void previewsAndHeadDoNotCountAsClicks() {
        var request=new org.springframework.mock.web.MockHttpServletRequest("GET","/r/example");
        request.addHeader("User-Agent","Mozilla/5.0");assertTrue(ShortLinks.count(request));
        request.setMethod("HEAD");assertFalse(ShortLinks.count(request));request.setMethod("GET");
        request.addHeader("Purpose","prefetch");assertFalse(ShortLinks.count(request));request.removeHeader("Purpose");
        request.removeHeader("User-Agent");request.addHeader("User-Agent","facebookexternalhit/1.1");assertFalse(ShortLinks.count(request));
    }
    WishlistService.Wishlist list;
    UUID gift;
    @BeforeEach void setup() {
        db.update("delete from wishlist");
        list=service.create("owner",new Api.ListInput("Baby shower","Godh bharai"));
        list=service.saveGift("owner",list.id(),null,input(1));gift=list.items().getFirst().id();
        list=service.publish("owner",list.id(),true);
    }
    Api.GiftInput input(int quantity) { return new Api.GiftInput("Cotton swaddle","Sleep","0–3 months","https://example.com/gift",99900,quantity); }
    String request() { return service.requestClaim(list.shareId(),gift,UUID.randomUUID()+"@example.test").token(); }
    String secret() { return UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-",""); }
    void status(int status,Runnable action) { assertEquals(status,assertThrows(ResponseStatusException.class,action::run).getStatusCode().value()); }
    int race(Callable<Boolean> a,Callable<Boolean> b) throws Exception {
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch ready=new CountDownLatch(2), go=new CountDownLatch(1);
            var tasks=List.of(a,b).stream().map(task->pool.submit(()->{ready.countDown();go.await();return task.call();})).toList();
            assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();
            int successes=0;for(var task:tasks)if(task.get(15,TimeUnit.SECONDS))successes++;return successes;
        }
    }
    boolean attempt(Runnable task) { try{task.run();return true;}catch(ResponseStatusException e){assertEquals(409,e.getStatusCode().value());return false;} }
    @Test void emailDoesNotAllocateAndConfirmationRetriesAreBound() {
        String token=request(), management=secret();assertEquals(1,service.shared(list.shareId()).items().getFirst().available());
        var claim=service.confirm(token,management);assertEquals(claim.id(),service.confirm(token,management).id());status(404,()->service.confirm(token,secret()));
        assertEquals(0,service.shared(list.shareId()).items().getFirst().available());
    }
    @Test void lastUnitRace() throws Exception {
        String a=request(),b=request();assertEquals(1,race(()->attempt(()->service.confirm(a,secret())),()->attempt(()->service.confirm(b,secret()))));
        assertEquals(1,db.queryForObject("select count(*) from gift_claim",Integer.class));
    }
    @Test void quantityEditCompetesWithClaim() throws Exception {
        service.saveGift("owner",list.id(),gift,input(2));service.confirm(request(),secret());String token=request();
        assertEquals(1,race(()->attempt(()->service.confirm(token,secret())),()->attempt(()->service.saveGift("owner",list.id(),gift,input(1)))));
        var g=service.get("owner",list.id()).items().getFirst();assertTrue(g.held()+g.purchased()<=g.quantity());
    }
    @Test void expiryFreesInventoryWithoutCleanupAndExpiredTokenFails() {
        String token=request();db.update("update claim_request set expires_at=clock_timestamp()-interval '1 second'");status(410,()->service.confirm(token,secret()));
        String management=secret();service.confirm(request(),management);db.update("update gift_claim set held_until=clock_timestamp()-interval '1 second'");
        assertEquals(1,service.shared(list.shareId()).items().getFirst().available());status(410,()->service.record(management,"purchased"));service.confirm(request(),secret());
    }
    @Test void revokedSharePreventsNewClaimsButExistingClaimsWork() {
        String token=request(),management=secret();service.confirm(request(),management);service.publish("owner",list.id(),false);
        status(404,()->service.shared(list.shareId()));status(404,()->service.confirm(token,secret()));
        assertEquals("purchased",service.record(management,"purchased").status());
        assertNotEquals(list.shareId(),service.publish("owner",list.id(),true).shareId());
    }
    @Test void ownershipDeletionAndQuantityRules() {
        status(404,()->service.get("other",list.id()));status(404,()->service.delete("other",list.id()));status(404,()->service.saveGift("other",list.id(),gift,input(2)));
        String management=secret();service.confirm(request(),management);status(409,()->service.removeGift("owner",list.id(),gift));
        service.delete("owner",list.id());status(404,()->service.claim(management));
    }
    @Test void purchaseAndReleaseAreIdempotentAndFinal() {
        String management=secret();service.confirm(request(),management);service.record(management,"purchased");assertEquals("purchased",service.record(management,"purchased").status());status(409,()->service.record(management,"released"));
        service.saveGift("owner",list.id(),gift,input(2));String second=secret();service.confirm(request(),second);service.record(second,"released");assertEquals("released",service.record(second,"released").status());status(409,()->service.record(second,"purchased"));
    }
    @Test void urlValidationAndRecipientLimit() {
        status(400,()->service.saveGift("owner",list.id(),gift,new Api.GiftInput("Test","Sleep","","javascript:alert(1)",0,1)));
        status(400,()->service.saveGift("owner",list.id(),gift,new Api.GiftInput("Test","Sleep","","https://user:password@example.com",0,1)));
        for(int i=0;i<5;i++)service.requestClaim(list.shareId(),gift,"guest@example.test");status(429,()->service.requestClaim(list.shareId(),gift,"guest@example.test"));
    }

    @Test void newGiftMetadataAndIdempotencyArePreserved() {
        UUID request=UUID.randomUUID();
        var input=new Api.GiftInput("New outfit","Clothing","Note","https://www.amazon.in/dp/B012345678",75000,2,"0–3 months · sage","essential","extension",request);
        var saved=service.saveGift("owner",list.id(),null,input);
        assertEquals(2,saved.items().size());
        assertEquals(2,service.saveGift("owner",list.id(),null,input).items().size());
        var gift=saved.items().stream().filter(g->g.name().equals("New outfit")).findFirst().orElseThrow();
        assertEquals("Amazon India",gift.merchant());assertEquals("essential",gift.priority());assertEquals("0–3 months · sage",gift.variant());
        status(409,()->service.saveGift("owner",list.id(),null,new Api.GiftInput("Different outfit","Clothing","Note",null,null,1,"","nice-to-have","manual",request)));
        assertEquals("Other store",Retailer.merchant("https://amazon.in.attacker.example/product"));
    }

    @Test void setupPersistsAndCompletionRetriesCreateOnlyOneRegistry() {
        var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("setup-host","unused");
        var draft=new RegistryApi.Draft(3,"Planning ahead","Our registry","Naamkaran","Thank you!","budget",List.of("swaddles","muslins"));
        registry.save(auth,draft);assertEquals(draft,registry.draft(auth));
        var input=new RegistryApi.Complete(UUID.randomUUID(),draft);
        var created=registry.complete(auth,input);assertEquals(2,created.items().size());
        assertEquals(created.id(),registry.complete(auth,input).id());
        assertEquals(1,service.lists("local|setup-host").size());
        assertEquals("Thank you!",created.greeting());
        var published=service.publish("local|setup-host",created.id(),true);
        assertEquals("Thank you!",service.shared(published.shareId()).greeting());
        assertFalse(service.shared(published.shareId()).toString().contains("Planning ahead"));
        status(409,()->registry.complete(auth,new RegistryApi.Complete(input.requestId(),new RegistryApi.Draft(3,"","Other title","","","empty",List.of()))));
        service.deleteAccount("local|setup-host");
        assertEquals(0,db.queryForObject("select count(*) from onboarding where owner_subject='local|setup-host'",Integer.class));
    }

    @Test void checklistIsPrivateAndDoesNotChangeInventory() {
        var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("check-host","unused");
        var another=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("another","unused");
        var owned=service.create("local|check-host",new Api.ListInput("Checklist",""));
        service.saveGift("local|check-host",owned.id(),null,input(1));
        assertEquals("have",registry.decide(auth,owned.id(),"swaddles",new RegistryApi.Decision("have")).get("swaddles"));
        assertEquals(1,service.get("local|check-host",owned.id()).items().getFirst().available());
        assertEquals(0,service.get("local|check-host",owned.id()).items().getFirst().purchased());
        status(404,()->registry.checklist(another,owned.id()));
        status(404,()->registry.decide(auth,owned.id(),"unrecognized",new RegistryApi.Decision("have")));
    }
}
