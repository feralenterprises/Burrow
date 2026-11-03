package gg.feral.burrow.core;

import java.util.*;

/** Minimal strict JSON reader for bundled, reviewed resources (no reflection, no network).
 *  Objects become LinkedHashMap, arrays ArrayList, numbers Double/Long, plus String/Boolean/null. */
public final class MiniJson {
    private final String s; private int i;
    private MiniJson(String s){this.s=s;}
    public static Object parse(String text) {
        MiniJson p=new MiniJson(Objects.requireNonNull(text));
        p.ws(); Object v=p.value(); p.ws();
        if(p.i!=p.s.length())throw p.err("trailing data");
        return v;
    }
    private Object value() {
        if(i>=s.length())throw err("eof");
        char c=s.charAt(i);
        switch(c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': lit("true"); return Boolean.TRUE;
            case 'f': lit("false"); return Boolean.FALSE;
            case 'n': lit("null"); return null;
            default: return number();
        }
    }
    private Map<String,Object> object() {
        Map<String,Object> m=new LinkedHashMap<>(); i++; ws();
        if(peek('}')){i++;return m;}
        while(true){ ws(); String k=string(); ws(); expect(':'); ws(); m.put(k,value()); ws();
            if(peek(',')){i++;continue;} expect('}'); return m; }
    }
    private List<Object> array() {
        List<Object> a=new ArrayList<>(); i++; ws();
        if(peek(']')){i++;return a;}
        while(true){ ws(); a.add(value()); ws(); if(peek(',')){i++;continue;} expect(']'); return a; }
    }
    private String string() {
        expect('"'); StringBuilder b=new StringBuilder();
        while(true) {
            if(i>=s.length())throw err("unterminated string");
            char c=s.charAt(i++);
            if(c=='"')return b.toString();
            if(c=='\\') {
                char e=s.charAt(i++);
                switch(e){case '"':b.append('"');break;case '\\':b.append('\\');break;case '/':b.append('/');break;
                    case 'b':b.append('\b');break;case 'f':b.append('\f');break;case 'n':b.append('\n');break;
                    case 'r':b.append('\r');break;case 't':b.append('\t');break;
                    case 'u':b.append((char)Integer.parseInt(s.substring(i,i+4),16));i+=4;break;
                    default: throw err("bad escape");}
            } else b.append(c);
        }
    }
    private Object number() {
        int start=i;
        while(i<s.length() && "+-0123456789.eE".indexOf(s.charAt(i))>=0)i++;
        String t=s.substring(start,i);
        if(t.isEmpty())throw err("unexpected character");
        if(t.contains(".")||t.contains("e")||t.contains("E"))return Double.parseDouble(t);
        return Long.parseLong(t);
    }
    private void lit(String w){ if(!s.startsWith(w,i))throw err("expected "+w); i+=w.length(); }
    private boolean peek(char c){return i<s.length() && s.charAt(i)==c;}
    private void expect(char c){ if(!peek(c))throw err("expected '"+c+"'"); i++; }
    private void ws(){ while(i<s.length() && Character.isWhitespace(s.charAt(i)))i++; }
    private IllegalArgumentException err(String m){return new IllegalArgumentException("JSON "+m+" at "+i);}
}
