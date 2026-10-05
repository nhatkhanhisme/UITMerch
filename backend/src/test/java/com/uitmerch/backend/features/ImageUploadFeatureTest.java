package com.uitmerch.backend.features;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.common.security.SafeImageFile;
import com.uitmerch.backend.common.service.StorageService;
import com.uitmerch.backend.common.util.FileUploadResponse;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.io.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class ImageUploadFeatureTest extends BackendFeatureTest {
    @MockBean StorageService storage;
    private byte[] png() throws Exception {
        var output=new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",output);
        return output.toByteArray();
    }
    @Test void uploadsRequireAuthenticationRoleAndOrganizationOwnership() throws Exception {
        reset(storage);var org=organization();var owner=users.findById(org.getOwnerId()).orElseThrow();
        var file=new MockMultipartFile("file","photo.png","image/png",png());String route="/api/v1/uploads/organizations/"+org.getId()+"/logo";
        mvc.perform(multipart("/api/v1/uploads/avatar").file(file)).andExpect(status().isUnauthorized());
        mvc.perform(multipart(route).file(file).header("Authorization","Bearer "+token(user(UserRole.CUSTOMER)))).andExpect(status().isForbidden());
        mvc.perform(multipart(route).file(file).header("Authorization","Bearer "+token(user(UserRole.ORGANIZER)))).andExpect(status().isNotFound());
        verifyNoInteractions(storage);
        when(storage.uploadFile(any(),eq("org-assets"),eq("organizations/"+org.getId()+"/logo")))
            .thenReturn(FileUploadResponse.builder().fileUrl("https://storage.example.test/image.png").build());
        mvc.perform(multipart(route).file(file).header("Authorization","Bearer "+token(owner))).andExpect(status().isOk());
        verify(storage).uploadFile(isA(SafeImageFile.class),eq("org-assets"),eq("organizations/"+org.getId()+"/logo"));
    }
    @Test void spoofedMimeSvgAndOversizedInputNeverReachStorageAndPolyglotsAreReencoded() throws Exception {
        reset(storage);var customer=user(UserRole.CUSTOMER);String access=token(customer);
        for(var file:new MockMultipartFile[]{new MockMultipartFile("file","image.png","image/png","<script>alert(1)</script>".getBytes()),
            new MockMultipartFile("file","image.svg","image/svg+xml","<svg/>".getBytes()),
            new MockMultipartFile("file","huge.png","image/png",new byte[10*1024*1024+1])})
            mvc.perform(multipart("/api/v1/uploads/avatar").file(file).header("Authorization","Bearer "+access)).andExpect(status().isBadRequest());
        verifyNoInteractions(storage);
        var source=new ByteArrayOutputStream();source.write(png());source.write("<script>injected</script>".getBytes());
        when(storage.uploadFile(any(),eq("avatars"),eq(customer.getId().toString()))).thenAnswer(call->{
            SafeImageFile image=call.getArgument(0);assertThat(new String(image.getBytes(),java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("<script>");
            assertThat(image.getContentType()).isEqualTo("image/png");
            return FileUploadResponse.builder().fileUrl("https://storage.example.test/image.png").build();
        });
        mvc.perform(multipart("/api/v1/uploads/avatar").file(new MockMultipartFile("file","../../image.html","image/png",source.toByteArray()))
            .header("Authorization","Bearer "+access)).andExpect(status().isOk());
    }
}
