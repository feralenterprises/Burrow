package gg.feral.tpt.shell;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

/**
 * W08E candidate downloader, primary page-image lookup and fallback file title. No user accounts, no RuneLite calls, no automatic approvals.
 * Fetches from the OSRS Wiki MediaWiki API, and writes to UNAPPROVED staging only.
 * Source metadata is recorded but does not establish a redistribution license.
 */
public final class WikiBossArtFetcher {
    private static final String[] BOSSES = {"scurrius", "sarachnis", "bryophyta"};
    private static final String ROOT = "https://oldschool.runescape.wiki";
    private static final Path STAGING = Path.of("artwork-review","wiki-staged-unapproved");
    private static final long LIMIT = 8L*1024*1024;
    private static final Pattern URL = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
    private static final Pattern MIME = Pattern.compile("\\\"mime\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final byte[] PNG = {(byte)137,80,78,71,13,10,26,10};

    private WikiBossArtFetcher() {}
    private static String unescape(String s){return s.replace("\\/","/").replace("\\\\","\\");}
    private static URI parseApiImage(String json) throws Exception {
        if(json.contains("\"missing\":true") || json.contains("\"missing\":\"\""))throw new IOException("File title not present on OSRS Wiki");
        Matcher mime=MIME.matcher(json);if(!mime.find()||!mime.group(1).equalsIgnoreCase("image/png"))throw new IOException("Wiki returned no PNG imageinfo");
        Matcher m=URL.matcher(json);if(!m.find())throw new IOException("Wiki API did not return an original image URL");
        return validateImageUrl(unescape(m.group(1)));
    }
    /** Look up the actual page's lead image, instead of assuming File:<Boss>.png. */
    private static String pageImageApiUrl(String boss) {
        String title=Character.toUpperCase(boss.charAt(0))+boss.substring(1);
        return ROOT+"/api.php?action=query&prop=pageimages&titles="+URLEncoder.encode(title,StandardCharsets.UTF_8)+"&piprop=original&format=json&formatversion=2";
    }
    private static URI parsePageImage(String json) throws Exception {
        // pageimages => pages[].original.source; do not accidentally use a different link.
        int start=json.indexOf("\"original\"");
        if(start<0)throw new IOException("Boss page did not provide a lead image");
        int end=json.indexOf('}', start);
        if(end<0)throw new IOException("Incomplete pageimages metadata");
        String block=json.substring(start,end);
        Pattern source=Pattern.compile("\"source\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher m=source.matcher(block);
        if(!m.find())throw new IOException("Lead image metadata missing original source");
        return validateImageUrl(unescape(m.group(1)));
    }
    private static URI validateImageUrl(String raw) throws Exception {
        URI u=URI.create(raw);
        if(!"https".equals(u.getScheme()) || !Set.of("oldschool.runescape.wiki", "runescape.wiki").contains(u.getHost()))
            throw new SecurityException("Untrusted image host");
        if(!u.getPath().toLowerCase(Locale.ROOT).endsWith(".png"))
            throw new IOException("Lead image is not PNG; needs manual source review");
        return u;
    }
    private static byte[] readLimited(InputStream stream) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n;
        try(InputStream in=stream){while((n=in.read(b))!=-1){if((long)out.size()+n>LIMIT)throw new IOException("Image exceeds 8 MiB");out.write(b,0,n);}}
        return out.toByteArray();
    }
    private static BufferedImage checkImage(byte[] b)throws Exception {
        if(b.length<100||b.length>LIMIT)throw new IOException("Empty or oversized image");
        for(int i=0;i<PNG.length;i++)if(b[i]!=PNG[i])throw new IOException("Not PNG signature");
        BufferedImage im=ImageIO.read(new ByteArrayInputStream(b));
        if(im==null||im.getWidth()<128||im.getHeight()<128||im.getWidth()>4096||im.getHeight()>4096)throw new IOException("Image must be 128–4096px on both axes");
        if(!im.getColorModel().hasAlpha())throw new IOException("Boss model image lacks transparency");
        int clear=0,all=0;
        for(int y=0;y<im.getHeight();y+=Math.max(1,im.getHeight()/128))for(int x=0;x<im.getWidth();x+=Math.max(1,im.getWidth()/128)){
            all++;if(((im.getRGB(x,y)>>>24)&255)<230)clear++;
        }
        if(clear*10<all)throw new IOException("Transparent background less than 10%");
        return im;
    }
    private static String sha256(byte[] b)throws Exception {
        byte[] d=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder out=new StringBuilder();for(byte x:d)out.append(String.format("%02x",x&255));return out.toString();
    }
    private static String apiUrl(String boss) {
        String filename=Character.toUpperCase(boss.charAt(0))+boss.substring(1)+".png";
        String q="File:"+filename;
        return ROOT+"/api.php?action=query&prop=imageinfo&titles="+URLEncoder.encode(q, StandardCharsets.UTF_8)+"&iiprop=url%7Csize%7Cmime%7Csha1&format=json&formatversion=2";
    }
    private static byte[] fetch(HttpClient client, URI uri)throws Exception {
        HttpRequest req=HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(18)).header("User-Agent","FeralEnterprises-TrashPanda-UI-art-review/0.1 (standalone personal prototype)").build();
        HttpResponse<InputStream> resp=client.send(req,HttpResponse.BodyHandlers.ofInputStream());
        if(resp.statusCode()!=200){resp.body().close();throw new IOException("HTTP "+resp.statusCode());}
        return readLimited(resp.body());
    }
    private static void stage(String boss,URI url,byte[] png,BufferedImage im)throws Exception {
        Files.createDirectories(STAGING);
        Path target=STAGING.resolve(boss+"-wiki-candidate.png");
        Path manifest=STAGING.resolve(boss+"-wiki-candidate.properties");
        // Files never move into approved-models or the compiled JAR.
        Path temp=Files.createTempFile(STAGING,boss+"-incoming-", ".png");
        try {
            Files.write(temp,png);
            try {Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
        String meta="# UNAPPROVED wiki image. OWNER REVIEW + RIGHTS REVIEW REQUIRED.\n"+
            "boss="+boss+"\napproved=false\nrights_cleared=false\n"+
            "source_page="+ROOT+"/w/"+Character.toUpperCase(boss.charAt(0))+boss.substring(1)+"\n"+
            "exact_image_url="+url+"\nsha256="+sha256(png)+"\n"+
            "width="+im.getWidth()+"\nheight="+im.getHeight()+"\n"+
            "instruction=Open 04-REVIEW-BOSS-ART-WINDOWS.cmd and review this candidate\n";
        Files.writeString(manifest,meta,StandardCharsets.UTF_8);
    }
    /** Download lead image first, exact filename fallback; both remain UNAPPROVED. */
    static String fetchOne(String boss) throws Exception {
        if (!Set.of(BOSSES).contains(boss)) throw new IllegalArgumentException("Unsupported boss id");
        HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(8)).build();
        URI source;
        String resolution;
        try {
            String json=new String(fetch(client,URI.create(pageImageApiUrl(boss))),StandardCharsets.UTF_8);
            source=parsePageImage(json);
            resolution="pageimages lead image";
        } catch(IOException pageImageFailure) {
            // A rejected or absent lead image can fall back to an exact File:Boss.png.
            // A connection failure is reported directly rather than issuing redundant requests.
            if(pageImageFailure instanceof java.net.ConnectException)
                throw new IOException("Cannot connect to OSRS Wiki API; neither model nor rights verified",pageImageFailure);
            try {
                String json=new String(fetch(client,URI.create(apiUrl(boss))),StandardCharsets.UTF_8);
                source=parseApiImage(json);
                resolution="File:<Boss>.png fallback";
            } catch(Exception fallbackFailure) {
                throw new IOException("No approved-source PNG candidate: pageimages: "+pageImageFailure.getMessage()+"; filename fallback: "+fallbackFailure.getMessage(),fallbackFailure);
            }
        }
        byte[] png=fetch(client,source);
        BufferedImage im=checkImage(png);
        stage(boss,source,png,im);
        return boss+" | "+resolution+" | "+im.getWidth()+"x"+im.getHeight()+" | SHA-256 "+sha256(png).substring(0,16)+"... | UNAPPROVED";
    }
    private static void runFetch() {
        int staged=0;
        for(String boss:BOSSES){
            System.out.println("[CHECK] "+boss+" from OSRS Wiki (UNAPPROVED only)");
            try{
                System.out.println("[STAGED] "+fetchOne(boss));staged++;
            }catch(Exception ex){System.out.println("[NOT STAGED] "+boss+": "+ex.getClass().getSimpleName()+" · "+ex.getMessage());}
        }
        System.out.println("[RESULT] "+staged+"/3 image-shape candidates staged, NONE approved or installed.");
        if(staged<3)System.out.println("[HOLD] Offline, blocked API, or unsuitable source image. The current OSRS sprite fallback stays.");
    }
    private static void selfTest()throws Exception {
        String fixture="{\"query\":{\"pages\":[{\"imageinfo\":[{\"mime\":\"image/png\",\"url\":\"https:\\/\\/oldschool.runescape.wiki\\/images\\/Scurrius.png\"}]}]}}";
        if(!parseApiImage(fixture).toString().equals("https://oldschool.runescape.wiki/images/Scurrius.png"))throw new AssertionError("Imageinfo URL fixture failed");
        String pageFixture="{\"query\":{\"pages\":[{\"title\":\"Scurrius\",\"original\":{\"source\":\"https:\\/\\/oldschool.runescape.wiki\\/images\\/Example_Scurrius_render.png\",\"width\":800,\"height\":720}}]}}";
        if(!parsePageImage(pageFixture).toString().endsWith("Example_Scurrius_render.png"))throw new AssertionError("Pageimages original model URL resolution failed");
        if(!pageImageApiUrl("scurrius").contains("prop=pageimages"))throw new AssertionError("Lead image lookup not used");
        boolean invalidPageRejected=false;
        try{parsePageImage("{\"original\":{\"source\":\"https://evil.example/Scurrius.png\"}}");}catch(SecurityException expected){invalidPageRejected=true;}
        if(!invalidPageRejected)throw new AssertionError("Untrusted lead image host accepted");

        for(String malicious:new String[]{"{\"mime\":\"image/png\",\"url\":\"https://evil.example/Scurrius.png\"}","{\"mime\":\"image/jpeg\",\"url\":\"https://oldschool.runescape.wiki/images/Scurrius.png\"}"}){
            boolean rejected=false;try{parseApiImage(malicious);}catch(Exception expected){rejected=true;}if(!rejected)throw new AssertionError("Untrusted imageinfo accepted");
        }
        BufferedImage im=new BufferedImage(200,220,BufferedImage.TYPE_INT_ARGB);Graphics2D g=im.createGraphics();g.setColor(Color.GREEN);g.fillRect(40,40,100,100);g.dispose();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(im,"PNG",bytes);if(checkImage(bytes.toByteArray()).getWidth()!=200)throw new AssertionError("PNG fixture rejected");
        if(sha256(bytes.toByteArray()).length()!=64)throw new AssertionError("Checksum failure");
        byte[] tiny=new byte[PNG.length];System.arraycopy(PNG,0,tiny,0,tiny.length);
        boolean rejected=false;try{checkImage(tiny);}catch(Exception expected){rejected=true;}if(!rejected)throw new AssertionError("Tiny PNG accepted");
        for(String boss:BOSSES)if(Files.exists(Path.of("assets","verified-models",boss+"-model.png")))throw new AssertionError("Unapproved models in approved path");
        boolean invalidBossRejected=false;try {fetchOne("../../scurrius");}catch(IllegalArgumentException ok){invalidBossRejected=true;}
        if(!invalidBossRejected)throw new AssertionError("Invalid boss name accepted");
        boolean opaqueRejected=false;
        BufferedImage opaque=new BufferedImage(200,220,BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream opaqueData=new ByteArrayOutputStream();ImageIO.write(opaque,"PNG",opaqueData);
        try {checkImage(opaqueData.toByteArray());}catch(IOException expected){opaqueRejected=true;}
        if(!opaqueRejected)throw new AssertionError("Opaque image accepted");
        System.out.println("PASS W08E: lead-page original lookup, secure fallback, URL allowlist, boss allowlist, PNG/transparency, SHA-256, no autoapproval");
    }
    public static void main(String[] args)throws Exception{
        if(args.length==1&&args[0].equals("--self-test")){selfTest();return;}
        if(args.length==1&&args[0].equals("--fetch-all")){runFetch();return;}
        System.out.println("Usage: java ...WikiBossArtFetcher --fetch-all | --self-test");
    }
}
