package com.uitmerch.backend.common.security;

import com.uitmerch.backend.common.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;

/** Decode within fixed pixel bounds and re-encode without caller-controlled metadata or trailing content. */
public final class ImageUploadPolicy {
    private static final int MAX_BYTES=10*1024*1024;
    public record Image(byte[] bytes,String mime,String extension) {}
    public static Image validate(MultipartFile file) {
        if(file==null || file.isEmpty() || file.getSize()>MAX_BYTES) throw invalid();
        String declared=file.getContentType();
        if(!Set.of("image/jpeg","image/png","image/gif","image/webp").contains(declared==null?"":declared.toLowerCase(Locale.ROOT))) throw invalid();
        try(var input=file.getInputStream()) {
            byte[] source=input.readNBytes(MAX_BYTES+1);
            if(source.length>MAX_BYTES) throw invalid();
            String actual=signature(source);
            if(!actual.equalsIgnoreCase(declared)) throw invalid();
            try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
                var readers=ImageIO.getImageReaders(stream);
                if(!readers.hasNext()) throw invalid();
                var reader=readers.next();
                try {
                    reader.setInput(stream,true,true);
                    int width=reader.getWidth(0),height=reader.getHeight(0);
                    if(width<1 || height<1 || width>4096 || height>4096 || (long)width*height>16_000_000) throw invalid();
                    var image=reader.read(0);
                    var output=new ByteArrayOutputStream();
                    String format=actual.equals("image/jpeg")?"jpeg":"png";
                    if(!ImageIO.write(image,format,output) || output.size()>MAX_BYTES) throw invalid();
                    return new Image(output.toByteArray(),"image/"+format,format.equals("jpeg")?"jpg":"png");
                } finally { reader.dispose(); }
            }
        } catch(IOException | IllegalArgumentException ex) { throw invalid(); }
    }
    private static String signature(byte[] b) {
        if(b.length>=8 && b[0]==(byte)0x89 && b[1]=='P' && b[2]=='N' && b[3]=='G' && b[4]==13 && b[5]==10 && b[6]==26 && b[7]==10) return "image/png";
        if(b.length>=3 && b[0]==(byte)0xff && b[1]==(byte)0xd8 && b[2]==(byte)0xff) return "image/jpeg";
        if(b.length>=6 && new String(b,0,6,java.nio.charset.StandardCharsets.US_ASCII).matches("GIF8[79]a")) return "image/gif";
        if(b.length>=12 && new String(b,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") && new String(b,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        throw invalid();
    }
    private static ValidationException invalid() { return new ValidationException("Upload a valid JPEG, PNG, GIF or WebP image of at most 10MB and 4096 pixels per side. SVG is not supported."); }
}
