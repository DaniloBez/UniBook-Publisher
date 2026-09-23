package com.unibook.publisher.production.service;

import org.springframework.stereotype.Service;

@Service
public class DummyFileStorageService implements  FileStorageService {
    @Override
    public String readTextContent(String fileUrl) {
        return "Reading file... " + fileUrl;
    }
}
