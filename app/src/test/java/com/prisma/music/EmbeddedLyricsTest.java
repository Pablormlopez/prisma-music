package com.prisma.music;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public class EmbeddedLyricsTest {
 private byte[] cat(byte[]... parts)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();for(byte[] p:parts)b.write(p);return b.toByteArray();}
 private byte[] text(String s){return s.getBytes(StandardCharsets.UTF_8);}
 private byte[] int32(int n)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();new DataOutputStream(b).writeInt(n);return b.toByteArray();}
 private byte[] little(int n)throws Exception{return int32(Integer.reverseBytes(n));}
 private byte[] id3(String type,byte[] body)throws Exception{byte[] frame=cat(text(type),int32(body.length),new byte[2],body);int n=frame.length;return cat(new byte[]{'I','D','3',3,0,0,(byte)(n>>21&127),(byte)(n>>14&127),(byte)(n>>7&127),(byte)(n&127)},frame);}
 private byte[] box(String name,byte[] data)throws Exception{return cat(int32(data.length+8),name.getBytes(StandardCharsets.ISO_8859_1),data);}
 @Test public void mp3Utf8AndUtf16Lyrics()throws Exception{assertEquals("Hola canción",EmbeddedLyrics.read(new ByteArrayInputStream(id3("USLT",cat(new byte[]{3,'e','s','p',0},text("Hola canción"))))));byte[] body=cat(new byte[]{1,'e','s','p',(byte)255,(byte)254,0,0},"Letra en español".getBytes(StandardCharsets.UTF_16));assertEquals("Letra en español",EmbeddedLyrics.read(new ByteArrayInputStream(id3("USLT",body))));}
 @Test public void synchronizedMilliseconds()throws Exception{byte[] body=cat(new byte[]{3,'e','s','p',2,1,0},text("Hola"),new byte[]{0},int32(1250));assertEquals("[00:01.250]Hola",EmbeddedLyrics.read(new ByteArrayInputStream(id3("SYLT",body))));}
 @Test public void flacVorbisComments()throws Exception{byte[] value=text("LYRICS=[00:01]Texto incrustado"),comments=cat(little(0),little(1),little(value.length),value);byte[] h={(byte)132,(byte)(comments.length>>16),(byte)(comments.length>>8),(byte)comments.length};assertEquals("[00:01]Texto incrustado",EmbeddedLyrics.read(new ByteArrayInputStream(cat(text("fLaC"),h,comments))));}
 @Test public void m4aItunesLyricsSkipsAudio()throws Exception{byte[] data=box("data",cat(int32(1),int32(0),text("Letra M4A")));byte[] file=cat(box("ftyp",text("M4A     ")),box("mdat",new byte[12]),box("moov",box("udta",box("meta",cat(new byte[4],box("ilst",box("\u00a9lyr",data)))))));assertEquals("Letra M4A",EmbeddedLyrics.read(new ByteArrayInputStream(file)));}
 @Test public void malformedAndOversizedTagsReturnEmpty()throws Exception{assertEquals("",EmbeddedLyrics.read(new ByteArrayInputStream(new byte[2])));assertEquals("",EmbeddedLyrics.read(new ByteArrayInputStream(new byte[]{'I','D','3',3,0,0,127,127,127,127,0,0})));byte[] truncated=id3("USLT",new byte[]{3,'e','n','g',0});assertEquals("",EmbeddedLyrics.read(new ByteArrayInputStream(truncated)));}
}
