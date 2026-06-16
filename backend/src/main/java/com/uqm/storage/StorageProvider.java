package com.uqm.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageProvider {

    void store(MultipartFile file, String storedKey);

    Resource load(String storedKey);

    boolean exists(String storedKey);
}
