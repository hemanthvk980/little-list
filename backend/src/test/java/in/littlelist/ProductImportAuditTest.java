package in.littlelist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
/** Opt-in network diagnostic, not a deterministic CI test or product seed. */
@EnabledIfSystemProperty(named="retailer.audit",matches="true")
class ProductImportAuditTest {
 @Test void measure() throws Exception {
  var json=new ObjectMapper();var preview=new ProductPreview(json);
  var products=json.readTree(Files.readString(Path.of(System.getProperty("audit.input"))));
  List<Map<String,Object>> results=new ArrayList<>();
  for(var product:products){String asin=product.path("asin").asText();long start=System.nanoTime();var row=new LinkedHashMap<String,Object>();row.put("asin",asin);
   try{var p=preview.fetch("https://www.amazon.in/dp/"+asin);row.put("name",p.name());row.put("price",p.price());row.put("image",p.imageUrl());row.put("message",p.message());}catch(Exception e){row.put("error",e.getClass().getSimpleName());}
   row.put("ms",(System.nanoTime()-start)/1000000);results.add(row);
  }
  Files.writeString(Path.of(System.getProperty("audit.output")),json.writerWithDefaultPrettyPrinter().writeValueAsString(results));
 }
}
