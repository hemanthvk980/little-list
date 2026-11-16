package in.littlelist;
import java.net.URI;
import java.util.Locale;
final class Retailer {
    static String merchant(String value) {
        if(value==null || value.isBlank()) return "Local shop / no link";
        try {
            String host=URI.create(value).getHost().toLowerCase(Locale.ROOT);
            if(host.equals("amazon.in")||host.endsWith(".amazon.in")||host.equals("amzn.in"))return "Amazon India";
            if(host.equals("flipkart.com")||host.endsWith(".flipkart.com")||host.equals("fkrt.it"))return "Flipkart";
            if(host.equals("firstcry.com")||host.endsWith(".firstcry.com"))return "FirstCry";
            return "Other store";
        } catch(Exception e) { return "Other store"; }
    }
}
