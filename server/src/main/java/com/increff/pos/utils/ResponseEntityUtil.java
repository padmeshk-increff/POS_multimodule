package com.increff.pos.utils;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class ResponseEntityUtil {

    public static ResponseEntity<byte[]> buildPdfResponse(byte[] fileBytes, String fileName) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_PDF);

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(fileName)
                .build();
        headers.setContentDisposition(contentDisposition);

        headers.setContentLength(fileBytes.length);

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }

    public static ResponseEntity<byte[]> buildTsvResponse(byte[] fileBytes, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/tab-separated-values"));

        // 1. Build a "attachment" ContentDisposition
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(fileName)
                .build();

        // 2. Set it on the headers
        headers.setContentDisposition(contentDisposition);

        headers.setContentLength(fileBytes.length);

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }
}

