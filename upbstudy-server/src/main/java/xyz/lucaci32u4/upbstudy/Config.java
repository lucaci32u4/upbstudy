package xyz.lucaci32u4.upbstudy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
public record Config(@NotBlank(message = "API host must not be blank") String apiHost,
                     @Positive(message = "API port must be a positive integer") int apiPort,
                     @NotBlank(message = "API key must not be blank") String apiKey,
                     @NotBlank(message = "SMTP host must not be blank") String smtpHost,
                     @Positive(message = "SMTP port must be a positive integer") int smtpPort,
                     @NotBlank(message = "SMTP email must not be blank") String smtpEmail,
                     @NotBlank(message = "SMTP password must not be blank") String smtpPassword,
                     @NotBlank(message = "Email destination must not be blank") String emailDestination,
                     @NotBlank(message = "Email subject must not be blank") String emailSubject,
                     @NotNull(message = "Email body cannot be null") String emailBody,
                     @NotBlank(message = "Email attachment name must not be blank")
                        @Pattern(message = "Email attachment must end in .docx", regexp = ".+\\.docx") String attachmentName,
                     @NotBlank(message = "Reservation name must not be null") String reservationName,
                     @NotBlank(message = "Reservation faculty must not be blank") String reservationFaculty,
                     @NotBlank(message = "Reservation permit number must not be blank") String reservationPermitNo) {

    private static final String TEMPLATE_IDNAME = "TEMPLATE_IDNAME";
    private static final String TEMPLATE_FACULTY = "TEMPLATE_FACULTY";
    private static final String TEMPLATE_IDNUM = "TEMPLATE_IDNUM";
    private static final String TEMPLATE_DAYHOUR = "TEMPLATE_DAYHOUR";
    private static final String TARGET_REPLACE_FILE = "word/document.xml";

    private byte[] findReplaceContent(byte[] input, String dayhourContent) {
        String source = new String(input);
        Map<String, String> replaces = Map.of(
                TEMPLATE_IDNAME, reservationName,
                TEMPLATE_FACULTY, reservationFaculty,
                TEMPLATE_IDNUM, reservationPermitNo,
                TEMPLATE_DAYHOUR, dayhourContent
        );
        for (var entry : replaces.entrySet()) {
            source = source.replaceAll(entry.getKey(), entry.getValue());
        }
        return source.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] assembleReservationFile(String dayhourContent) {
        try {
            InputStream iis = this.getClass().getResourceAsStream("/form.docx");
            ZipInputStream zis = new ZipInputStream(iis);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            ZipEntry entryIn;
            while ((entryIn = zis.getNextEntry()) != null) {
                if (!entryIn.getName().equalsIgnoreCase(TARGET_REPLACE_FILE)) {
                    zos.putNextEntry(entryIn);
                    zos.write(zis.readAllBytes());
                } else {
                    zos.putNextEntry(new ZipEntry(TARGET_REPLACE_FILE));
                    zos.write(findReplaceContent(zis.readAllBytes(), dayhourContent));
                }
                zos.closeEntry();
            }
            zis.close();
            iis.close();
            zos.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to create form", e);
        }
        return null;
    }

}
