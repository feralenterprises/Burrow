package gg.feral.tpt.shell;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Independent source-artwork review station for the TPT UI shell.
 * It NEVER writes to assets/verified-models and NEVER approves an asset.
 * This prevents an arbitrary downloaded picture from being marked genuine.
 */
public final class BossArtReviewer {
    private static final String[] NAMES={"scurrius","sarachnis","bryophyta"};
    private static final Color BG=new Color(25,27,27), PANEL=new Color(35,37,36), GOLD=new Color(204,163,96), WHITE=new Color(234,228,214), MUTED=new Color(180,176,164);
    private static final byte[] PNG={(byte)137,80,78,71,13,10,26,10};
    private JFrame window;
    private JPanel canvas;
    private JComboBox<String> chooser;
    private JTextField url;
    private JTextArea rights;
    private JLabel status, large, actual, fallback, details;
    private JButton stage;
    private Path selected;
    private BufferedImage image;
    private String digest;

    private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format("%02x",x&255));return b.toString();}
    private static BufferedImage validate(Path path) throws Exception {
        if(path==null||!Files.isRegularFile(path))throw new IllegalArgumentException("Choose a PNG file first");
        if(Files.size(path)<100||Files.size(path)>8*1024*1024)throw new IllegalArgumentException("PNG must be between 100B and 8MB");
        byte[] head=new byte[8];try(InputStream in=Files.newInputStream(path)){if(in.read(head)!=8||!Arrays.equals(head,PNG))throw new IllegalArgumentException("File is not a PNG");}
        BufferedImage im=ImageIO.read(path.toFile());
        if(im==null||im.getWidth()<128||im.getHeight()<128||im.getWidth()>4096||im.getHeight()>4096)throw new IllegalArgumentException("Need a 128-4096px PNG model image");
        if(!im.getColorModel().hasAlpha())throw new IllegalArgumentException("Need a transparent OSRS model render, not an opaque screenshot");
        int transparent=0,total=0;
        for(int y=0;y<im.getHeight();y+=Math.max(1,im.getHeight()/128)) for(int x=0;x<im.getWidth();x+=Math.max(1,im.getWidth()/128)) {
            total++;if(((im.getRGB(x,y)>>>24)&255)<230)transparent++;
        }
        if(transparent*10<total)throw new IllegalArgumentException("At least 10% of the art should be transparent background");
        return im;
    }
    private static String digest(Path file) throws Exception {return hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
    private static BufferedImage fitted(BufferedImage im,int w,int h){
        BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        if(im==null)return out;
        Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scale=Math.min((w-2.0)/im.getWidth(),(h-2.0)/im.getHeight());
        int iw=Math.max(1,(int)Math.round(im.getWidth()*scale)),ih=Math.max(1,(int)Math.round(im.getHeight()*scale));
        g.drawImage(im,(w-iw)/2,(h-ih)/2,iw,ih,null);g.dispose();return out;
    }
    private static JLabel label(String s,int size,Color c){JLabel l=new JLabel(s);l.setFont(new Font("Dialog",Font.PLAIN,size));l.setForeground(c);return l;}
    private static JPanel box(int w,int h){JPanel p=new JPanel(new BorderLayout());p.setBackground(PANEL);p.setPreferredSize(new Dimension(w,h));p.setBorder(BorderFactory.createLineBorder(new Color(109,86,57)));return p;}
    private static JPanel row(Component... parts){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,8,2));p.setOpaque(false);int height=28;for(Component part:parts){p.add(part);height=Math.max(height,part.getPreferredSize().height+5);}p.setMaximumSize(new Dimension(540,height));p.setAlignmentX(Component.LEFT_ALIGNMENT);return p;}
    private static JLabel imageLabel(int w,int h){JLabel l=new JLabel("Awaiting authentic PNG",SwingConstants.CENTER);l.setForeground(MUTED);l.setPreferredSize(new Dimension(w,h));return l;}
    private static JLabel title(String s){JLabel l=label(s,17,GOLD);l.setFont(l.getFont().deriveFont(Font.BOLD));return l;}
    private static BufferedImage fallback(String boss){
        try(InputStream in=BossArtReviewer.class.getResourceAsStream("/assets/"+boss+"-osrs.png")) {
            return in==null?null:ImageIO.read(in);
        }catch(Exception ex){return null;}
    }
    private JPanel build(){
        JPanel root=new JPanel();root.setBackground(BG);root.setBorder(new EmptyBorder(14,16,12,16));root.setLayout(new BoxLayout(root,BoxLayout.Y_AXIS));root.setPreferredSize(new Dimension(560,770));
        JLabel head=title("TrashPanda Tracker | Wave 8E: Boss Art Review");head.setAlignmentX(Component.LEFT_ALIGNMENT);root.add(head);
        root.add(row(label("LOCAL REVIEW ONLY",11,GOLD),label("No TPT account or bank connection",11,MUTED)));
        root.add(Box.createVerticalStrut(12));
        JPanel pick=row(label("Boss",13,WHITE),chooser=new JComboBox<>(new String[]{"Scurrius","Sarachnis","Bryophyta"}));
        JButton load=new JButton("Load local PNG");pick.add(load);root.add(pick);
        JPanel wikiRow=row();
        JButton fetchWiki=new JButton("Fetch Wiki lead image");wikiRow.add(fetchWiki);
        JButton wiki=new JButton("Review Wiki candidate");wikiRow.add(wiki);root.add(wikiRow);
        root.add(Box.createVerticalStrut(4));
        JPanel visual=row();
        JPanel nativeBox=box(118,150);nativeBox.add(label("225px slot preview",11,WHITE),BorderLayout.NORTH);
        actual=imageLabel(61,69);JPanel slot=row(actual);nativeBox.add(slot,BorderLayout.CENTER);visual.add(nativeBox);
        JPanel expanded=box(185,214);expanded.add(label("Enlarged inspection",11,WHITE),BorderLayout.NORTH);
        large=imageLabel(170,169);expanded.add(large,BorderLayout.CENTER);visual.add(expanded);
        JPanel old=box(145,150);old.add(label("Current OSRS sprite",11,WHITE),BorderLayout.NORTH);
        fallback=imageLabel(61,69);old.add(row(fallback),BorderLayout.CENTER);visual.add(old);visual.setMaximumSize(new Dimension(540,228));visual.setPreferredSize(new Dimension(522,228));root.add(visual);
        root.add(Box.createVerticalStrut(11));
        details=label("No full-resolution model candidate loaded",12,MUTED);details.setAlignmentX(Component.LEFT_ALIGNMENT);root.add(details);
        root.add(Box.createVerticalStrut(11));
        root.add(label("Exact source URL (or local capture evidence)",12,WHITE));
        url=new JTextField();url.setMaximumSize(new Dimension(530,31));url.setToolTipText("Record the source of the exact bytes, not merely the wiki boss article");root.add(url);
        root.add(Box.createVerticalStrut(10));
        root.add(label("Rights/provenance explanation (required; not a permission grant)",12,WHITE));
        rights=new JTextArea(3,45);rights.setLineWrap(true);rights.setWrapStyleWord(true);rights.setBackground(PANEL);rights.setForeground(WHITE);rights.setCaretColor(WHITE);
        JScrollPane rscroll=new JScrollPane(rights);rscroll.setMaximumSize(new Dimension(530,78));root.add(rscroll);
        root.add(Box.createVerticalStrut(12));
        stage=new JButton("Stage review candidate (NOT approved)");stage.setEnabled(false);
        root.add(row(stage));
        root.add(Box.createVerticalStrut(8));
        status=label("Status: no model files approved",12,GOLD);root.add(status);
        root.add(Box.createVerticalStrut(14));
        JTextArea help=new JTextArea("Imported art stays UNAPPROVED. Check model identity, source, rights and checksum before requesting owner approval. No installed TPT files or approved assets change here.");
        help.setWrapStyleWord(true);help.setLineWrap(true);help.setOpaque(false);help.setForeground(MUTED);help.setFont(new Font("Dialog",Font.PLAIN,12));help.setEditable(false);help.setMaximumSize(new Dimension(520,90));root.add(help);
        load.addActionListener(ev->{JFileChooser file=new JFileChooser();file.setDialogTitle("Choose a genuine transparent OSRS boss model PNG");int v=file.showOpenDialog(window);if(v==JFileChooser.APPROVE_OPTION)loadFile(file.getSelectedFile().toPath());});
        wiki.addActionListener(ev->loadWikiCandidate());
        fetchWiki.addActionListener(ev->{
            final String boss=NAMES[chooser.getSelectedIndex()];
            fetchWiki.setEnabled(false);
            status.setText("Fetching original OSRS Wiki PNG for "+boss+" (unapproved)…");
            new SwingWorker<String,Void>() {
                protected String doInBackground() throws Exception{return WikiBossArtFetcher.fetchOne(boss);}
                protected void done(){
                    fetchWiki.setEnabled(true);
                    try{
                        String note=get();
                        if(NAMES[chooser.getSelectedIndex()].equals(boss)){
                            loadWikiCandidate();
                            if(image!=null)status.setText("Wiki candidate imported, NOT APPROVED: "+note);
                        }else status.setText("Wiki candidate staged for "+boss+". Select that boss to review.");
                    }catch(Exception failure){
                        Throwable cause=failure.getCause()==null?failure:failure.getCause();
                        status.setText("Fetch failed; existing sprites unchanged: "+cause.getClass().getSimpleName());
                    }
                }
            }.execute();
        });
        chooser.addActionListener(ev->{selected=null;image=null;digest=null;stage.setEnabled(false);refresh();});
        stage.addActionListener(ev->stage());
        refresh();return root;
    }
    private void loadWikiCandidate(){
        String boss=NAMES[chooser.getSelectedIndex()];
        Path staged=Path.of("artwork-review","wiki-staged-unapproved",boss+"-wiki-candidate.png");
        if(!Files.isRegularFile(staged)){
            status.setText("NO CANDIDATE: first run 06-FETCH-WIKI-BOSS-MODELS-WINDOWS.cmd");
            return;
        }
        loadFile(staged);
        if(image==null)return;
        Path info=staged.resolveSibling(boss+"-wiki-candidate.properties");
        try(InputStream in=Files.newInputStream(info)){
            Properties prop=new Properties();prop.load(in);
            url.setText(prop.getProperty("exact_image_url",""));
            rights.setText("");
            status.setText("Wiki candidate loaded, UNAPPROVED. Check rights before staging.");
        }catch(Exception e){status.setText("Image loaded, metadata missing: investigate source before review");}
    }
    private void loadFile(Path file){
        try{BufferedImage candidate=validate(file);selected=file;image=candidate;digest=digest(file);stage.setEnabled(true);status.setText("Candidate loaded for inspection (NOT approved)");refresh();}
        catch(Exception ex){selected=null;image=null;digest=null;stage.setEnabled(false);status.setText("REJECTED: "+ex.getMessage());refresh();}
    }
    private void refresh(){
        if(chooser==null||actual==null)return;
        String boss=NAMES[chooser.getSelectedIndex()];
        BufferedImage old=fallback(boss);fallback.setText(old==null?"missing":"");if(old!=null)fallback.setIcon(new ImageIcon(fitted(old,61,69)));
        if(image==null){actual.setIcon(null);actual.setText("No model");large.setIcon(null);large.setText("No candidate");details.setText("No full-resolution model candidate loaded");}
        else {actual.setText("");actual.setIcon(new ImageIcon(fitted(image,61,69)));large.setText("");large.setIcon(new ImageIcon(fitted(image,170,169)));details.setText(image.getWidth()+" × "+image.getHeight()+" PNG · SHA-256 "+digest.substring(0,18)+"…");}
    }
    private void stage(){
        if(image==null||selected==null){status.setText("REJECTED: No candidate image");return;}
        String original=url.getText().trim();String note=rights.getText().trim();
        if(!(original.startsWith("https://")||original.startsWith("local-capture:"))||note.length()<15){status.setText("REJECTED: exact URL/local capture source and detailed rights note required");return;}
        String boss=NAMES[chooser.getSelectedIndex()];
        try{
            Path target=Path.of("artwork-review","candidates");Files.createDirectories(target);
            Path png=target.resolve(boss+"-candidate.png");Files.copy(selected,png,StandardCopyOption.REPLACE_EXISTING);
            String contents="# UNAPPROVED artwork candidate — NEVER LOAD INTO TPT AS APPROVED\n"+
                "boss="+boss+"\nowner_approved=false\nsource_url="+original.replace("\\","\\\\")+"\n"+
                "rights_note="+note.replace("\n"," ").replace("\\","\\\\")+"\nsha256="+digest+"\n"+
                "size="+image.getWidth()+"x"+image.getHeight()+"\n"+
                "capture_date=2026-11-03\n";
            Files.writeString(target.resolve(boss+"-candidate.properties"),contents,StandardCharsets.UTF_8);
            status.setText("STAGED unapproved: artwork-review/candidates/"+boss+"-candidate.png");
        }catch(Exception ex){status.setText("Could not stage: "+ex.getMessage());}
    }
    private static void selfTest() throws Exception {
        Path temp=Files.createTempDirectory("tpt-w08c-review-");
        try {
            BufferedImage valid=new BufferedImage(200,240,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=valid.createGraphics();g.setColor(new Color(30,100,50));g.fillOval(20,10,150,200);g.dispose();
            Path image=temp.resolve("synthetic-qa.png");ImageIO.write(valid,"PNG",image.toFile());
            if(validate(image).getWidth()!=200)throw new AssertionError("Valid synthetic preview rejected");
            if(digest(image).length()!=64)throw new AssertionError("SHA-256 failed");
            BufferedImage tiny=new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);Path small=temp.resolve("tiny.png");ImageIO.write(tiny,"PNG",small.toFile());
            boolean rejected=false;try{validate(small);}catch(IllegalArgumentException yes){rejected=true;}
            if(!rejected)throw new AssertionError("Small sprite accepted as model");
            if(Files.exists(Path.of("assets","verified-models","scurrius-model.png")))throw new AssertionError("Approved model unexpectedly present");
            for(String boss:NAMES)if(fallback(boss)==null)throw new AssertionError("Missing authentic sprite fallback: "+boss);
            System.out.println("PASS W08C: artifact preview validation, transparent PNG checks, SHA-256, miniature sprite rejection, approved-art isolation");
        }finally{try(var s=Files.list(temp)){s.forEach(p->{try{Files.delete(p);}catch(Exception ignored){}});}Files.delete(temp);}
    }
    private static void render(Path path) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            try {
                BossArtReviewer reviewer=new BossArtReviewer();
                JFrame f=new JFrame();f.setUndecorated(true);f.setContentPane(reviewer.build());
                f.pack();f.setSize(560,770);f.setLocation(0,0);f.setVisible(true);
                f.validate();
                BufferedImage img=new BufferedImage(560,770,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=img.createGraphics();f.getContentPane().printAll(g);g.dispose();
                Files.createDirectories(path.getParent());ImageIO.write(img,"PNG",path.toFile());f.dispose();
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private void launch(){window=new JFrame("TPT | Artwork Review · Not Approved");window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);window.setContentPane(build());window.pack();window.setMinimumSize(new Dimension(560,700));window.setLocationRelativeTo(null);window.setVisible(true);}
    public static void main(String[] args)throws Exception {
        if(args.length>0&&args[0].equals("--self-test")){selfTest();return;}
        if(args.length>1&&args[0].equals("--render")){render(Path.of(args[1]));return;}
        SwingUtilities.invokeLater(()->new BossArtReviewer().launch());
    }
}
