package com.uitmerch.backend.common.security;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
/** Validated image, with a canonical filename and MIME type. */
public record SafeImageFile(ImageUploadPolicy.Image image) implements MultipartFile {
    public String getName(){return "file";}
    public String getOriginalFilename(){return "image."+image.extension();}
    public String getContentType(){return image.mime();}
    public boolean isEmpty(){return image.bytes().length==0;}
    public long getSize(){return image.bytes().length;}
    public byte[] getBytes(){return image.bytes();}
    public InputStream getInputStream(){return new ByteArrayInputStream(image.bytes());}
    public void transferTo(File target)throws IOException{java.nio.file.Files.write(target.toPath(),image.bytes());}
}
