package com.premisave.property.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtils {

    public static Pageable createPageable(int page, int size, String sortBy) {
        if (size > Constants.MAX_PAGE_SIZE) {
            size = Constants.MAX_PAGE_SIZE;
        }
        if (page < 0) page = 0;

        return PageRequest.of(page, size, Sort.by(sortBy).descending());
    }

    private PaginationUtils() {}
}