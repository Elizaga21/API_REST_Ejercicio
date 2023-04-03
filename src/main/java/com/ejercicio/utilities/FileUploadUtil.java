package com.ejercicio.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.apache.commons.lang3.RandomStringUtils;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileUploadUtil {

    public String saveFile(String fileName, MultipartFile multipartFile)
            throws IOException {
        Path uploadPath = Paths.get("Files-Upload");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath); //se crea el archivo en una carpeta en el directorio del proyecto
        }

        String fileCode = RandomStringUtils.randomAlphanumeric(8); //Una vez creado el arhivo, se añade 8 caracteres aleatorios para identificarlo.

        //TRY WITH RESOURCES, cuando el try lleva parametros
        //Los recursos que se pueden manejar son los que implementan la interfaz closeable.
        try (InputStream inputStream = multipartFile.getInputStream()) { //Se utiliza para cerrar el archivo
            Path filePath = uploadPath.resolve(fileCode + "-" + fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new IOException("Could not save file: " + fileName, ioe);
        }

        return fileCode;
    }
}
