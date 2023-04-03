package com.ejercicio.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ejercicio.entities.Cliente;
import com.ejercicio.model.FileUploadResponse;
import com.ejercicio.services.ClienteService;
import com.ejercicio.utilities.FileDownloadUtil;
import com.ejercicio.utilities.FileUploadUtil;

import jakarta.validation.Valid;

@RestController // Devuelve un JSON
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    @Autowired
    private FileDownloadUtil fileDownloadUtil;

    @GetMapping
    public ResponseEntity<List<Cliente>> findAll(@RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {

        ResponseEntity<List<Cliente>> responseEntity = null;
        List<Cliente> clientes = new ArrayList<>();

        Sort sortByNombre = Sort.by("nombre");

        if (page != null && size != null) {

            // Con paginacion y ordenamiento
            try {
                Pageable pageable = PageRequest.of(page, size, sortByNombre);
                Page<Cliente> clientesPaginados = clienteService.findAll(pageable);
                clientes = clientesPaginados.getContent(); // Saca el contenido de productos
                responseEntity = new ResponseEntity<List<Cliente>>(clientes, HttpStatus.OK);

            } catch (Exception e) {
                responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

            }

        } else {
            // Sin paginacion pero con ordenamiento
            try {
                clientes = clienteService.findAll(sortByNombre);
                responseEntity = new ResponseEntity<List<Cliente>>(clientes, HttpStatus.OK);
            } catch (Exception e) {
                responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);

            }
        }

        return responseEntity;

    }

     /**
     * Recupera un CLIENTE por el id.
     * Va a responder a una peticion del tipo, por ejemplo:
     * http://localhost:8080/clientes/2
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable(name = "id") Integer id) {

        ResponseEntity<Map<String, Object>> responseEntity = null;
        Map<String, Object> responseAsMap = new HashMap<>(); // Para mandar un status + un mensaje se debe crear un mapa

        Map<String, Object> responseAsError = new HashMap<>();

        try {
            Cliente cliente = clienteService.findById(id);

            if (cliente != null) {
                String successMessage = "Se ha encontrado el cliente con id: " + id;
                responseAsMap.put("mensaje", successMessage);
                responseAsMap.put("cliente", cliente);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.OK);

            } else {
                String errorMessage = "No se ha encontrado el cliente con id:";
                responseAsMap.put("error", errorMessage);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            String errorGrave = "Error grave";
            responseAsMap.put("error", errorGrave);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;

    }

    @PostMapping(consumes = "multipart/form-data") //El consumes es para añadir imagenes y otro tipo de documentos - Viene dentro de la petición, no viene parámetros con el POST
    @Transactional
    public ResponseEntity<Map<String, Object>> insert(@Valid 
    @RequestPart(name = "cliente") Cliente cliente,
    BindingResult result,
    @RequestPart(name = "file") MultipartFile file) throws IOException { // En el cuepo de la peticion va el objeto
                                                                                                                    
        Map<String, Object> responseAsMap = new HashMap<>();

        ResponseEntity<Map<String, Object>> responseEntity = null;

        /**
         * Primero: Comprobar si hay errores en el producto recibido - VALIDACION
         */

        if (result.hasErrors()) {
            List<String> errorMessage = new ArrayList<>();

            for (ObjectError error : result.getAllErrors()) {
                errorMessage.add(error.getDefaultMessage()); // Muestras los mensajes de la Entity Producto

            }
            responseAsMap.put("errores", errorMessage);

            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.BAD_REQUEST);
            return responseEntity;
        }

        // //Si no hay errores persistimos el producto, comprobando previamente si nos han enviado un archivo o imagen
        if (!file.isEmpty()) {
            String fileCode = fileUploadUtil.saveFile(file.getOriginalFilename(), file);
            cliente.setImagenProducto(fileCode + "-" + file.getOriginalFilename());

            //Devolver respecto al file recibido

            FileUploadResponse fileUploadResponse = FileUploadResponse
            .builder()
            .fileName(fileCode + "-" + file.getOriginalFilename())
            .downLoadURI("/productos/downloadFile/" + fileCode + "-" + file.getOriginalFilename())
            .size(file.getSize())
            .build();

            responseAsMap.put("info de la imagen:", fileUploadResponse);
        }

        Cliente clienteDataBase = clienteService.save(cliente);
        try {
            if (clienteDataBase != null) {
                String mensaje = "El cliente se ha creado correctamente";
                responseAsMap.put("mensaje", mensaje);
                responseAsMap.put("cliente", clienteDataBase);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.CREATED);
            } else {
                // No se ha creado el producto
                String mensaje2 = "El cliente no se ha creado";
                responseAsMap.put("mensaje", mensaje2);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap,
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (DataAccessException e) {
            String errorGrave = "Ha tenido lugar un error grave" + "la causa puede ser "
                    + e.getMostSpecificCause(); // especifica la causa especifica del error.
            responseAsMap.put("errorGrave", errorGrave);

            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Si no hay errores se ejecuta este return, se persiste el producto
        return responseEntity;
    }
    
    /**
     * Método para actualizar un cliente
     */

     @PutMapping("/{id}") // Modificar
     @Transactional
     public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Cliente cliente, BindingResult result,
             @PathVariable(name = "id") Integer id) { // En el cuerpo de la peticion va un objeto
 
         Map<String, Object> responseAsMap = new HashMap<>();
 
         ResponseEntity<Map<String, Object>> responseEntity = null;
 
         /**
          * Primero: Comprobar si hay errores en el cliente recibido - VALIDACION
          */
 
         if (result.hasErrors()) {
             List<String> errorMessage = new ArrayList<>();
 
             for (ObjectError error : result.getAllErrors()) {
                 errorMessage.add(error.getDefaultMessage()); // Muestras los mensajes de la Entity Producto
 
             }
             responseAsMap.put("errores", errorMessage);
 
             responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.BAD_REQUEST);
             return responseEntity;
         }
 
         // Si no hay errores, persistimos el cliente
         // Vinculando previamente el id que se recibe con el cliente
 
         cliente.setId(id); // En el JSON, con el save se modifica ese elemento
         Cliente clienteDataBase = clienteService.save(cliente);
 
         try {
 
             if (clienteDataBase != null) {
                 String mensaje = "El cliente se ha actualizado correctamente";
                 responseAsMap.put("mensaje", mensaje);
                 responseAsMap.put("cliente", clienteDataBase);
                 responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.OK);
             } else {
                 // No se ha actualizado el producto
                 String mensaje2 = "El cliente no se ha actualizado";
                 responseAsMap.put("mensaje", mensaje2);
                 responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap,
                         HttpStatus.INTERNAL_SERVER_ERROR);
             }
         } catch (DataAccessException e) {
             String errorGrave = "Ha tenido lugar un error grave" + "la causa puede ser "
                     + e.getMostSpecificCause(); // especifica la causa especifica del error.
             responseAsMap.put("errorGrave", errorGrave);
 
             responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
         }
 
         // Si no hay errores se ejecuta este return, se actualiza el producto
         //La presentación no se guarda porque no hay capas de Service de
         return responseEntity;
     }

     /**
     * Método de eliminar producto
     */

     @DeleteMapping("/{id}") // Modificar
     @Transactional
     public ResponseEntity<Map<String, Object>> delete (@Valid @RequestBody Cliente cliente, BindingResult result,
             @PathVariable(name = "id") Integer id) { // En el cuerpo de la peticion va un objeto
 
         Map<String, Object> responseAsMap = new HashMap<>();
 
         ResponseEntity<Map<String, Object>> responseEntity = null;
 
         /**
          * Primero: Comprobar si hay errores en el cliente recibido - VALIDACION
          */

 
         if (result.hasErrors()) {
             List<String> errorMessage = new ArrayList<>();
 
             for (ObjectError error : result.getAllErrors()) {
                 errorMessage.add(error.getDefaultMessage()); // Muestras los mensajes de la Entity Producto
 
             }
             responseAsMap.put("errores", errorMessage);
 
             responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.BAD_REQUEST);
             return responseEntity;
         }
 
         // Si no hay errores, eliminamos el producto
         // Vinculando previamente el id que se recibe con el producto
 
         //producto.setId(id); // En el JSON, con el save se modifica ese elemento

         Cliente clienteDataBase = clienteService.findById(id);
 
         try {
 
             if (clienteDataBase != null) {
                 String mensaje = "El cliente se ha eliminado correctamente";
                 clienteService.delete(clienteDataBase);
                 responseAsMap.put("mensaje", mensaje);
                 responseAsMap.put("cliente", clienteDataBase);
                 responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.OK);
             } else {
                 // No se ha actualizado el producto
                 String mensaje2 = "El cliente no se ha borrado";
                 responseAsMap.put("mensaje", mensaje2);
                 responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap,
                         HttpStatus.INTERNAL_SERVER_ERROR);
             }
         } catch (DataAccessException e) {
             String errorGrave = "Ha tenido lugar un error grave" + "la causa puede ser "
                     + e.getMostSpecificCause(); // especifica la causa especifica del error.
             responseAsMap.put("errorGrave", errorGrave);
 
             responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
         }
 
         // Si no hay errores se ejecuta este return, se actualiza el producto
         //La presentación no se guarda porque no hay capas de Service de
         return responseEntity;
     }


       /**
     *  Implementa filedownnload end point API 
     **/    
    @GetMapping("/downloadFile/{fileCode}")
    public ResponseEntity<?> downloadFile(@PathVariable(name = "fileCode") String fileCode) {

        Resource resource = null;

        try {
            resource = fileDownloadUtil.getFileAsResource(fileCode);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        if (resource == null) {
            return new ResponseEntity<>("File not found ", HttpStatus.NOT_FOUND);
        }

        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

        return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
        .body(resource);

    }

}
