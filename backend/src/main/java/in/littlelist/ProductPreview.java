package in.littlelist;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Explicit user-initiated preview. No checkout, background scraping, cookies or CAPTCHA bypass. */
@RestController
@RequestMapping("/api/v1/products")
class ProductPreview {
    private static final Set<String> HOSTS=Set.of("amazon.in","www.amazon.in","m.amazon.in","amzn.in","amzn.to","flipkart.com","www.flipkart.com","dl.flipkart.com","fkrt.it","firstcry.com","www.firstcry.com");
    private static final Set<String> IMAGES=Set.of("m.media-amazon.com","images-na.ssl-images-amazon.com","images-eu.ssl-images-amazon.com","rukminim1.flixcart.com","rukminim2.flixcart.com","rukminim3.flixcart.com","cdn.fcglcdn.com","cdn.firstcry.com");
    private final ObjectMapper json;
    private final Semaphore slots=new Semaphore(4);
    ProductPreview(ObjectMapper json){this.json=json;}
    record Input(@NotBlank @Size(max=2048) String url){}
    record Preview(String name,String price,String variant,String imageUrl,String retailerUrl,String message){}
    @PostMapping("/preview")
    ResponseEntity<Preview> preview(@Valid @RequestBody Input input,HttpSession session){
        synchronized(session){
            long now=System.currentTimeMillis();
            Long start=(Long)session.getAttribute("previewStart");
            int count=start==null||now-start>60000?0:(Integer)session.getAttribute("previewCount");
            if(count>=10)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Please wait a minute before reviewing another link.");
            if(count==0)session.setAttribute("previewStart",now);
            session.setAttribute("previewCount",count+1);
        }
        Preview result;
        if(!slots.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Product lookup is busy. Please try again shortly.");
        try { result=fetch(input.url()); }
        catch(Exception e){result=empty(input.url(),"We couldn't retrieve this product. Add the details below or use the Chrome button on the product page.");}
        finally {slots.release();}
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
    static URI allowed(String value){
        URI u=URI.create(value);
        if(!"https".equals(u.getScheme())||u.getHost()==null||!HOSTS.contains(u.getHost().toLowerCase(Locale.ROOT))||u.getRawUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))throw new IllegalArgumentException("Unsupported URL");
        return u;
    }
    static String image(String value){
        try {URI u=URI.create(value);return value.length()<=2048&&"https".equals(u.getScheme())&&u.getHost()!=null&&IMAGES.contains(u.getHost().toLowerCase(Locale.ROOT))&&u.getRawUserInfo()==null&&u.getPort()==-1?u.toString():"";}catch(Exception e){return "";}
    }
    Preview fetch(String value)throws Exception{
        URI u=allowed(value);
        // Every redirect must stay on an exact retailer host; never fetch arbitrary user hosts.
        for(int hop=0;hop<4;hop++){
            for(InetAddress ip:InetAddress.getAllByName(u.getHost()))if(ip.isAnyLocalAddress()||ip.isLoopbackAddress()||ip.isLinkLocalAddress()||ip.isSiteLocalAddress()||ip.isMulticastAddress()||(ip.getAddress().length==16&&(ip.getAddress()[0]&0xfe)==0xfc))throw new IllegalArgumentException("Nonpublic address");
            var r=Jsoup.connect(u.toString()).userAgent("LittleList/1.0 product preview").timeout(3000).maxBodySize(1_000_000).followRedirects(false).ignoreHttpErrors(true).execute();
            if(r.statusCode()>=300&&r.statusCode()<400){u=allowed(u.resolve(r.header("Location")).toString());continue;}
            if(r.statusCode()!=200||r.contentType()==null||!r.contentType().toLowerCase(Locale.ROOT).contains("text/html"))return empty(value,"The store couldn't provide details. You can still add this gift manually.");
            return parse(r.parse(),u.toString());
        }
        return empty(value,"This link redirects too many times. Paste the full product URL instead.");
    }
    static Preview empty(String url,String message){return new Preview("","","","",url,message);}
    Preview parse(Document doc,String url){
        List<JsonNode> products=new ArrayList<>();
        for(var node:doc.select("script[type=application/ld+json]").stream().limit(20).toList()){
            if(node.data().length()>200000)continue;
            try{find(json.readTree(node.data()),products,0);}catch(Exception ignored){}
        }
        JsonNode p=products.size()==1?products.getFirst():json.createObjectNode();
        String name=text(p,"name"),price="",variant="",img="";
        var offer=p.path("offers");
        if(offer.isObject()&&"INR".equals(text(offer,"priceCurrency")))price=price(text(offer,"price"));
        var im=p.path("image");if(im.isArray())im=im.path(0);
        img=image(im.isTextual()?im.asText():text(im,"url"));
        variant=String.join(" · ",List.of(text(p,"size"),text(p,"color")).stream().filter(v->!v.isBlank()).toList());
        String host=URI.create(url).getHost();String titleSelector=null,priceSelector=null;
        if(host.endsWith("amazon.in")&&url.matches(".*/(?:dp|gp/product|gp/aw/d)/[A-Za-z0-9]{10}.*")){titleSelector="#productTitle";priceSelector="#corePriceDisplay_desktop_feature_div .a-price .a-offscreen";}
        else if(host.endsWith("flipkart.com")&&URI.create(url).getPath().contains("/p/")){titleSelector="h1";priceSelector=".Nx9bqj.CxhGGd";}
        else if(host.endsWith("firstcry.com")&&URI.create(url).getPath().contains("/product-detail")){titleSelector="h1";priceSelector="#prod_price";}
        if(products.size()>1)return empty(url,"Several products were found. Please choose the details for your selected item.");
        if(titleSelector!=null){String visible=doc.select(titleSelector).text();if(!visible.isBlank())name=visible;}
        if(name.isBlank()&&titleSelector!=null)name=meta(doc,"og:title");
        if(name.isBlank())return empty(url,"Product details weren't available. Add them below or use the Chrome button.");
        if(img.isBlank())img=image(meta(doc,"og:image"));
        if(img.isBlank()&&host.endsWith("amazon.in")){var landing=doc.selectFirst("#landingImage");if(landing!=null){img=image(landing.attr("data-old-hires"));if(img.isBlank())img=image(landing.attr("src"));}}
        if(price.isBlank()&&"INR".equals(meta(doc,"product:price:currency")))price=price(meta(doc,"product:price:amount"));
        if(price.isBlank()&&priceSelector!=null){String visible=doc.select(priceSelector).text();if(visible.matches("(?:₹|Rs\\.?|INR)\\s*[\\d,]+(?:\\.\\d{1,2})?"))price=price(visible.replaceFirst("^(?:₹|Rs\\.?|INR)\\s*",""));}
        if(host.endsWith("amazon.in")){String v=doc.select("#variation_size_name .selection, #variation_color_name .selection").eachText().stream().reduce((a,b)->a+" · "+b).orElse("");if(!v.isBlank())variant=v;}
        return new Preview(limit(name,120),price,limit(variant,120),img,url,"Details suggested by the store. Check the image, variant and price before saving; prices can change.");
    }
    private static void find(JsonNode n,List<JsonNode> result,int depth){
        if(n==null||depth>6||result.size()>1)return;
        if(n.isArray()){for(int i=0;i<Math.min(100,n.size());i++)find(n.get(i),result,depth+1);}
        else if(n.isObject()){var type=n.path("@type");if(type.asText().equals("Product")||(type.isArray()&&type.toString().contains("\"Product\"")))result.add(n);else find(n.get("@graph"),result,depth+1);}
    }
    static String price(String value){try{String v=value.replace(",","").trim();if(!v.matches("\\d+(\\.\\d{1,2})?"))return "";BigDecimal p=new BigDecimal(v);return p.compareTo(new BigDecimal("21474836.47"))<=0?p.toPlainString():"";}catch(Exception e){return "";}}
    private static String text(JsonNode n,String key){var v=n.path(key);return v.isValueNode()?v.asText(""):"";}
    private static String limit(String s,int max){return s.substring(0,Math.min(s.length(),max));}
    private static String meta(Document d,String key){var e=d.selectFirst("meta[property=\""+key+"\"],meta[name=\""+key+"\"]");return e==null?"":e.attr("content");}
}
